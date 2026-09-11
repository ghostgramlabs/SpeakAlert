package com.ghostgramlabs.speakalert.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*

import org.junit.Test


class ReviewPromptPersistenceTest {

    // DataStore 1.0 uses an open-file rename unsupported by Windows local JVM tests.
    // Exercise repository persistence and transaction behavior with the DataStore contract.
    private class MemoryPreferencesStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        private val mutex = Mutex()
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            mutex.withLock { transform(data.value).also { data.value = it } }
    }

    private val day = ReviewUsage.DAY
    private val start = 100 * day

    @Test fun `concurrent claims show only one prompt and persist cooldown`() = runTest {
        val store = MemoryPreferencesStore()
        val repository = SettingsRepository(store)
        repeat(3) { repository.recordReminderDelivery(start + it * day) }
        val claims = List(5) {
            async { repository.claimRatingPrompt(start + 8 * day) }
        }.awaitAll()
        assertEquals(1, claims.count { it })
        val recreated = SettingsRepository(store)
        assertFalse(recreated.claimRatingPrompt(start + 9 * day))
        assertTrue(recreated.claimRatingPrompt(start + 68 * day))
    }

    @Test fun `existing opt out survives new usage tracking`() = runTest {
        val store = MemoryPreferencesStore()
        val repository = SettingsRepository(store)
        repository.setRatingPromptDecided(true)
        repeat(3) { repository.recordReminderDelivery(start + it * day) }
        assertFalse(SettingsRepository(store).claimRatingPrompt(start + 100 * day))
    }

    @Test fun `app opens cannot qualify without reminder usage`() = runTest {
        val store = MemoryPreferencesStore()
        val repository = SettingsRepository(store)
        repeat(10) { repository.incrementAppOpenCount() }
        assertFalse(repository.claimRatingPrompt(start + 100 * day))
    }
}
