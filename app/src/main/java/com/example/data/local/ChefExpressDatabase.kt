package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RecipeEntity::class, CategoryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class ChefExpressDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "chef_express.db"
    }
}
