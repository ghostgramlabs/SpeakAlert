package com.ghostgramlabs.speakalert.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * Helper for reminder audio source strings which can be:
 * 1) local absolute file paths (existing behavior)
 * 2) persisted content URIs from system picker (new behavior)
 */
object ReminderAudioSource {

    fun isContentUri(source: String?): Boolean {
        if (source.isNullOrBlank()) return false
        return runCatching { Uri.parse(source).scheme == "content" }.getOrDefault(false)
    }

    fun isPlayable(context: Context, source: String?): Boolean {
        if (source.isNullOrBlank()) return false
        return if (isContentUri(source)) {
            canReadContentUri(context, source)
        } else {
            File(source).exists()
        }
    }

    fun toUri(source: String): Uri {
        return if (isContentUri(source)) {
            Uri.parse(source)
        } else {
            Uri.fromFile(File(source))
        }
    }

    fun resolveDisplayName(context: Context, source: String?): String? {
        if (source.isNullOrBlank()) return null
        if (!isContentUri(source)) {
            return File(source).name.takeIf { it.isNotBlank() }
        }
        val uri = runCatching { Uri.parse(source) }.getOrNull() ?: return null
        val resolver = context.contentResolver
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull() ?: uri.lastPathSegment
    }

    private fun canReadContentUri(context: Context, source: String): Boolean {
        val uri = runCatching { Uri.parse(source) }.getOrNull() ?: return false
        return runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
    }
}

