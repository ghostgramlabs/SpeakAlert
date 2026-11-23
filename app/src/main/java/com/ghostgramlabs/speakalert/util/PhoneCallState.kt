package com.ghostgramlabs.speakalert.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

/** Whether the phone is on a call, so a reminder does not sound over it. */
object PhoneCallState {

    fun isInCall(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        // MODE_IN_COMMUNICATION is left out: some devices report it without any call.
        return isTelecomInCall(context) || audioManager?.mode == AudioManager.MODE_IN_CALL
    }

    @SuppressLint("MissingPermission")
    private fun isTelecomInCall(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return try {
            (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.isInCall == true
        } catch (_: Exception) {
            false
        }
    }
}
