package com.chitch.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chitch.app.di.AppContainer
import com.chitch.app.ui.components.AppDrawerContent
import com.chitch.app.ui.components.AppTopBar
import com.chitch.app.ui.nav.ChitchNavHost
import com.chitch.app.ui.theme.ChitchTheme
import kotlinx.coroutines.launch

@Composable
fun ChitchApp(container: AppContainer) {
    val systemDarkDefault = isSystemInDarkTheme()
    var isDarkTheme by rememberSaveable { mutableStateOf(systemDarkDefault) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    ChitchTheme(darkTheme = isDarkTheme) {
        val navController = rememberNavController()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        BackHandler(enabled = drawerState.isOpen) {
            scope.launch { drawerState.close() }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawerContent(
                    currentRoute = currentRoute,
                    onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
                    onCloseDrawer = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets.navigationBars,
                topBar = {
                    AppTopBar(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it }
                    )
                }
            ) { innerPadding ->
                ChitchNavHost(
                    navController = navController,
                    container = container,
                    searchQuery = searchQuery,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        }
                )
            }
        }
    }
}
