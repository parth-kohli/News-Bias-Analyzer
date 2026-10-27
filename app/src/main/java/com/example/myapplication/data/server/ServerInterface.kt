package com.example.myapplication.data.server

import com.example.myapplication.response.BiasedArticles
import com.example.myapplication.response.NewsArticle
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NewsApiService {

    @GET("api/news")
    suspend fun getAllNews(): List<NewsArticle>

    @GET("api/news/{id}/biased")
    suspend fun getBiasedNews(@Path("id") id: String): List<BiasedArticles>

    @GET("api/news/search")
    suspend fun searchNews(@Query("query") query: String, @Query("page")page: Int): List<NewsArticle>

    @GET("api/news/category")
    suspend fun getByCategory(@Query("name") category: String): List<NewsArticle>

    @GET("api/news/today")
    suspend fun getTodaysNews(): List<NewsArticle>
}
