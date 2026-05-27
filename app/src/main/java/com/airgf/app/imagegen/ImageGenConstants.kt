package com.airgf.app.imagegen

object ImageGenConstants {
    const val MODEL_URL =
        "https://huggingface.co/On-device/stable-diffusion-v1-5-mediapipe/resolve/main/sd15-mediapipe.zip"
    const val MODEL_FILENAME = "sd15-mediapipe.zip"
    const val MODEL_DISPLAY_NAME = "Stable Diffusion"
    const val EXPECTED_SIZE_BYTES = 2_050_000_000L
    const val MIN_FREE_SPACE_BYTES = 3_221_225_472L
    const val MODEL_DIR = "sd_model"
}
