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
    fun `toEntity preserves existing favorite and checked ingredients when refreshing full recipe`() {
        val existing = RecipeEntity(
            id = "52771",
            name = "Arrabiata",
            category = "Vegetarian",
            area = "Italian",
            instructions = "Old instructions",
            thumbnailUrl = "https://img/old.jpg",
            youtubeUrl = null,
            sourceUrl = null,
            tagsCsv = "Pasta",
            ingredientsJson = RecipeMappers.encodeIngredients(
                listOf(
                    IngredientLocalModel("Penne", "400g", isChecked = true),
                    IngredientLocalModel("Garlic", "2 cloves", isChecked = false)
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
        assertEquals("Boil pasta and simmer sauce.", domain.instructions)
        assertEquals(2, domain.ingredients.size)
        assertTrue(domain.ingredients[0].isChecked)
        assertEquals("450g", domain.ingredients[0].measure)
        assertEquals(listOf("Pasta", "Spicy"), domain.tags)
    }

    @Test
    fun `toEntity summary from category filter does not erase existing offline details`() {
        val existing = RecipeEntity(
            id = "52844",
            name = "Lasagne",
            category = "Pasta",
            area = "Italian",
            instructions = "Bake for 40 minutes.",
            thumbnailUrl = "https://img/lasagne.jpg",
            youtubeUrl = null,
            sourceUrl = null,
            tagsCsv = "Classic",
            ingredientsJson = RecipeMappers.encodeIngredients(
                listOf(IngredientLocalModel("Beef", "500g", isChecked = true))
            ),
            isFavorite = true,
            isDetailLoaded = true,
            lastSyncedAt = 100L
        )

        // Simulating filter.php response which only returns id, name, and thumb
        val summaryDto = MealDto(
            idMeal = "52844",
            strMeal = "Lasagne",
            strMealThumb = "https://img/lasagne.jpg"
        )

        val mapped = summaryDto.toEntity(existing = existing, fallbackCategory = "Pasta", nowMillis = 200L)

        assertTrue(mapped.isFavorite)
        assertTrue(mapped.isDetailLoaded)
        assertEquals("Bake for 40 minutes.", mapped.instructions)
        assertEquals("Italian", mapped.area)
        assertEquals(1, RecipeMappers.decodeIngredients(mapped.ingredientsJson).size)
    }
}
