package com.airgf.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.airgf.app.MainActivity
import com.airgf.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GfNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun showGfMessage(gfName: String, message: String): Boolean {
        ensureChannel()
        if (!canPostNotifications()) return false

        val openChatIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_CHAT_FROM_NOTIFICATION, true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_CHAT,
            openChatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(gfName)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(nextNotificationId(), notification)
        return true
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val systemNotificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        if (systemNotificationManager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Girlfriend messages",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Proactive messages from your AI girlfriend"
        }
        systemNotificationManager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean =
        context.hasNotificationPermission()

    private fun nextNotificationId(): Int =
        (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

    companion object {
        const val CHANNEL_ID = "gf_messages"
        const val EXTRA_OPEN_CHAT_FROM_NOTIFICATION = "open_chat_from_notification"

        private const val REQUEST_CODE_OPEN_CHAT = 7001
    }
}
