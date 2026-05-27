package com.airgf.app.core.di

import com.airgf.app.data.repository.ChatRepositoryImpl
import com.airgf.app.data.repository.GfConfigRepositoryImpl
import com.airgf.app.data.repository.ImageGenRepositoryImpl
import com.airgf.app.data.repository.ModelRepositoryImpl
import com.airgf.app.data.repository.UserRepositoryImpl
import com.airgf.app.domain.repository.ChatRepository
import com.airgf.app.domain.repository.GfConfigRepository
import com.airgf.app.domain.repository.ImageGenRepository
import com.airgf.app.domain.repository.ModelRepository
import com.airgf.app.domain.repository.UserRepository
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
}
