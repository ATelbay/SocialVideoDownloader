package com.videograb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.videograb.feature.download.navigation.DownloadRoute
import com.videograb.feature.history.navigation.HistoryRoute
import com.videograb.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val currentBackStack by navController.currentBackStackEntryAsState()
            val currentDestination = currentBackStack?.destination

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentDestination?.route == DownloadRoute::class.qualifiedName,
                            onClick = {
                                navController.navigate(DownloadRoute) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = stringResource(R.string.nav_download),
                                )
                            },
                            label = { Text(stringResource(R.string.nav_download)) },
                        )
                        NavigationBarItem(
                            selected = currentDestination?.route == HistoryRoute::class.qualifiedName,
                            onClick = {
                                navController.navigate(HistoryRoute) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = stringResource(R.string.nav_history),
                                )
                            },
                            label = { Text(stringResource(R.string.nav_history)) },
                        )
                    }
                },
            ) { innerPadding ->
                AppNavHost(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}
