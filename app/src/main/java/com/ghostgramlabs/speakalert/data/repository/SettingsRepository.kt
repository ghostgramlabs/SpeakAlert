package com.ghostgramlabs.speakalert.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val AUTO_PLAY_ENABLED = booleanPreferencesKey("auto_play_enabled")
        val AUTO_PLAY_ON_UNLOCK_ONLY = booleanPreferencesKey("auto_play_on_unlock_only")
        val SPEAK_TEXT_IF_NO_VOICE = booleanPreferencesKey("speak_text_if_no_voice")
        val TONE_ONLY_MODE = booleanPreferencesKey("tone_only_mode")
        val TONE_ONLY_ALERT_TONE_URI = stringPreferencesKey("tone_only_alert_tone_uri")
        val DEFAULT_SNOOZE_DURATION = intPreferencesKey("default_snooze_duration")
        val DEFAULT_MISSED_POLICY = stringPreferencesKey("default_missed_policy") // "FIRE" or "SKIP"
        val DEBUG_LOGGING_ENABLED = booleanPreferencesKey("debug_logging_enabled")
        val APP_VOLUME = floatPreferencesKey("app_volume")
        val LOOP_TIMEOUT_MINUTES = intPreferencesKey("loop_timeout_minutes") // 0 = never, 1/2/5/10 minutes
        
        // Quiet Time Keys
        val QUIET_TIME_ENABLED = booleanPreferencesKey("quiet_time_enabled")
        val QUIET_TIME_START_HOUR = intPreferencesKey("quiet_time_start_hour")
        val QUIET_TIME_START_MINUTE = intPreferencesKey("quiet_time_start_minute")
        val QUIET_TIME_END_HOUR = intPreferencesKey("quiet_time_end_hour")
        val QUIET_TIME_END_MINUTE = intPreferencesKey("quiet_time_end_minute")
        
        val THEME_MODE = intPreferencesKey("theme_mode") // 0 = System, 1 = Light, 2 = Dark
        val FULL_SCREEN_ALERT_ENABLED = booleanPreferencesKey("full_screen_alert_enabled")
        val BATTERY_OPTIMIZATION_PROMPT_SHOWN = booleanPreferencesKey("battery_optimization_prompt_shown")
        val LAST_WHATS_NEW_VERSION_SHOWN = stringPreferencesKey("last_whats_new_version_shown")
        
        // Android 15 FGS Boot Guard
        val LAST_BOOT_TIMESTAMP = longPreferencesKey("last_boot_timestamp")
    }

    val autoPlayEnabled: Flow<Boolean> = dataStore.data.map { it[AUTO_PLAY_ENABLED] ?: true }
    val autoPlayOnUnlockOnly: Flow<Boolean> = dataStore.data.map { it[AUTO_PLAY_ON_UNLOCK_ONLY] ?: false }
    val speakTextIfNoVoice: Flow<Boolean> = dataStore.data.map { it[SPEAK_TEXT_IF_NO_VOICE] ?: true }
    val toneOnlyMode: Flow<Boolean> = dataStore.data.map { it[TONE_ONLY_MODE] ?: false }
    val toneOnlyAlertToneUri: Flow<String?> = dataStore.data.map { it[TONE_ONLY_ALERT_TONE_URI] }
    val defaultSnoozeDuration: Flow<Int> = dataStore.data.map { it[DEFAULT_SNOOZE_DURATION] ?: 5 } // Minutes (Default 5)
    val defaultMissedPolicy: Flow<String> = dataStore.data.map { it[DEFAULT_MISSED_POLICY] ?: "FIRE_ON_RESUME" }
    val debugLoggingEnabled: Flow<Boolean> = dataStore.data.map { it[DEBUG_LOGGING_ENABLED] ?: false }
    val appVolume: Flow<Float> = dataStore.data.map { it[APP_VOLUME] ?: 1.0f }
    val loopTimeoutMinutes: Flow<Int> = dataStore.data.map { it[LOOP_TIMEOUT_MINUTES] ?: 10 } // Default 10 min, 0 = never
    
    // Quiet Time Flows (Default: 10 PM to 7 AM)
    val quietTimeEnabled: Flow<Boolean> = dataStore.data.map { it[QUIET_TIME_ENABLED] ?: false }
    val quietTimeStartHour: Flow<Int> = dataStore.data.map { it[QUIET_TIME_START_HOUR] ?: 22 }
    val quietTimeStartMinute: Flow<Int> = dataStore.data.map { it[QUIET_TIME_START_MINUTE] ?: 0 }
    val quietTimeEndHour: Flow<Int> = dataStore.data.map { it[QUIET_TIME_END_HOUR] ?: 7 }
    val quietTimeEndMinute: Flow<Int> = dataStore.data.map { it[QUIET_TIME_END_MINUTE] ?: 0 }
    
    val themeMode: Flow<Int> = dataStore.data.map { it[THEME_MODE] ?: 0 }
    val fullScreenAlertEnabled: Flow<Boolean> = dataStore.data.map { it[FULL_SCREEN_ALERT_ENABLED] ?: false }
    val batteryOptimizationPromptShown: Flow<Boolean> = dataStore.data.map {
        it[BATTERY_OPTIMIZATION_PROMPT_SHOWN] ?: false
    }
    val lastWhatsNewVersionShown: Flow<String?> = dataStore.data.map {
        it[LAST_WHATS_NEW_VERSION_SHOWN]
    }

    suspend fun setAutoPlayEnabled(enabled: Boolean) {
        dataStore.edit { it[AUTO_PLAY_ENABLED] = enabled }
    }

    suspend fun setAutoPlayOnUnlockOnly(enabled: Boolean) {
        dataStore.edit { it[AUTO_PLAY_ON_UNLOCK_ONLY] = enabled }
    }

    suspend fun setSpeakTextIfNoVoice(enabled: Boolean) {
        dataStore.edit { it[SPEAK_TEXT_IF_NO_VOICE] = enabled }
    }

    suspend fun setToneOnlyMode(enabled: Boolean) {
        dataStore.edit { it[TONE_ONLY_MODE] = enabled }
    }

    suspend fun setToneOnlyAlertToneUri(uri: String?) {
        dataStore.edit {
            if (uri.isNullOrBlank()) {
                it.remove(TONE_ONLY_ALERT_TONE_URI)
            } else {
                it[TONE_ONLY_ALERT_TONE_URI] = uri
            }
        }
    }

    suspend fun setDefaultSnoozeDuration(minutes: Int) {
        dataStore.edit { it[DEFAULT_SNOOZE_DURATION] = minutes }
    }

    suspend fun setDefaultMissedPolicy(policy: String) {
        dataStore.edit { it[DEFAULT_MISSED_POLICY] = policy }
    }
    
    suspend fun setDebugLoggingEnabled(enabled: Boolean) {
        dataStore.edit { it[DEBUG_LOGGING_ENABLED] = enabled }
    }

    suspend fun setAppVolume(volume: Float) {
        dataStore.edit { it[APP_VOLUME] = volume }
    }
    
    suspend fun setLoopTimeoutMinutes(minutes: Int) {
        dataStore.edit { it[LOOP_TIMEOUT_MINUTES] = minutes }
    }
    
    suspend fun setQuietTimeEnabled(enabled: Boolean) {
        dataStore.edit { it[QUIET_TIME_ENABLED] = enabled }
    }
    
    suspend fun setQuietTimeStart(hour: Int, minute: Int) {
        dataStore.edit { 
            it[QUIET_TIME_START_HOUR] = hour 
            it[QUIET_TIME_START_MINUTE] = minute
        }
    }
    
    suspend fun setQuietTimeEnd(hour: Int, minute: Int) {
        dataStore.edit { 
            it[QUIET_TIME_END_HOUR] = hour 
            it[QUIET_TIME_END_MINUTE] = minute
        }
    }
    
    suspend fun setThemeMode(mode: Int) {
        dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setFullScreenAlertEnabled(enabled: Boolean) {
        dataStore.edit { it[FULL_SCREEN_ALERT_ENABLED] = enabled }
    }

    suspend fun setBatteryOptimizationPromptShown(shown: Boolean) {
        dataStore.edit { it[BATTERY_OPTIMIZATION_PROMPT_SHOWN] = shown }
    }

    suspend fun setLastWhatsNewVersionShown(version: String) {
        dataStore.edit { it[LAST_WHATS_NEW_VERSION_SHOWN] = version }
    }

    val lastBootTimestamp: Flow<Long> = dataStore.data.map { it[LAST_BOOT_TIMESTAMP] ?: 0L }

    suspend fun setLastBootTimestamp(timestamp: Long) {
        dataStore.edit { it[LAST_BOOT_TIMESTAMP] = timestamp }
    }
}
