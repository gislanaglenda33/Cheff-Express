package com.example.presentation.favorites

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Recipe
import com.example.domain.repository.NetworkMonitor
import com.example.domain.usecase.ObserveFavoriteRecipesUseCase
import com.example.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState
    data class Success(
        val recipes: List<Recipe>,
        val filterQuery: String,
        val totalFavoritesCount: Int,
        val isOnline: Boolean
    ) : FavoritesUiState

    data class Empty(
        val filterQuery: String,
        val totalFavoritesCount: Int,
        val isOnline: Boolean
    ) : FavoritesUiState
}

class FavoritesViewModel(
    observeFavoriteRecipesUseCase: ObserveFavoriteRecipesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    private val filterQuery = MutableStateFlow("")

    val uiState: StateFlow<FavoritesUiState> = combine(
        observeFavoriteRecipesUseCase(),
        filterQuery,
        networkMonitor.isOnline
    ) { favorites, query, isOnline ->
        val normalized = query.trim()
        val filtered = if (normalized.isBlank()) {
            favorites
        } else {
            favorites.filter { recipe ->
                recipe.name.contains(normalized, ignoreCase = true) ||
                    recipe.category.contains(normalized, ignoreCase = true) ||
                    recipe.area.contains(normalized, ignoreCase = true)
            }
        }
        if (filtered.isEmpty()) {
            FavoritesUiState.Empty(
                filterQuery = query,
                totalFavoritesCount = favorites.size,
                isOnline = isOnline
            )
        } else {
            FavoritesUiState.Success(
                recipes = filtered,
                filterQuery = query,
                totalFavoritesCount = favorites.size,
                isOnline = isOnline
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FavoritesUiState.Loading
    )

    fun onFilterQueryChanged(query: String) {
        filterQuery.value = query
    }

    fun onToggleFavorite(recipeId: String) {
        viewModelScope.launch {
            toggleFavoriteUseCase(recipeId)
        }
    }
}
