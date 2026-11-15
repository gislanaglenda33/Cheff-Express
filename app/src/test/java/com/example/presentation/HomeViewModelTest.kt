package com.example.presentation

import com.example.domain.model.Category
import com.example.domain.model.Recipe
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.NetworkMonitor
import com.example.domain.repository.RecipeRepository
import com.example.domain.usecase.GetRandomChefRecipeUseCase
import com.example.domain.usecase.ObserveCategoriesUseCase
import com.example.domain.usecase.ObserveRecipesUseCase
import com.example.domain.usecase.SyncCatalogUseCase
import com.example.domain.usecase.ToggleFavoriteUseCase
import com.example.presentation.home.HomeEvent
import com.example.presentation.home.HomeUiState
import com.example.presentation.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeRepo : RecipeRepository {
        val recipesFlow = MutableStateFlow(
            listOf(
                Recipe("1", "Pasta Primavera", "Pasta", "Italian", "Cook pasta", ""),
                Recipe("2", "Chicken Curry", "Chicken", "Indian", "Simmer curry", "")
            )
        )
        val categoriesFlow = MutableStateFlow(
            listOf(
                Category("c1", "Pasta", "", ""),
                Category("c2", "Chicken", "", "")
            )
        )

        override fun observeRecipes(query: String, category: String?): Flow<List<Recipe>> =
            recipesFlow.map { list ->
                list.filter {
                    (category == null || it.category == category) &&
                        (query.isBlank() || it.name.contains(query, ignoreCase = true))
                }
            }

        override fun observeFavoriteRecipes(): Flow<List<Recipe>> = flowOf(emptyList())
        override fun observeCategories(): Flow<List<Category>> = categoriesFlow
        override fun observeRecipeById(recipeId: String): Flow<Recipe?> = flowOf(null)
        override suspend fun syncInitialCatalog(forceRefresh: Boolean): SyncOutcome = SyncOutcome.Success(2)
        override suspend fun searchAndCacheRecipes(query: String): SyncOutcome = SyncOutcome.Success(1)
        override suspend fun syncCategoryRecipes(category: String): SyncOutcome = SyncOutcome.Success(1)
        override suspend fun ensureRecipeDetailLoaded(recipeId: String): SyncOutcome = SyncOutcome.Success(1)
        override suspend fun toggleFavorite(recipeId: String) = Unit
        override suspend fun toggleIngredientChecked(recipeId: String, ingredientIndex: Int) = Unit
        override suspend fun getRandomRecipeId(): String = "2"
    }

    private class FakeMonitor : NetworkMonitor {
        override val isOnline = MutableStateFlow(true)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `HomeViewModel transitions to Success and filters by category and search`() = runTest(testDispatcher) {
        val repo = FakeRepo()
        val monitor = FakeMonitor()
        val vm = HomeViewModel(
            observeRecipesUseCase = ObserveRecipesUseCase(repo),
            observeCategoriesUseCase = ObserveCategoriesUseCase(repo),
            syncCatalogUseCase = SyncCatalogUseCase(repo),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(repo),
            getRandomChefRecipeUseCase = GetRandomChefRecipeUseCase(repo),
            networkMonitor = monitor
        )

        val collectorJob = launch { vm.uiState.collect {} }
        advanceUntilIdle()

        val initial = vm.uiState.value
        assertTrue(initial is HomeUiState.Success)
        assertEquals(2, (initial as HomeUiState.Success).recipes.size)

        vm.onCategorySelected("Pasta")
        advanceUntilIdle()
        val pastaState = vm.uiState.value as HomeUiState.Success
        assertEquals(1, pastaState.recipes.size)
        assertEquals("Pasta Primavera", pastaState.recipes.first().name)

        vm.onSearchQueryChanged("nonexistent")
        advanceUntilIdle()
        assertTrue(vm.uiState.value is HomeUiState.Empty)

        vm.onClearFilters()
        advanceUntilIdle()
        assertEquals(2, (vm.uiState.value as HomeUiState.Success).recipes.size)

        collectorJob.cancel()
    }

    @Test
    fun `onSurpriseChefClick emits NavigateToRecipe event`() = runTest(testDispatcher) {
        val repo = FakeRepo()
        val vm = HomeViewModel(
            observeRecipesUseCase = ObserveRecipesUseCase(repo),
            observeCategoriesUseCase = ObserveCategoriesUseCase(repo),
            syncCatalogUseCase = SyncCatalogUseCase(repo),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(repo),
            getRandomChefRecipeUseCase = GetRandomChefRecipeUseCase(repo),
            networkMonitor = FakeMonitor()
        )

        var emittedEvent: HomeEvent? = null
        val eventJob = launch {
            emittedEvent = vm.events.first()
        }

        vm.onSurpriseChefClick()
        advanceUntilIdle()

        assertEquals(HomeEvent.NavigateToRecipe("2"), emittedEvent)
        eventJob.cancel()
    }
}
