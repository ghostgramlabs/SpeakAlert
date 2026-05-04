package com.ghostgramlabs.speakalert.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationSupport {
    private const val TAG = "BatteryOptimization"

    fun isBatteryOptimizationEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
        return !powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false

        val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (startIfResolvable(context, requestIntent)) {
            return true
        }

        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (startIfResolvable(context, fallbackIntent)) {
            return true
        }

        val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return startIfResolvable(context, appSettingsIntent)
    }

    private fun startIfResolvable(context: Context, intent: Intent): Boolean {
        val packageManager = context.packageManager
        if (intent.resolveActivity(packageManager) == null) {
            Log.w(TAG, "No activity found for intent: ${intent.action}")
            return false
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Resolved activity failed to launch for intent: ${intent.action}", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "Not allowed to launch intent: ${intent.action}", e)
            false
        } catch (e: RuntimeException) {
            Log.w(TAG, "Failed to launch intent: ${intent.action}", e)
            false
        }
    }
}
