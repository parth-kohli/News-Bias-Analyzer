package com.example.myapplication.data.ViewModels

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.CacheServerRepo
import com.example.myapplication.response.NewsArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(private val repository: CacheServerRepo): ViewModel() {
    val _articles = MutableStateFlow<List<NewsArticle>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    val articles: StateFlow<List<NewsArticle>> = _articles
    @RequiresApi(Build.VERSION_CODES.O)
    fun getTodayNews(){
        _articles.value=emptyList()
        viewModelScope.launch {
            _isLoading.value = true // <-- START loading
            try {
                val freshNews = repository.fetchTodaysNews()
                println(freshNews)
                _articles.value = freshNews
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to fetch news: ${e.message}")
            } finally {
                _isLoading.value = false
            }

        }
    }
}