package com.airgf.app.presentation.splash

import androidx.lifecycle.ViewModel
import com.airgf.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    val userRepository: UserRepository,
) : ViewModel()
