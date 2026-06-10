package com.socialvideodownloader.core.domain.di

import javax.inject.Qualifier

// Hilt qualifier annotation for the IO dispatcher, used by Android modules that inject
// CoroutineDispatcher via Hilt. KMP platform adapters (shared/*) take their dispatcher at the
// platform-adapter boundary (e.g. Dispatchers.IO in androidMain file storage), which the project
// rules explicitly permit, so no Koin-side qualifier is needed.
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
