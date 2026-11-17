package com.example.data.repository

import com.example.data.local.CategoryDao
import com.example.data.local.InitialRecipeSeed
import com.example.data.local.RecipeDao
import com.example.data.mapper.RecipeMappers
import com.example.data.mapper.RecipeMappers.toDomain
import com.example.data.mapper.RecipeMappers.toEntity
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class OfflineFirstRecipeRepositoryImpl(
    private val api: TheMealDbApi,
    private val recipeDao: RecipeDao,
    private val categoryDao: CategoryDao,
    private val networkMonitor: NetworkMonitor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
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
                    categoryDao.upsertCategories(categoriesDto.map { it.toEntity() })
                }

                val mealsDto = api.searchMeals("").meals.orEmpty()
                val now = System.currentTimeMillis()
                val existingMap = if (mealsDto.isNotEmpty()) {
                    recipeDao.getRecipesByIds(mealsDto.map { it.idMeal }).associateBy { it.id }
                } else {
                    emptyMap()
                }
                val entities = mealsDto.map { dto ->
                    dto.toEntity(existing = existingMap[dto.idMeal], nowMillis = now)
                }
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
                val mealsDto = api.searchMeals(query).meals.orEmpty()
                val now = System.currentTimeMillis()
                val existingMap = if (mealsDto.isNotEmpty()) {
                    recipeDao.getRecipesByIds(mealsDto.map { it.idMeal }).associateBy { it.id }
                } else {
                    emptyMap()
                }
                val entities = mealsDto.map { dto ->
                    dto.toEntity(existing = existingMap[dto.idMeal], nowMillis = now)
                }
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
                val mealsDto = api.filterByCategory(category).meals.orEmpty()
                val now = System.currentTimeMillis()
                val existingMap = if (mealsDto.isNotEmpty()) {
                    recipeDao.getRecipesByIds(mealsDto.map { it.idMeal }).associateBy { it.id }
                } else {
                    emptyMap()
                }
                val entities = mealsDto.map { dto ->
                    dto.toEntity(
                        existing = existingMap[dto.idMeal],
                        fallbackCategory = category,
                        nowMillis = now
                    )
                }
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
                    val entity = remoteMeal.toEntity(existing = local)
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
                    recipeDao.upsertRecipe(meal.toEntity(existing = existing))
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
