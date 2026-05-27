package com.airgf.app.domain.repository

import com.airgf.app.domain.model.GfProfile
import kotlinx.coroutines.flow.Flow

interface GfConfigRepository {
    suspend fun getProfile(): GfProfile?
    fun getProfileFlow(): Flow<GfProfile?>
    suspend fun saveProfile(profile: GfProfile)
    suspend fun delete()
}
