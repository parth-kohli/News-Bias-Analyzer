package com.example.myapplication.screens

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.myapplication.response.NewsArticle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.myapplication.LoadingScreen
import com.example.myapplication.data.ViewModels.ArticlesViewModel

import com.example.myapplication.response.BiasedArticles


@Composable
fun ArticleScreen(
    innerPadding: PaddingValues,
    articleid: String,
    articleViewModel: ArticlesViewModel,
    onBackPressed: () -> Unit
) {
    LaunchedEffect(Unit) {
        articleViewModel.getArticle(articleid)
    }
    val article by articleViewModel.article.collectAsState()
    val isLoading by articleViewModel.isLoadingArticle.collectAsState()

    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("Article") }
    LaunchedEffect(isLoading) {
        if (!isLoading && article!=null) articleViewModel.getBiased(articleid)
    }
    if (isLoading){
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingScreen()
        }
    }
    else {
        if (article==null){
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                Text("Article Not Found", color=Color.White)
            }

        }
        else {
            val biasedArticles = articleViewModel.biasedArticles.collectAsState()
            val isBiasedLoading = articleViewModel.isLoadingBiased.collectAsState()

            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                Spacer(Modifier.height(32.dp))
                ArticleTopBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onBackPressed = onBackPressed
                )
                when (selectedTab) {
                    "Article" -> ArticleContent(article = article!!, articleViewModel)
                    "Analytics" -> if (isBiasedLoading.value){ Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingScreen()}} else {AnalyticsContent(article!!, biasedArticles.value)}
                }
            }
        }
    }
}


@Composable
fun ArticleTopBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onBackPressed: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBackPressed) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription= "Back",
                tint = Color.White
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
        ) {
            TabButton(
                text = "Article",
                isSelected = selectedTab == "Article",
                onClick = { onTabSelected("Article") }
            )
            TabButton(
                text = "Analytics",
                isSelected = selectedTab == "Analytics",
                onClick = { onTabSelected("Analytics") }
            )
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected) Color.White.copy(alpha = 0.25f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(text, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}
@Composable
fun ArticleContent(article: NewsArticle, articleViewModel: ArticlesViewModel) {
    val saved = articleViewModel.isSaved.collectAsState()
    val updating = articleViewModel.isUpdatingSaved.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = "Article Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                )
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.End) {
                        IconButton({if (!updating.value) {if (saved.value) articleViewModel.removeSaved(article) else articleViewModel.addSaved(article)} }, Modifier.padding(5.dp).clip(CircleShape).background(Color.Black.copy(alpha=0.65f))){
                            if (updating.value) LoadingScreen()
                            else if (saved.value) Icon(Icons.Default.Bookmark, "", Modifier.size(30.dp), tint = Color.White)
                            else Icon(Icons.Outlined.BookmarkBorder, "", Modifier.size(30.dp))
                        }
                        val context= LocalContext.current


                        IconButton({
                            val deepLinkUrl = "newsapp://article/${article.id}"
                            val shareText = """
                            Check out this article:
                             *${article.headline}*
                             Open in our app: $deepLinkUrl""".trimIndent()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                putExtra(Intent.EXTRA_SUBJECT, article.headline)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Article")
                            context.startActivity(shareIntent)
                        }, Modifier.padding(5.dp).clip(CircleShape).background(Color.Black.copy(alpha=0.65f))){
                            Icon(Icons.Default.Share, "", Modifier.size(30.dp), tint = Color.White)
                        }

                }
            }
        }
        item {
            // Headline
            Text(
                text = article.headline,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            // Source and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = article.source,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = article.date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    fontStyle = FontStyle.Italic
                )
            }
        }
        item {
            // Full Article Text
            Text(
                text = article.article,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                lineHeight = 24.sp
            )
        }
    }
}

