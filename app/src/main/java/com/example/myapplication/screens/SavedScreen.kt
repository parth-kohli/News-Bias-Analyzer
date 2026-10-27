package com.example.myapplication.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.LoadingScreen
import com.example.myapplication.data.ViewModels.SavedViewModel

import com.example.myapplication.response.NewsArticle

@Composable
fun SavedScreen(
    innerPadding: PaddingValues,
    savedViewModel: SavedViewModel,
    onArticleClick: (String) -> Unit
) {
    val savedArticles by savedViewModel.articles.collectAsState()
    val isLoading by savedViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        savedViewModel.getSaved()
    }

    if (isLoading){
        Box(
            modifier = Modifier
                .fillMaxSize(), contentAlignment = Alignment.Center
        )
        {
            LoadingScreen()
        }
    }
    else{
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(Modifier.height(12.dp))

            Text(
                text = "Saved Articles",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))

            if (savedArticles.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "No saved articles",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "You have no saved articles.",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                // Display the list of saved articles
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(savedArticles) { article ->
                        // Use a modified, clickable NewsBlock
                        NewsBlock(
                            newsArticle = article,
                            onArticleClick = { onArticleClick(article.id) },
                            true
                        )
                    }
                }
            }
        }
    }
}
