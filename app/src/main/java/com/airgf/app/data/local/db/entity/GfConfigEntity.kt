package com.airgf.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gf_config")
data class GfConfigEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val visualTemplate: String,
    val personalityTraits: String,
    val relationshipType: String,
    val voiceOption: String,
    val spicyModeEnabled: Boolean,
    val customPromptAdditions: String?,
)
