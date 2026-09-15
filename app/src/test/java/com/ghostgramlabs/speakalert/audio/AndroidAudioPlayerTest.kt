package com.ghostgramlabs.speakalert.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.*

class AndroidAudioPlayerTest {
    private val context = mock<Context>()
    private val uri = mock<Uri>()
    private val first = mock<MediaPlayer>()
    private val second = mock<MediaPlayer>()
    private val messages = mutableListOf<String>()
    private val queue = ArrayDeque(listOf(first, second))
    private val player = AndroidAudioPlayer(context, { queue.removeFirst() }, { messages.add(it) })

    private fun prepared(media: MediaPlayer): MediaPlayer.OnPreparedListener =
        argumentCaptor<MediaPlayer.OnPreparedListener>().apply {
            verify(media).setOnPreparedListener(capture())
        }.firstValue

    private fun error(media: MediaPlayer): MediaPlayer.OnErrorListener =
        argumentCaptor<MediaPlayer.OnErrorListener>().apply {
            verify(media).setOnErrorListener(capture())
        }.firstValue

    @Test fun `preview prepares asynchronously and starts only when ready`() {
        player.playUri(uri)
        verify(first).prepareAsync()
        verify(first, never()).prepare()
        verify(first, never()).start()
        assertFalse(player.isPlaying())
        assertEquals(0, player.getDuration())
        assertEquals(0, player.getCurrentPosition())
        verify(first, never()).getDuration()
        prepared(first).onPrepared(first)
        verify(first).start()
    }

    @Test fun `stop while preparing releases without stop and ignores late readiness`() {
        player.playUri(uri)
        val callback = prepared(first)
        player.stop()
        callback.onPrepared(first)
        verify(first).release()
        verify(first, never()).stop()
        verify(first, never()).start()
    }

    @Test fun `replaced preview ignores old prepared error and completion callbacks`() {
        var completions = 0
        player.onCompletion = { completions++ }
        player.playUri(uri)
        val ready = prepared(first)
        val failed = error(first)
        val completed = argumentCaptor<MediaPlayer.OnCompletionListener>().apply {
            verify(first).setOnCompletionListener(capture())
        }.firstValue
        player.playUri(uri)
        ready.onPrepared(first)
        assertTrue(failed.onError(first, 1, 0))
        completed.onCompletion(first)
        verify(first, never()).start()
        verify(second, never()).release()
        assertEquals(0, completions)
        assertTrue(messages.isEmpty())
        prepared(second).onPrepared(second)
        verify(second).start()
    }

    @Test fun `pause seek and volume during loading apply after preparation`() {
        player.playUri(uri)
        player.pause()
        player.seekTo(500)
        player.setVolume(0.3f)
        verify(first, never()).pause()
        verify(first, never()).seekTo(any<Int>())
        prepared(first).onPrepared(first)
        verify(first, never()).start()
        verify(first).seekTo(500)
        verify(first).setVolume(0.3f, 0.3f)
        player.resume()
        verify(first).start()
    }

    @Test fun `async error releases and clears caller state exactly once`() {
        Mockito.mockStatic(Log::class.java).use {
            var completions = 0
            player.onCompletion = { completions++ }
            player.playUri(uri)
            val failed = error(first)
            assertTrue(failed.onError(first, 1, 0))
            assertTrue(failed.onError(first, 1, 0))
            verify(first).release()
            assertEquals(1, completions)
            assertEquals(1, messages.size)
            assertFalse(player.isPlaying())
        }
    }

    @Test fun `invalid source releases and resets preview state`() {
        Mockito.mockStatic(Log::class.java).use {
            var completions = 0
            player.onCompletion = { completions++ }
            doThrow(IllegalArgumentException("Invalid source")).whenever(first).setDataSource(context, uri)
            player.playUri(uri)
            verify(first).release()
            verify(first, never()).prepareAsync()
            assertEquals(1, completions)
        }
    }
}
