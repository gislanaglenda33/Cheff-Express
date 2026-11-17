package com.example.presentation.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.presentation.components.EmptyChefState
import com.example.presentation.components.LoadingChefState
import com.example.presentation.components.OfflineStatusBanner
import com.example.presentation.components.RecipeEditorialCard

@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onFilterQueryChange: (String) -> Unit,
    onRecipeClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentQuery = when (uiState) {
        is FavoritesUiState.Success -> uiState.filterQuery
        is FavoritesUiState.Empty -> uiState.filterQuery
        FavoritesUiState.Loading -> ""
    }
    val totalCount = when (uiState) {
        is FavoritesUiState.Success -> uiState.totalFavoritesCount
        is FavoritesUiState.Empty -> uiState.totalFavoritesCount
        FavoritesUiState.Loading -> 0
    }
    val isOnline = when (uiState) {
        is FavoritesUiState.Success -> uiState.isOnline
        is FavoritesUiState.Empty -> uiState.isOnline
        FavoritesUiState.Loading -> true
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 260.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("favorites_grid")
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "fav_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.favorites_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.favorites_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BookmarkAdded,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "$totalCount",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "fav_offline") {
            OfflineStatusBanner(
                isOnline = isOnline,
                isSyncing = false,
                onSyncClick = {}
            )
        }

        if (totalCount > 0) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "fav_search") {
                OutlinedTextField(
                    value = currentQuery,
                    onValueChange = onFilterQueryChange,
                    placeholder = { Text("Filtrar receitas salvas…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (currentQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onFilterQueryChange("") },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.clear_filters)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("favorites_search_input")
                )
            }
        }

        when (uiState) {
            FavoritesUiState.Loading -> {
                item(span = { GridItemSpan(maxLineSpan) }, key = "fav_loading") {
                    LoadingChefState(message = "Abrindo seu livro de receitas…")
                }
            }

            is FavoritesUiState.Empty -> {
                item(span = { GridItemSpan(maxLineSpan) }, key = "fav_empty") {
                    if (uiState.totalFavoritesCount > 0 && uiState.filterQuery.isNotBlank()) {
                        EmptyChefState(
                            title = "Nenhum favorito corresponde à busca",
                            description = "Limpe o filtro para ver todas as suas ${uiState.totalFavoritesCount} receitas salvas.",
                            actionLabel = stringResource(R.string.clear_filters),
                            onActionClick = { onFilterQueryChange("") },
                            useIllustration = false
                        )
                    } else {
                        EmptyChefState(
                            title = stringResource(R.string.empty_favorites_title),
                            description = stringResource(R.string.empty_favorites_desc),
                            actionLabel = "Explorar receitas",
                            onActionClick = onExploreClick,
                            useIllustration = true
                        )
                    }
                }
            }

            is FavoritesUiState.Success -> {
                items(
                    items = uiState.recipes,
                    key = { it.id }
                ) { recipe ->
                    val openRecipe = remember(recipe.id) { { onRecipeClick(recipe.id) } }
                    val toggleFav = remember(recipe.id) { { onToggleFavorite(recipe.id) } }
                    RecipeEditorialCard(
                        recipe = recipe,
                        onClick = openRecipe,
                        onToggleFavorite = toggleFav
                    )
                }
            }
        }
    }
}
