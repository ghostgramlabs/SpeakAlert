package com.ghostgramlabs.speakalert.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes

/**
 * Launches a system picker, telling the user instead of crashing when the device has no app for
 * it (some stripped-down ROMs and Go/TV builds ship without the documents or ringtone picker).
 */
fun <I> ActivityResultLauncher<I>.launchOrToast(
    context: Context,
    input: I,
    @StringRes unavailableMessage: Int
) {
    try {
        launch(input)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(unavailableMessage), Toast.LENGTH_LONG).show()
    }
}
