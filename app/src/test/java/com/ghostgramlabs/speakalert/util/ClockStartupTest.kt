package com.ghostgramlabs.speakalert.util

import android.content.Context
import android.content.ContentResolver
import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.ghostgramlabs.speakalert.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class ClockStartupTest {
    @Test fun `saved override is used before collector runs and device changes cannot override it`() =
        checkClock(cached = true, stored = true)

    @Test fun `device clock changes update visible state and settings when no override exists`() =
        checkClock(cached = null, stored = null)

    @Test fun `explicit twelve hour preference also survives a twenty four hour device clock`() =
        checkClock(cached = false, stored = false)

    @Test fun `legacy preference is migrated before the first clock value is exposed`() =
        checkClock(cached = null, stored = true, hasCache = false)

    private fun checkClock(cached: Boolean?, stored: Boolean?, hasCache: Boolean = true) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        TimeFormat.setForTesting(false)
        try {
            mockStatic(Looper::class.java).use {
                mockConstruction(Handler::class.java).use {
                    mockStatic(Settings.System::class.java).use { system ->
                        val uri = mock<Uri>()
                        system.`when`<Uri> { Settings.System.getUriFor(Settings.System.TIME_12_24) }.thenReturn(uri)
                        val context = mock<Context>()
                        val resolver = mock<ContentResolver>()
                        val prefs = mock<SharedPreferences>()
                        val editor = mock<SharedPreferences.Editor>()
                        val repository = mock<SettingsRepository>()
                        val setting = MutableStateFlow(stored)
                        whenever(context.applicationContext).thenReturn(context)
                        whenever(context.contentResolver).thenReturn(resolver)
                        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
                        whenever(prefs.contains("mode")).thenReturn(hasCache)
                        whenever(prefs.getInt(eq("mode"), any())).thenReturn(when (cached) {
                            true -> 1
                            false -> 0
                            null -> -1
                        })
                        whenever(prefs.edit()).thenReturn(editor)
                        whenever(editor.putInt(any(), any())).thenReturn(editor)
                        whenever(repository.use24HourTimeOverride).thenReturn(setting)
                        var device24 = cached == false
                        var redraws = 0
                        TimeFormat.initialize(context, backgroundScope, repository,
                            readDeviceClock = { device24 }, onClockChanged = { redraws++ })
                        assertEquals((if (hasCache) cached else stored) ?: device24, TimeFormat.use24Hour)
                        val observer = argumentCaptor<ContentObserver>()
                        verify(resolver).registerContentObserver(eq(uri), eq(false), observer.capture())
                        runCurrent()
                        device24 = true
                        observer.firstValue.onChange(false)
                        assertEquals(stored ?: true, TimeFormat.use24Hour)
                        assertEquals(TimeFormat.use24Hour, TimeFormat.changes.value)
                        device24 = false
                        observer.firstValue.onChange(false)
                        assertEquals(stored ?: false, TimeFormat.use24Hour)
                        assertEquals(TimeFormat.use24Hour, TimeFormat.changes.value)
                        if (stored != false) assertTrue(redraws > 0)
                        setting.value = false
                        runCurrent()
                        device24 = true
                        observer.firstValue.onChange(false)
                        assertFalse(TimeFormat.use24Hour)
                    }
                }
            }
        } finally {
            TimeFormat.setForTesting(false)
            Dispatchers.resetMain()
        }
    }
}
