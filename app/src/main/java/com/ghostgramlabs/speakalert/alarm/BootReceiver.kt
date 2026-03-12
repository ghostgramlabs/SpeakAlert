package com.ghostgramlabs.speakalert.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ghostgramlabs.speakalert.util.FileLogger
import com.ghostgramlabs.speakalert.widget.SpeakAlertWidgetUpdater
import kotlinx.coroutines.launch

/**
 * Receiver for BOOT_COMPLETED, TIME_CHANGED, and TIMEZONE_CHANGED broadcasts.
 *
 * IMPORTANT: On Android 15+, BOOT_COMPLETED receivers CANNOT start restricted
 * foreground service types (including mediaPlayback). To comply, this receiver
 * delegates ALL work to WorkManager via [BootRescheduleWorker], which runs
 * outside the BOOT_COMPLETED broadcast context and can safely trigger alarm
 * rescheduling that may eventually lead to foreground service starts.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            val asyncScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

            asyncScope.launch {
                try {
                    FileLogger.log("BOOT_RECEIVER: Received ${intent.action}")
                    
                    // ANDROID 15 GUARD: Persist boot timestamp
                    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                        try {
                            val app = context.applicationContext as com.ghostgramlabs.speakalert.VoiceReminderApp
                            val settingsRepository = app.container.settingsRepository
                            val now = System.currentTimeMillis()
                            settingsRepository.setLastBootTimestamp(now)
                            FileLogger.log("BOOT_RECEIVER: Persisted boot timestamp: $now")
                        } catch (e: Exception) {
                            FileLogger.logError("BOOT_RECEIVER", "Failed to persist boot timestamp", e)
                        }
                    }

                    // Enqueue Worker for safe rescheduling (OUTSIDE boot context)
                    val workRequest = OneTimeWorkRequestBuilder<BootRescheduleWorker>()
                        .setInputData(
                            workDataOf(BootRescheduleWorker.KEY_TRIGGER_ACTION to intent.action)
                        )
                        .build()
                    WorkManager.getInstance(context).enqueueUniqueWork(
                        BootRescheduleWorker.WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
                    FileLogger.log("BOOT_RECEIVER: WorkManager job enqueued")
                    SpeakAlertWidgetUpdater.requestUpdate(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
