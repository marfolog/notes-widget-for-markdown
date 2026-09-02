package com.marfolog.noteswidgetformarkdown.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

internal val DarkColorScheme = darkColorScheme(
    primary = BlueDarkPrimary,
    onPrimary = Color(0xFF00325B),
    primaryContainer = Color(0xFF00497F),
    onPrimaryContainer = Color(0xFFD4E3FF),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F8),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199),
    secondary = BlueDarkSecondary,
    onSecondary = Color(0xFF20333F),
    tertiary = BlueDarkTertiary,
    onTertiary = Color(0xFF2C2D4D)
)

internal val LightColorScheme = lightColorScheme(
    primary = BlueLightPrimary,
    onPrimary = Color(0xFFFFFFFF),
    // Bez kontejneru a povrchu by M3 doplnil vychozi fialovou ze sablony.
    primaryContainer = Color(0xFFD4E3FF),
    onPrimaryContainer = Color(0xFF001C3B),
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF101C2B),
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    secondary = BlueLightSecondary,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = BlueLightTertiary,
    onTertiary = Color(0xFFFFFFFF)
)

@Composable
fun NotesWidgetForMarkdownTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Vypnuto zamerne: s dynamickou barvou by nastaveni na kazdem telefonu vypadalo jinak
    // a nesedelo by s widgetem, ktery si barvy nese sam.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}