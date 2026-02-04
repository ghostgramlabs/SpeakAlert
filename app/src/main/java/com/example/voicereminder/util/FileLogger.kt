package com.example.voicereminder.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FileLogger writes debug logs to a file in Downloads folder
 * that can be accessed via File Manager
 */
object FileLogger {
    
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    // Default to false, enabled via Settings
    var isEnabled: Boolean = false
    
    fun getLogFile(): File? = logFile
    
    fun init(context: Context) {
        try {
            // Use Downloads folder which is accessible via File Manager
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            logFile = File(downloadsDir, "voice_reminder_debug.txt")
            
            // Write header on init
            log("=== VoiceReminder Debug Log Started ===")
            log("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            log("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to app internal storage
            try {
                logFile = File(context.filesDir, "voice_reminder_debug.txt")
                log("=== VoiceReminder Debug Log (Internal) ===")
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }
    
    @Synchronized
    fun log(message: String) {
        if (!isEnabled) return
        try {
            val timestamp = dateFormat.format(Date())
            val logLine = "[$timestamp] $message\n"
            
            logFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.append(logLine)
                }
            }
            
            // Also log to Logcat
            android.util.Log.d("FileLogger", message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val errorMsg = if (throwable != null) {
            "$tag: $message - ${throwable.message}\n${throwable.stackTraceToString().take(500)}"
        } else {
            "$tag: $message"
        }
        log("[ERROR] $errorMsg")
    }
    
    fun getLogFilePath(): String? {
        return logFile?.absolutePath
    }
    
    fun clearLog() {
        try {
            logFile?.writeText("")
            log("=== Log Cleared ===")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
