package com.ghostgramlabs.speakalert.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SettingsResetTest {

    private class MemoryPreferencesStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        private val mutex = Mutex()
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            mutex.withLock { transform(data.value).also { data.value = it } }
    }

    @Test fun `reset restores defaults and keeps app bookkeeping`() = runTest {
        val store = MemoryPreferencesStore()
        var clockMirror: Boolean? = true
        val repository = SettingsRepository(store, cacheClockOverride = { clockMirror = it })

        repository.setAutoPlayEnabled(false)
        repository.setThemeMode(2)
        repository.setDefaultSnoozeDuration(30)
        repository.setQuietTimeEnabled(true)
        repository.setQuietTimeStart(20, 15)
        repository.setLoopTimeoutMinutes(0)
        repository.setUse24HourTime(true)
        repository.setCardDoneColor("red")
        repository.setShowAudioFileSection(false)
        repository.setPauseRemindersUntil(123_456L)
        repository.setLastWhatsNewVersionShown("2.0.46")
        repository.setBatteryOptimizationPromptShown(true)

        repository.resetToDefaults()

        assertTrue(repository.autoPlayEnabled.first())
        assertEquals(0, repository.themeMode.first())
        assertEquals(5, repository.defaultSnoozeDuration.first())
        assertFalse(repository.quietTimeEnabled.first())
        assertEquals(22, repository.quietTimeStartHour.first())
        assertEquals(0, repository.quietTimeStartMinute.first())
        assertEquals(10, repository.loopTimeoutMinutes.first())
        assertNull(repository.use24HourTimeOverride.first())
        assertNull(clockMirror)
        assertNull(repository.cardDoneColor.first())
        assertTrue(repository.showAudioFileSection.first())

        // Not preferences: an active pause, first-run prompts and release notes survive.
        assertEquals(123_456L, repository.pauseRemindersUntil.first())
        assertEquals("2.0.46", repository.lastWhatsNewVersionShown.first())
        assertTrue(repository.batteryOptimizationPromptShown.first())
    }
}
