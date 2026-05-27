package com.airgf.app.core.di

import com.airgf.app.notification.ProactiveMessageScheduler
import com.airgf.app.notification.ProactiveScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds
    abstract fun bindProactiveMessageScheduler(
        scheduler: ProactiveScheduler,
    ): ProactiveMessageScheduler
}
