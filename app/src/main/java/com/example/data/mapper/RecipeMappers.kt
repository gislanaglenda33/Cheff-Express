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
        nowMillis: Long = System.currentTimeMillis(),
        translatedName: String? = null,
        translatedInstructions: String? = null,
        extraIngredientTranslations: Map<String, String> = emptyMap(),
        extraMeasureTranslations: Map<String, String> = emptyMap(),
        extraTagTranslations: Map<String, String> = emptyMap()
    ): RecipeEntity {
        val hasFullDetails = !strInstructions.isNullOrBlank()
        val existingIngredients = existing?.let { decodeIngredients(it.ingredientsJson) }.orEmpty()
        val checkedLookup = existingIngredients.associate { it.name.lowercase() to it.isChecked }

        val mergedIngredientsJson = if (hasFullDetails) {
            val mapped = extractIngredientPairs().map { (rawName, rawMeasure) ->
                val localizedName = if (CulinaryPtBrLocalizer.hasIngredientTranslation(rawName)) {
                    CulinaryPtBrLocalizer.translateIngredient(rawName)
                } else {
                    extraIngredientTranslations[rawName.trim().lowercase()]
                        ?: CulinaryPtBrLocalizer.translateIngredient(rawName)
                }
                val baseMeasure = CulinaryPtBrLocalizer.translateMeasure(rawMeasure)
                val localizedMeasure = if (baseMeasure == rawMeasure.trim()) {
                    extraMeasureTranslations[rawMeasure.trim().lowercase()] ?: baseMeasure
                } else {
                    baseMeasure
                }
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
                .map { rawTag ->
                    val trimmed = rawTag.trim()
                    if (CulinaryPtBrLocalizer.hasTagTranslation(trimmed)) {
                        CulinaryPtBrLocalizer.translateTag(trimmed)
                    } else {
                        extraTagTranslations[trimmed.lowercase()]
                            ?: CulinaryPtBrLocalizer.translateTag(trimmed)
                    }
                }
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

        val resolvedName = translatedName?.takeIf { it.isNotBlank() } ?: strMeal.trim()
        val resolvedInstructions = if (hasFullDetails) {
            translatedInstructions?.takeIf { it.isNotBlank() } ?: strInstructions.orEmpty().trim()
        } else {
            existing?.instructions.orEmpty()
        }

        return RecipeEntity(
            id = idMeal,
            name = resolvedName,
            category = resolvedCategory,
            area = resolvedArea,
            instructions = resolvedInstructions,
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

    fun CategoryDto.toEntity(translatedDescription: String? = null): CategoryEntity = CategoryEntity(
        id = idCategory,
        name = CulinaryPtBrLocalizer.translateCategory(strCategory),
        thumbnailUrl = strCategoryThumb.orEmpty(),
        description = translatedDescription?.takeIf { it.isNotBlank() }
            ?: strCategoryDescription.orEmpty().trim()
    )

    fun CategoryEntity.toDomain(): Category = Category(
        id = id,
        name = name,
        thumbnailUrl = thumbnailUrl,
        description = description
    )
}
