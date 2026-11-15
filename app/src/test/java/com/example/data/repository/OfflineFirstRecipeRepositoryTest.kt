package com.example.data.repository

import com.example.data.local.CategoryDao
import com.example.data.local.CategoryEntity
import com.example.data.local.RecipeDao
import com.example.data.local.RecipeEntity
import com.example.data.remote.CategoriesResponseDto
import com.example.data.remote.CategoryDto
import com.example.data.remote.MealDto
import com.example.data.remote.MealsResponseDto
import com.example.data.remote.TheMealDbApi
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.NetworkMonitor
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFirstRecipeRepositoryTest {

    private class FakeNetworkMonitor(initialOnline: Boolean) : NetworkMonitor {
        override val isOnline = MutableStateFlow(initialOnline)
    }

    private class InMemoryRecipeDao : RecipeDao {
        private val store = MutableStateFlow<Map<String, RecipeEntity>>(emptyMap())

        override fun observeRecipes(query: String, category: String?): Flow<List<RecipeEntity>> =
            store.map { map ->
                map.values.filter { r ->
                    (category == null || r.category == category) &&
                        (query.isBlank() || r.name.contains(query, ignoreCase = true))
                }.sortedBy { it.name }
            }

        override fun observeFavoriteRecipes(): Flow<List<RecipeEntity>> =
            store.map { map -> map.values.filter { it.isFavorite } }

        override fun observeRecipeById(id: String): Flow<RecipeEntity?> =
            store.map { it[id] }

        override suspend fun getRecipeById(id: String): RecipeEntity? = store.value[id]
        override suspend fun getRecipesByIds(ids: List<String>): List<RecipeEntity> =
            ids.mapNotNull { store.value[it] }

        override suspend fun getRecipeCount(): Int = store.value.size
        override suspend fun getRandomLocalRecipeId(): String? = store.value.keys.firstOrNull()

        override suspend fun upsertRecipes(recipes: List<RecipeEntity>) {
            val next = store.value.toMutableMap()
            recipes.forEach { next[it.id] = it }
            store.value = next
        }

        override suspend fun upsertRecipe(recipe: RecipeEntity) {
            store.value = store.value + (recipe.id to recipe)
        }

        override suspend fun updateFavorite(id: String, isFavorite: Boolean) {
            val current = store.value[id] ?: return
            store.value = store.value + (id to current.copy(isFavorite = isFavorite))
        }

        override suspend fun updateIngredients(id: String, ingredientsJson: String) {
            val current = store.value[id] ?: return
            store.value = store.value + (id to current.copy(ingredientsJson = ingredientsJson))
        }
    }

    private class InMemoryCategoryDao : CategoryDao {
        private val store = MutableStateFlow<List<CategoryEntity>>(emptyList())
        override fun observeCategories(): Flow<List<CategoryEntity>> = store
        override suspend fun getCategoryCount(): Int = store.value.size
        override suspend fun upsertCategories(categories: List<CategoryEntity>) {
            store.value = categories
        }
    }

    private class FakeMealApi(var shouldThrowIo: Boolean = false) : TheMealDbApi {
        override suspend fun getCategories(): CategoriesResponseDto {
            if (shouldThrowIo) throw IOException("No network")
            return CategoriesResponseDto(
                listOf(CategoryDto("1", "Pasta", "https://img/pasta.png", "Pasta dishes"))
            )
        }

        override suspend fun searchMeals(query: String): MealsResponseDto {
            if (shouldThrowIo) throw IOException("No network")
            return MealsResponseDto(
                listOf(
                    MealDto(
                        idMeal = "9001",
                        strMeal = "Carbonara",
                        strCategory = "Pasta",
                        strArea = "Italian",
                        strInstructions = "Cook spaghetti and toss with egg and pecorino.",
                        strMealThumb = "https://img/carbonara.jpg",
                        strIngredient1 = "Spaghetti",
                        strMeasure1 = "400g"
                    )
                )
            )
        }

        override suspend fun filterByCategory(category: String): MealsResponseDto =
            searchMeals("")

        override suspend fun getMealById(id: String): MealsResponseDto =
            searchMeals("")

        override suspend fun getRandomMeal(): MealsResponseDto =
            searchMeals("")
    }

    @Test
    fun `offline first launch seeds local database and returns OfflineFallback`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val recipeDao = InMemoryRecipeDao()
        val categoryDao = InMemoryCategoryDao()
        val repo = OfflineFirstRecipeRepositoryImpl(
            api = FakeMealApi(),
            recipeDao = recipeDao,
            categoryDao = categoryDao,
            networkMonitor = FakeNetworkMonitor(initialOnline = false),
            ioDispatcher = dispatcher
        )

        val outcome = repo.syncInitialCatalog(forceRefresh = false)
        val cachedRecipes = repo.observeRecipes("", null).first()

        assertEquals(SyncOutcome.OfflineFallback, outcome)
        assertTrue(cachedRecipes.isNotEmpty())
        assertTrue(repo.observeCategories().first().isNotEmpty())
    }

    @Test
    fun `online sync persists remote recipes and preserves local favorites and ingredient checks`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val recipeDao = InMemoryRecipeDao()
        val categoryDao = InMemoryCategoryDao()
        val api = FakeMealApi()
        val repo = OfflineFirstRecipeRepositoryImpl(
            api = api,
            recipeDao = recipeDao,
            categoryDao = categoryDao,
            networkMonitor = FakeNetworkMonitor(initialOnline = true),
            ioDispatcher = dispatcher
        )

        val syncOutcome = repo.syncInitialCatalog(forceRefresh = true)
        assertTrue(syncOutcome is SyncOutcome.Success)

        repo.toggleFavorite("9001")
        repo.toggleIngredientChecked("9001", 0)

        // Second sync should keep favorite and checked ingredient intact
        repo.syncInitialCatalog(forceRefresh = true)
        val recipe = repo.observeRecipeById("9001").first()

        assertNotNull(recipe)
        assertTrue(recipe!!.isFavorite)
        assertTrue(recipe.ingredients[0].isChecked)

        // Toggling favorite off removes from favorites
        repo.toggleFavorite("9001")
        val updated = repo.observeRecipeById("9001").first()
        assertFalse(updated!!.isFavorite)
    }

    @Test
    fun `getRandomRecipeId falls back to local database when offline`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val recipeDao = InMemoryRecipeDao()
        val repo = OfflineFirstRecipeRepositoryImpl(
            api = FakeMealApi(shouldThrowIo = true),
            recipeDao = recipeDao,
            categoryDao = InMemoryCategoryDao(),
            networkMonitor = FakeNetworkMonitor(initialOnline = false),
            ioDispatcher = dispatcher
        )

        repo.syncInitialCatalog(forceRefresh = false)
        val randomId = repo.getRandomRecipeId()

        assertNotNull(randomId)
    }
}
