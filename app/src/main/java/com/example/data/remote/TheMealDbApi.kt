package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface TheMealDbApi {

    @GET("categories.php")
    suspend fun getCategories(): CategoriesResponseDto

    @GET("search.php")
    suspend fun searchMeals(@Query("s") query: String): MealsResponseDto

    @GET("filter.php")
    suspend fun filterByCategory(@Query("c") category: String): MealsResponseDto

    @GET("lookup.php")
    suspend fun getMealById(@Query("i") id: String): MealsResponseDto

    @GET("random.php")
    suspend fun getRandomMeal(): MealsResponseDto

    companion object {
        const val BASE_URL = "https://www.themealdb.com/api/json/v1/1/"
    }
}
