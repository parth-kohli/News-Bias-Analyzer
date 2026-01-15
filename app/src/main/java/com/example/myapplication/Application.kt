package com.example.myapplication
import android.app.Application
import androidx.room.Room
import com.example.myapplication.data.CacheServerRepo
import com.example.myapplication.data.ViewModelFactory.ViewModelFactory
import com.example.myapplication.data.room.AppDataBase
import com.example.myapplication.data.room.BiasedArticleDao
import com.example.myapplication.data.room.NewsArticleDao
import com.example.myapplication.data.room.SavedArticleDao
import com.example.myapplication.data.room.SearchedItemsDao
// <-- You need to create this file
import com.example.myapplication.data.server.NewsApiService // <-- You need to create this file
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyApplication : Application() {
    private val BASE_URL = "https://58d53f04c1fb.ngrok-free.app/"
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val newsApiService: NewsApiService by lazy {
        retrofit.create(NewsApiService::class.java)
    }

    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDataBase::class.java,
            "app_database"
        ).build()
    }

    val newsArticleDao: NewsArticleDao by lazy {
        database.newsArticleDao() // <-- Assumes your AppDatabase has this function
    }
    val biasedArticleDao: BiasedArticleDao by lazy {
        database.biasedArticleDao() // <-- Assumes your AppDatabase has this function
    }
    val savedArticleDao: SavedArticleDao by lazy {
        database.savedArticleDao() // <-- Assumes your AppDatabase has this function
    }
    val searchedItemsDao: SearchedItemsDao by lazy {
        database.searchedItemsDao() // <-- Assumes your AppDatabase has this function
    }

    val repository by lazy {
        CacheServerRepo(newsApiService, newsArticleDao, biasedArticleDao, savedArticleDao, searchedItemsDao)
    }

    val viewModelFactory by lazy {
        ViewModelFactory(repository)
    }
}