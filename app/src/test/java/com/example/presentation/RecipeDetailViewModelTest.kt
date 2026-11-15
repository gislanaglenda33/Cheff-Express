package com.example.presentation

import com.example.domain.model.Category
import com.example.domain.model.IngredientItem
import com.example.domain.model.Recipe
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.NetworkMonitor
import com.example.domain.repository.RecipeRepository
import com.example.domain.usecase.ObserveRecipeDetailUseCase
import com.example.domain.usecase.ToggleFavoriteUseCase
import com.example.domain.usecase.ToggleIngredientCheckUseCase
import com.example.presentation.detail.RecipeDetailUiState
import com.example.presentation.detail.RecipeDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeDetailRepo : RecipeRepository {
        val recipeFlow = MutableStateFlow<Recipe?>(
            Recipe(
                id = "52771",
                name = "Arrabiata",
                category = "Vegetarian",
                area = "Italian",
                instructions = "Boil pasta",
                thumbnailUrl = "",
                ingredients = listOf(IngredientItem("Penne", "400g", false)),
                isFavorite = false,
                isDetailLoaded = true
            )
        )

        override fun observeRecipes(query: String, category: String?): Flow<List<Recipe>> = flowOf(emptyList())
        override fun observeFavoriteRecipes(): Flow<List<Recipe>> = flowOf(emptyList())
        override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun observeRecipeById(recipeId: String): Flow<Recipe?> = recipeFlow
        override suspend fun syncInitialCatalog(forceRefresh: Boolean): SyncOutcome = SyncOutcome.Success(1)
        override suspend fun searchAndCacheRecipes(query: String): SyncOutcome = SyncOutcome.Success(1)
        override suspend fun syncCategoryRecipes(category: String): SyncOutcome = SyncOutcome.Success(1)
        override suspend fun ensureRecipeDetailLoaded(recipeId: String): SyncOutcome = SyncOutcome.Success(1)

        override suspend fun toggleFavorite(recipeId: String) {
            recipeFlow.value = recipeFlow.value?.let { it.copy(isFavorite = !it.isFavorite) }
        }

        override suspend fun toggleIngredientChecked(recipeId: String, ingredientIndex: Int) {
            val current = recipeFlow.value ?: return
            val updated = current.ingredients.mapIndexed { idx, item ->
                if (idx == ingredientIndex) item.copy(isChecked = !item.isChecked) else item
            }
            recipeFlow.value = current.copy(ingredients = updated)
        }

        override suspend fun getRandomRecipeId(): String? = "52771"
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
    fun `RecipeDetailViewModel toggles favorite and ingredient check reactively`() = runTest(testDispatcher) {
        val repo = FakeDetailRepo()
        val vm = RecipeDetailViewModel(
            recipeId = "52771",
            observeRecipeDetailUseCase = ObserveRecipeDetailUseCase(repo),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(repo),
            toggleIngredientCheckUseCase = ToggleIngredientCheckUseCase(repo),
            networkMonitor = FakeMonitor()
        )

        val job = launch { vm.uiState.collect {} }
        advanceUntilIdle()

        val initial = vm.uiState.value as RecipeDetailUiState.Success
        assertFalse(initial.recipe.isFavorite)
        assertFalse(initial.recipe.ingredients.first().isChecked)

        vm.onToggleFavorite()
        vm.onToggleIngredient(0)
        advanceUntilIdle()

        val updated = vm.uiState.value as RecipeDetailUiState.Success
        assertTrue(updated.recipe.isFavorite)
        assertTrue(updated.recipe.ingredients.first().isChecked)
        job.cancel()
    }

    @Test
    fun `Kitchen timer updates presets, counts down, and resets`() = runTest(testDispatcher) {
        val vm = RecipeDetailViewModel(
            recipeId = "52771",
            observeRecipeDetailUseCase = ObserveRecipeDetailUseCase(FakeDetailRepo()),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(FakeDetailRepo()),
            toggleIngredientCheckUseCase = ToggleIngredientCheckUseCase(FakeDetailRepo()),
            networkMonitor = FakeMonitor()
        )

        vm.setTimerPresetMinutes(10)
        assertEquals("10:00", vm.timerState.value.formattedTime)

        vm.addMinuteToTimer()
        assertEquals("11:00", vm.timerState.value.formattedTime)

        vm.toggleTimerRunning()
        assertTrue(vm.timerState.value.isRunning)
        advanceTimeBy(3_100L)
        assertEquals("10:57", vm.timerState.value.formattedTime)

        vm.resetTimer()
        assertFalse(vm.timerState.value.isRunning)
        assertEquals("11:00", vm.timerState.value.formattedTime)
    }
}
