package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChefExpressDatabaseTest {

    private lateinit var database: ChefExpressDatabase
    private lateinit var recipeDao: RecipeDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ChefExpressDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        recipeDao = database.recipeDao()
        categoryDao = database.categoryDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun `insert seed recipes and filter by query category and favorite`() = runTest {
        categoryDao.upsertCategories(InitialRecipeSeed.categories)
        recipeDao.upsertRecipes(InitialRecipeSeed.recipes())

        val all = recipeDao.observeRecipes("", null).first()
        assertEquals(5, all.size)

        val pastaOnly = recipeDao.observeRecipes("", "Pasta").first()
        assertEquals(1, pastaOnly.size)
        assertEquals("Lasagne", pastaOnly.first().name)

        val garlicSearch = recipeDao.observeRecipes("Garlic", null).first()
        assertTrue(garlicSearch.isNotEmpty())

        recipeDao.updateFavorite("52844", true)
        val favorites = recipeDao.observeFavoriteRecipes().first()
        assertEquals(1, favorites.size)
        assertEquals("52844", favorites.first().id)
    }
}
