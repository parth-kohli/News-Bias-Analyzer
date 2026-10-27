package com.example.myapplication.data.room

import androidx.room.PrimaryKey

@androidx.room.Entity(tableName = "saved_articles")
data class SavedArticles (
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val headline:String,
    val description:String,
    val article: String,
    val imageUrl: String,
    val date: String,
    val category: String,
    val centerBias: Float,
    val leftBias: Float,
    val rightBias: Float,
    val source: String
)