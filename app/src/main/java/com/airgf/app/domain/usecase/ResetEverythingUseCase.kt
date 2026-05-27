package com.airgf.app.domain.usecase

import com.airgf.app.core.util.ImageCleanup
import com.airgf.app.domain.repository.ChatRepository
import com.airgf.app.domain.repository.GfConfigRepository
import com.airgf.app.domain.repository.ModelRepository
import com.airgf.app.domain.repository.UserRepository
import com.airgf.app.notification.ProactiveMessageScheduler
import javax.inject.Inject

class ResetEverythingUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val gfConfigRepository: GfConfigRepository,
    private val userRepository: UserRepository,
    private val modelRepository: ModelRepository,
    private val proactiveScheduler: ProactiveMessageScheduler,
    private val imageStorageUtil: ImageCleanup,
) {
    suspend operator fun invoke() {
        val existingModelPath = modelRepository.getModelPath()

        proactiveScheduler.disable()
        chatRepository.deleteAllMessages()
        gfConfigRepository.delete()
        userRepository.clearAll()
        imageStorageUtil.cleanupAllImages()

        if (existingModelPath != null) {
            modelRepository.setModelDownloaded(existingModelPath)
        }
    }
}