data class PieChartSlice(val label: String, val value: Float, val color: Color)
@Composable
fun AnalyticsContent(article: NewsArticle, biasedArticles: List<BiasedArticles>) {
    if (biasedArticles.isEmpty()){
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No Biased Articles", color= Color.White)
        }
    }
    else {
        var left = article.leftBias
        var right = article.rightBias
        var center = article.centerBias
        for (bias in biasedArticles) {
            left += bias.leftBias
            right += bias.rightBias
            center += bias.centerBias
        }
        left /= biasedArticles.size + 1
        right /= biasedArticles.size + 1
        center /= biasedArticles.size + 1
        val leftArticle = biasedArticles.maxBy { it.leftBias }
        val rightArticle = biasedArticles.maxBy { it.rightBias }
        for (i in biasedArticles){
            println("${i.source}, ${i.leftBias}, ${i.rightBias}")
        }
        val centerArticle = BiasedArticles(article.id, article.headline, article.article, article.description, article.imageUrl, article.source, article.date, article.centerBias, article.leftBias, article.leftBias, article.source, article.id)

        val pieChartData = listOf(
            PieChartSlice("Left", left, Color(0xFF3B82F6)),   // Blue
            PieChartSlice("Center", center, Color(0xFF8B5CF6)), // Purple
            PieChartSlice("Right", right, Color(0xFFEF4444))    // Red
        )

        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp).clip(RoundedCornerShape(24.dp)).background(
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            item() {
                Text(
                    "Bias Analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            item() {
                AnimatedPieChart(
                    data = pieChartData,
                    modifier = Modifier.size(200.dp) // Control the size of the chart here
                )
            }

            item() {
                ChartLegend(data = pieChartData)
            }
            item {
                // Add a divider for better visual separation
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color.White.copy(alpha = 0.2f)
                )
            }
            item {
                // 3. Add the new Perspective360View
                Perspective360View(
                    leftArticle = leftArticle,
                    centerArticle = centerArticle,
                    rightArticle = rightArticle
                )
            }

        }
    }
}

@Composable
fun Perspective360View(
    leftArticle: BiasedArticles?,
    centerArticle: BiasedArticles?,
    rightArticle: BiasedArticles?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "360° View",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Display a card for each perspective if an article is found
        leftArticle?.let {
            PerspectiveCard(article = it, biasColor = Color(0xFF3B82F6)) // Blue
        }
        centerArticle?.let {
            PerspectiveCard(article = it, biasColor = Color(0xFF8B5CF6)) // Purple
        }
        rightArticle?.let {
            PerspectiveCard(article = it, biasColor = Color(0xFFEF4444)) // Red
        }
    }
}

/**
 * A single glass card that displays the description and source for one biased perspective.
 */
@Composable
fun PerspectiveCard(article: BiasedArticles, biasColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(biasColor.copy(alpha = 0.5f))
            .border(1.dp, biasColor.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Source of the biased article
        Text(
            text = article.source,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        // Description from the biased article
        Text(
            text = "\"${article.description}\"",
            color = Color.White,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp
        )
    }
}

/**
 * The main composable that draws and animates the pie chart slices.
 */
@Composable
fun AnimatedPieChart(
    data: List<PieChartSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 80f
) {
    // Animate the total progress of the chart from 0f to 1f
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val totalValue = data.sumOf { it.value.toDouble() }.toFloat()

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f // Start from the top

            data.forEach { slice ->
                val sweepAngle = (slice.value / totalValue) * 360f * animationProgress.value
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
            }
        }
    }
}
/**
 * Displays a legend for the pie chart with color indicators and percentages.
 */
@Composable
fun ChartLegend(data: List<PieChartSlice>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        data.forEach { slice ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(slice.color, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(slice.label, color = Color.White.copy(alpha = 0.8f))
                }
                Text(
                    text = "${(slice.value * 100).toInt()}%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun BiasIndicator(label: String, value: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White.copy(alpha = 0.8f))
            Text("${(value * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value)
                    .height(8.dp)
                    .background(color, RoundedCornerShape(50))
            )
        }
    }
}