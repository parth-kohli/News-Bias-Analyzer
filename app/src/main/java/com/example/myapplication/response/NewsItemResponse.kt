package com.example.myapplication.response

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "news_articles")
@Serializable
data class NewsArticle(
    @PrimaryKey
    val id: String,
    val headline: String,
    val description: String,
    val article: String,
    val imageUrl: String,
    val date: String,
    val category: String,
    val centerBias: Float,
    val leftBias: Float,
    val rightBias: Float,
    val source: String
)

@Entity(tableName = "biased_articles")
@Serializable
data class BiasedArticles(
    @PrimaryKey
    val id: String,
    val headline: String,
    val article: String,
    val description: String,
    val imageUrl: String,
    val date: String,
    val category: String,
    val centerBias: Float,
    val leftBias: Float,
    val rightBias: Float,
    val source: String,
    val articleId: String
)