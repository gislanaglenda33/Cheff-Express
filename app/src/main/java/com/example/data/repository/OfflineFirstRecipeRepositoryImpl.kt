package com.example.data.repository

import com.example.data.local.CategoryDao
import com.example.data.local.InitialRecipeSeed
import com.example.data.local.RecipeDao
import com.example.data.local.RecipeEntity
import com.example.data.mapper.CulinaryPtBrLocalizer
import com.example.data.mapper.NoOpRecipeTextTranslator
import com.example.data.mapper.RecipeMappers
import com.example.data.mapper.RecipeMappers.toDomain
import com.example.data.mapper.RecipeMappers.toEntity
import com.example.data.mapper.RecipeTextTranslator
import com.example.data.remote.MealDto
import com.example.data.remote.TheMealDbApi
import com.example.domain.model.AppError
import com.example.domain.model.Category
import com.example.domain.model.Recipe
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.NetworkMonitor
import com.example.domain.repository.RecipeRepository
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class OfflineFirstRecipeRepositoryImpl(
    private val api: TheMealDbApi,
    private val recipeDao: RecipeDao,
    private val categoryDao: CategoryDao,
    private val networkMonitor: NetworkMonitor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val textTranslator: RecipeTextTranslator = NoOpRecipeTextTranslator
) : RecipeRepository {

    override fun observeRecipes(query: String, category: String?): Flow<List<Recipe>> =
        recipeDao.observeRecipes(query, category).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeFavoriteRecipes(): Flow<List<Recipe>> =
        recipeDao.observeFavoriteRecipes().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeCategories().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeRecipeById(recipeId: String): Flow<Recipe?> =
        recipeDao.observeRecipeById(recipeId).map { entity ->
            entity?.toDomain()
        }

    override suspend fun syncInitialCatalog(forceRefresh: Boolean): SyncOutcome =
        withContext(ioDispatcher) {
            val localCount = recipeDao.getRecipeCount()
            if (!networkMonitor.isOnline.value) {
                seedIfEmpty()
                return@withContext SyncOutcome.OfflineFallback
            }
            if (!forceRefresh && localCount >= 10 && categoryDao.getCategoryCount() > 0) {
                return@withContext SyncOutcome.Success(localCount)
            }

            try {
                val categoriesDto = api.getCategories().categories.orEmpty()
                if (categoriesDto.isNotEmpty()) {
                    val rawDescriptions = categoriesDto.map { it.strCategoryDescription.orEmpty().trim() }
                    val translatedDescriptions = textTranslator.translateBatchEnToPt(rawDescriptions)
                    val categoryEntities = categoriesDto.mapIndexed { idx, dto ->
                        dto.toEntity(translatedDescription = translatedDescriptions.getOrNull(idx))
                    }
                    categoryDao.upsertCategories(categoryEntities)
                }

                val mealsDto = api.searchMeals("").meals.orEmpty()
                val now = System.currentTimeMillis()
                val existingMap = if (mealsDto.isNotEmpty()) {
                    recipeDao.getRecipesByIds(mealsDto.map { it.idMeal }).associateBy { it.id }
                } else {
                    emptyMap()
                }
                val entities = localizeAndMapMeals(
                    mealsDto = mealsDto,
                    existingMap = existingMap,
                    fallbackCategory = null,
                    nowMillis = now
                )
                if (entities.isNotEmpty()) {
                    recipeDao.upsertRecipes(entities)
                } else {
                    seedIfEmpty()
                }
                SyncOutcome.Success(entities.size)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                seedIfEmpty()
                if (recipeDao.getRecipeCount() > 0 && !forceRefresh) {
                    SyncOutcome.OfflineFallback
                } else {
                    SyncOutcome.Failure(mapException(e))
                }
            }
        }

    override suspend fun searchAndCacheRecipes(query: String): SyncOutcome =
        withContext(ioDispatcher) {
            if (!networkMonitor.isOnline.value) {
                return@withContext SyncOutcome.OfflineFallback
            }
            try {
                val trimmedQuery = query.trim()
                val apiQuery = textTranslator.translatePtToEn(trimmedQuery)
                var mealsDto = api.searchMeals(apiQuery).meals.orEmpty()
                if (mealsDto.isEmpty() && !apiQuery.equals(trimmedQuery, ignoreCase = true)) {
                    mealsDto = api.searchMeals(trimmedQuery).meals.orEmpty()
                }
                val now = System.currentTimeMillis()
                val existingMap = if (mealsDto.isNotEmpty()) {
                    recipeDao.getRecipesByIds(mealsDto.map { it.idMeal }).associateBy { it.id }
                } else {
                    emptyMap()
                }
                val entities = localizeAndMapMeals(
                    mealsDto = mealsDto,
                    existingMap = existingMap,
                    fallbackCategory = null,
                    nowMillis = now
                )
                if (entities.isNotEmpty()) {
                    recipeDao.upsertRecipes(entities)
                }
                SyncOutcome.Success(entities.size)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                SyncOutcome.Failure(mapException(e))
            }
        }

    override suspend fun syncCategoryRecipes(category: String): SyncOutcome =
        withContext(ioDispatcher) {
            if (!networkMonitor.isOnline.value) {
                return@withContext SyncOutcome.OfflineFallback
            }
            try {
                val apiCategory = CulinaryPtBrLocalizer.toApiCategoryKey(category)
                val localizedCategory = CulinaryPtBrLocalizer.translateCategory(category)
                val mealsDto = api.filterByCategory(apiCategory).meals.orEmpty()
                val now = System.currentTimeMillis()
                val existingMap = if (mealsDto.isNotEmpty()) {
                    recipeDao.getRecipesByIds(mealsDto.map { it.idMeal }).associateBy { it.id }
                } else {
                    emptyMap()
                }
                val entities = localizeAndMapMeals(
                    mealsDto = mealsDto,
                    existingMap = existingMap,
                    fallbackCategory = localizedCategory,
                    nowMillis = now
                )
                if (entities.isNotEmpty()) {
                    recipeDao.upsertRecipes(entities)
                }
                SyncOutcome.Success(entities.size)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                SyncOutcome.Failure(mapException(e))
            }
        }

    override suspend fun ensureRecipeDetailLoaded(recipeId: String): SyncOutcome =
        withContext(ioDispatcher) {
            val local = recipeDao.getRecipeById(recipeId)
            if (local != null && local.isDetailLoaded && local.instructions.isNotBlank()) {
                return@withContext SyncOutcome.Success(1)
            }
            if (!networkMonitor.isOnline.value) {
                return@withContext if (local != null) {
                    SyncOutcome.OfflineFallback
                } else {
                    SyncOutcome.Failure(AppError.NetworkUnavailable)
                }
            }
            try {
                val remoteMeal = api.getMealById(recipeId).meals.orEmpty().firstOrNull()
                if (remoteMeal != null) {
                    val existingMap = if (local != null) mapOf(local.id to local) else emptyMap()
                    val entity = localizeAndMapMeals(
                        mealsDto = listOf(remoteMeal),
                        existingMap = existingMap,
                        fallbackCategory = null,
                        nowMillis = System.currentTimeMillis()
                    ).first()
                    recipeDao.upsertRecipe(entity)
                    SyncOutcome.Success(1)
                } else {
                    SyncOutcome.Failure(AppError.NotFound)
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                if (local != null) {
                    SyncOutcome.OfflineFallback
                } else {
                    SyncOutcome.Failure(mapException(e))
                }
            }
        }

    override suspend fun toggleFavorite(recipeId: String) = withContext(ioDispatcher) {
        val current = recipeDao.getRecipeById(recipeId) ?: return@withContext
        recipeDao.updateFavorite(recipeId, !current.isFavorite)
    }

    override suspend fun toggleIngredientChecked(recipeId: String, ingredientIndex: Int) =
        withContext(ioDispatcher) {
            val current = recipeDao.getRecipeById(recipeId) ?: return@withContext
            val list = RecipeMappers.decodeIngredients(current.ingredientsJson).toMutableList()
            if (ingredientIndex in list.indices) {
                val item = list[ingredientIndex]
                list[ingredientIndex] = item.copy(isChecked = !item.isChecked)
                recipeDao.updateIngredients(recipeId, RecipeMappers.encodeIngredients(list))
            }
        }

    override suspend fun getRandomRecipeId(): String? = withContext(ioDispatcher) {
        if (networkMonitor.isOnline.value) {
            try {
                val meal = api.getRandomMeal().meals.orEmpty().firstOrNull()
                if (meal != null) {
                    val existing = recipeDao.getRecipeById(meal.idMeal)
                    val existingMap = if (existing != null) mapOf(existing.id to existing) else emptyMap()
                    val entity = localizeAndMapMeals(
                        mealsDto = listOf(meal),
                        existingMap = existingMap,
                        fallbackCategory = null,
                        nowMillis = System.currentTimeMillis()
                    ).first()
                    recipeDao.upsertRecipe(entity)
                    return@withContext meal.idMeal
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                // Fall back to local cache seamlessly
            }
        }
        recipeDao.getRandomLocalRecipeId()
    }

    private suspend fun localizeAndMapMeals(
        mealsDto: List<MealDto>,
        existingMap: Map<String, RecipeEntity>,
        fallbackCategory: String?,
        nowMillis: Long
    ): List<RecipeEntity> {
        if (mealsDto.isEmpty()) return emptyList()

        val rawTitles = mealsDto.map { dto ->
            val existing = existingMap[dto.idMeal]
            if (existing != null && existing.isDetailLoaded && dto.strInstructions.isNullOrBlank()) {
                ""
            } else {
                dto.strMeal.trim()
            }
        }
        val translatedTitles = if (rawTitles.any { it.isNotBlank() }) {
            textTranslator.translateBatchEnToPt(rawTitles)
        } else {
            rawTitles
        }

        val unmappedIngredients = linkedSetOf<String>()
        val unmappedMeasures = linkedSetOf<String>()
        val unmappedTags = linkedSetOf<String>()

        mealsDto.forEach { dto ->
            if (!dto.strInstructions.isNullOrBlank()) {
                dto.extractIngredientPairs().forEach { (rawIng, rawMeasure) ->
                    val trimmedIng = rawIng.trim()
                    if (trimmedIng.isNotBlank() && !CulinaryPtBrLocalizer.hasIngredientTranslation(trimmedIng)) {
                        unmappedIngredients.add(trimmedIng)
                    }
                    val trimmedMeasure = rawMeasure.trim()
                    if (trimmedMeasure.isNotBlank()) {
                        val localMeasure = CulinaryPtBrLocalizer.translateMeasure(trimmedMeasure)
                        if (localMeasure == trimmedMeasure && needsRemoteMeasureTranslation(trimmedMeasure)) {
                            unmappedMeasures.add(trimmedMeasure)
                        }
                    }
                }
                dto.strTags.orEmpty().split(",").forEach { rawTag ->
                    val trimmedTag = rawTag.trim()
                    if (trimmedTag.isNotBlank() && !CulinaryPtBrLocalizer.hasTagTranslation(trimmedTag)) {
                        unmappedTags.add(trimmedTag)
                    }
                }
            }
        }

        val ingredientList = unmappedIngredients.toList()
        val measureList = unmappedMeasures.toList()
        val tagList = unmappedTags.toList()

        val extraIngredientsMap = if (ingredientList.isNotEmpty()) {
            val translated = textTranslator.translateBatchEnToPt(ingredientList)
            ingredientList.indices.associate { idx ->
                ingredientList[idx].lowercase() to (translated.getOrNull(idx) ?: ingredientList[idx])
            }
        } else {
            emptyMap()
        }

        val extraMeasuresMap = if (measureList.isNotEmpty()) {
            val translated = textTranslator.translateBatchEnToPt(measureList)
            measureList.indices.associate { idx ->
                measureList[idx].lowercase() to (translated.getOrNull(idx) ?: measureList[idx])
            }
        } else {
            emptyMap()
        }

        val extraTagsMap = if (tagList.isNotEmpty()) {
            val translated = textTranslator.translateBatchEnToPt(tagList)
            tagList.indices.associate { idx ->
                tagList[idx].lowercase() to (translated.getOrNull(idx) ?: tagList[idx])
            }
        } else {
            emptyMap()
        }

        val translatedInstructionsMap = mutableMapOf<String, String>()
        val dtosWithInstructions = mealsDto.filter { !it.strInstructions.isNullOrBlank() }
        if (dtosWithInstructions.isNotEmpty()) {
            coroutineScope {
                dtosWithInstructions.chunked(4).forEach { wave ->
                    val waveResults = wave.map { dto ->
                        async {
                            dto.idMeal to textTranslator.translateEnToPt(dto.strInstructions.orEmpty().trim())
                        }
                    }.awaitAll()
                    waveResults.forEach { (id, translated) ->
                        translatedInstructionsMap[id] = translated
                    }
                }
            }
        }

        return mealsDto.mapIndexed { index, dto ->
            val existing = existingMap[dto.idMeal]
            val resolvedTitle = if (existing != null && existing.isDetailLoaded && dto.strInstructions.isNullOrBlank()) {
                existing.name
            } else {
                translatedTitles.getOrNull(index)?.takeIf { it.isNotBlank() } ?: dto.strMeal.trim()
            }
            dto.toEntity(
                existing = existing,
                fallbackCategory = fallbackCategory,
                nowMillis = nowMillis,
                translatedName = resolvedTitle,
                translatedInstructions = translatedInstructionsMap[dto.idMeal],
                extraIngredientTranslations = extraIngredientsMap,
                extraMeasureTranslations = extraMeasuresMap,
                extraTagTranslations = extraTagsMap
            )
        }
    }

    private fun needsRemoteMeasureTranslation(measure: String): Boolean {
        val lower = measure.lowercase()
        if (lower in setOf("g", "kg", "ml", "l", "oz", "lb", "lbs", "cl")) return false
        return Regex("[a-zA-Z]{3,}").containsMatchIn(lower)
    }

    private suspend fun seedIfEmpty() {
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.upsertCategories(InitialRecipeSeed.categories)
        }
        if (recipeDao.getRecipeCount() == 0) {
            recipeDao.upsertRecipes(InitialRecipeSeed.recipes())
        }
    }

    private fun mapException(error: Throwable): AppError = when (error) {
        is UnknownHostException -> AppError.NetworkUnavailable
        is SocketTimeoutException -> AppError.Timeout
        is HttpException -> AppError.ServerError(error.code(), error.message())
        is IOException -> AppError.NetworkUnavailable
        else -> AppError.Unknown(error.localizedMessage)
    }
}
