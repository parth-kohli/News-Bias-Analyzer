package com.example.myapplication.screens
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.LoadingScreen
import com.example.myapplication.data.ViewModels.HomeViewModel
import com.example.myapplication.response.NewsArticle
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.calculateCurrentOffsetForPage
import com.google.accompanist.pager.rememberPagerState
import kotlin.math.absoluteValue
import kotlin.math.min

@RequiresApi(Build.VERSION_CODES.O)
@OptIn
@Composable
fun HomeScreen(innerPadding: PaddingValues, homeViewModel: HomeViewModel, onStart: MutableState<Boolean>, onArticleClick: (String)->Unit){
    val newsArticles by homeViewModel.articles.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    LaunchedEffect(Unit) {
        if (onStart.value) {
            onStart.value=false
            homeViewModel.getTodayNews()
        }
    }

        Column(modifier= Modifier.fillMaxSize().padding(innerPadding), horizontalAlignment = Alignment.CenterHorizontally) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(), contentAlignment = Alignment.Center
                )
                {
                    LoadingScreen()
                }
            }
            else if (newsArticles.isEmpty()){
                Box(
                    modifier = Modifier
                        .fillMaxSize(), contentAlignment = Alignment.Center
                ){
                    Text(text = "No news to show\nThe server might be down")
                }
            }
            else {
                LazyColumn(
                    Modifier.fillMaxWidth(), state = rememberLazyListState()
                ) {
                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                        Text("TOP PICKS: ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(16.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        StackedGlassCarousel(newsArticles.subList(0, min(5, newsArticles.size))) {
                            onArticleClick(it)
                        }
                        Spacer(modifier = Modifier.height(25.dp))
                    }
                    item{
                        Text("READ MORE: ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White,  modifier = Modifier.padding(16.dp))
                    }
                    items(newsArticles.size) { index ->
                        val newsArticle = newsArticles[index]
                        NewsBlock(newsArticle, { onArticleClick(it) })
                    }
                }
            }
        }


}
@Composable
fun NewsBlock(newsArticle: NewsArticle, onArticleClick: (String) -> Unit, saved: Boolean = false){
    Spacer(modifier = Modifier.height(15.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = {onArticleClick(newsArticle.id)})
            .clip(RoundedCornerShape(24.dp)) // Slightly more rounded corners
            // 1. A more dynamic diagonal background gradient
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.1f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                )
            )
            // 2. A gradient border to simulate a glossy edge
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.15f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                ),
                shape = RoundedCornerShape(24.dp)
            )
            // 3. A more subtle, diffused shadow
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0x33FFFFFF), // A white glow from the top
                ambientColor = Color(0x33000000) // A soft black shadow underneath
            )
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = {onArticleClick(newsArticle.id)}), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = newsArticle.imageUrl,
                contentDescription = "something",
                modifier = Modifier.fillMaxWidth(0.4f).aspectRatio(1f),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.padding(start = 16.dp).clickable(onClick = {onArticleClick(newsArticle.id)}),
                verticalArrangement = Arrangement.Center,
            ) {
                // 4. Refined typography for better hierarchy
                Text(
                    text = newsArticle.headline,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = newsArticle.description, // Corrected spelling
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines= 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    // Muted for secondary info
                )
            }
        }
        if (saved){
            Row(Modifier.fillMaxWidth().padding()) {
                Icon(Icons.Default.Bookmark, "", tint = Color.White)
            }
        }
    }
}
@OptIn(ExperimentalPagerApi::class)
@Composable
fun StackedGlassCarousel(items: List<NewsArticle>, onArticleClick: (String) -> Unit) {
    val pagerState = rememberPagerState()

    HorizontalPager(
        count = items.size,
        state = pagerState,
        // Provide generous horizontal padding to see the side items
        contentPadding = PaddingValues(horizontal = 64.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp) // A bit taller for the 3D effect
    ) { page ->
        val item = items[page]

        // 1. Calculate the offset from the center page
        val pageOffset = calculateCurrentOffsetForPage(page).absoluteValue

        // Box containing the entire glass card
        Box(
            modifier = Modifier
                .zIndex(1f - pageOffset) // 2. Higher zIndex for center item
                .graphicsLayer {
                    // 3. Apply 3D transformations
                    // Scale the item based on its distance from the center
                    val scale = lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
                    scaleX = scale
                    scaleY = scale

                    // Fade the item based on its distance
                    alpha = lerp(start = 0.5f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
                }
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.1f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset.Infinite
                    )
                )
                .clickable(onClick = {onArticleClick(item.id)})
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            // Content (Image and Text) remains the same
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {onArticleClick(item.id)})
                    .height(380.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {onArticleClick(item.id)})
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = item.headline,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = item.description,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}