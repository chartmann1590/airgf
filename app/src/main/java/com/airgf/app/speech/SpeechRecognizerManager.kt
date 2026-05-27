package com.airgf.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class SpeechRecognitionError(
    val message: String,
    val recoverable: Boolean = true,
)

@Singleton
class SpeechRecognizerManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null

    fun startListening(
        onReady: () -> Unit,
        onBeginning: () -> Unit,
        onEnd: () -> Unit,
        onResult: (String) -> Unit,
        onError: (SpeechRecognitionError) -> Unit,
    ) {
        mainHandler.post {
            destroyRecognizer()
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError(
                    SpeechRecognitionError(
                        message = "Speech recognition is not available on this device.",
                        recoverable = false,
                    ),
                )
                return@post
            }

            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(
                    object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) = onReady()
                        override fun onBeginningOfSpeech() = onBeginning()
                        override fun onRmsChanged(rmsdB: Float) = Unit
                        override fun onBufferReceived(buffer: ByteArray?) = Unit
                        override fun onEndOfSpeech() = onEnd()
                        override fun onPartialResults(partialResults: Bundle?) = Unit
                        override fun onEvent(eventType: Int, params: Bundle?) = Unit

                        override fun onError(error: Int) {
                            onError(error.toRecognitionError())
                        }

                        override fun onResults(results: Bundle?) {
                            val text = results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                                .trim()
                            if (text.isBlank()) {
                                onError(SpeechRecognitionError("I didn't catch that. Try again."))
                            } else {
                                onResult(text)
                            }
                        }
                    },
                )
                startListening(recognitionIntent())
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            recognizer?.stopListening()
        }
    }

    fun cancelListening() {
        mainHandler.post {
            destroyRecognizer()
        }
    }

    private fun recognitionIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
        }

    private fun destroyRecognizer() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun Int.toRecognitionError(): SpeechRecognitionError =
        when (this) {
            SpeechRecognizer.ERROR_AUDIO -> SpeechRecognitionError("Microphone audio failed. Try again.")
            SpeechRecognizer.ERROR_CLIENT -> SpeechRecognitionError("Speech recognition stopped.")
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                SpeechRecognitionError("Microphone permission is required for calls.", recoverable = false)
            }
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            -> SpeechRecognitionError("Speech recognition network error. Try again.")
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            -> SpeechRecognitionError("I didn't catch that. Try again.")
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechRecognitionError("Speech recognition is busy. Try again.")
            SpeechRecognizer.ERROR_SERVER -> SpeechRecognitionError("Speech recognition service error. Try again.")
            else -> SpeechRecognitionError("Speech recognition failed. Try again.")
        }
}
