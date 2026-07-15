package com.airgf.app.core.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.airgf.app.ads.AdCoordinatorViewModel
import com.airgf.app.ads.findActivity
import com.airgf.app.presentation.onboarding.GfCustomizationScreen
import com.airgf.app.presentation.onboarding.ImageModelDownloadScreen
import com.airgf.app.presentation.onboarding.ModelDownloadScreen
import com.airgf.app.presentation.onboarding.OnboardingViewModel
import com.airgf.app.presentation.onboarding.SetupCompleteScreen
import com.airgf.app.presentation.onboarding.UserProfileScreen
import com.airgf.app.presentation.onboarding.VoiceSelectionScreen
import com.airgf.app.presentation.onboarding.WelcomeScreen
import com.airgf.app.presentation.character.CharacterScreen
import com.airgf.app.presentation.call.CallScreen
import com.airgf.app.presentation.chat.ChatScreen
import com.airgf.app.presentation.components.MainBottomNav
import com.airgf.app.presentation.settings.SettingsScreen
import com.airgf.app.presentation.splash.SplashScreen
import kotlinx.coroutines.flow.StateFlow

private const val ONBOARDING_GRAPH = "onboarding"

private val onboardingEnterTransition = slideInHorizontally { it } + fadeIn()
private val onboardingExitTransition = slideOutHorizontally { -it / 3 } + fadeOut()
private val onboardingPopEnterTransition = slideInHorizontally { -it } + fadeIn()
private val onboardingPopExitTransition = slideOutHorizontally { it / 3 } + fadeOut()

