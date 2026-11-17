package com.example.presentation.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AppError
import com.example.domain.model.Category
import com.example.domain.model.Recipe
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.NetworkMonitor
import com.example.domain.usecase.GetRandomChefRecipeUseCase
import com.example.domain.usecase.ObserveCategoriesUseCase
import com.example.domain.usecase.ObserveRecipesUseCase
import com.example.domain.usecase.SyncCatalogUseCase
import com.example.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val recipes: List<Recipe>,
        val categories: List<Category>,
        val selectedCategory: String?,
        val searchQuery: String,
        val isOnline: Boolean,
        val isSyncing: Boolean
    ) : HomeUiState

    data class Empty(
        val categories: List<Category>,
        val selectedCategory: String?,
        val searchQuery: String,
        val isOnline: Boolean,
        val isSyncing: Boolean
    ) : HomeUiState

    data class Error(
        val message: String,
        val isOnline: Boolean
    ) : HomeUiState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val observeRecipesUseCase: ObserveRecipesUseCase,
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val syncCatalogUseCase: SyncCatalogUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getRandomChefRecipeUseCase: GetRandomChefRecipeUseCase,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val isSyncing = MutableStateFlow(false)
    private val hasCompletedInitialLoad = MutableStateFlow(false)
    private val fatalErrorMessage = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private val recipesFlow = combine(
        searchQuery,
        selectedCategory
    ) { query, category -> query to category }
        .flatMapLatest { (query, category) ->
            observeRecipesUseCase(query, category)
        }

    private val categoriesFlow = observeCategoriesUseCase()

    val uiState: StateFlow<HomeUiState> = combine(
        recipesFlow,
        categoriesFlow,
        combine(searchQuery, selectedCategory) { q, c -> q to c },
        combine(networkMonitor.isOnline, isSyncing, hasCompletedInitialLoad, fatalErrorMessage) { online, syncing, loaded, err ->
            SyncMeta(online, syncing, loaded, err)
        }
    ) { recipes, categories, (query, category), meta ->
        when {
            !meta.hasLoaded && recipes.isEmpty() -> HomeUiState.Loading
            meta.errorMessage != null && recipes.isEmpty() -> HomeUiState.Error(
                message = meta.errorMessage,
                isOnline = meta.isOnline
            )
            recipes.isEmpty() -> HomeUiState.Empty(
                categories = categories,
                selectedCategory = category,
                searchQuery = query,
                isOnline = meta.isOnline,
                isSyncing = meta.isSyncing
            )
            else -> HomeUiState.Success(
                recipes = recipes,
                categories = categories,
                selectedCategory = category,
                searchQuery = query,
                isOnline = meta.isOnline,
                isSyncing = meta.isSyncing
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading
    )

    init {
        initialSync()
        observeDebouncedSearch()
    }

    private fun initialSync() {
        viewModelScope.launch {
            isSyncing.value = true
            fatalErrorMessage.value = null
            when (val outcome = syncCatalogUseCase(forceRefresh = false)) {
                is SyncOutcome.Success -> Unit
                is SyncOutcome.OfflineFallback -> Unit
                is SyncOutcome.Failure -> {
                    fatalErrorMessage.value = outcome.error.toReadableMessage()
                }
            }
            hasCompletedInitialLoad.value = true
            isSyncing.value = false
        }
    }

    private fun observeDebouncedSearch() {
        searchQuery
            .debounce(400)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.trim().length >= 2) {
                    isSyncing.value = true
                    syncCatalogUseCase.searchRemote(query)
                    isSyncing.value = false
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onCategorySelected(categoryName: String?) {
        val target = if (selectedCategory.value == categoryName) null else categoryName
        selectedCategory.value = target
        if (target != null) {
            viewModelScope.launch {
                isSyncing.value = true
                syncCatalogUseCase.syncCategory(target)
                isSyncing.value = false
            }
        }
    }

    fun onClearFilters() {
        searchQuery.value = ""
        selectedCategory.value = null
    }

    fun onRefresh() {
        viewModelScope.launch {
            isSyncing.value = true
            fatalErrorMessage.value = null
            when (val outcome = syncCatalogUseCase(forceRefresh = true)) {
                is SyncOutcome.Success -> {
                    _events.tryEmit(HomeEvent.ShowMessage("Catálogo atualizado (${outcome.syncedCount} receitas)"))
                }
                is SyncOutcome.OfflineFallback -> {
                    _events.tryEmit(HomeEvent.ShowMessage("Sem conexão. Exibindo receitas salvas no dispositivo."))
                }
                is SyncOutcome.Failure -> {
                    val msg = outcome.error.toReadableMessage()
                    fatalErrorMessage.value = msg
                    _events.tryEmit(HomeEvent.ShowMessage(msg))
                }
            }
            hasCompletedInitialLoad.value = true
            isSyncing.value = false
        }
    }

    fun onToggleFavorite(recipeId: String) {
        viewModelScope.launch {
            toggleFavoriteUseCase(recipeId)
        }
    }

    fun onSurpriseChefClick() {
        viewModelScope.launch {
            isSyncing.value = true
            val randomId = getRandomChefRecipeUseCase()
            isSyncing.value = false
            if (randomId != null) {
                _events.tryEmit(HomeEvent.NavigateToRecipe(randomId))
            } else {
                _events.tryEmit(HomeEvent.ShowMessage("Nenhuma receita disponível para sugestão."))
            }
        }
    }

    private fun AppError.toReadableMessage(): String = when (this) {
        AppError.NetworkUnavailable -> "Sem conexão com a internet. Verifique sua rede e tente novamente."
        AppError.Timeout -> "A conexão demorou muito para responder. Tente novamente."
        is AppError.ServerError -> "Erro no servidor de receitas ($code)."
        AppError.NotFound -> "Receita não encontrada."
        is AppError.Unknown -> message ?: "Não foi possível carregar as receitas."
    }

    private data class SyncMeta(
        val isOnline: Boolean,
        val isSyncing: Boolean,
        val hasLoaded: Boolean,
        val errorMessage: String?
    )
}

sealed interface HomeEvent {
    data class NavigateToRecipe(val recipeId: String) : HomeEvent
    data class ShowMessage(val message: String) : HomeEvent
}
