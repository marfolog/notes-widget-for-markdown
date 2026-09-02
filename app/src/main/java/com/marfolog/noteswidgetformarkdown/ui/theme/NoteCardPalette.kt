package com.marfolog.noteswidgetformarkdown.ui.theme

import androidx.compose.ui.graphics.Color
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor

/**
 * The colours a user can give a note card, in one place.
 *
 * The widget draws them through Glance and the settings screen previews them through Compose. They
 * used to be written out twice, which meant the two would drift apart the first time one changed.
 *
 * [NoteCardColor.Default] is deliberately absent: it follows the theme, and each renderer resolves
 * it from its own theme rather than from a fixed value here.
 */
object NoteCardPalette {

    fun background(color: NoteCardColor): Color? = when (color) {
        NoteCardColor.Default -> null
        NoteCardColor.Rose -> Color(0xFFFFDAD6)
        NoteCardColor.Red -> Color(0xFFFFB4AB)
        NoteCardColor.Amber -> Color(0xFFFFDEA6)
        NoteCardColor.Orange -> Color(0xFFFFDCC2)
        NoteCardColor.Mint -> Color(0xFFBCECCB)
        NoteCardColor.Green -> Color(0xFFCDEDA3)
        NoteCardColor.Sky -> Color(0xFFC9E6FF)
        NoteCardColor.Blue -> Color(0xFFD0E4FF)
        NoteCardColor.Lavender -> Color(0xFFE7DEFF)
        NoteCardColor.Custom -> null
    }

    fun content(color: NoteCardColor): Color? = when (color) {
        NoteCardColor.Default -> null
        NoteCardColor.Rose -> Color(0xFF410002)
        NoteCardColor.Red -> Color(0xFF690005)
        NoteCardColor.Amber -> Color(0xFF2A1800)
        NoteCardColor.Orange -> Color(0xFF331100)
        NoteCardColor.Mint -> Color(0xFF00210F)
        NoteCardColor.Green -> Color(0xFF102000)
        NoteCardColor.Sky -> Color(0xFF001E30)
        NoteCardColor.Blue -> Color(0xFF001D36)
        NoteCardColor.Lavender -> Color(0xFF1D1735)
        NoteCardColor.Custom -> null
    }

    /** Accepts `#RRGGBB` or `RRGGBB`; anything else is not a colour and returns null. */
    fun parseCustom(value: String?): Color? {
        val normalized = value?.trim().orEmpty()
        if (!HEX.matches(normalized)) {
            return null
        }
        return Color(("FF" + normalized.removePrefix("#")).toLong(16))
    }

    /** Black or white, whichever stays readable on a custom colour. */
    fun contentForCustom(value: String?): Color {
        val hex = value?.trim()?.removePrefix("#").orEmpty()
        if (!Regex("^[0-9A-Fa-f]{6}$").matches(hex)) {
            return Color(0xFF111111)
        }
        val red = hex.substring(0, 2).toInt(16)
        val green = hex.substring(2, 4).toInt(16)
        val blue = hex.substring(4, 6).toInt(16)
        val luminance = (0.299 * red) + (0.587 * green) + (0.114 * blue)
        return if (luminance > 150) Color(0xFF111111) else Color(0xFFFFFFFF)
    }

    private val HEX = Regex("^#?[0-9A-Fa-f]{6}$")
}
