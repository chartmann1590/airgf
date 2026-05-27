package com.airgf.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val name: String,
    val age: Int,
    val interests: List<String>,
    val communicationStyle: CommunicationStyle,
)
