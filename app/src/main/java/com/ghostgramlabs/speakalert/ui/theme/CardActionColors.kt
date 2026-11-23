package com.ghostgramlabs.speakalert.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.ghostgramlabs.speakalert.R

/**
 * Colors a user can give the Done and Delete buttons on Home's reminder cards. Stored by [key]
 * rather than as a raw color so each keeps a readable shade in both light and dark themes.
 */
enum class CardButtonColor(val key: String, val light: Color, val dark: Color, @StringRes val label: Int) {
    RED("red", Color(0xFFC62828), Color(0xFFFF8A80), R.string.color_red),
    ORANGE("orange", Color(0xFFD84315), Color(0xFFFFAB76), R.string.color_orange),
    GREEN("green", Color(0xFF2E7D32), Color(0xFF81C784), R.string.color_green),
    TEAL("teal", Color(0xFF00796B), Color(0xFF4DB6AC), R.string.color_teal),
    BLUE("blue", Color(0xFF1565C0), Color(0xFF90CAF9), R.string.color_blue),
    PURPLE("purple", Color(0xFF6A1B9A), Color(0xFFCE93D8), R.string.color_purple),
    PINK("pink", Color(0xFFAD1457), Color(0xFFF48FB1), R.string.color_pink),
    GRAY("gray", Color(0xFF546E7A), Color(0xFFB0BEC5), R.string.color_gray);

    fun shade(darkTheme: Boolean): Color = if (darkTheme) dark else light

    companion object {
        /** Null for a missing or unknown key, which means the app's own color. */
        fun fromKey(key: String?): CardButtonColor? = values().firstOrNull { it.key == key }
    }
}

/** The chosen button colors, already resolved for the current theme; null keeps the default. */
data class CardActionColors(val done: Color? = null, val delete: Color? = null)

val LocalCardActionColors = staticCompositionLocalOf { CardActionColors() }
