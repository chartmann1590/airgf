package com.airgf.app.llm

object ModelConstants {
    const val MODEL_URL =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
    const val MODEL_FILENAME = "gemma-4-e2b.litertlm"
    const val MODEL_DISPLAY_NAME = "Gemma 4 E2B"
    const val EXPECTED_SIZE_BYTES = 2_580_000_000L
    const val MIN_FREE_SPACE_BYTES = 3_221_225_472L // 3 GB
}
