package com.ghostgramlabs.speakalert.alarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.ghostgramlabs.speakalert.VoiceReminderApp
import com.ghostgramlabs.speakalert.data.model.MissedReminderEntity
import com.ghostgramlabs.speakalert.domain.RecurrenceUtils
import com.ghostgramlabs.speakalert.domain.models.MissedPolicy
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import com.ghostgramlabs.speakalert.domain.models.TimeUnit
import com.ghostgramlabs.speakalert.service.ReminderPlaybackService
import com.ghostgramlabs.speakalert.util.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        // Grace windows for determining if a reminder is "late"
        private const val GRACE_WINDOW_INTERVAL_MS = 60 * 1000L       // 60 seconds for minutes/hours
        private const val GRACE_WINDOW_CALENDAR_MS = 5 * 60 * 1000L  // 5 minutes for daily/weekly/monthly
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminderId", -1L)
        if (reminderId == -1L) {
            FileLogger.log("ALARM: Received but no reminderId")
            return
        }

        FileLogger.log("ALARM: Received for reminder $reminderId")
        Log.d("VoiceReminder", "Alarm Received for reminder $reminderId")
        
        val notificationHelper = NotificationHelper(context)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                FileLogger.log("ALARM: Starting coroutine processing")
                
                val app = context.applicationContext as VoiceReminderApp
                val repository = app.container.reminderRepository
                val settingsRepository = app.container.settingsRepository
                val scheduler = app.container.alarmScheduler
                val missedRepository = app.container.missedReminderRepository

                FileLogger.log("ALARM: Got app components")

                val reminder = repository.getReminder(reminderId)
                if (reminder == null) {
                    FileLogger.log("ALARM: Reminder $reminderId NOT FOUND in database!")
                    withContext(Dispatchers.Main) {
                        notificationHelper.showNotification(
                            reminderId,
                            "SpeakAlert",
                            "Reminder not found in database"
                        )
                    }
                    return@launch
                }

                FileLogger.log("ALARM: Found reminder - title='${reminder.title}', recurrenceType=${reminder.recurrenceType}")

                val now = System.currentTimeMillis()
                val scheduledTime = reminder.nextTriggerAt
                
                // CRITICAL FIX: explicit check to prevent double-firing (e.g. after reboot)
                // If we already fired for this scheduled time (or later), do not fire again.
                // We use a small buffer (1 second) just in case of minor timestamp differences, but strictly logic is >=
                if (reminder.lastFiredAt != null && reminder.lastFiredAt!! >= scheduledTime) {
                    FileLogger.log("ALARM: Reminder $reminderId already fired at ${reminder.lastFiredAt} (>= $scheduledTime). Skipping.")
                    return@launch
                }

                val delay = now - scheduledTime
                val graceWindow = getGraceWindow(reminder.recurrenceType, reminder.recurrenceJson)
                val isLate = delay > graceWindow
                
                FileLogger.log("ALARM: scheduledTime=$scheduledTime, now=$now, delay=${delay}ms, graceWindow=${graceWindow}ms, isLate=$isLate")

                // Get missed policy from recurrence model (default: SKIP_TO_NEXT)
                val recurrenceModel = RecurrenceUtils.fromJson(reminder.recurrenceType, reminder.recurrenceJson)
                val missedPolicy = recurrenceModel?.missedPolicy ?: MissedPolicy.SKIP_TO_NEXT
                
                FileLogger.log("ALARM: missedPolicy=$missedPolicy")

                
                // ===== DND & QUIET TIME CHECK =====
                // Force "Missed" if DND is active or if we are in Quiet Time
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val isDndActive = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
                
                val quietTimeEnabled = settingsRepository.quietTimeEnabled.first()
                var isQuietTime = false
                if (quietTimeEnabled) {
                    val startHour = settingsRepository.quietTimeStartHour.first()
                    val startMinute = settingsRepository.quietTimeStartMinute.first()
                    val endHour = settingsRepository.quietTimeEndHour.first()
                    val endMinute = settingsRepository.quietTimeEndMinute.first()
                    
                    if (isTimeInWindow(now, startHour, startMinute, endHour, endMinute)) {
                        isQuietTime = true
                    }
                }
                
                if (isQuietTime || isDndActive) {
                    FileLogger.log("ALARM: Silenced by ${if (isDndActive) "DND" else "Quiet Time"} ($now)")
                    
                    // Add to Missed Inbox
                    val missedEntry = MissedReminderEntity(
                        reminderId = reminder.id,
                        title = reminder.title ?: "SpeakAlert",
                        scheduledTime = scheduledTime,
                        detectedTime = now,
                        reminderText = reminder.reminderText
                    )
                    missedRepository.insertMissedReminder(missedEntry)
                    
                    // Schedule Next Recurrence
                    var updatedReminder = reminder.copy(lastFiredAt = now, snoozeUntil = null)
                    updatedReminder = RecurrenceUtils.updateForNextOccurrence(updatedReminder)
                    val nextTrigger = RecurrenceUtils.computeNextTrigger(updatedReminder, now)
                    
                    if (nextTrigger != null) {
                         updatedReminder = updatedReminder.copy(nextTriggerAt = nextTrigger)
                         scheduler.schedule(updatedReminder)
                         FileLogger.log("ALARM: Quiet Time - Scheduled next at $nextTrigger")
                    } else {
                        // If one-time reminder fell in quiet time?
                        if (reminder.recurrenceType == RecurrenceType.NONE) {
                             updatedReminder = updatedReminder.copy(isCompleted = true, completedAt = now)
                             FileLogger.log("ALARM: Quiet Time - Marked one-time reminder as completed/missed")
                        }
                    }
                    repository.updateReminder(updatedReminder)
                    
                    return@launch // EXIT - No notification
                }

                // Handle late/missed reminders (Existing Logic)
                if (isLate && reminder.recurrenceType != RecurrenceType.NONE) {
                    FileLogger.log("ALARM: Reminder is LATE (missed)")
                    
                    when (missedPolicy) {
                        MissedPolicy.SKIP_TO_NEXT -> {
                            // Log to missed inbox, skip notification, schedule next
                            FileLogger.log("ALARM: SKIP_TO_NEXT - logging to missed inbox and scheduling next")
                            
                            // Add to missed inbox
                            val missedEntry = MissedReminderEntity(
                                reminderId = reminder.id,
                                title = reminder.title ?: "SpeakAlert",
                                scheduledTime = scheduledTime,
                                detectedTime = now,
                                reminderText = reminder.reminderText
                            )
                            missedRepository.insertMissedReminder(missedEntry)
                            
                            // Schedule next occurrence without showing notification
                            var updatedReminder = reminder.copy(lastFiredAt = now, snoozeUntil = null)
                            updatedReminder = RecurrenceUtils.updateForNextOccurrence(updatedReminder)
                            val nextTrigger = RecurrenceUtils.computeNextTrigger(updatedReminder, now)
                            
                            if (nextTrigger != null) {
                                updatedReminder = updatedReminder.copy(nextTriggerAt = nextTrigger)
                                scheduler.schedule(updatedReminder)
                                FileLogger.log("ALARM: Scheduled next occurrence at $nextTrigger")
                            }
                            repository.updateReminder(updatedReminder)
                            
                            return@launch // Exit early - no notification
                        }
                        
                        MissedPolicy.FIRE_ON_RESUME -> {
                            // Show "Missed" notification (no autoplay), then schedule next
                            FileLogger.log("ALARM: FIRE_ON_RESUME - showing missed notification")
                            
                            withContext(Dispatchers.Main) {
                                notificationHelper.showNotification(
                                    reminder.id,
                                    "Missed: ${reminder.title ?: "SpeakAlert"}",
                                    "Scheduled for ${formatTime(scheduledTime)} - tap to play",
                                    audioPath = reminder.audioPath,
                                    reminderText = reminder.reminderText
                                )
                            }
                            
                            // Still schedule next occurrence
                            var updatedReminder = reminder.copy(lastFiredAt = now, snoozeUntil = null)
                            updatedReminder = RecurrenceUtils.updateForNextOccurrence(updatedReminder)
                            val nextTrigger = RecurrenceUtils.computeNextTrigger(updatedReminder, now)
                            
                            if (nextTrigger != null) {
                                updatedReminder = updatedReminder.copy(nextTriggerAt = nextTrigger)
                                scheduler.schedule(updatedReminder)
                            }
                            repository.updateReminder(updatedReminder)
                            
                            return@launch // Exit - showed missed notification, no autoplay
                        }
                    }
                }

                // ===== NORMAL ON-TIME REMINDER HANDLING =====
                
                // Get settings
                val autoPlayEnabled = settingsRepository.autoPlayEnabled.first()
                val unlockedOnly = settingsRepository.autoPlayOnUnlockOnly.first()
                val speakText = settingsRepository.speakTextIfNoVoice.first()
                
                FileLogger.log("ALARM: Settings - autoPlay=$autoPlayEnabled, unlockedOnly=$unlockedOnly, speakText=$speakText")
                
                val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                val isLocked = keyguardManager.isKeyguardLocked
                
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val inCall = audioManager.mode == AudioManager.MODE_IN_CALL || audioManager.mode == AudioManager.MODE_IN_COMMUNICATION

                val audioPath = reminder.audioPath
                val hasAudio = !audioPath.isNullOrBlank() && java.io.File(audioPath).exists()
                val hasText = !reminder.reminderText.isNullOrBlank()
                
                FileLogger.log("ALARM: State - locked=$isLocked, inCall=$inCall, hasAudio=$hasAudio, hasText=$hasText")
                
                val displayText = reminder.reminderText ?: "Tap to view"
                val title = reminder.title ?: "SpeakAlert"

                // ===== ANDROID 15 BOOT & AUTOPLAY LOGIC =====
                val isBootReschedule = intent.getBooleanExtra("isBootReschedule", false)
                
                // GUARD: Check if we are too close to boot time (2 minutes)
                // If the app is still in the "boot completion" window, we must NOT start
                // a mediaPlayback foreground service.
                val lastBoot = settingsRepository.lastBootTimestamp.first()
                val timeSinceBoot = now - lastBoot
                val isCloseToBoot = timeSinceBoot < 120_000L // 2 minutes
                val isAndroid15OrAbove = Build.VERSION.SDK_INT >= 35

                // ANDROID 15+ RESTRICTION: Block autoplay only while still in the boot window.
                // isBootReschedule is retained for diagnostics and for identifying boot-origin alarms.
                val bootBlocked = isAndroid15OrAbove && isCloseToBoot
                
                if (bootBlocked) {
                    FileLogger.log("ALARM: Autoplay BLOCKED. android15+=$isAndroid15OrAbove, isBootReschedule=$isBootReschedule, timeSinceBoot=${timeSinceBoot/1000}s (threshold 120s)")
                }
                
                val canAutoPlay = autoPlayEnabled && 
                                 !inCall && 
                                 !(unlockedOnly && isLocked) && 
                                 (hasAudio || (hasText && speakText)) &&
                                 !bootBlocked
                
                FileLogger.log("ALARM: android15+=$isAndroid15OrAbove, bootReschedule=$isBootReschedule, bootBlocked=$bootBlocked, canAutoPlay=$canAutoPlay")

                if (canAutoPlay) {
                    FileLogger.log("ALARM: Attempting to start service for autoplay")
                    try {
                        withContext(Dispatchers.Main) {
                            if (hasAudio) {
                                FileLogger.log("ALARM: Starting service with audio: $audioPath, loop=${reminder.loopPlayback}")
                                ReminderPlaybackService.start(
                                    context = context,
                                    id = reminder.id,
                                    title = title,
                                    audioPath = audioPath!!,
                                    ttsText = null,
                                    loop = reminder.loopPlayback,
                                    isFromBootContext = isCloseToBoot
                                )
                            } else {
                                FileLogger.log("ALARM: Starting service with TTS, loop=${reminder.loopPlayback}")
                                ReminderPlaybackService.start(
                                    context = context,
                                    id = reminder.id,
                                    title = title,
                                    audioPath = null,
                                    ttsText = reminder.reminderText,
                                    loop = reminder.loopPlayback,
                                    isFromBootContext = isCloseToBoot
                                )
                            }
                        }
                        FileLogger.log("ALARM: Service started successfully")
                    } catch (e: Exception) {
                        // Catches ForegroundServiceStartNotAllowedException or any SecurityException
                        FileLogger.logError("ALARM", "Failed to start service (likely Android 15 FGS restriction)", e)
                        // Note: If service fails, the notification is still shown below, fulfilling the "Tap to play" fallback.
                    }
                    
                    // ALWAYS show notification after autoplay (it stays until user dismisses)
                    withContext(Dispatchers.Main) {
                        notificationHelper.showNotification(
                            reminder.id,
                            title,
                            displayText,
                            audioPath = if (hasAudio) audioPath else null,
                            reminderText = if (hasText) reminder.reminderText else null
                        )
                    }
                    FileLogger.log("ALARM: Showed notification after autoplay")
                } else {
                    FileLogger.log("ALARM: Showing standard notification (no autoplay)")
                    withContext(Dispatchers.Main) {
                        notificationHelper.showNotification(
                            reminder.id,
                            title,
                            displayText,
                            audioPath = if (hasAudio) audioPath else null,
                            reminderText = if (hasText) reminder.reminderText else null
                        )
                    }
                    FileLogger.log("ALARM: Notification shown")
                }

                // Update state and AUTO-RESCHEDULE for recurring reminders
                // This ensures the next trigger time is reflected in the UI immediately
                var updatedReminder = reminder.copy(
                    lastFiredAt = now,
                    snoozeUntil = null
                )
                
                if (reminder.recurrenceType != RecurrenceType.NONE) {
                    FileLogger.log("ALARM: Recurring reminder - auto-rescheduling")
                    // Advance recurrence count if needed
                    updatedReminder = RecurrenceUtils.updateForNextOccurrence(updatedReminder)
                    // Compute next trigger time
                    val nextTrigger = RecurrenceUtils.computeNextTrigger(updatedReminder, now)
                    if (nextTrigger != null) {
                        updatedReminder = updatedReminder.copy(nextTriggerAt = nextTrigger)
                        scheduler.schedule(updatedReminder)
                        FileLogger.log("ALARM: Auto-rescheduled next occurrence at $nextTrigger")
                    } else {
                        // Recurrence ended
                        updatedReminder = updatedReminder.copy(isCompleted = true, completedAt = now)
                        scheduler.cancel(updatedReminder)
                        FileLogger.log("ALARM: Recurrence ended, marked completed")
                    }
                } else {
                    // One-time reminder: mark completed immediately after firing
                    // This moves it to the 'Done' tab automatically
                    FileLogger.log("ALARM: One-time reminder - marking completed")
                    updatedReminder = updatedReminder.copy(isCompleted = true, completedAt = now)
                    scheduler.cancel(updatedReminder)
                }
                
                repository.updateReminder(updatedReminder)
                FileLogger.log("ALARM: Database updated")
                
            } catch (e: Exception) {
                FileLogger.logError("ALARM", "Error processing alarm", e)
                try {
                    withContext(Dispatchers.Main) {
                        notificationHelper.showNotification(
                            reminderId,
                            "SpeakAlert",
                            "Error: ${e.message?.take(50) ?: "Unknown"}"
                        )
                    }
                    FileLogger.log("ALARM: Showed error notification")
                } catch (e2: Exception) {
                    FileLogger.logError("ALARM", "Failed to show error notification", e2)
                }
            } finally {
                FileLogger.log("ALARM: Processing complete, finishing pending result")
                pendingResult.finish()
            }
        }
    }

    /**
     * Returns the grace window in milliseconds based on recurrence type.
     * - Interval-based (minutes/hours): 60 seconds
     * - Calendar-based (daily/weekly/monthly): 5 minutes
     */
    private fun getGraceWindow(recurrenceType: RecurrenceType, recurrenceJson: String?): Long {
        if (recurrenceType == RecurrenceType.NONE) {
            // One-time reminders: always show even if late
            return Long.MAX_VALUE
        }
        
        if (recurrenceType == RecurrenceType.CUSTOM) {
            // Check if it's interval-based (minutes/hours)
            val model = RecurrenceUtils.fromJson(recurrenceType, recurrenceJson)
            if (model is com.ghostgramlabs.speakalert.domain.models.RecurrenceModel.Custom) {
                return when (model.unit) {
                    TimeUnit.MINUTES, TimeUnit.HOURS -> GRACE_WINDOW_INTERVAL_MS
                    else -> GRACE_WINDOW_CALENDAR_MS
                }
            }
        }
        
        // Daily, Weekly, Monthly use calendar-based grace window
        return GRACE_WINDOW_CALENDAR_MS
    }

    private fun formatTime(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }
    
    private fun isTimeInWindow(now: Long, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): Boolean {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = now
        val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(java.util.Calendar.MINUTE)
        
        val currentTotalMinutes = currentHour * 60 + currentMinute
        val startTotalMinutes = startHour * 60 + startMinute
        val endTotalMinutes = endHour * 60 + endMinute
        
        return if (startTotalMinutes <= endTotalMinutes) {
            // Standard range (e.g. 9 AM to 5 PM)
            currentTotalMinutes in startTotalMinutes until endTotalMinutes
        } else {
            // Crosses midnight (e.g. 10 PM to 7 AM)
            currentTotalMinutes >= startTotalMinutes || currentTotalMinutes < endTotalMinutes
        }
    }
}
