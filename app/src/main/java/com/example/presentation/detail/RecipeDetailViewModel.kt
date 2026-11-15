package com.example.presentation.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AppError
import com.example.domain.model.Recipe
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.NetworkMonitor
import com.example.domain.usecase.ObserveRecipeDetailUseCase
import com.example.domain.usecase.ToggleFavoriteUseCase
import com.example.domain.usecase.ToggleIngredientCheckUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
sealed interface RecipeDetailUiState {
    data object Loading : RecipeDetailUiState
    data class Success(
        val recipe: Recipe,
        val isOnline: Boolean,
        val isHydratingDetail: Boolean
    ) : RecipeDetailUiState

    data class Error(
        val message: String,
        val isOnline: Boolean
    ) : RecipeDetailUiState
}

@Immutable
data class KitchenTimerState(
    val totalSeconds: Int = 300,
    val remainingSeconds: Int = 300,
    val isRunning: Boolean = false
) {
    val formattedTime: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return "%02d:%02d".format(mins, secs)
        }

    val isFinished: Boolean
        get() = remainingSeconds == 0 && totalSeconds > 0
}

class RecipeDetailViewModel(
    private val recipeId: String,
    private val observeRecipeDetailUseCase: ObserveRecipeDetailUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleIngredientCheckUseCase: ToggleIngredientCheckUseCase,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    private val isHydratingDetail = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val _timerState = MutableStateFlow(KitchenTimerState())
    val timerState: StateFlow<KitchenTimerState> = _timerState.asStateFlow()
    private var timerJob: Job? = null

    val uiState: StateFlow<RecipeDetailUiState> = combine(
        observeRecipeDetailUseCase(recipeId),
        networkMonitor.isOnline,
        isHydratingDetail,
        errorMessage
    ) { recipe, isOnline, hydrating, error ->
        when {
            recipe != null -> RecipeDetailUiState.Success(
                recipe = recipe,
                isOnline = isOnline,
                isHydratingDetail = hydrating
            )
            error != null -> RecipeDetailUiState.Error(
                message = error,
                isOnline = isOnline
            )
            else -> RecipeDetailUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecipeDetailUiState.Loading
    )

    init {
        loadDetailIfNeeded()
    }

    fun loadDetailIfNeeded() {
        viewModelScope.launch {
            isHydratingDetail.value = true
            errorMessage.value = null
            when (val outcome = observeRecipeDetailUseCase.refreshIfNeeded(recipeId)) {
                is SyncOutcome.Success -> Unit
                is SyncOutcome.OfflineFallback -> Unit
                is SyncOutcome.Failure -> {
                    errorMessage.value = when (outcome.error) {
                        AppError.NetworkUnavailable -> "Receita ainda não está em cache e você está offline."
                        AppError.Timeout -> "Tempo esgotado ao buscar os detalhes da receita."
                        AppError.NotFound -> "Receita não encontrada."
                        is AppError.ServerError -> "Erro no servidor (${outcome.error.code})."
                        is AppError.Unknown -> outcome.error.message ?: "Erro inesperado ao carregar receita."
                    }
                }
            }
            isHydratingDetail.value = false
        }
    }

    fun onToggleFavorite() {
        viewModelScope.launch {
            toggleFavoriteUseCase(recipeId)
        }
    }

    fun onToggleIngredient(index: Int) {
        viewModelScope.launch {
            toggleIngredientCheckUseCase(recipeId, index)
        }
    }

    fun setTimerPresetMinutes(minutes: Int) {
        timerJob?.cancel()
        val seconds = (minutes * 60).coerceAtLeast(60)
        _timerState.value = KitchenTimerState(
            totalSeconds = seconds,
            remainingSeconds = seconds,
            isRunning = false
        )
    }

    fun addMinuteToTimer() {
        _timerState.update { current ->
            val updatedRemaining = current.remainingSeconds + 60
            val updatedTotal = maxOf(current.totalSeconds, updatedRemaining)
            current.copy(totalSeconds = updatedTotal, remainingSeconds = updatedRemaining)
        }
    }

    fun toggleTimerRunning() {
        val current = _timerState.value
        if (current.isRunning) {
            timerJob?.cancel()
            _timerState.update { it.copy(isRunning = false) }
        } else {
            val startFrom = if (current.remainingSeconds <= 0) current.totalSeconds else current.remainingSeconds
            _timerState.update { it.copy(remainingSeconds = startFrom, isRunning = true) }
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (_timerState.value.remainingSeconds > 0 && _timerState.value.isRunning) {
                    delay(1_000L)
                    _timerState.update { state ->
                        val next = (state.remainingSeconds - 1).coerceAtLeast(0)
                        state.copy(
                            remainingSeconds = next,
                            isRunning = next > 0 && state.isRunning
                        )
                    }
                }
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerState.update { state ->
            state.copy(remainingSeconds = state.totalSeconds, isRunning = false)
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
