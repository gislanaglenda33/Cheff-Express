package com.example.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.domain.model.Category
import com.example.domain.model.IngredientItem
import com.example.domain.model.Recipe
import com.example.presentation.detail.KitchenTimerState
import com.example.presentation.detail.RecipeDetailScreen
import com.example.presentation.detail.RecipeDetailUiState
import com.example.presentation.home.HomeScreen
import com.example.presentation.home.HomeUiState
import com.example.ui.theme.ChefExpressTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChefExpressUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `HomeScreen shows offline banner and recipe cards when offline`() {
        val sampleRecipe = Recipe(
            id = "52771",
            name = "Spicy Arrabiata Penne",
            category = "Vegetarian",
            area = "Italian",
            instructions = "Boil pasta.",
            thumbnailUrl = "",
            ingredients = listOf(IngredientItem("Penne", "400g")),
            isFavorite = false,
            isDetailLoaded = true
        )
        var clickedRecipeId: String? = null

        composeTestRule.setContent {
            ChefExpressTheme {
                HomeScreen(
                    uiState = HomeUiState.Success(
                        recipes = listOf(sampleRecipe),
                        categories = listOf(Category("1", "Vegetarian", "", "")),
                        selectedCategory = null,
                        searchQuery = "",
                        isOnline = false,
                        isSyncing = false
                    ),
                    onSearchQueryChange = {},
                    onCategorySelected = {},
                    onClearFilters = {},
                    onRefresh = {},
                    onSurpriseChef = {},
                    onRecipeClick = { clickedRecipeId = it },
                    onToggleFavorite = {}
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("offline_status_banner").assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_recipe_grid")
            .performScrollToNode(hasTestTag("recipe_card_52771"))
        composeTestRule.onNodeWithText("Spicy Arrabiata Penne").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recipe_card_52771").performClick()
        assertEquals("52771", clickedRecipeId)
    }

    @Test
    fun `RecipeDetailScreen renders ingredients checklist and triggers toggle`() {
        val recipe = Recipe(
            id = "52771",
            name = "Spicy Arrabiata Penne",
            category = "Vegetarian",
            area = "Italian",
            instructions = "Boil water.\nCook pasta.",
            thumbnailUrl = "",
            ingredients = listOf(
                IngredientItem("Penne Rigate", "400g", isChecked = false),
                IngredientItem("Olive Oil", "3 tbsp", isChecked = true)
            ),
            isFavorite = true,
            isDetailLoaded = true
        )
        var toggledIndex = -1
        var timerClicked = false

        composeTestRule.setContent {
            ChefExpressTheme {
                RecipeDetailScreen(
                    uiState = RecipeDetailUiState.Success(
                        recipe = recipe,
                        isOnline = false,
                        isHydratingDetail = false
                    ),
                    timerState = KitchenTimerState(totalSeconds = 300, remainingSeconds = 300, isRunning = false),
                    onBackClick = {},
                    onToggleFavorite = {},
                    onToggleIngredient = { toggledIndex = it },
                    onRetry = {},
                    onSetTimerPreset = {},
                    onAddMinute = {},
                    onToggleTimer = { timerClicked = true },
                    onResetTimer = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("recipe_detail_list")
            .performScrollToNode(hasTestTag("timer_toggle_button"))
        composeTestRule.onNodeWithText("05:00").assertIsDisplayed()
        composeTestRule.onNodeWithTag("timer_toggle_button").performClick()
        assertTrue(timerClicked)

        composeTestRule.onNodeWithTag("recipe_detail_list")
            .performScrollToNode(hasTestTag("ingredient_row_0"))
        composeTestRule.onNodeWithTag("ingredient_row_0").performClick()
        assertEquals(0, toggledIndex)
    }
}
