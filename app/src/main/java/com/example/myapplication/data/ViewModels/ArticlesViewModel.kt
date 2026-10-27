package com.example.myapplication.data.ViewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.CacheServerRepo
import com.example.myapplication.response.BiasedArticles
import com.example.myapplication.response.NewsArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ArticlesViewModel(private val repository: CacheServerRepo) : ViewModel() {
    private val _biasedArticles = MutableStateFlow<List<BiasedArticles>>(emptyList())
    val biasedArticles: StateFlow<List<BiasedArticles>> = _biasedArticles
    private val _article = MutableStateFlow<NewsArticle?>(null)
    val article: StateFlow<NewsArticle?> = _article
    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved
    private val _isLoadingArticle = MutableStateFlow(false)
    val isLoadingArticle: StateFlow<Boolean> = _isLoadingArticle
    private val _isLoadingBiased = MutableStateFlow(false)
    val isLoadingBiased: StateFlow<Boolean> = _isLoadingBiased
    private val _isUpdatingSaved = MutableStateFlow(false)
    val isUpdatingSaved: StateFlow<Boolean> = _isUpdatingSaved

    fun getArticle(id: String) {
        viewModelScope.launch {
            _isLoadingArticle.value = true
            try {
                val fetchedArticle = repository.fetchArticle(id)
                println(fetchedArticle)
                val savedList = repository.fetchSaved()

                if (fetchedArticle != null) {
                    _article.value = fetchedArticle
                    _isSaved.value = savedList.any { it.id == fetchedArticle.id }
                } else {
                    val savedArticle = savedList.find { it.id == id }
                    _article.value = savedArticle
                    _isSaved.value = savedArticle != null
                }

            } catch (e: Exception) {
                Log.e("ArticlesViewModel", "Error getting article: ${e.message}")
            } finally {
                _isLoadingArticle.value = false
            }
        }
    }

    fun getBiased(id: String) {
        _biasedArticles.value = emptyList()
        viewModelScope.launch {
            _isLoadingBiased.value = true
            try {
                _biasedArticles.value = repository.returnBiasedArticle(id)
            } catch (e: Exception) {
                Log.e("ArticlesViewModel", "Error getting biased articles: ${e.message}")
            } finally {
                _isLoadingBiased.value = false
            }
        }
    }

    fun addSaved(newsArticle: NewsArticle) {
        viewModelScope.launch {
            _isUpdatingSaved.value = true
            try {
                val result = repository.addSaved(newsArticle)
                _isSaved.value = result
            } catch (e: Exception) {
                Log.e("ArticlesViewModel", "Error adding saved article: ${e.message}")
            } finally {
                _isUpdatingSaved.value = false
            }
        }
    }

    fun removeSaved(newsArticle: NewsArticle) {
        viewModelScope.launch {
            _isUpdatingSaved.value = true
            try {
                val result = repository.deleteSaved(newsArticle)
                _isSaved.value = !result
            } catch (e: Exception) {
                Log.e("ArticlesViewModel", "Error removing saved article: ${e.message}")
            } finally {
                _isUpdatingSaved.value = false
            }
        }
    }
}