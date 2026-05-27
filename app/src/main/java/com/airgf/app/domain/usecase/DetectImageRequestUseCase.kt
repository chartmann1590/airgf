package com.airgf.app.domain.usecase

import javax.inject.Inject

class DetectImageRequestUseCase @Inject constructor() {
    private val imageRegex = Regex("\\[IMAGE:\\s*(.+?)]", RegexOption.IGNORE_CASE)

    operator fun invoke(rawResponse: String): Pair<String, String?> {
        val match = imageRegex.find(rawResponse)
        val prompt = match?.groupValues?.get(1)?.trim()
        val cleanText = rawResponse.replace(imageRegex, "").trim()
        return cleanText to prompt
    }
}
