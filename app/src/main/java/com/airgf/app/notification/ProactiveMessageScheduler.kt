package com.airgf.app.notification

interface ProactiveMessageScheduler {
    suspend fun sync()
    fun disable()
}
