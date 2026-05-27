package com.airgf.app.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.airgf.app.presentation.theme.Primary

@Composable
fun SplashScreen(
    onNavigateToWelcome: () -> Unit,
    onNavigateToChat: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Primary)
    }

    LaunchedEffect(Unit) {
        val complete = viewModel.userRepository.isOnboardingComplete()
        if (complete) {
            onNavigateToChat()
        } else {
            onNavigateToWelcome()
        }
    }
}
