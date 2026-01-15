    package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.window.SplashScreen
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.myapplication.data.ViewModels.ArticlesViewModel
import com.example.myapplication.data.ViewModels.HomeViewModel
import com.example.myapplication.data.ViewModels.SavedViewModel
import com.example.myapplication.data.ViewModels.SearchViewModel
import com.example.myapplication.data.allNewsArticles
import com.example.myapplication.data.room.UserSettings
import com.example.myapplication.response.NewsArticle
import com.example.myapplication.screens.ArticleScreen
import com.example.myapplication.screens.HomeScreen
import com.example.myapplication.screens.OnboardingScreen
import com.example.myapplication.screens.SavedScreen
import com.example.myapplication.screens.SearchScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

    object Routes {
        const val ONBOARDING = "onboarding"
        const val HOME = "home"
        const val SEARCH = "search"
        const val SAVED = "saved"
        const val ARTICLE = "article/{articleId}"
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun App(
        modifier: Modifier = Modifier,
        homeViewModel: HomeViewModel,
        searchViewModel: SearchViewModel,
        savedViewModel: SavedViewModel,
        articlesViewModel: ArticlesViewModel
    ) {
        val onStart = remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            onStart.value=true
        }


        val appNavigationState = rememberAppNavigationState()
        val currentScreen = appNavigationState.currentScreen
        val navController = rememberNavController()

        val navBackStackEntry by navController.currentBackStackEntryAsState()

        val currentRoute = navBackStackEntry?.destination?.route



        val showBottomBar = currentRoute in listOf(Routes.HOME, Routes.SEARCH, Routes.SAVED)
        BackHandler(enabled = appNavigationState.canGoBack) {
            appNavigationState.navigateBack()
        }

        Column(
            Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0A0A12),
                            Color(0xFF1C1B3A),
                            Color(0xFF2E2B60),
                            Color(0xFF4338CA)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1200f, 1800f)
                    )
                )
                .fillMaxSize()
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                bottomBar = {
                    // Only show the bottom bar on the main screens
                    if (showBottomBar) {
                        GradientBottomNavBar(navController = navController)
                    }
                }
                ,
                topBar = {
                    if (showBottomBar) AppTopBar(Modifier.padding(WindowInsets.statusBars.asPaddingValues()))
                }
            ) { innerPadding ->
                // NavHost is where your screens are swapped
                NavHost(
                    navController = navController,
                    startDestination = Routes.ONBOARDING,
                    modifier = Modifier.padding()
                ) {
                    composable(Routes.ONBOARDING) {
                        OnboardingScreen(onFinish = {

                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.ONBOARDING) {
                                    inclusive = true
                                }
                            }
                        })
                    }

                    composable(Routes.HOME) {
                        HomeScreen(innerPadding, homeViewModel, onStart){ articleId ->
                            navController.navigate("article/$articleId")
                        }
                    }

                    composable(Routes.SEARCH) {
                        // SearchScreen()
                        SearchScreen(innerPadding, searchViewModel,onBackPressed = {navController.popBackStack()}){ articleId ->
                            navController.navigate("article/$articleId")
                        }  // Placeholder
                    }

                    composable(Routes.SAVED) {
                        // SavedScreen()
                        SavedScreen (innerPadding, savedViewModel){ articleId ->
                            navController.navigate("article/$articleId")
                        } // Placeholder
                    }

                    composable(
                        route = Routes.ARTICLE,
                        deepLinks = listOf(
                            navDeepLink {
                                // Match the custom scheme
                                uriPattern = "newsapp://article/{articleId}"
                                action = Intent.ACTION_VIEW
                            }),
                        arguments = listOf(navArgument("articleId") { type = NavType.IntType }),

                            enterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(300)
                                )
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -fullWidth },
                                    animationSpec = tween(300)
                                )
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -fullWidth },
                                    animationSpec = tween(300)
                                )
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(300)
                                )
                            }

                    ) { backStackEntry ->
                        val articleId = backStackEntry.arguments?.getInt("articleId")
                        ArticleScreen(innerPadding, articleId!!, articlesViewModel) { navController.popBackStack()} // Placeholder
                    }
                }
            }
        }
    }
    data class BottomNavItem(
        val icon: ImageVector,
        val name: String,
        val route: String
    )


    @Composable
    fun GradientBottomNavBar(navController: NavController) {
        // 1. Define your navigation items, now with their corresponding routes
        val navItems = listOf(
            BottomNavItem(icon = Icons.Default.Home, name = "Home", route = "home"),
            BottomNavItem(icon = Icons.Default.Search, name = "Search", route = "search"),
            BottomNavItem(icon = Icons.Default.Bookmark, name = "Saved", route = "saved")
        )

        // Observe the navigation back stack to determine the current route
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Main container with the purple-to-black gradient
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp) // Explicit height for a clean look
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3F006D), // A dark, rich purple
                            Color(0xFF000000)  // Pure black
                        )
                    )
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                navItems.forEach { item ->
                    // 2. Determine if the item is selected based on the current route
                    val isSelected = currentRoute == item.route

                    // This Box is the main container for each item
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .animateContentSize()
                            .clip(RoundedCornerShape(50)) // Pill/Circle shape
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.25f),
                                                    Color.White.copy(alpha = 0.1f)
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.4f),
                                                    Color.White.copy(alpha = 0.15f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(50)
                                        )
                                } else {
                                    Modifier
                                }
                            )
                            // 3. The clickable modifier now uses the NavController
                            .clickable {
                                navController.navigate(item.route) {
                                    // Pop up to the start destination to avoid building a large back stack
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid re-launching the same screen
                                    launchSingleTop = true
                                    // Restore state when re-selecting a previously selected item
                                    restoreState = true
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.name,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )

                            AnimatedVisibility(visible = isSelected) {
                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SplashScreen(modifier :Modifier, doneLoading: () -> Unit    ){
    var name by remember { mutableStateOf("") }
    var showCursor by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // typing effect
        val target = "Insert_Name()"
        for (i in target.indices) {
            delay(100L)
            name += target[i]
        }

        // blinking cursor
        while (true) {
            delay(200L) // blink speed
            showCursor = !showCursor
        }
    }
    LaunchedEffect(Unit) {
        delay(6000L)
        doneLoading()
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val width = maxWidth
        val height = maxHeight
        Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
//            Image(
//                painter = painterResource(id = R.drawable.logo),
//                contentDescription = "logo",
//                modifier = Modifier.size(width * 0.5f)
//            )
            Mp4LoadingAnimation(Modifier.fillMaxWidth(0.75f).aspectRatio(1f).clip(CircleShape))

            Spacer(modifier = Modifier.height(height/900f *50))
            Text("IN NEWS", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }

}


    @Composable
    fun LoadingScreen() {
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // A vibrant blue from your analytics chart
            PulsingShape(
                delayMillis = 0,
                shapeType = ShapeType.Circle,
                color = Color(0xFF3B82F6)
            )
            Spacer(modifier = Modifier.width(20.dp))

            // The deep purple that matches your theme's accent
            PulsingShape(
                delayMillis = 300,
                shapeType = ShapeType.Square,
                color = Color(0xFF8B5CF6)
            )
            Spacer(modifier = Modifier.width(20.dp))

            // The bold red from your analytics chart
            PulsingShape(
                delayMillis = 600,
                shapeType = ShapeType.Triangle,
                color = Color(0xFFEF4444)
            )
        }
    }
enum class ShapeType { Circle, Square, Triangle }

@Composable
fun PulsingShape(
    delayMillis: Int,
    shapeType: ShapeType,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")

    // Scale pulse animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    // Rotation synced with pulsing
    val rotation = when (shapeType) {
        ShapeType.Square -> {
            // 0 -> 90 degrees depending on scale
            ((scale - 0.7f) / (1.3f - 0.7f)) * 90f
        }
        ShapeType.Triangle -> {
            // 0 -> 120 degrees depending on scale
            ((scale - 0.7f) / (1.3f - 0.7f)) * 120f
        }
        else -> 0f // circle doesn't rotate
    }

    when (shapeType) {
        ShapeType.Circle -> {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .scale(scale)
                    .background(color, CircleShape)
            )
        }

        ShapeType.Square -> {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .scale(scale)
                    .rotate(rotation)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }

        ShapeType.Triangle -> {
            Canvas(
                modifier = Modifier
                    .size(36.dp)
                    .scale(scale)
                    .rotate(rotation)
            ) {
                val side = size.width
                val height = (side * Math.sqrt(3.0) / 2f).toFloat()

                val path = Path().apply {
                    moveTo(side / 2f, 0f)              // top vertex
                    lineTo(side, height)               // bottom right
                    lineTo(0f, height)                 // bottom left
                    close()
                }
                drawPath(path, color)
            }
        }

    }
}

    @Composable
    fun Mp4LoadingAnimation(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(
                    "android.resource://${context.packageName}/${R.raw.introanimation}"
                )
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_OFF
                setPlaybackParameters(
                    PlaybackParameters(2.0f)
                )
                playWhenReady = true
                prepare()
            }
        }

        // 5. Manage the player's lifecycle
        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
            }
        }

        // 6. Add the PlayerView to Compose
        AndroidView(
            modifier = modifier,
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode= RESIZE_MODE_ZOOM

                }
            }
        )
    }

    @Composable
    fun AppTopBar(modifier: Modifier = Modifier) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp) // Standard height for top bars
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center // Align content to the start
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo), // Your logo file
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(32.dp) // Adjust size as needed
            )

            Spacer(modifier = Modifier.width(8.dp)) // Space between logo and text

            // App Name
            Text(
                text = "IN NEWS",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }