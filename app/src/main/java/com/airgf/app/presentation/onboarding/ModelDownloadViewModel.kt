package com.airgf.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgf.app.data.model.DownloadState
import com.airgf.app.domain.repository.ModelRepository
import com.airgf.app.llm.ModelConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelDownloadViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ModelDownloadUiState())
    val state: StateFlow<ModelDownloadUiState> = _state.asStateFlow()

    private var downloadJob: Job? = null
    private var tipJob: Job? = null

    private val tips = listOf(
        "Generating personality matrices…",
        "Syncing emotional responses…",
        "Optimizing memory retrieval…",
    )

    init {
        startTipRotation()
    }

    fun startDownload() {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    phase = DownloadPhase.Checking,
                    errorType = null,
                    errorMessage = null,
                    percent = 0f,
                    bytesDownloaded = 0L,
                    totalBytes = ModelConstants.EXPECTED_SIZE_BYTES,
                    speedBytesPerSec = 0L,
                    etaSeconds = null,
                )
            }

            if (modelRepository.isModelDownloaded()) {
                modelRepository.getModelPath()?.let { modelRepository.setModelDownloaded(it) }
                _state.update {
                    it.copy(
                        phase = DownloadPhase.Complete,
                        percent = 100f,
                        bytesDownloaded = it.totalBytes,
                    )
                }
                return@launch
            }

            if (!modelRepository.isNetworkAvailable()) {
                _state.update {
                    it.copy(
                        phase = DownloadPhase.Error,
                        errorType = DownloadErrorType.NoNetwork,
                    )
                }
                return@launch
            }

            if (!modelRepository.hasSufficientStorage()) {
                _state.update {
                    it.copy(
                        phase = DownloadPhase.Error,
                        errorType = DownloadErrorType.InsufficientStorage,
                    )
                }
                return@launch
            }

            _state.update { it.copy(phase = DownloadPhase.Downloading) }

            var lastBytes = 0L
            var lastSampleTime = System.currentTimeMillis()

            modelRepository.downloadModel().collect { downloadState ->
                when (downloadState) {
                    is DownloadState.Idle -> Unit
                    is DownloadState.Progress -> {
                        val now = System.currentTimeMillis()
                        val elapsedMs = (now - lastSampleTime).coerceAtLeast(1L)
                        val deltaBytes = downloadState.bytesDownloaded - lastBytes
                        val speed = if (deltaBytes > 0 && elapsedMs >= 500) {
                            lastBytes = downloadState.bytesDownloaded
                            lastSampleTime = now
                            (deltaBytes * 1000L) / elapsedMs
                        } else {
                            _state.value.speedBytesPerSec
                        }

                        val remaining = downloadState.totalBytes - downloadState.bytesDownloaded
                        val eta = if (speed > 0) remaining / speed else null

                        _state.update {
                            it.copy(
                                phase = DownloadPhase.Downloading,
                                percent = downloadState.percent * 100f,
                                bytesDownloaded = downloadState.bytesDownloaded,
                                totalBytes = downloadState.totalBytes,
                                speedBytesPerSec = speed,
                                etaSeconds = eta,
                            )
                        }
                    }
                    is DownloadState.Complete -> {
                        _state.update {
                            it.copy(
                                phase = DownloadPhase.Complete,
                                percent = 100f,
                                bytesDownloaded = it.totalBytes,
                                speedBytesPerSec = 0L,
                                etaSeconds = 0L,
                            )
                        }
                    }
                    is DownloadState.Error -> {
                        _state.update {
                            it.copy(
                                phase = DownloadPhase.Error,
                                errorType = DownloadErrorType.DownloadFailed,
                                errorMessage = downloadState.message,
                            )
                        }
                    }
                }
            }
        }
    }

    fun retry() {
        startDownload()
    }

    private fun startTipRotation() {
        tipJob?.cancel()
        tipJob = viewModelScope.launch {
            while (isActive) {
                delay(3000)
                _state.update { it.copy(tipIndex = (it.tipIndex + 1) % tips.size) }
            }
        }
    }

    fun currentTip(): String = tips[_state.value.tipIndex]

    override fun onCleared() {
        tipJob?.cancel()
        downloadJob?.cancel()
        super.onCleared()
    }
}