@Composable
fun AirGfNavGraph(
    navController: NavHostController = rememberNavController(),
    openChatFromNotificationEvents: StateFlow<Int>? = null,
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val notificationEventCount = openChatFromNotificationEvents?.collectAsStateWithLifecycle()?.value ?: 0

    val context = LocalContext.current
    val adCoordinator: AdCoordinatorViewModel = hiltViewModel()
    val isSubscribed by adCoordinator.isSubscribed.collectAsStateWithLifecycle()
    val mainTabRoutes = remember {
        setOf(Route.Chat.path, Route.Call.path, Route.Character.path, Route.Settings.path)
    }

    LaunchedEffect(currentRoute, isSubscribed) {
        if (!isSubscribed && currentRoute in mainTabRoutes) {
            context.findActivity()?.let { activity ->
                adCoordinator.interstitialAdManager.showIfReady(activity)
            }
        }
    }

    LaunchedEffect(notificationEventCount) {
        if (notificationEventCount <= 0) return@LaunchedEffect

        navController.navigate(Route.Chat.path) {
            launchSingleTop = true
            restoreState = true

            if (currentRoute == null ||
                currentRoute == Route.Splash.path ||
                currentRoute.startsWith("onboarding/")
            ) {
                popUpTo(Route.Splash.path) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Route.Splash.path,
    ) {
        composable(Route.Splash.path) {
            SplashScreen(
                onNavigateToWelcome = {
                    navController.navigate(ONBOARDING_GRAPH) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                },
                onNavigateToChat = {
                    navController.navigate(Route.Chat.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                },
            )
        }

        navigation(
            route = ONBOARDING_GRAPH,
            startDestination = Route.Welcome.path,
        ) {
            composable(
                route = Route.Welcome.path,
                enterTransition = { onboardingEnterTransition },
                exitTransition = { onboardingExitTransition },
                popEnterTransition = { onboardingPopEnterTransition },
                popExitTransition = { onboardingPopExitTransition },
            ) { entry ->
                val splashViewModel: com.airgf.app.presentation.splash.SplashViewModel = hiltViewModel()
                var canContinue by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    canContinue = splashViewModel.userRepository.isOnboardingComplete()
                }
                WelcomeScreen(
                    onGetStarted = { navController.navigate(Route.UserProfile.path) },
                    onContinueExisting = if (canContinue) {
                        {
                            navController.navigate(Route.Chat.path) {
                                popUpTo(ONBOARDING_GRAPH) { inclusive = true }
                            }
                        }
                    } else {
                        null
                    },
                )
            }

            composable(
                route = Route.UserProfile.path,
                enterTransition = { onboardingEnterTransition },
                exitTransition = { onboardingExitTransition },
                popEnterTransition = { onboardingPopEnterTransition },
                popExitTransition = { onboardingPopExitTransition },
            ) { entry ->
                val parentEntry = remember(entry) { navController.getBackStackEntry(ONBOARDING_GRAPH) }
                val viewModel: OnboardingViewModel = hiltViewModel(parentEntry)
                UserProfileScreen(
                    viewModel = viewModel,
                    onContinue = { navController.navigate(Route.GfCustomization.path) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Route.GfCustomization.path,
                enterTransition = { onboardingEnterTransition },
                exitTransition = { onboardingExitTransition },
                popEnterTransition = { onboardingPopEnterTransition },
                popExitTransition = { onboardingPopExitTransition },
            ) { entry ->
                val parentEntry = remember(entry) { navController.getBackStackEntry(ONBOARDING_GRAPH) }
                val viewModel: OnboardingViewModel = hiltViewModel(parentEntry)
                GfCustomizationScreen(
                    viewModel = viewModel,
                    onContinue = { navController.navigate(Route.VoiceSelection.path) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Route.VoiceSelection.path,
                enterTransition = { onboardingEnterTransition },
                exitTransition = { onboardingExitTransition },
                popEnterTransition = { onboardingPopEnterTransition },
                popExitTransition = { onboardingPopExitTransition },
            ) { entry ->
                val parentEntry = remember(entry) { navController.getBackStackEntry(ONBOARDING_GRAPH) }
                val viewModel: OnboardingViewModel = hiltViewModel(parentEntry)
                VoiceSelectionScreen(
                    viewModel = viewModel,
                    onContinue = { navController.navigate(Route.ModelDownload.path) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Route.ModelDownload.path,
                enterTransition = { onboardingEnterTransition },
                exitTransition = { onboardingExitTransition },
                popEnterTransition = { onboardingPopEnterTransition },
                popExitTransition = { onboardingPopExitTransition },
            ) {
                ModelDownloadScreen(
                    onContinue = { navController.navigate(Route.ImageModelDownload.path) },
                )
            }

            composable(
                route = Route.ImageModelDownload.path,
                enterTransition = { onboardingEnterTransition },
                exitTransition = { onboardingExitTransition },
                popEnterTransition = { onboardingPopEnterTransition },
                popExitTransition = { onboardingPopExitTransition },
            ) {
                ImageModelDownloadScreen(
                    onContinue = { navController.navigate(Route.SetupComplete.path) },
                )
            }

            composable(
                route = Route.SetupComplete.path,
                enterTransition = { onboardingEnterTransition },
                exitTransition = { onboardingExitTransition },
                popEnterTransition = { onboardingPopEnterTransition },
                popExitTransition = { onboardingPopExitTransition },
            ) { entry ->
                val parentEntry = remember(entry) { navController.getBackStackEntry(ONBOARDING_GRAPH) }
                val viewModel: OnboardingViewModel = hiltViewModel(parentEntry)
                SetupCompleteScreen(
                    viewModel = viewModel,
                    onStartChatting = {
                        navController.navigate(Route.Chat.path) {
                            popUpTo(ONBOARDING_GRAPH) { inclusive = true }
                        }
                    },
                )
            }
        }

        composable(Route.Chat.path) {
            ChatScreen(navController = navController)
        }
        composable(Route.Call.path) {
            CallScreen(navController = navController)
        }
        composable(Route.Character.path) {
            CharacterScreen(navController = navController)
        }
        composable(Route.Settings.path) {
            SettingsScreen(navController = navController)
        }
    }
}

@Composable
private fun MainPlaceholderScreen(
    label: String,
    currentRoute: Route,
    navController: NavHostController,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            androidx.compose.material3.Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        MainBottomNav(
            currentRoute = currentRoute,
            onNavigate = { route ->
                navController.navigate(route.path) {
                    launchSingleTop = true
                    popUpTo(Route.Chat.path) { saveState = true }
                    restoreState = true
                }
            },
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
