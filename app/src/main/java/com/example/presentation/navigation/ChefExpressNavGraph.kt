package com.example.presentation.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.presentation.detail.RecipeDetailScreen
import com.example.presentation.detail.RecipeDetailViewModel
import com.example.presentation.favorites.FavoritesScreen
import com.example.presentation.favorites.FavoritesViewModel
import com.example.presentation.home.HomeEvent
import com.example.presentation.home.HomeScreen
import com.example.presentation.home.HomeViewModel
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data object DiscoverRoute

@Serializable
data object FavoritesRoute

@Serializable
data class RecipeDetailRoute(val recipeId: String)

private data class TopLevelNavItem(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelDestinations = listOf(
    TopLevelNavItem(
        route = DiscoverRoute,
        labelRes = R.string.nav_discover,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_discover"
    ),
    TopLevelNavItem(
        route = FavoritesRoute,
        labelRes = R.string.nav_favorites,
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        testTag = "nav_favorites"
    )
)

@Composable
fun ChefExpressAppRoot(
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevel = currentDestination?.let { dest ->
        dest.hasRoute<DiscoverRoute>() || dest.hasRoute<FavoritesRoute>()
    } ?: true
    val useNavigationRail = widthSizeClass != WindowWidthSizeClass.Compact
    val snackbarHostState = remember { SnackbarHostState() }

    val navigateToTopLevel: (Any) -> Unit = remember(navController) {
        { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = if (isTopLevel) WindowInsets.safeDrawing else WindowInsets(0),
        bottomBar = {
            if (isTopLevel && !useNavigationRail) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    topLevelDestinations.forEach { item ->
                        val selected = when (item.route) {
                            DiscoverRoute -> currentDestination?.hasRoute<DiscoverRoute>() == true
                            FavoritesRoute -> currentDestination?.hasRoute<FavoritesRoute>() == true
                            else -> false
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTopLevel(item.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.labelRes)
                                )
                            },
                            label = { Text(text = stringResource(item.labelRes)) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isTopLevel && useNavigationRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.statusBarsPadding()
                ) {
                    topLevelDestinations.forEach { item ->
                        val selected = when (item.route) {
                            DiscoverRoute -> currentDestination?.hasRoute<DiscoverRoute>() == true
                            FavoritesRoute -> currentDestination?.hasRoute<FavoritesRoute>() == true
                            else -> false
                        }
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTopLevel(item.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.labelRes)
                                )
                            },
                            label = { Text(text = stringResource(item.labelRes)) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = DiscoverRoute,
                modifier = Modifier.weight(1f)
            ) {
                composable<DiscoverRoute> {
                    val viewModel: HomeViewModel = koinViewModel()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    LaunchedEffect(viewModel) {
                        viewModel.events.collect { event ->
                            when (event) {
                                is HomeEvent.NavigateToRecipe -> {
                                    navController.navigate(RecipeDetailRoute(event.recipeId))
                                }
                                is HomeEvent.ShowMessage -> {
                                    snackbarHostState.showSnackbar(event.message)
                                }
                            }
                        }
                    }

                    HomeScreen(
                        uiState = uiState,
                        onSearchQueryChange = viewModel::onSearchQueryChanged,
                        onCategorySelected = viewModel::onCategorySelected,
                        onClearFilters = viewModel::onClearFilters,
                        onRefresh = viewModel::onRefresh,
                        onSurpriseChef = viewModel::onSurpriseChefClick,
                        onRecipeClick = { recipeId ->
                            navController.navigate(RecipeDetailRoute(recipeId))
                        },
                        onToggleFavorite = viewModel::onToggleFavorite
                    )
                }

                composable<FavoritesRoute> {
                    val viewModel: FavoritesViewModel = koinViewModel()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    FavoritesScreen(
                        uiState = uiState,
                        onFilterQueryChange = viewModel::onFilterQueryChanged,
                        onRecipeClick = { recipeId ->
                            navController.navigate(RecipeDetailRoute(recipeId))
                        },
                        onToggleFavorite = viewModel::onToggleFavorite,
                        onExploreClick = { navigateToTopLevel(DiscoverRoute) }
                    )
                }

                composable<RecipeDetailRoute> { backStack ->
                    val route: RecipeDetailRoute = backStack.toRoute()
                    val viewModel: RecipeDetailViewModel = koinViewModel(
                        key = route.recipeId,
                        parameters = { parametersOf(route.recipeId) }
                    )
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val timerState by viewModel.timerState.collectAsStateWithLifecycle()

                    RecipeDetailScreen(
                        uiState = uiState,
                        timerState = timerState,
                        onBackClick = { navController.popBackStack() },
                        onToggleFavorite = viewModel::onToggleFavorite,
                        onToggleIngredient = viewModel::onToggleIngredient,
                        onRetry = viewModel::loadDetailIfNeeded,
                        onSetTimerPreset = viewModel::setTimerPresetMinutes,
                        onAddMinute = viewModel::addMinuteToTimer,
                        onToggleTimer = viewModel::toggleTimerRunning,
                        onResetTimer = viewModel::resetTimer
                    )
                }
            }
        }
    }
}
