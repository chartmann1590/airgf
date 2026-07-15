package com.airgf.app.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** AdMob/Billing/UMP calls all require an Activity, but Compose only guarantees a Context. */
fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
