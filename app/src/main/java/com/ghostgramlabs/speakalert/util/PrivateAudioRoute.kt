package com.ghostgramlabs.speakalert.util

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

object PrivateAudioRoute {

    fun hasExternalPrivateRoute(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return false
        // BLUETOOTH_CONNECT is required to read device names or to drive SCO via
        // setCommunicationDevice — not to enumerate outputs or route media. Detect
        // every supported output regardless of that permission.
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .any { device -> device.isExternalPrivateDevice() }
    }

    private fun AudioDeviceInfo.isExternalPrivateDevice(): Boolean {
        return when (type) {
            AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> true
            else -> false
        }
    }
}
