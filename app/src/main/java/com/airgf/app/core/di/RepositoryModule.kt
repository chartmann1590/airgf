package com.airgf.app.core.di

import com.airgf.app.core.util.ImageCleanup
import com.airgf.app.core.util.ImageStorageUtil
import com.airgf.app.data.repository.ChatRepositoryImpl
import com.airgf.app.data.repository.GfConfigRepositoryImpl
import com.airgf.app.data.repository.ImageGenRepositoryImpl
import com.airgf.app.data.repository.ModelRepositoryImpl
import com.airgf.app.data.repository.UserRepositoryImpl
import com.airgf.app.data.repository.MemoryRepositoryImpl
import com.airgf.app.data.repository.SubscriptionRepositoryImpl
import com.airgf.app.domain.repository.ChatRepository
import com.airgf.app.domain.repository.GfConfigRepository
import com.airgf.app.domain.repository.ImageGenRepository
import com.airgf.app.domain.repository.ModelRepository
import com.airgf.app.domain.repository.UserRepository
import com.airgf.app.domain.repository.MemoryRepository
import com.airgf.app.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindGfConfigRepository(impl: GfConfigRepositoryImpl): GfConfigRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(impl: ModelRepositoryImpl): ModelRepository

    @Binds
    @Singleton
    abstract fun bindImageGenRepository(impl: ImageGenRepositoryImpl): ImageGenRepository

    @Binds
    @Singleton
    abstract fun bindImageCleanup(impl: ImageStorageUtil): ImageCleanup

    @Binds
    @Singleton
    abstract fun bindMemoryRepository(impl: MemoryRepositoryImpl): MemoryRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository
}
