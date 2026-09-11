package com.ghostgramlabs.speakalert.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ghostgramlabs.speakalert.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single source of truth for clock style across the app.
 *
 * Times are rendered from Compose screens, a widget's RemoteViews factory, notification builders
 * and pure utility functions, so the value is held here rather than threaded through every call
 * site. It is backed by Compose state so screens recompose the moment the setting changes.
 *
 * Until the user makes an explicit choice this mirrors the device's clock setting.
 */
object TimeFormat {

    private var use24HourState by mutableStateOf(false)
    private val clockFlow = MutableStateFlow(false)
    val changes = clockFlow.asStateFlow()
    private var savedOverride: Boolean? = null
    private var clockObserver: android.database.ContentObserver? = null

    /** True when times should render as 17:30 rather than 5:30 PM. */
    val use24Hour: Boolean
        get() = use24HourState

    /** Time-only pattern for [SimpleDateFormat]; embed in composite patterns as needed. */
    val timePattern: String
        get() = if (use24HourState) "HH:mm" else "h:mm a"

    fun formatTime(timestamp: Long): String =
        SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(timestamp))

    /** Formats [datePattern] followed by the current time pattern, e.g. "MMM d" -> "MMM d, 17:30". */
    fun formatDateTime(timestamp: Long, datePattern: String, separator: String = ", "): String =
        SimpleDateFormat("$datePattern$separator$timePattern", Locale.getDefault()).format(Date(timestamp))

    /**
     * Seeds from the device clock setting, then follows the user's stored preference.
     * Safe to call once from Application startup.
     */
    internal fun initialize(
        context: Context,
        scope: CoroutineScope,
        repository: SettingsRepository,
        readDeviceClock: () -> Boolean = {
            android.text.format.DateFormat.is24HourFormat(context.applicationContext)
        },
        onClockChanged: () -> Unit = {
            com.ghostgramlabs.speakalert.widget.SpeakAlertWidgetUpdater.requestUpdate(context.applicationContext)
        }
    ) {
        val appContext = context.applicationContext
        // Migrate older DataStore-only installations once. Subsequent starts use the small
        // synchronous mirror, just as the window theme does. Do not seed with the device
        // format when the user has already saved a different explicit app preference.
        savedOverride = if (ClockPreferences.hasValue(appContext)) {
            ClockPreferences.read(appContext)
        } else {
            runCatching {
                // Bound the one-time migration so storage trouble cannot consume the
                // foreground-service startup deadline. The async collector retries later.
                runBlocking(Dispatchers.IO) {
                    kotlinx.coroutines.withTimeout(500L) { repository.use24HourTimeOverride.first() }
                }.also { ClockPreferences.write(appContext, it) }
            }.getOrElse { null }
        }
        fun refresh() {
            val resolved = resolveClockFormat(savedOverride, readDeviceClock())
            if (use24HourState != resolved) {
                use24HourState = resolved
                clockFlow.value = resolved
                onClockChanged()
            }
        }
        refresh()
        clockObserver?.let { appContext.contentResolver.unregisterContentObserver(it) }
        clockObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { refresh() }
        }.also {
            appContext.contentResolver.registerContentObserver(
                android.provider.Settings.System.getUriFor(android.provider.Settings.System.TIME_12_24),
                false, it
            )
        }
        scope.launch {
            repository.use24HourTimeOverride.collect { override ->
                withContext(Dispatchers.Main) {
                    savedOverride = override
                    ClockPreferences.write(appContext, override)
                    refresh()
                }
            }
        }
    }

    /** Test seam — sets the value directly without a DataStore. */
    internal fun setForTesting(enabled: Boolean) {
        use24HourState = enabled
        clockFlow.value = enabled
    }
}

internal fun resolveClockFormat(override: Boolean?, deviceUses24Hour: Boolean): Boolean =
    override ?: deviceUses24Hour
