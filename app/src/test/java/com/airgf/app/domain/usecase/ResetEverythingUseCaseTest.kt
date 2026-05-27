package com.airgf.app.domain.usecase

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
    @Test
    fun `clears app data and preserves downloaded model reference`() = runBlocking {
        val chatRepository = FakeChatRepository()
        val gfRepository = FakeGfConfigRepository()
        val userRepository = FakeUserRepository()
        val modelRepository = FakeModelRepository(modelPath = "/tmp/model.litertlm")
        val scheduler = FakeProactiveMessageScheduler()

        ResetEverythingUseCase(
            chatRepository = chatRepository,
            gfConfigRepository = gfRepository,
            userRepository = userRepository,
            modelRepository = modelRepository,
            proactiveScheduler = scheduler,
        )()

        assertEquals(1, scheduler.disableCalls)
        assertTrue(chatRepository.deleteAllMessagesCalled)
        assertTrue(gfRepository.deleted)
        assertTrue(userRepository.cleared)
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
        )()

        assertNull(modelRepository.setModelDownloadedPath)
    }
}
