package com.marfolog.noteswidgetformarkdown.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders

/**
 * Wraps widget content so it uses the same colours as the rest of the app.
 *
 * Below Android 12 there are no system colours to borrow, and the two halves of the app would
 * otherwise disagree: Compose falls back to the palette in [LightColorScheme], while a bare
 * GlanceTheme falls back to the stock Material baseline. The app's minimum is Android 11, so that
 * mismatch is not a corner case.
 */
@Composable
fun WidgetTheme(content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceTheme(content = content)
    } else {
        GlanceTheme(
            colors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme),
            content = content
        )
    }
}
