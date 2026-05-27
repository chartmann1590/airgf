package com.airgf.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.airgf.app.core.navigation.AirGfNavGraph
import com.airgf.app.notification.GfNotificationManager
import com.airgf.app.presentation.theme.AirGfTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val openChatFromNotificationEvents = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            AirGfTheme {
                AirGfNavGraph(
                    openChatFromNotificationEvents = openChatFromNotificationEvents,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(GfNotificationManager.EXTRA_OPEN_CHAT_FROM_NOTIFICATION, false) == true) {
            openChatFromNotificationEvents.update { it + 1 }
            intent.removeExtra(GfNotificationManager.EXTRA_OPEN_CHAT_FROM_NOTIFICATION)
        }
    }
}
