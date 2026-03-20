package com.socialvideodownloader.core.billing.di

import com.socialvideodownloader.core.billing.PlayBillingRepository
import com.socialvideodownloader.core.domain.repository.BillingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {
    @Binds
    abstract fun bindBillingRepository(impl: PlayBillingRepository): BillingRepository
}
