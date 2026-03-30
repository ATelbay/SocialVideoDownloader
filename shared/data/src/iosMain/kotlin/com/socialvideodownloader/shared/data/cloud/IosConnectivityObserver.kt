package com.socialvideodownloader.shared.data.cloud

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS connectivity observer that monitors network availability.
 *
 * On the full implementation path this would use:
 *   - `Network.framework` NWPathMonitor via Swift
 *   - Updates exposed as a [Flow<Boolean>] to the Kotlin layer
 *
 * Because NWPathMonitor is a Swift-native API, it is bridged through [PlatformConnectivityProvider].
 *
 * Usage:
 * ```kotlin
 * val observer = IosConnectivityObserver(connectivityProvider)
 * observer.observeConnectivity().collect { isOnline ->
 *     if (isOnline) syncManager.processPendingOperations()
 * }
 * ```
 *
 * TODO: Implement [PlatformConnectivityProvider] in Swift using NWPathMonitor, register with Koin.
 */
class IosConnectivityObserver(
    private val connectivityProvider: PlatformConnectivityProvider,
) {
    /** Returns a cold [Flow] that emits true when the network is reachable. */
    fun observeConnectivity(): Flow<Boolean> = connectivityProvider.observeIsOnline()
}

/**
 * Bridge interface implemented by Swift using NWPathMonitor.
 *
 * Swift implementation outline:
 * ```swift
 * class NWPathConnectivityProvider: PlatformConnectivityProvider {
 *     private let monitor = NWPathMonitor()
 *     private let subject = PassthroughSubject<Bool, Never>()
 *
 *     func observeIsOnline() -> some Flow<KotlinBoolean> {
 *         // Bridge NWPathMonitor updates to Kotlin Flow via SKIE or callbacks
 *     }
 * }
 * ```
 *
 * TODO: Implement in Swift and register in KoinHelper.
 */
interface PlatformConnectivityProvider {
    /**
     * Returns a [Flow] that emits `true` when the network is reachable (any interface),
     * and `false` when offline.
     *
     * The first emission should reflect the current network state immediately.
     */
    fun observeIsOnline(): Flow<Boolean>
}

/**
 * Stub [PlatformConnectivityProvider] that always reports the device as online.
 *
 * Used as the default provider until the Swift NWPathMonitor implementation is wired.
 * Safe for use in testing and in the simulator.
 */
class StubConnectivityProvider : PlatformConnectivityProvider {
    private val _isOnline = MutableStateFlow(true)

    override fun observeIsOnline(): Flow<Boolean> = _isOnline.asStateFlow()
}
