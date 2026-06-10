package com.socialvideodownloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.socialvideodownloader.feature.download.navigation.DownloadRoute
import com.socialvideodownloader.feature.history.navigation.HistoryRoute
import com.socialvideodownloader.feature.library.navigation.LibraryRoute
import com.socialvideodownloader.navigation.AppNavHost
import com.socialvideodownloader.shared.ui.components.PillNavItem
import com.socialvideodownloader.shared.ui.components.PillNavigationBar
import com.socialvideodownloader.shared.ui.components.SvdNavIcons
import com.socialvideodownloader.shared.ui.theme.SvdBg
import com.socialvideodownloader.shared.ui.theme.SvdThemeAndroid
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
        )
        val sharedUrl = getSharedUrl(intent)
        setContent {
            // Keep the SVD warm-editorial brand palette. Dynamic Color (Material You) would
            // override colorScheme.primary with a wallpaper-derived tint (often blue), which
            // repaints the brand-orange buttons/controls. Brand color > device wallpaper here.
            SvdThemeAndroid(dynamicColor = false) {
                val navController = rememberNavController().also { this@MainActivity.navController = it }
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentDestination = currentBackStack?.destination

                val selectedIndex =
                    when {
                        currentDestination?.hasRoute<HistoryRoute>() == true -> 2
                        currentDestination?.hasRoute<LibraryRoute>() == true -> 1
                        else -> 0
                    }

                val navItems =
                    listOf(
                        PillNavItem(stringResource(R.string.nav_download), SvdNavIcons.Download),
                        PillNavItem(stringResource(R.string.nav_library), SvdNavIcons.Library),
                        PillNavItem(stringResource(R.string.nav_history), SvdNavIcons.History),
                    )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = SvdBg,
                    bottomBar = {
                        PillNavigationBar(
                            items = navItems,
                            selectedIndex = selectedIndex,
                            onSelect = { index ->
                                when (index) {
                                    0 ->
                                        navController.navigate(DownloadRoute()) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    1 ->
                                        navController.navigate(LibraryRoute) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    2 ->
                                        navController.navigate(HistoryRoute) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                }
                            },
                        )
                    },
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        startDestination = DownloadRoute(initialUrl = sharedUrl),
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val url = getSharedUrl(intent) ?: return
        val controller = navController ?: return
        controller.navigate(DownloadRoute(initialUrl = url)) {
            popUpTo(controller.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = false
        }
    }

    private fun getSharedUrl(intent: Intent): String? {
        if (intent.action != Intent.ACTION_SEND) return null
        return intent.getStringExtra(Intent.EXTRA_TEXT)
    }
}
