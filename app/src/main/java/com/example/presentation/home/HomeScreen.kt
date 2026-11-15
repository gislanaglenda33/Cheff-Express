package com.example.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.Category
import com.example.presentation.components.EmptyChefState
import com.example.presentation.components.ErrorChefState
import com.example.presentation.components.LoadingChefState
import com.example.presentation.components.OfflineStatusBanner
import com.example.presentation.components.RecipeEditorialCard

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onSurpriseChef: () -> Unit,
    onRecipeClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentQuery = when (uiState) {
        is HomeUiState.Success -> uiState.searchQuery
        is HomeUiState.Empty -> uiState.searchQuery
        else -> ""
    }
    val currentCategory = when (uiState) {
        is HomeUiState.Success -> uiState.selectedCategory
        is HomeUiState.Empty -> uiState.selectedCategory
        else -> null
    }
    val categories = when (uiState) {
        is HomeUiState.Success -> uiState.categories
        is HomeUiState.Empty -> uiState.categories
        else -> emptyList()
    }
    val isOnline = when (uiState) {
        is HomeUiState.Success -> uiState.isOnline
        is HomeUiState.Empty -> uiState.isOnline
        is HomeUiState.Error -> uiState.isOnline
        HomeUiState.Loading -> true
    }
    val isSyncing = when (uiState) {
        is HomeUiState.Success -> uiState.isSyncing
        is HomeUiState.Empty -> uiState.isSyncing
        else -> false
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 260.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("home_recipe_grid")
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "hero_banner") {
            EditorialHeroBanner(
                isOnline = isOnline,
                isSyncing = isSyncing,
                onSurpriseChef = onSurpriseChef,
                onRefresh = onRefresh
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "offline_banner") {
            OfflineStatusBanner(
                isOnline = isOnline,
                isSyncing = isSyncing,
                onSyncClick = onRefresh
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "search_bar") {
            SearchRecipeBar(
                query = currentQuery,
                onQueryChange = onSearchQueryChange
            )
        }

        if (categories.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "categories_row") {
                CategoryFilterRow(
                    categories = categories,
                    selectedCategory = currentCategory,
                    onCategorySelected = onCategorySelected
                )
            }
        }

        when (uiState) {
            HomeUiState.Loading -> {
                item(span = { GridItemSpan(maxLineSpan) }, key = "loading") {
                    LoadingChefState(message = stringResource(R.string.loading_recipes))
                }
            }

            is HomeUiState.Error -> {
                item(span = { GridItemSpan(maxLineSpan) }, key = "error") {
                    ErrorChefState(
                        title = stringResource(R.string.error_load_title),
                        message = uiState.message,
                        onRetry = onRefresh
                    )
                }
            }

            is HomeUiState.Empty -> {
                item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                    EmptyChefState(
                        title = stringResource(R.string.empty_recipes_title),
                        description = stringResource(R.string.empty_recipes_desc),
                        actionLabel = if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) {
                            stringResource(R.string.clear_filters)
                        } else {
                            stringResource(R.string.retry)
                        },
                        onActionClick = if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) {
                            onClearFilters
                        } else {
                            onRefresh
                        }
                    )
                }
            }

            is HomeUiState.Success -> {
                item(span = { GridItemSpan(maxLineSpan) }, key = "section_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.selectedCategory ?: "Receitas em Destaque",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${uiState.recipes.size} pratos",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

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

@Composable
private fun EditorialHeroBanner(
    isOnline: Boolean,
    isSyncing: Boolean,
    onSurpriseChef: () -> Unit,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp)
            .clip(MaterialTheme.shapes.extraLarge)
    ) {
        Image(
            painter = painterResource(R.drawable.img_hero_culinary),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1F120D).copy(alpha = 0.88f),
                            Color(0xFF3E180E).copy(alpha = 0.58f),
                            Color.Black.copy(alpha = 0.28f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = stringResource(R.string.hero_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = CircleShape
                ) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isSyncing,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = stringResource(R.string.sync_now),
                            tint = Color.White
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.hero_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = if (isOnline) {
                        stringResource(R.string.hero_subtitle)
                    } else {
                        "Você está offline — suas receitas salvas continuam prontas."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onSurpriseChef,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("surprise_chef_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.chef_surprise),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchRecipeBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = stringResource(R.string.search_placeholder),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
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
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_search_input")
    )
}

@Composable
private fun CategoryFilterRow(
    categories: List<Category>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item(key = "cat_all") {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text(text = stringResource(R.string.category_all)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("category_chip_all")
            )
        }
        items(
            items = categories,
            key = { it.id }
        ) { category ->
            val isSelected = selectedCategory == category.name
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category.name) },
                label = { Text(text = category.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("category_chip_${category.name}")
            )
        }
    }
}
