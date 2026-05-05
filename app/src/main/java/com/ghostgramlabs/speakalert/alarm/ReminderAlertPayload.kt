package com.ghostgramlabs.speakalert.alarm

import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.util.APP_DISPLAY_NAME

internal data class ReminderAlertPayload(
    val title: String,
    val message: String,
    val playbackAudioPath: String?,
    val playbackText: String?,
    val autoplayOnTap: Boolean,
    val isFollowUpAlert: Boolean
)

internal fun buildReminderAlertPayload(
    reminder: ReminderEntity,
    isFollowUpTrigger: Boolean,
    hasPlayableAudio: Boolean,
    hasAudioConfigured: Boolean
): ReminderAlertPayload {
    val followUpMessage = buildFollowUpMessage(reminder)
    val message = when {
        isFollowUpTrigger -> followUpMessage
        !reminder.reminderText.isNullOrBlank() -> reminder.reminderText
        hasAudioConfigured && !hasPlayableAudio -> "Selected audio file is unavailable. Tap to choose another file."
        else -> "Tap to view"
    }

    return ReminderAlertPayload(
        title = if (isFollowUpTrigger) {
            reminder.title?.takeIf { it.isNotBlank() } ?: "Follow-Up Check"
        } else {
            reminder.title?.takeIf { it.isNotBlank() } ?: APP_DISPLAY_NAME
        },
        message = message,
        playbackAudioPath = reminder.audioPath?.takeIf { !isFollowUpTrigger && hasPlayableAudio },
        playbackText = when {
            isFollowUpTrigger -> followUpMessage
            !reminder.reminderText.isNullOrBlank() -> reminder.reminderText
            else -> null
        },
        autoplayOnTap = !isFollowUpTrigger,
        isFollowUpAlert = isFollowUpTrigger
    )
}

internal fun buildFollowUpMessage(reminder: ReminderEntity): String {
    val subject = reminder.title
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: reminder.reminderText
            ?.takeIf { it.isNotBlank() }
            ?.let { text ->
                val trimmed = text.trim()
                val words = trimmed.split(Regex("\\s+"))
                if (words.size > 8) words.take(8).joinToString(" ") else trimmed
            }
        ?: "this reminder"
    return "Did you complete $subject?"
}

internal fun shouldAutoPlayReminder(
    autoPlayEnabled: Boolean,
    inCall: Boolean,
    unlockedOnly: Boolean,
    isLocked: Boolean,
    playbackAudioPath: String?,
    playbackText: String?,
    isFollowUpTrigger: Boolean,
    speakTextIfNoVoice: Boolean,
    bootBlocked: Boolean,
    toneOnlyMode: Boolean,
    blockedByDnd: Boolean = false
): Boolean {
    if (!autoPlayEnabled || inCall || bootBlocked || toneOnlyMode || blockedByDnd) return false
    if (unlockedOnly && isLocked) return false
    if (!playbackAudioPath.isNullOrBlank()) return true
    if (playbackText.isNullOrBlank()) return false

    // Follow-up checks are an explicit reliability feature and should still ask
    // their confirmation question even when normal automatic spoken fallback is off.
    return isFollowUpTrigger || speakTextIfNoVoice
}
