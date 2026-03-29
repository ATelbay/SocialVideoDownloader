package com.socialvideodownloader.core.domain.di

import org.koin.core.qualifier.named

/**
 * Koin named qualifier for the IO dispatcher.
 * Use this in shared KMP modules to inject CoroutineDispatcher(Dispatchers.IO).
 *
 * Android modules using Hilt should use the @IoDispatcher annotation
 * from the androidMain source set instead.
 */
val IoDispatcherQualifier = named("IoDispatcher")
