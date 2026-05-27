package com.airgf.app.domain.usecase

import com.airgf.app.core.util.ImageCleanup
import com.airgf.app.testutil.FakeChatRepository
import com.airgf.app.testutil.FakeGfConfigRepository
import com.airgf.app.testutil.FakeModelRepository
import com.airgf.app.testutil.FakeProactiveMessageScheduler
import com.airgf.app.testutil.FakeUserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResetEverythingUseCaseTest {

    private class FakeImageCleanup : ImageCleanup {
        var cleanupCalled = false
        override fun cleanupAllImages() {
            cleanupCalled = true
        }
    }

    @Test
    fun `clears app data and preserves downloaded model reference`() = runBlocking {
        val chatRepository = FakeChatRepository()
        val gfRepository = FakeGfConfigRepository()
        val userRepository = FakeUserRepository()
        val modelRepository = FakeModelRepository(modelPath = "/tmp/model.litertlm")
        val scheduler = FakeProactiveMessageScheduler()
        val imageCleanup = FakeImageCleanup()

        ResetEverythingUseCase(
            chatRepository = chatRepository,
            gfConfigRepository = gfRepository,
            userRepository = userRepository,
            modelRepository = modelRepository,
            proactiveScheduler = scheduler,
            imageStorageUtil = imageCleanup,
        )()

        assertEquals(1, scheduler.disableCalls)
        assertTrue(chatRepository.deleteAllMessagesCalled)
        assertTrue(gfRepository.deleted)
        assertTrue(userRepository.cleared)
        assertTrue(imageCleanup.cleanupCalled)
        assertEquals("/tmp/model.litertlm", modelRepository.setModelDownloadedPath)
    }

    @Test
    fun `does not restore model path when none exists`() = runBlocking {
        val modelRepository = FakeModelRepository(modelPath = null)

        ResetEverythingUseCase(
            chatRepository = FakeChatRepository(),
            gfConfigRepository = FakeGfConfigRepository(),
            userRepository = FakeUserRepository(),
            modelRepository = modelRepository,
            proactiveScheduler = FakeProactiveMessageScheduler(),
            imageStorageUtil = FakeImageCleanup(),
        )()

        assertNull(modelRepository.setModelDownloadedPath)
    }
}
