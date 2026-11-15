package com.example.domain.repository

import com.example.domain.model.Category
import com.example.domain.model.Recipe
import com.example.domain.model.SyncOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}

interface RecipeRepository {
    fun observeRecipes(query: String, category: String?): Flow<List<Recipe>>
    fun observeFavoriteRecipes(): Flow<List<Recipe>>
    fun observeCategories(): Flow<List<Category>>
    fun observeRecipeById(recipeId: String): Flow<Recipe?>

    suspend fun syncInitialCatalog(forceRefresh: Boolean = false): SyncOutcome
    suspend fun searchAndCacheRecipes(query: String): SyncOutcome
    suspend fun syncCategoryRecipes(category: String): SyncOutcome
    suspend fun ensureRecipeDetailLoaded(recipeId: String): SyncOutcome
    suspend fun toggleFavorite(recipeId: String)
    suspend fun toggleIngredientChecked(recipeId: String, ingredientIndex: Int)
    suspend fun getRandomRecipeId(): String?
}
