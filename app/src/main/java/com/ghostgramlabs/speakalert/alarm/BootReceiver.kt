package com.ghostgramlabs.speakalert.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ghostgramlabs.speakalert.VoiceReminderApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val app = context.applicationContext as VoiceReminderApp
            val repository = app.container.reminderRepository
            val scheduler = app.container.alarmScheduler

            CoroutineScope(Dispatchers.IO).launch {
                val activeReminders = repository.getAllActiveReminders()
                activeReminders.forEach { reminder ->
                    // Reschedule if in future
                    // If in past, maybe fire ASAP? 
                    // Scheduler handles setExact logic.
                    // Spec: "return to execution... fire ASAP when device resumes"
                    // AlarmManager setExact on past time triggers immediately, so just scheduling is enough.
                    scheduler.schedule(reminder)
                }
            }
        }
    }
}
