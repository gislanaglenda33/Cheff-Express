package com.example.domain.usecase

import com.example.domain.model.Category
import com.example.domain.model.IngredientItem
import com.example.domain.model.Recipe
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeUseCasesTest {

    private class FakeRecipeRepository : RecipeRepository {
        var lastObservedQuery: String? = null
        var lastObservedCategory: String? = null
        var toggledFavoriteId: String? = null
        var toggledIngredientCall: Pair<String, Int>? = null
        var randomIdToReturn: String? = "52771"

        private val sampleRecipes = MutableStateFlow(
            listOf(
                Recipe(
                    id = "52771",
                    name = "Spicy Arrabiata Penne",
                    category = "Vegetarian",
                    area = "Italian",
                    instructions = "STEP 1: Boil water.\n2. Cook pasta.\n\nServe hot.",
                    thumbnailUrl = "https://example.com/1.jpg",
                    ingredients = listOf(IngredientItem("Penne", "400g"))
                )
            )
        )

        override fun observeRecipes(query: String, category: String?): Flow<List<Recipe>> {
            lastObservedQuery = query
            lastObservedCategory = category
            return sampleRecipes
        }

        override fun observeFavoriteRecipes(): Flow<List<Recipe>> = flowOf(emptyList())
        override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun observeRecipeById(recipeId: String): Flow<Recipe?> = flowOf(sampleRecipes.value.firstOrNull())
        override suspend fun syncInitialCatalog(forceRefresh: Boolean): SyncOutcome = SyncOutcome.Success(1)
        override suspend fun searchAndCacheRecipes(query: String): SyncOutcome = SyncOutcome.Success(1)
        override suspend fun syncCategoryRecipes(category: String): SyncOutcome = SyncOutcome.Success(1)
        override suspend fun ensureRecipeDetailLoaded(recipeId: String): SyncOutcome = SyncOutcome.Success(1)

        override suspend fun toggleFavorite(recipeId: String) {
            toggledFavoriteId = recipeId
        }

        override suspend fun toggleIngredientChecked(recipeId: String, ingredientIndex: Int) {
            toggledIngredientCall = recipeId to ingredientIndex
        }

        override suspend fun getRandomRecipeId(): String? = randomIdToReturn
    }

    @Test
    fun `ObserveRecipesUseCase normalizes blank query and category`() = runTest {
        val repo = FakeRecipeRepository()
        val useCase = ObserveRecipesUseCase(repo)

        val result = useCase(query = "  pasta  ", category = "   ").first()

        assertEquals("pasta", repo.lastObservedQuery)
        assertEquals(null, repo.lastObservedCategory)
        assertEquals(1, result.size)
    }

    @Test
    fun `ToggleIngredientCheckUseCase ignores negative index and forwards valid index`() = runTest {
        val repo = FakeRecipeRepository()
        val useCase = ToggleIngredientCheckUseCase(repo)

        useCase("52771", -1)
        assertEquals(null, repo.toggledIngredientCall)

        useCase("52771", 2)
        assertEquals("52771" to 2, repo.toggledIngredientCall)
    }

    @Test
    fun `ToggleFavoriteUseCase and GetRandomChefRecipeUseCase delegate to repository`() = runTest {
        val repo = FakeRecipeRepository()
        val favUseCase = ToggleFavoriteUseCase(repo)
        val randomUseCase = GetRandomChefRecipeUseCase(repo)

        favUseCase("52771")
        val randomId = randomUseCase()

        assertEquals("52771", repo.toggledFavoriteId)
        assertEquals("52771", randomId)
    }

    @Test
    fun `Recipe instructionSteps cleans step prefixes and blank lines`() {
        val recipe = Recipe(
            id = "1",
            name = "Test",
            category = "Pasta",
            area = "Italian",
            instructions = "STEP 1: Boil water.\r\n2. Add salt.\n\n3) Drain pasta.",
            thumbnailUrl = ""
        )

        val steps = recipe.instructionSteps
        assertEquals(listOf("Boil water.", "Add salt.", "Drain pasta."), steps)
        assertTrue(steps.none { it.isBlank() })
    }
}
