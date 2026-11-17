package com.example.domain.usecase

import com.example.domain.model.Category
import com.example.domain.model.Recipe
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow

class ObserveRecipesUseCase(
    private val repository: RecipeRepository
) {
    operator fun invoke(query: String, category: String?): Flow<List<Recipe>> {
        val normalizedQuery = query.trim()
        val normalizedCategory = category?.takeIf { it.isNotBlank() }
        return repository.observeRecipes(normalizedQuery, normalizedCategory)
    }
}

class ObserveCategoriesUseCase(
    private val repository: RecipeRepository
) {
    operator fun invoke(): Flow<List<Category>> = repository.observeCategories()
}

class ObserveFavoriteRecipesUseCase(
    private val repository: RecipeRepository
) {
    operator fun invoke(): Flow<List<Recipe>> = repository.observeFavoriteRecipes()
}

class ObserveRecipeDetailUseCase(
    private val repository: RecipeRepository
) {
    operator fun invoke(recipeId: String): Flow<Recipe?> = repository.observeRecipeById(recipeId)

    suspend fun refreshIfNeeded(recipeId: String): SyncOutcome =
        repository.ensureRecipeDetailLoaded(recipeId)
}

class SyncCatalogUseCase(
    private val repository: RecipeRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): SyncOutcome =
        repository.syncInitialCatalog(forceRefresh)

    suspend fun searchRemote(query: String): SyncOutcome =
        repository.searchAndCacheRecipes(query.trim())

    suspend fun syncCategory(category: String): SyncOutcome =
        repository.syncCategoryRecipes(category)
}

class ToggleFavoriteUseCase(
    private val repository: RecipeRepository
) {
    suspend operator fun invoke(recipeId: String) {
        repository.toggleFavorite(recipeId)
    }
}

class ToggleIngredientCheckUseCase(
    private val repository: RecipeRepository
) {
    suspend operator fun invoke(recipeId: String, ingredientIndex: Int) {
        if (ingredientIndex >= 0) {
            repository.toggleIngredientChecked(recipeId, ingredientIndex)
        }
    }
}

class GetRandomChefRecipeUseCase(
    private val repository: RecipeRepository
) {
    suspend operator fun invoke(): String? = repository.getRandomRecipeId()
}
