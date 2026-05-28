package com.airgf.app.data.repository

import com.airgf.app.data.local.db.dao.GfConfigDao
import com.airgf.app.data.local.db.entity.GfConfigEntity
import com.airgf.app.domain.model.GfProfile
import com.airgf.app.domain.model.PersonalityTrait
import com.airgf.app.domain.model.RelationshipType
import com.airgf.app.domain.model.VisualTemplate
import com.airgf.app.domain.model.VoiceOption
import com.airgf.app.domain.repository.GfConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GfConfigRepositoryImpl @Inject constructor(
    private val gfConfigDao: GfConfigDao,
) : GfConfigRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val stringListSerializer = ListSerializer(String.serializer())

    override suspend fun getProfile(): GfProfile? =
        gfConfigDao.getConfig()?.toDomain()

    override fun getProfileFlow(): Flow<GfProfile?> =
        gfConfigDao.getConfigFlow().map { it?.toDomain() }

    override suspend fun saveProfile(profile: GfProfile) {
        gfConfigDao.upsert(profile.toEntity())
    }

    override suspend fun delete() {
        gfConfigDao.delete()
    }

    private fun GfConfigEntity.toDomain(): GfProfile {
        val traits: List<PersonalityTrait> = runCatching {
            json.decodeFromString(stringListSerializer, personalityTraits)
                .mapNotNull { name -> runCatching { PersonalityTrait.valueOf(name) }.getOrNull() }
        }.getOrDefault(emptyList())

        return GfProfile(
            name = name,
            visualTemplate = VisualTemplate.fromPersistedName(visualTemplate),
            personalityTraits = traits,
            relationshipType = runCatching { RelationshipType.valueOf(relationshipType) }
                .getOrDefault(RelationshipType.ROMANTIC),
            voiceOption = runCatching { VoiceOption.valueOf(voiceOption) }
                .getOrDefault(VoiceOption.SOFT),
            spicyModeEnabled = spicyModeEnabled,
            customPromptAdditions = customPromptAdditions,
        )
    }

    private fun GfProfile.toEntity(): GfConfigEntity = GfConfigEntity(
        id = 1,
        name = name,
        visualTemplate = visualTemplate.name,
        personalityTraits = json.encodeToString(stringListSerializer, personalityTraits.map { it.name }),
        relationshipType = relationshipType.name,
        voiceOption = voiceOption.name,
        spicyModeEnabled = spicyModeEnabled,
        customPromptAdditions = customPromptAdditions,
    )
}
