package com.socialvideodownloader.shared.data.di

import org.koin.dsl.module

/**
 * Shared Koin module for cross-platform data wiring that is genuinely common to
 * every target.
 *
 * The Room KMP database, DAOs, and the shared [DownloadRepository] are deliberately
 * NOT defined here. On Android the database is owned by the Hilt graph in
 * :core:data ([com.socialvideodownloader.core.data.di.DatabaseModule] /
 * RepositoryModule); registering a second Koin-managed Room instance over the same
 * physical file (with a different SQLite driver) would risk lock contention and
 * corruption. The iOS targets provide these bindings in
 * [com.socialvideodownloader.shared.data.di.iosDataModule] instead.
 *
 * Keeping the module (currently empty) preserves the existing Koin start-up wiring
 * on both platforms without behavioural change.
 */
val sharedDataModule = module { }
