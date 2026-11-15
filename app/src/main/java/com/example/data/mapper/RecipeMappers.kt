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
            val mapped = extractIngredientPairs().map { (name, measure) ->
                IngredientLocalModel(
                    name = name,
                    measure = measure,
                    isChecked = checkedLookup[name.lowercase()] ?: false
                )
            }
            encodeIngredients(mapped)
        } else {
            existing?.ingredientsJson ?: "[]"
        }

        val resolvedTagsCsv = if (!strTags.isNullOrBlank()) {
            strTags
        } else {
            existing?.tagsCsv.orEmpty()
        }

        return RecipeEntity(
            id = idMeal,
            name = strMeal.trim(),
            category = strCategory?.takeIf { it.isNotBlank() }
                ?: existing?.category?.takeIf { it.isNotBlank() }
                ?: fallbackCategory.orEmpty(),
            area = strArea?.takeIf { it.isNotBlank() } ?: existing?.area.orEmpty(),
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
        name = strCategory.trim(),
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
