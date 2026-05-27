package com.airgf.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class CommunicationStyle(val displayName: String) {
    CASUAL("Keep it casual"),
    DEEP("Go deep"),
    FUNNY("Make me laugh"),
}
