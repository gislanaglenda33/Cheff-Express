package com.example.data.mapper

import com.example.data.local.IngredientLocalModel
import com.example.data.local.RecipeEntity
import com.example.data.mapper.RecipeMappers.toDomain
import com.example.data.mapper.RecipeMappers.toEntity
import com.example.data.remote.MealDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeMappersTest {

    @Test
    fun `toEntity preserves existing favorite and checked ingredients and localizes to PT-BR`() {
        val existing = RecipeEntity(
            id = "52771",
            name = "Arrabiata",
            category = "Vegetariano",
            area = "Italiana",
            instructions = "Old instructions",
            thumbnailUrl = "https://img/old.jpg",
            youtubeUrl = null,
            sourceUrl = null,
            tagsCsv = "Massa",
            ingredientsJson = RecipeMappers.encodeIngredients(
                listOf(
                    IngredientLocalModel("Penne", "400g", isChecked = true),
                    IngredientLocalModel("Alho", "2 dentes", isChecked = false)
                )
            ),
            isFavorite = true,
            isDetailLoaded = true,
            lastSyncedAt = 100L
        )

        val dto = MealDto(
            idMeal = "52771",
            strMeal = "Spicy Arrabiata Penne",
            strCategory = "Vegetarian",
            strArea = "Italian",
            strInstructions = "Boil pasta and simmer sauce.",
            strMealThumb = "https://img/new.jpg",
            strTags = "Pasta,Spicy",
            strIngredient1 = "Penne",
            strMeasure1 = "450g",
            strIngredient2 = "Garlic",
            strMeasure2 = "3 cloves"
        )

        val mapped = dto.toEntity(existing = existing, nowMillis = 999L)
        val domain = mapped.toDomain()

        assertTrue(domain.isFavorite)
        assertEquals("Spicy Arrabiata Penne", domain.name)
        assertEquals("Vegetariano", domain.category)
        assertEquals("Italiana", domain.area)
        assertEquals("Boil pasta and simmer sauce.", domain.instructions)
        assertEquals(2, domain.ingredients.size)
        assertTrue(domain.ingredients[0].isChecked)
        assertEquals("Alho", domain.ingredients[1].name)
        assertEquals("3 dentes", domain.ingredients[1].measure)
        assertEquals(listOf("Massa", "Picante"), domain.tags)
    }

    @Test
    fun `toEntity summary from category filter does not erase existing offline details`() {
        val existing = RecipeEntity(
            id = "52844",
            name = "Lasanha",
            category = "Massas",
            area = "Italiana",
            instructions = "Asse por 40 minutos.",
            thumbnailUrl = "https://img/lasagne.jpg",
            youtubeUrl = null,
            sourceUrl = null,
            tagsCsv = "Clássico",
            ingredientsJson = RecipeMappers.encodeIngredients(
                listOf(IngredientLocalModel("Carne Moída", "500g", isChecked = true))
            ),
            isFavorite = true,
            isDetailLoaded = true,
            lastSyncedAt = 100L
        )

        val summaryDto = MealDto(
            idMeal = "52844",
            strMeal = "Lasagne",
            strMealThumb = "https://img/lasagne.jpg"
        )

        val mapped = summaryDto.toEntity(existing = existing, fallbackCategory = "Pasta", nowMillis = 200L)

        assertTrue(mapped.isFavorite)
        assertTrue(mapped.isDetailLoaded)
        assertEquals("Asse por 40 minutos.", mapped.instructions)
        assertEquals("Italiana", mapped.area)
        assertEquals("Massas", mapped.category)
        assertEquals(1, RecipeMappers.decodeIngredients(mapped.ingredientsJson).size)
    }

    @Test
    fun `CulinaryPtBrLocalizer converts categories, origins, and measurements accurately`() {
        assertEquals("Frutos do Mar", CulinaryPtBrLocalizer.translateCategory("Seafood"))
        assertEquals("Seafood", CulinaryPtBrLocalizer.toApiCategoryKey("Frutos do Mar"))
        assertEquals("Japonesa", CulinaryPtBrLocalizer.translateArea("Japanese"))
        assertEquals("2 col. sopa", CulinaryPtBrLocalizer.translateMeasure("2 tbsp"))
        assertEquals("1/2 xícara", CulinaryPtBrLocalizer.translateMeasure("1/2 cup"))
    }
}
