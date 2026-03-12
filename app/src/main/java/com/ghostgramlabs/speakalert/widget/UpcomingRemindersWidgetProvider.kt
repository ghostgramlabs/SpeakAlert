package com.ghostgramlabs.speakalert.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.ghostgramlabs.speakalert.MainActivity
import com.ghostgramlabs.speakalert.R
import com.ghostgramlabs.speakalert.VoiceReminderApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UpcomingRemindersWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            val app = context.applicationContext as VoiceReminderApp
            val upcomingItems = loadUpcomingReminders(app)
            val missedItems = loadMissedReminders(app)

            appWidgetIds.forEach { widgetId ->
                val maxRows = maxRowsForWidget(appWidgetManager, widgetId)
                appWidgetManager.updateAppWidget(
                    widgetId,
                    buildRemoteViews(
                        context = context,
                        items = selectItemsForWidget(
                            upcomingItems = upcomingItems,
                            missedItems = missedItems,
                            maxRows = maxRows
                        )
                    )
                )
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    private fun buildRemoteViews(context: Context, items: List<WidgetItem>): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_upcoming_reminders).apply {
            bindRow(context, items.getOrNull(0), R.id.upcoming_row_1, R.id.upcoming_title_1, R.id.upcoming_time_1)
            bindRow(context, items.getOrNull(1), R.id.upcoming_row_2, R.id.upcoming_title_2, R.id.upcoming_time_2)
            bindRow(context, items.getOrNull(2), R.id.upcoming_row_3, R.id.upcoming_title_3, R.id.upcoming_time_3)
            bindRow(context, items.getOrNull(3), R.id.upcoming_row_4, R.id.upcoming_title_4, R.id.upcoming_time_4)
            val hasItems = items.isNotEmpty()
            val hasMissedItems = items.any { it.isMissed }
            setTextViewText(
                R.id.upcoming_subtitle,
                if (hasMissedItems) "Upcoming and missed reminders" else "Upcoming reminders"
            )
            setViewVisibility(R.id.upcoming_empty, if (hasItems) View.GONE else View.VISIBLE)
            if (!hasItems) {
                setTextViewText(R.id.upcoming_empty, "No upcoming or missed reminders")
            }
        }
    }

    private fun RemoteViews.bindRow(
        context: Context,
        item: WidgetItem?,
        rowId: Int,
        titleId: Int,
        timeId: Int
    ) {
        if (item == null) {
            setViewVisibility(rowId, View.GONE)
            return
        }

        setViewVisibility(rowId, View.VISIBLE)
        setTextViewText(titleId, item.title)
        setTextViewText(timeId, item.whenLabel)
        setOnClickPendingIntent(
            rowId,
            PendingIntent.getActivity(
                context,
                item.stableRequestCode,
                buildOpenAppIntent(context, item.reminderId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
    }

    private fun buildOpenAppIntent(context: Context, reminderId: Long?): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (reminderId != null) {
                putExtra("reminderId", reminderId)
            }
        }
    }

    private suspend fun loadUpcomingReminders(app: VoiceReminderApp): List<WidgetItem> {
        val now = System.currentTimeMillis()
        return app.container.reminderRepository.getAllActiveReminders()
            .filter { (it.snoozeUntil ?: it.nextTriggerAt) >= now }
            .sortedBy { it.snoozeUntil ?: it.nextTriggerAt }
            .map { reminder ->
                val triggerAt = reminder.snoozeUntil ?: reminder.nextTriggerAt
                WidgetItem(
                    stableRequestCode = reminder.id.hashCode(),
                    reminderId = reminder.id,
                    title = buildDisplayTitle(reminder.title, reminder.reminderText),
                    whenLabel = formatWhen(triggerAt),
                    isMissed = false
                )
            }
    }

    private suspend fun loadMissedReminders(app: VoiceReminderApp): List<WidgetItem> {
        return app.container.missedReminderRepository.allMissedReminders
            .first()
            .sortedByDescending { it.detectedTime }
            .map { missed ->
                WidgetItem(
                    stableRequestCode = (-1L - missed.id).hashCode(),
                    reminderId = missed.reminderId.takeIf { it > 0L },
                    title = buildDisplayTitle(missed.title, missed.reminderText),
                    whenLabel = "Missed \u2022 ${formatWhen(missed.scheduledTime)}",
                    isMissed = true
                )
            }
    }

    private fun selectItemsForWidget(
        upcomingItems: List<WidgetItem>,
        missedItems: List<WidgetItem>,
        maxRows: Int
    ): List<WidgetItem> {
        if (maxRows <= 0) return emptyList()
        if (upcomingItems.isEmpty()) return missedItems.take(maxRows)
        if (missedItems.isEmpty()) return upcomingItems.take(maxRows)

        val rows = mutableListOf<WidgetItem>()

        // Reserve space for one missed and one upcoming item so both are visible when available.
        rows += missedItems.first()
        if (rows.size < maxRows) rows += upcomingItems.first()

        val remaining = maxRows - rows.size
        if (remaining > 0) {
            rows += upcomingItems.drop(1).take(remaining)
        }
        val stillRemaining = maxRows - rows.size
        if (stillRemaining > 0) {
            rows += missedItems.drop(1).take(stillRemaining)
        }

        return rows
    }

    private fun buildDisplayTitle(title: String?, reminderText: String?): String {
        val userTitle = title
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals("SpeakAlert", ignoreCase = true) }
        if (userTitle != null) return userTitle

        val textFallback = reminderText
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { text ->
                val words = text.split(Regex("\\s+"))
                if (words.size > 8) words.take(8).joinToString(" ") else text
            }
        return textFallback ?: "Reminder"
    }

    private fun formatWhen(triggerAt: Long): String {
        val cal = java.util.Calendar.getInstance()
        val todayYear = cal.get(java.util.Calendar.YEAR)
        val today = cal.get(java.util.Calendar.DAY_OF_YEAR)
        cal.timeInMillis = triggerAt
        val targetYear = cal.get(java.util.Calendar.YEAR)
        val targetDay = cal.get(java.util.Calendar.DAY_OF_YEAR)
        val time = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(triggerAt))
        return when {
            targetYear == todayYear && targetDay == today -> {
                "Today \u2022 $time"
            }
            targetYear == todayYear && targetDay == today + 1 -> "Tomorrow \u2022 $time"
            else -> "${java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(triggerAt))} \u2022 $time"
        }
    }

    private fun maxRowsForWidget(appWidgetManager: AppWidgetManager, widgetId: Int): Int {
        val minHeight = appWidgetManager.getAppWidgetOptions(widgetId)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 150)
        return when {
            minHeight >= 220 -> 4
            minHeight >= 160 -> 3
            else -> 2
        }
    }

    private data class WidgetItem(
        val stableRequestCode: Int,
        val reminderId: Long?,
        val title: String,
        val whenLabel: String,
        val isMissed: Boolean
    )
}
