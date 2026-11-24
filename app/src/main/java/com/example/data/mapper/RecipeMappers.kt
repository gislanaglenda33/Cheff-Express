package com.example.data.mapper

import com.example.data.local.CategoryEntity
import com.example.data.local.IngredientLocalModel
import com.example.data.local.RecipeEntity
import com.example.data.remote.CategoryDto
import com.example.data.remote.MealDto
import com.example.domain.model.Category
import com.example.domain.model.IngredientItem
import com.example.domain.model.Recipe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RecipeMappers {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeIngredients(items: List<IngredientLocalModel>): String =
        json.encodeToString(items)

    fun decodeIngredients(rawJson: String): List<IngredientLocalModel> {
        if (rawJson.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<IngredientLocalModel>>(rawJson)
        }.getOrDefault(emptyList())
    }

    fun MealDto.toEntity(
        existing: RecipeEntity? = null,
        fallbackCategory: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): RecipeEntity {
        val hasFullDetails = !strInstructions.isNullOrBlank()
        val existingIngredients = existing?.let { decodeIngredients(it.ingredientsJson) }.orEmpty()
        val checkedLookup = existingIngredients.associate { it.name.lowercase() to it.isChecked }

        val mergedIngredientsJson = if (hasFullDetails) {
            val mapped = extractIngredientPairs().map { (rawName, rawMeasure) ->
                val localizedName = CulinaryPtBrLocalizer.translateIngredient(rawName)
                val localizedMeasure = CulinaryPtBrLocalizer.translateMeasure(rawMeasure)
                val wasChecked = checkedLookup[localizedName.lowercase()]
                    ?: checkedLookup[rawName.lowercase()]
                    ?: false
                IngredientLocalModel(
                    name = localizedName,
                    measure = localizedMeasure,
                    isChecked = wasChecked
                )
            }
            encodeIngredients(mapped)
        } else {
            existing?.ingredientsJson ?: "[]"
        }

        val resolvedTagsCsv = if (!strTags.isNullOrBlank()) {
            strTags.split(",")
                .map { CulinaryPtBrLocalizer.translateTag(it) }
                .filter { it.isNotBlank() }
                .joinToString(",")
        } else {
            existing?.tagsCsv.orEmpty()
        }

        val resolvedCategory = strCategory?.takeIf { it.isNotBlank() }
            ?.let { CulinaryPtBrLocalizer.translateCategory(it) }
            ?: existing?.category?.takeIf { it.isNotBlank() }
            ?: fallbackCategory?.let { CulinaryPtBrLocalizer.translateCategory(it) }.orEmpty()

        val resolvedArea = strArea?.takeIf { it.isNotBlank() }
            ?.let { CulinaryPtBrLocalizer.translateArea(it) }
            ?: existing?.area.orEmpty()

        return RecipeEntity(
            id = idMeal,
            name = strMeal.trim(),
            category = resolvedCategory,
            area = resolvedArea,
            instructions = if (hasFullDetails) strInstructions.orEmpty().trim() else existing?.instructions.orEmpty(),
            thumbnailUrl = strMealThumb?.takeIf { it.isNotBlank() } ?: existing?.thumbnailUrl.orEmpty(),
            youtubeUrl = strYoutube?.takeIf { it.isNotBlank() } ?: existing?.youtubeUrl,
            sourceUrl = strSource?.takeIf { it.isNotBlank() } ?: existing?.sourceUrl,
            tagsCsv = resolvedTagsCsv,
            ingredientsJson = mergedIngredientsJson,
            isFavorite = existing?.isFavorite ?: false,
            isDetailLoaded = hasFullDetails || (existing?.isDetailLoaded == true),
            lastSyncedAt = nowMillis
        )
    }

    fun RecipeEntity.toDomain(): Recipe {
        val decodedIngredients = decodeIngredients(ingredientsJson).map {
            IngredientItem(
                name = it.name,
                measure = it.measure,
                isChecked = it.isChecked
            )
        }
        val parsedTags = tagsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return Recipe(
            id = id,
            name = name,
            category = category.ifBlank { "Geral" },
            area = area.ifBlank { "Internacional" },
            instructions = instructions,
            thumbnailUrl = thumbnailUrl,
            youtubeUrl = youtubeUrl,
            sourceUrl = sourceUrl,
            tags = parsedTags,
            ingredients = decodedIngredients,
            isFavorite = isFavorite,
            isDetailLoaded = isDetailLoaded,
            lastSyncedAt = lastSyncedAt
        )
    }

    fun CategoryDto.toEntity(): CategoryEntity = CategoryEntity(
        id = idCategory,
        name = CulinaryPtBrLocalizer.translateCategory(strCategory),
        thumbnailUrl = strCategoryThumb.orEmpty(),
        description = strCategoryDescription.orEmpty().trim()
    )

    fun CategoryEntity.toDomain(): Category = Category(
        id = id,
        name = name,
        thumbnailUrl = thumbnailUrl,
        description = description
    )
}
