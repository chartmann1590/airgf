package com.airgf.app.llm

object ModelConstants {
    const val MODEL_URL =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
    const val MODEL_FILENAME = "gemma-4-e2b.litertlm"
    const val MODEL_DISPLAY_NAME = "Gemma 4 E2B"
    const val EXPECTED_SIZE_BYTES = 2_580_000_000L
    const val MIN_FREE_SPACE_BYTES = 3_221_225_472L // 3 GB
}

enum class ModelVariant(
    val displayName: String,
    val url: String,
    val filename: String,
    val expectedSizeBytes: Long,
    val minimumFreeSpaceBytes: Long,
    val qualityDescription: String,
) {
    E2B(
        displayName = "Gemma 4 E2B",
        url = ModelConstants.MODEL_URL,
        filename = ModelConstants.MODEL_FILENAME,
        expectedSizeBytes = ModelConstants.EXPECTED_SIZE_BYTES,
        minimumFreeSpaceBytes = ModelConstants.MIN_FREE_SPACE_BYTES,
        qualityDescription = "Best compatibility and speed",
    ),
    E4B(
        displayName = "Gemma 4 E4B",
        url = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
        filename = "gemma-4-e4b.litertlm",
        expectedSizeBytes = 4_900_000_000L,
        minimumFreeSpaceBytes = 6_000_000_000L,
        qualityDescription = "Higher quality for powerful phones",
    ),
}
