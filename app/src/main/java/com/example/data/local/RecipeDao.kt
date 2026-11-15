package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query(
        """
        SELECT * FROM recipes
        WHERE (:category IS NULL OR category = :category)
          AND (
            :query = ''
            OR name LIKE '%' || :query || '%'
            OR category LIKE '%' || :query || '%'
            OR area LIKE '%' || :query || '%'
            OR ingredientsJson LIKE '%' || :query || '%'
          )
        ORDER BY isFavorite DESC, name ASC
        """
    )
    fun observeRecipes(query: String, category: String?): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE isFavorite = 1 ORDER BY name ASC")
    fun observeFavoriteRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    fun observeRecipeById(id: String): Flow<RecipeEntity?>

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getRecipeById(id: String): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE id IN (:ids)")
    suspend fun getRecipesByIds(ids: List<String>): List<RecipeEntity>

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun getRecipeCount(): Int

    @Query("SELECT id FROM recipes ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomLocalRecipeId(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecipes(recipes: List<RecipeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecipe(recipe: RecipeEntity)

    @Query("UPDATE recipes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE recipes SET ingredientsJson = :ingredientsJson WHERE id = :id")
    suspend fun updateIngredients(id: String, ingredientsJson: String)
}

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)
}
