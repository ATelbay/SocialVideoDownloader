package com.socialvideodownloader.core.cloud.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.socialvideodownloader.core.domain.di.IoDispatcher
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.SyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncManager: SyncManager,
    private val backupPreferences: BackupPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: Flow<Boolean> = _isConnected.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isConnected.value = true
            scope.launch {
                if (backupPreferences.observeIsBackupEnabled().first()) {
                    syncManager.processPendingOperations()
                }
            }
        }

        override fun onLost(network: Network) {
            _isConnected.value = false
        }
    }

    fun register() {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Set initial connectivity state
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        _isConnected.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    fun unregister() {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }
}
