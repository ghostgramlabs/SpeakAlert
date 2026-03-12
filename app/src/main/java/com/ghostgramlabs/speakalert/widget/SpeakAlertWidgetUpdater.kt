package com.ghostgramlabs.speakalert.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object SpeakAlertWidgetUpdater {

    fun requestUpdate(context: Context) {
        requestProviderUpdate(context, QuickReminderWidgetProvider::class.java)
        requestProviderUpdate(context, UpcomingRemindersWidgetProvider::class.java)
    }

    private fun requestProviderUpdate(context: Context, providerClass: Class<*>) {
        val manager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, providerClass)
        val widgetIds = manager.getAppWidgetIds(componentName)
        if (widgetIds.isEmpty()) return
        context.sendBroadcast(
            Intent(context, providerClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            }
        )
    }
}
