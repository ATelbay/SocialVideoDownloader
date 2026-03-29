package com.socialvideodownloader.core.domain.di

import javax.inject.Qualifier

/**
 * Hilt qualifier annotation for the IO dispatcher.
 * Used by Android modules that inject CoroutineDispatcher via Hilt.
 *
 * Shared KMP modules should use [IoDispatcherQualifier] (Koin named qualifier) instead.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
