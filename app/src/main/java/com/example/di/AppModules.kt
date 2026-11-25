package com.example.di

import androidx.room.Room
import com.example.data.local.ChefExpressDatabase
import com.example.data.mapper.RecipeTextTranslator
import com.example.data.mapper.RemotePtBrTranslator
import com.example.data.network.AndroidNetworkMonitor
import com.example.data.remote.TheMealDbApi
import com.example.data.repository.OfflineFirstRecipeRepositoryImpl
import com.example.domain.repository.NetworkMonitor
import com.example.domain.repository.RecipeRepository
import com.example.domain.usecase.GetRandomChefRecipeUseCase
import com.example.domain.usecase.ObserveCategoriesUseCase
import com.example.domain.usecase.ObserveFavoriteRecipesUseCase
import com.example.domain.usecase.ObserveRecipeDetailUseCase
import com.example.domain.usecase.ObserveRecipesUseCase
import com.example.domain.usecase.SyncCatalogUseCase
import com.example.domain.usecase.ToggleFavoriteUseCase
import com.example.domain.usecase.ToggleIngredientCheckUseCase
import com.example.presentation.detail.RecipeDetailViewModel
import com.example.presentation.favorites.FavoritesViewModel
import com.example.presentation.home.HomeViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val dataModule = module {
    single {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    single {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(TheMealDbApi.BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
            .create(TheMealDbApi::class.java)
    }

    single {
        Room.databaseBuilder(
            androidContext(),
            ChefExpressDatabase::class.java,
            ChefExpressDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    single { get<ChefExpressDatabase>().recipeDao() }
    single { get<ChefExpressDatabase>().categoryDao() }

    single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
    single<RecipeTextTranslator> { RemotePtBrTranslator(okHttpClient = get()) }

    single<RecipeRepository> {
        OfflineFirstRecipeRepositoryImpl(
            api = get(),
            recipeDao = get(),
            categoryDao = get(),
            networkMonitor = get(),
            textTranslator = get()
        )
    }
}

val domainModule = module {
    factory { ObserveRecipesUseCase(get()) }
    factory { ObserveCategoriesUseCase(get()) }
    factory { ObserveFavoriteRecipesUseCase(get()) }
    factory { ObserveRecipeDetailUseCase(get()) }
    factory { SyncCatalogUseCase(get()) }
    factory { ToggleFavoriteUseCase(get()) }
    factory { ToggleIngredientCheckUseCase(get()) }
    factory { GetRandomChefRecipeUseCase(get()) }
}

val presentationModule = module {
    viewModel {
        HomeViewModel(
            observeRecipesUseCase = get(),
            observeCategoriesUseCase = get(),
            syncCatalogUseCase = get(),
            toggleFavoriteUseCase = get(),
            getRandomChefRecipeUseCase = get(),
            networkMonitor = get()
        )
    }
    viewModel {
        FavoritesViewModel(
            observeFavoriteRecipesUseCase = get(),
            toggleFavoriteUseCase = get(),
            networkMonitor = get()
        )
    }
    viewModel { (recipeId: String) ->
        RecipeDetailViewModel(
            recipeId = recipeId,
            observeRecipeDetailUseCase = get(),
            toggleFavoriteUseCase = get(),
            toggleIngredientCheckUseCase = get(),
            networkMonitor = get()
        )
    }
}

val appModules = listOf(dataModule, domainModule, presentationModule)
