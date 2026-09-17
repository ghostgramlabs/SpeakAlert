package com.ghostgramlabs.speakalert.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Flips an icon horizontally when the layout runs right to left.
 *
 * Arrows and chevrons carry a direction that belongs to the reading order, not to the glyph: a
 * back arrow points at where the text begins, and a row's chevron points at where it leads. Left
 * unmirrored in Arabic they aim away from what they refer to, which makes a tappable row look
 * inert and a back button look like it goes forward.
 *
 * Compose gained `Icons.AutoMirrored` for exactly this, but it arrived after the Compose BOM this
 * project pins, so the flip is done here instead. Reaching for it is a judgement each time:
 * transport controls must NOT be mirrored - play, stop and fast-forward point along time rather
 * than along the text - and neither must logos, checkmarks or anything whose shape is the
 * meaning.
 */
@Composable
fun Modifier.mirrorInRtl(): Modifier {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return if (isRtl) this.graphicsLayer { scaleX = -1f } else this
}
