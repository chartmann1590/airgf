package com.airgf.app.core.di

import com.airgf.app.data.repository.AvatarAssetRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AvatarAssetRepositoryEntryPoint {
    fun avatarAssetRepository(): AvatarAssetRepository
}
