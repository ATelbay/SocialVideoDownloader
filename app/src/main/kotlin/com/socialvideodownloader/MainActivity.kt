package com.socialvideodownloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.socialvideodownloader.core.ui.components.PillNavigationBar
import com.socialvideodownloader.core.ui.theme.SocialVideoDownloaderTheme
import com.socialvideodownloader.core.ui.theme.SvdBg
import com.socialvideodownloader.feature.download.navigation.DownloadRoute
import com.socialvideodownloader.feature.history.navigation.HistoryRoute
import com.socialvideodownloader.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sharedUrl = getSharedUrl(intent)
        setContent {
            SocialVideoDownloaderTheme {
                val navController = rememberNavController().also { this@MainActivity.navController = it }
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentDestination = currentBackStack?.destination

                val selectedIndex = when {
                    currentDestination?.hasRoute<HistoryRoute>() == true -> 1
                    else -> 0
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = SvdBg,
                    bottomBar = {
                        PillNavigationBar(
                            selectedIndex = selectedIndex,
                            onSelect = { index ->
                                when (index) {
                                    0 -> navController.navigate(DownloadRoute()) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    1 -> navController.navigate(HistoryRoute) {
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
        navController?.navigate(DownloadRoute(initialUrl = url)) {
            popUpTo(navController!!.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = false
        }
    }

    private fun getSharedUrl(intent: Intent): String? {
        if (intent.action != Intent.ACTION_SEND) return null
        return intent.getStringExtra(Intent.EXTRA_TEXT)
    }
}
