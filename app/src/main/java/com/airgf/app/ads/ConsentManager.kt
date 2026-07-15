package com.airgf.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Google's User Messaging Platform (UMP) SDK, required by Play policy
 * for a certified CMP when serving personalized ads to EEA/UK/Switzerland users.
 * No ad may be requested/loaded before [canRequestAds] is true.
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    /**
     * Requests the latest consent state and shows the consent form if required.
     * Call once per app start (e.g. from the splash screen) before initializing ads.
     */
    fun requestConsentAndLoadForm(
        activity: Activity,
        onConsentResolved: (canRequestAds: Boolean) -> Unit,
    ) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    onConsentResolved(consentInformation.canRequestAds())
                }
            },
            { onConsentResolved(consentInformation.canRequestAds()) },
        )
    }

    /**
     * Re-opens the consent form so EEA/UK/Switzerland users can change their choice later,
     * exposed via a "Privacy Options" row in Settings per Play policy.
     */
    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Debug-only helper to force the EEA consent flow on a test device (see [ConsentDebugSettings]). */
    fun debugGeography(): ConsentDebugSettings.Builder = ConsentDebugSettings.Builder(context)
}
