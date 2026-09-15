package com.ghostgramlabs.speakalert.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.ghostgramlabs.speakalert.util.UnnamedReminderTitle
import com.ghostgramlabs.speakalert.util.reminderDisplayTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UnnamedReminderTitleTest {
    private class Store : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        private val mutex = Mutex()
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences) =
            mutex.withLock { transform(data.value).also { data.value = it } }
    }

    @Test fun `existing installations keep creation time`() = runTest {
        val repository = SettingsRepository(Store(), existingInstallation = true)
        assertEquals(UnnamedReminderTitle.CREATION_TIME, repository.unnamedReminderTitle.first())
    }

    @Test fun `fresh installation default survives onboarding and future upgrades`() = runTest {
        val store = Store()
        val fresh = SettingsRepository(store, existingInstallation = false)
        assertEquals(UnnamedReminderTitle.REMINDER_TYPE, fresh.unnamedReminderTitle.first())
        fresh.setLastWhatsNewVersionShown("2.0.35")
        val upgrade = SettingsRepository(store, existingInstallation = true)
        assertEquals(UnnamedReminderTitle.REMINDER_TYPE, upgrade.unnamedReminderTitle.first())
    }

    @Test fun `restored existing preferences keep creation time on a fresh install`() = runTest {
        val repository = SettingsRepository(Store(), existingInstallation = false)
        repository.setLastWhatsNewVersionShown("2.0.34")
        assertEquals(UnnamedReminderTitle.CREATION_TIME, repository.unnamedReminderTitle.first())
    }

    @Test fun `explicit no title survives recreation and upgrades`() = runTest {
        val store = Store()
        SettingsRepository(store, existingInstallation = false).setUnnamedReminderTitle(UnnamedReminderTitle.NONE)
        assertEquals(UnnamedReminderTitle.NONE,
            SettingsRepository(store, existingInstallation = true).unnamedReminderTitle.first())
    }

    @Test fun `empty fallback never removes a user label or typed message`() {
        assertEquals("My recording", reminderDisplayTitle("My recording", null, ""))
        assertEquals("Take a break", reminderDisplayTitle(null, "Take a break", ""))
        assertEquals("", reminderDisplayTitle(null, null, ""))
        assertEquals("Voice reminder", reminderDisplayTitle(null, null, "Voice reminder"))
        assertEquals("Created at 9:30", reminderDisplayTitle(null, null, "Created at 9:30"))
    }

    @Test fun `legacy generated title uses the selected fallback and text still takes priority`() {
        assertEquals("", reminderDisplayTitle("Reminder at 9:30 AM", null, ""))
        assertEquals("Take a break", reminderDisplayTitle("Voice reminder", "Take a break", ""))
    }
}
