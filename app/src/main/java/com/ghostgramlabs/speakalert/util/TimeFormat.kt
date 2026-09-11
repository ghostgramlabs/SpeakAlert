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
    fun initialize(context: Context, scope: CoroutineScope, repository: SettingsRepository) {
        val appContext = context.applicationContext
        // Seed synchronously. An alarm can wake the process cold and build a notification before
        // DataStore has emitted, and that notification must not render in the wrong clock style.
        use24HourState = android.text.format.DateFormat.is24HourFormat(appContext)
        scope.launch {
            repository.use24HourTimeOverride.collect { override ->
                val resolved = override ?: android.text.format.DateFormat.is24HourFormat(appContext)
                // Compose state must be written from the main thread.
                withContext(Dispatchers.Main) {
                    if (use24HourState != resolved) {
                        use24HourState = resolved
                        // Widgets render into RemoteViews outside composition, so they only pick
                        // up a new clock style when their provider is asked to redraw.
                        com.ghostgramlabs.speakalert.widget.SpeakAlertWidgetUpdater
                            .requestUpdate(appContext)
                    }
                }
            }
        }
    }

    /** Test seam — sets the value directly without a DataStore. */
    internal fun setForTesting(enabled: Boolean) {
        use24HourState = enabled
    }
}
