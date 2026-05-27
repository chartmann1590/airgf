package com.airgf.app.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.airgf.app.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProactiveScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
) : ProactiveMessageScheduler {
    override suspend fun sync() {
        if (!userRepository.isOnboardingComplete() || !userRepository.areProactiveMessagesEnabled()) {
            disable()
            return
        }

        schedule(userRepository.getNotificationFrequency())
    }

    override fun disable() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private fun schedule(frequency: String) {
        val spec = notificationFrequencySpec(frequency)
        val workRequest = PeriodicWorkRequestBuilder<ProactiveMessageWorker>(
            spec.repeatIntervalHours,
            TimeUnit.HOURS,
            spec.flexIntervalHours,
            TimeUnit.HOURS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "gf_proactive"
    }
}

internal fun notificationFrequencySpec(frequency: String): FrequencySpec =
    when (frequency.lowercase()) {
        "rarely" -> FrequencySpec(repeatIntervalHours = 8, flexIntervalHours = 4)
        "often" -> FrequencySpec(repeatIntervalHours = 2, flexIntervalHours = 1)
        else -> FrequencySpec(repeatIntervalHours = 4, flexIntervalHours = 2)
    }

internal data class FrequencySpec(
    val repeatIntervalHours: Long,
    val flexIntervalHours: Long,
)
