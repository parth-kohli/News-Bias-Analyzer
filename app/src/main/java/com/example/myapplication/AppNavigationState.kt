package com.example.myapplication

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.snapshots.SnapshotStateList // Keep using SnapshotStateList
//List of values of screen
sealed class Screen(val route: String) {
    object Onboarding : Screen(Routes.ONBOARDING)
    object Home : Screen(Routes.HOME)
    object Search : Screen(Routes.SEARCH)
    object Saved : Screen(Routes.SAVED)
    data class Article(val articleId: String) :
        Screen(Routes.ARTICLE.replace("{articleId}", articleId))
}

class AppNavigationState {
    //Stack used for navigation between screens
    private val _backStackInternal = mutableStateListOf<Screen>()
    val backStack: List<Screen>
        @Composable get() = _backStackInternal.toList().subList(0, _topIndex + 1)
    //Top index of stack
    private var _topIndex by mutableStateOf(-1)

    //Cuurent top value (auto updates when top index is changed)
    val currentScreen: Screen?
        @SuppressLint("UnrememberedMutableState") @Composable get() =  derivedStateOf {
            if (_topIndex >= 0 && _topIndex < _backStackInternal.size) {
                _backStackInternal[_topIndex]
            } else {
                null
            }
        }.value
    //IsStackEmpty()
    val canGoBack: Boolean
        get() = _topIndex > 0

    init {
        //Stack Initialization
        _backStackInternal.add(Screen.Onboarding)
        _topIndex = 0
        println("Navigation Init: Stack=${_backStackInternal.map { it.route }}, TopIndex=$_topIndex")
    }

    fun navigateTo(screen: Screen) {

        if (_topIndex >= 0 && _backStackInternal.isNotEmpty() && _backStackInternal[_topIndex].route == screen.route) {
            println("Navigation: Already on ${screen.route}")
            return
        }

        if (_topIndex < _backStackInternal.size - 1) {
            _backStackInternal.removeRange(_topIndex + 1, _backStackInternal.size)
        }
        //Pushing new screen to stack
        _backStackInternal.add(screen)
        _topIndex++
        println("Navigation State: Stack=${_backStackInternal.map { it.route }}, TopIndex=$_topIndex")

    }

    fun finishOnboarding() {
        //Clearing and reinitializing stack
        _backStackInternal.clear()
        _backStackInternal.add(Screen.Home)
        _topIndex = 0
        println("Navigation State: Stack=${_backStackInternal.map { it.route }}, TopIndex=$_topIndex")
    }


    fun navigateBack() {
        if (canGoBack) {
            //Pop function
            _topIndex--

        } else {
            println("Navigation: Cannot go back from the initial screen.")
        }
    }

    fun navigateBottomBar(targetScreen: Screen) {
        val currentRoute = if (_topIndex >= 0) _backStackInternal[_topIndex].route else null
        if (currentRoute == targetScreen.route) {
            println("BottomNav: Already on ${targetScreen.route}")
            return
        }
        val existingIndex = _backStackInternal.subList(0, _topIndex + 1)
            .indexOfFirst { it.route == targetScreen.route }
        if (existingIndex != -1) {
            _topIndex = existingIndex
        } else {
            if (_topIndex < _backStackInternal.size - 1) {
                _backStackInternal.removeRange(_topIndex + 1, _backStackInternal.size)
            }
            _backStackInternal.add(targetScreen)
            _topIndex++
        }
    }
}

@Composable
fun rememberAppNavigationState(): AppNavigationState {
    return remember { AppNavigationState() }
}