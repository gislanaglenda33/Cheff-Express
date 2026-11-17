package com.example.domain.model

data class IngredientItem(
    val name: String,
    val measure: String,
    val isChecked: Boolean = false
)

data class Recipe(
    val id: String,
    val name: String,
    val category: String,
    val area: String,
    val instructions: String,
    val thumbnailUrl: String,
    val youtubeUrl: String? = null,
    val sourceUrl: String? = null,
    val tags: List<String> = emptyList(),
    val ingredients: List<IngredientItem> = emptyList(),
    val isFavorite: Boolean = false,
    val isDetailLoaded: Boolean = false,
    val lastSyncedAt: Long = 0L
) {
    val instructionSteps: List<String>
        get() = instructions
            .replace("\r\n", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { step ->
                step.replace(Regex("^(\\d+[.)]\\s*|STEP\\s+\\d+[:.\\s]*)", RegexOption.IGNORE_CASE), "").trim()
            }
            .filter { it.isNotBlank() }
}

data class Category(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val description: String
)

sealed class AppError {
    data object NetworkUnavailable : AppError()
    data object Timeout : AppError()
    data class ServerError(val code: Int, val message: String?) : AppError()
    data object NotFound : AppError()
    data class Unknown(val message: String?) : AppError()
}

sealed class SyncOutcome {
    data class Success(val syncedCount: Int) : SyncOutcome()
    data object OfflineFallback : SyncOutcome()
    data class Failure(val error: AppError) : SyncOutcome()
}
