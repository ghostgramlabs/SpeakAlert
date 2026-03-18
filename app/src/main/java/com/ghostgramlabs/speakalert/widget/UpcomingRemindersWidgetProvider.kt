package com.ghostgramlabs.speakalert.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.ghostgramlabs.speakalert.MainActivity
import com.ghostgramlabs.speakalert.R

class UpcomingRemindersWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.upcoming_list)
        appWidgetIds.forEach { widgetId ->
            appWidgetManager.updateAppWidget(widgetId, buildRemoteViews(context, widgetId))
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

    private fun buildRemoteViews(context: Context, widgetId: Int): RemoteViews {
        val remoteAdapterIntent = Intent(context, UpcomingRemindersRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        return RemoteViews(context.packageName, R.layout.widget_upcoming_reminders).apply {
            setRemoteAdapter(R.id.upcoming_list, remoteAdapterIntent)
            setEmptyView(R.id.upcoming_list, R.id.upcoming_empty)
            setPendingIntentTemplate(
                R.id.upcoming_list,
                PendingIntent.getActivity(
                    context,
                    widgetId,
                    buildOpenAppIntent(context),
                    templatePendingIntentFlags()
                )
            )
            setOnClickPendingIntent(
                R.id.upcoming_widget_root,
                PendingIntent.getActivity(
                    context,
                    widgetId + 10_000,
                    buildOpenAppIntent(context),
                    rootPendingIntentFlags()
                )
            )
        }
    }

    private fun buildOpenAppIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }

    private fun templatePendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
    }

    private fun rootPendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }
}
