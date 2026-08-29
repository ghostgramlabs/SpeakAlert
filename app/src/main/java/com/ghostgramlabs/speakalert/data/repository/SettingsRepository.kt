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
        val PRIVATE_PLAYBACK_ENABLED = booleanPreferencesKey("private_playback_enabled")
        val DND_BYPASS_ENABLED = booleanPreferencesKey("dnd_bypass_enabled")
        val TONE_ONLY_MODE = booleanPreferencesKey("tone_only_mode")
        val TONE_ONLY_ALERT_TONE_URI = stringPreferencesKey("tone_only_alert_tone_uri")
        val DEFAULT_SNOOZE_DURATION = intPreferencesKey("default_snooze_duration")
        val DEFAULT_FOLLOW_UP_MINUTES = intPreferencesKey("default_follow_up_minutes")
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
        
        val TTS_LANGUAGE_MODE = intPreferencesKey("tts_language_mode") // 0 = Auto-detect, 1 = Device language, 2 = English
        val PERSIST_UNTIL_DONE = booleanPreferencesKey("persist_until_done")
        val FOLLOW_UP_MAX_REPEATS = intPreferencesKey("follow_up_max_repeats") // How many times a follow-up check repeats before stopping
        val THEME_MODE = intPreferencesKey("theme_mode") // 0 = System, 1 = Light, 2 = Dark
        val FULL_SCREEN_ALERT_ENABLED = booleanPreferencesKey("full_screen_alert_enabled")
        val SHOW_VOICE_RECORDING_SECTION = booleanPreferencesKey("show_voice_recording_section")
        val EXPERIMENTAL_VOICE_ENHANCEMENT_ENABLED =
            booleanPreferencesKey("experimental_voice_enhancement_enabled")
        val SHOW_AUDIO_FILE_SECTION = booleanPreferencesKey("show_audio_file_section")
        val SHOW_TYPED_REMINDER_SECTION = booleanPreferencesKey("show_typed_reminder_section")
        val SHOW_SHORT_LABEL_SECTION = booleanPreferencesKey("show_short_label_section")
        val BATTERY_OPTIMIZATION_PROMPT_SHOWN = booleanPreferencesKey("battery_optimization_prompt_shown")
        val LAST_WHATS_NEW_VERSION_SHOWN = stringPreferencesKey("last_whats_new_version_shown")

        // In-app rating prompt tracking
        val APP_OPEN_COUNT = intPreferencesKey("app_open_count")
        val RATING_PROMPT_DECIDED = booleanPreferencesKey("rating_prompt_decided")
        val RATING_PROMPT_LAST_OPEN = intPreferencesKey("rating_prompt_last_open")

        // Android 15 FGS Boot Guard
        val LAST_BOOT_TIMESTAMP = longPreferencesKey("last_boot_timestamp")
    }

    val autoPlayEnabled: Flow<Boolean> = dataStore.data.map { it[AUTO_PLAY_ENABLED] ?: true }
    val autoPlayOnUnlockOnly: Flow<Boolean> = dataStore.data.map { it[AUTO_PLAY_ON_UNLOCK_ONLY] ?: false }
    val speakTextIfNoVoice: Flow<Boolean> = dataStore.data.map { it[SPEAK_TEXT_IF_NO_VOICE] ?: true }
    val privatePlaybackEnabled: Flow<Boolean> = dataStore.data.map { it[PRIVATE_PLAYBACK_ENABLED] ?: false }
    val dndBypassEnabled: Flow<Boolean> = dataStore.data.map { it[DND_BYPASS_ENABLED] ?: true }
    val toneOnlyMode: Flow<Boolean> = dataStore.data.map { it[TONE_ONLY_MODE] ?: false }
    val toneOnlyAlertToneUri: Flow<String?> = dataStore.data.map { it[TONE_ONLY_ALERT_TONE_URI] }
    val defaultSnoozeDuration: Flow<Int> = dataStore.data.map { it[DEFAULT_SNOOZE_DURATION] ?: 5 } // Minutes (Default 5)
    val defaultFollowUpMinutes: Flow<Int> = dataStore.data.map { it[DEFAULT_FOLLOW_UP_MINUTES] ?: 0 } // 0 = off (preserves existing behavior)
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
    
    val ttsLanguageMode: Flow<Int> = dataStore.data.map { it[TTS_LANGUAGE_MODE] ?: 0 }
    val persistUntilDone: Flow<Boolean> = dataStore.data.map { it[PERSIST_UNTIL_DONE] ?: false }
    // 0 = repeat until marked done. That is the default because it matches the behavior shipped
    // up to 2.0.31; a finite limit is an opt-in guardrail, never a silent downgrade.
    val followUpMaxRepeats: Flow<Int> = dataStore.data.map { (it[FOLLOW_UP_MAX_REPEATS] ?: 0).coerceIn(0, 10) }
    val themeMode: Flow<Int> = dataStore.data.map { it[THEME_MODE] ?: 0 }
    val fullScreenAlertEnabled: Flow<Boolean> = dataStore.data.map { it[FULL_SCREEN_ALERT_ENABLED] ?: false }
    val showVoiceRecordingSection: Flow<Boolean> = dataStore.data.map { it[SHOW_VOICE_RECORDING_SECTION] ?: true }
    // Software denoising uses the device codec stack and can vary by manufacturer. Keep it
    // opt-in until it has broad device coverage; reliable MIC capture is not gated by this.
    val experimentalVoiceEnhancementEnabled: Flow<Boolean> = dataStore.data.map {
        it[EXPERIMENTAL_VOICE_ENHANCEMENT_ENABLED] ?: false
    }
    val showAudioFileSection: Flow<Boolean> = dataStore.data.map { it[SHOW_AUDIO_FILE_SECTION] ?: true }
    val showTypedReminderSection: Flow<Boolean> = dataStore.data.map { it[SHOW_TYPED_REMINDER_SECTION] ?: true }
    val showShortLabelSection: Flow<Boolean> = dataStore.data.map { it[SHOW_SHORT_LABEL_SECTION] ?: true }
    val batteryOptimizationPromptShown: Flow<Boolean> = dataStore.data.map {
        it[BATTERY_OPTIMIZATION_PROMPT_SHOWN] ?: false
    }
    val lastWhatsNewVersionShown: Flow<String?> = dataStore.data.map {
        it[LAST_WHATS_NEW_VERSION_SHOWN]
    }

    val appOpenCount: Flow<Int> = dataStore.data.map { it[APP_OPEN_COUNT] ?: 0 }
    val ratingPromptDecided: Flow<Boolean> = dataStore.data.map { it[RATING_PROMPT_DECIDED] ?: false }
    val ratingPromptLastOpen: Flow<Int> = dataStore.data.map { it[RATING_PROMPT_LAST_OPEN] ?: 0 }

    suspend fun setAutoPlayEnabled(enabled: Boolean) {
        dataStore.edit { it[AUTO_PLAY_ENABLED] = enabled }
    }

    suspend fun setAutoPlayOnUnlockOnly(enabled: Boolean) {
        dataStore.edit { it[AUTO_PLAY_ON_UNLOCK_ONLY] = enabled }
    }

    suspend fun setSpeakTextIfNoVoice(enabled: Boolean) {
        dataStore.edit { it[SPEAK_TEXT_IF_NO_VOICE] = enabled }
    }

    suspend fun setPrivatePlaybackEnabled(enabled: Boolean) {
        dataStore.edit { it[PRIVATE_PLAYBACK_ENABLED] = enabled }
    }

    suspend fun setDndBypassEnabled(enabled: Boolean) {
        dataStore.edit { it[DND_BYPASS_ENABLED] = enabled }
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

    suspend fun setDefaultFollowUpMinutes(minutes: Int) {
        dataStore.edit { it[DEFAULT_FOLLOW_UP_MINUTES] = minutes.coerceIn(0, 240) }
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
    
    suspend fun setTtsLanguageMode(mode: Int) {
        dataStore.edit { it[TTS_LANGUAGE_MODE] = mode.coerceIn(0, 2) }
    }

    suspend fun setPersistUntilDone(enabled: Boolean) {
        dataStore.edit { it[PERSIST_UNTIL_DONE] = enabled }
    }

    suspend fun setFollowUpMaxRepeats(count: Int) {
        dataStore.edit { it[FOLLOW_UP_MAX_REPEATS] = count.coerceIn(0, 10) }
    }

    suspend fun setThemeMode(mode: Int) {
        dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setFullScreenAlertEnabled(enabled: Boolean) {
        dataStore.edit { it[FULL_SCREEN_ALERT_ENABLED] = enabled }
    }

    suspend fun setShowVoiceRecordingSection(visible: Boolean) {
        dataStore.edit { it[SHOW_VOICE_RECORDING_SECTION] = visible }
    }

    suspend fun setExperimentalVoiceEnhancementEnabled(enabled: Boolean) {
        dataStore.edit { it[EXPERIMENTAL_VOICE_ENHANCEMENT_ENABLED] = enabled }
    }

    suspend fun setShowAudioFileSection(visible: Boolean) {
        dataStore.edit { it[SHOW_AUDIO_FILE_SECTION] = visible }
    }

    suspend fun setShowTypedReminderSection(visible: Boolean) {
        dataStore.edit { it[SHOW_TYPED_REMINDER_SECTION] = visible }
    }

    suspend fun setShowShortLabelSection(visible: Boolean) {
        dataStore.edit { it[SHOW_SHORT_LABEL_SECTION] = visible }
    }

    suspend fun setBatteryOptimizationPromptShown(shown: Boolean) {
        dataStore.edit { it[BATTERY_OPTIMIZATION_PROMPT_SHOWN] = shown }
    }

    suspend fun setLastWhatsNewVersionShown(version: String) {
        dataStore.edit { it[LAST_WHATS_NEW_VERSION_SHOWN] = version }
    }

    /** Increments and returns the new app-open count. Used to pace the rating prompt. */
    suspend fun incrementAppOpenCount(): Int {
        var newCount = 0
        dataStore.edit {
            newCount = (it[APP_OPEN_COUNT] ?: 0) + 1
            it[APP_OPEN_COUNT] = newCount
        }
        return newCount
    }

    suspend fun setRatingPromptDecided(decided: Boolean) {
        dataStore.edit { it[RATING_PROMPT_DECIDED] = decided }
    }

    suspend fun setRatingPromptLastOpen(openCount: Int) {
        dataStore.edit { it[RATING_PROMPT_LAST_OPEN] = openCount }
    }

    val lastBootTimestamp: Flow<Long> = dataStore.data.map { it[LAST_BOOT_TIMESTAMP] ?: 0L }

    suspend fun setLastBootTimestamp(timestamp: Long) {
        dataStore.edit { it[LAST_BOOT_TIMESTAMP] = timestamp }
    }
}
