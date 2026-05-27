package com.airgf.app.core.navigation

sealed class Route(val path: String) {
    data object Splash : Route("splash")
    data object Welcome : Route("onboarding/welcome")
    data object UserProfile : Route("onboarding/user_profile")
    data object GfCustomization : Route("onboarding/gf_customization")
    data object VoiceSelection : Route("onboarding/voice_selection")
    data object ModelDownload : Route("onboarding/model_download")
    data object ImageModelDownload : Route("onboarding/image_model_download")
    data object SetupComplete : Route("onboarding/setup_complete")
    data object Chat : Route("main/chat")
    data object Call : Route("main/call")
    data object Character : Route("main/character")
    data object Settings : Route("main/settings")
}
