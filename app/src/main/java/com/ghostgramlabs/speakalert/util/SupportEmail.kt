package com.ghostgramlabs.speakalert.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ghostgramlabs.speakalert.R

/** Opens an editable draft. The user chooses whether to send it. */
fun openSupportEmail(context: Context, featureRequest: Boolean = false): Boolean {
    val subject = context.getString(
        if (featureRequest) R.string.feature_request_email_subject else R.string.support_email_subject,
        APP_DISPLAY_NAME
    )
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:${context.getString(R.string.support_email_address)}?subject=${Uri.encode(subject)}")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: android.content.ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
