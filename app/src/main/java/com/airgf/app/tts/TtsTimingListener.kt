package com.airgf.app.tts

import android.speech.tts.UtteranceProgressListener

class TtsTimingListener(
    private val text: String,
    private val utteranceId: String,
    private val onStart: () -> Unit,
    private val onWordStart: (word: String) -> Unit,
    private val onDone: () -> Unit,
    private val onError: () -> Unit,
) : UtteranceProgressListener() {

    override fun onStart(id: String?) {
        if (id != utteranceId) return
        onStart()
    }

    override fun onDone(id: String?) {
        if (id != utteranceId) return
        onDone()
    }

    @Deprecated("Deprecated in Java")
    override fun onError(id: String?) {
        if (id != utteranceId) return
        onError()
    }

    override fun onError(id: String?, errorCode: Int) {
        if (id != utteranceId) return
        onError()
    }

    override fun onRangeStart(id: String?, start: Int, end: Int, frame: Int) {
        if (id != utteranceId) return
        if (start < 0 || end > text.length || start >= end) return
        onWordStart(text.substring(start, end))
    }
}
