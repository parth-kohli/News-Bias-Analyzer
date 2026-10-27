package com.example.myapplication.data

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Query
import com.example.myapplication.data.room.BiasedArticleDao
import com.example.myapplication.data.room.NewsArticleDao
import com.example.myapplication.data.room.SavedArticleDao
import com.example.myapplication.data.room.SavedArticles
import com.example.myapplication.data.room.SearchedItems
import com.example.myapplication.data.room.SearchedItemsDao

import com.example.myapplication.data.server.NewsApiService
import com.example.myapplication.response.BiasedArticles
import com.example.myapplication.response.NewsArticle
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate

class CacheServerRepo(
    private val api: NewsApiService,
    private val newsdao: NewsArticleDao,
    private val biaseddao: BiasedArticleDao,
    private val saveddao: SavedArticleDao,
    private val searcheddao: SearchedItemsDao
) {
    suspend fun fetchBiasedArticle( id: String){
        val biased = api.getBiasedNews(id)
        biaseddao.insertAll(biased)
    }
    suspend fun returnBiasedArticle( id: String): List<BiasedArticles> = coroutineScope{
        try {
            val biased = api.getBiasedNews(id)
            biaseddao.insertAll(biased)
            return@coroutineScope biased
        }
        catch (e: Exception){
            val biased = biaseddao.getBiasedArticles(id)
            return@coroutineScope biased
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun fetchTodaysNews(): List<NewsArticle> = coroutineScope {
        try {
            val articles = api.getTodaysNews()
            newsdao.insertAll(articles)
            articles.map { article ->
                async {
                    fetchBiasedArticle(article.id)
                }
            }.awaitAll()
            return@coroutineScope articles
        }
        catch (e: Exception) {
            val articles = newsdao.searchToday(LocalDate.now().minusDays(1).toString())
            return@coroutineScope articles
        }
    }
    suspend fun searchNewsArticles( query: String, page: Int): List<NewsArticle> = coroutineScope{
        try {
            val result = api.searchNews(query, page)
            newsdao.insertAll(result)
            result.map { article ->
                async {
                    fetchBiasedArticle(article.id)
                }
            }.awaitAll()
            return@coroutineScope result
        }
        catch (e: Exception){
            val result=newsdao.searchDatabase(query)
            return@coroutineScope result
        }
    }
    suspend fun fetchByCategory( query: String): List<NewsArticle> = coroutineScope{
        try {
            val result = api.getByCategory(query)
            newsdao.insertAll(result)
            result.map { article ->
                async {
                    fetchBiasedArticle(article.id)
                }
            }.awaitAll()
            return@coroutineScope result
        }
        catch (e: Exception){
            val result=newsdao.searchCategory(query)
            return@coroutineScope result
        }
    }

    suspend fun fetchSaved(): List<NewsArticle>  {
        return saveddao.getAll()
    }

    suspend fun deleteSaved(newsArticle: NewsArticle): Boolean {
        try {
            val savedArticle = SavedArticles(
                id = newsArticle.id,
                headline = newsArticle.headline,
                description = newsArticle.description,
                article = newsArticle.article,
                imageUrl = newsArticle.imageUrl,
                date = newsArticle.date,
                category = newsArticle.category,
                centerBias = newsArticle.centerBias,
                leftBias = newsArticle.leftBias,
                rightBias = newsArticle.rightBias,
                source = newsArticle.source
            )
            saveddao.delete(savedArticle)
            return true
        }
        catch (e: Exception){
            return false
        }
    }

    suspend fun addSaved(newsArticle: NewsArticle): Boolean{
        try {
            val savedArticle = SavedArticles(
                id = newsArticle.id,
                headline = newsArticle.headline,
                description = newsArticle.description,
                article = newsArticle.article,
                imageUrl = newsArticle.imageUrl,
                date = newsArticle.date,
                category = newsArticle.category,
                centerBias = newsArticle.centerBias,
                leftBias = newsArticle.leftBias,
                rightBias = newsArticle.rightBias,
                source = newsArticle.source
            )
            saveddao.insert(savedArticle)
            return true
        }
        catch (e: Exception){
            return false
        }
    }

    suspend fun fetchArticle(id: String): NewsArticle?{
        return newsdao.fetchArticle(id)
    }
    suspend fun getRecentSearches(): List<SearchedItems> {
        return searcheddao.getAllSearchItemsList()
    }
    suspend fun addRecentSearch(searchTerm: String) {
        // 1. Get all current searches to check for duplicates
        val allSearches = searcheddao.getAllSearchItemsList()
        val existingItem = allSearches.find { it.search.equals(searchTerm, ignoreCase = true) }

        if (existingItem != null) {
            searcheddao.deleteSearchItem(existingItem)
        } else {
            val currentCount = searcheddao.getCount()
            if (currentCount >= 10) {
    
                searcheddao.deleteOldestItem()
            }
        }
        val newSearchItem = SearchedItems(
            id = System.currentTimeMillis(),
            search = searchTerm
        )
        searcheddao.insertSearchItem(newSearchItem)
    }
    
    suspend fun deleteRecentSearch(item: SearchedItems) {
        searcheddao.deleteSearchItem(item)
    }
    suspend fun clearAllRecentSearches() {
        searcheddao.clearAll()
    }

}