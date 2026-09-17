package com.ghostgramlabs.speakalert.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

object FullScreenIntentSupport {

    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.canUseFullScreenIntent()
    }

    /**
     * Open wherever this device lets the user grant full-screen access, returning whether any
     * screen actually opened.
     *
     * The dedicated screen is not guaranteed to exist: it arrived in Android 14, and OEM builds
     * are free to omit or rename it. Falling through to the app's own details page still gets
     * the user somewhere useful, and a device with neither must not take the app down over a
     * settings shortcut.
     */
    fun openSettings(context: Context): Boolean {
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            }
            add(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }

        for (action in candidates) {
            val intent = Intent(action).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching { context.startActivity(intent) }.isSuccess) return true
            Log.w(TAG, "No settings screen answered $action")
        }
        return false
    }

    private const val TAG = "FullScreenIntentSupport"
}
