package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class IngredientLocalModel(
    val name: String,
    val measure: String,
    val isChecked: Boolean = false
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val area: String,
    val instructions: String,
    val thumbnailUrl: String,
    val youtubeUrl: String?,
    val sourceUrl: String?,
    val tagsCsv: String,
    val ingredientsJson: String,
    val isFavorite: Boolean,
    val isDetailLoaded: Boolean,
    val lastSyncedAt: Long
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val thumbnailUrl: String,
    val description: String
)
