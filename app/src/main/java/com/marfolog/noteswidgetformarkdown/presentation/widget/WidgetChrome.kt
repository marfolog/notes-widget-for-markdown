package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider as dayNightColor
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardAppearance
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.ui.theme.NoteCardPalette
import com.marfolog.noteswidgetformarkdown.ui.theme.WidgetColors
import com.marfolog.noteswidgetformarkdown.ui.theme.WidgetTheme
import com.marfolog.noteswidgetformarkdown.util.UsageEvent

/**
 * Chrome shared by every widget flavour: the settings/refresh buttons, the bottom action bar
 * with the add buttons, and the git sync chip. Only the list of notes differs between widgets.
 */

// region Widget Action Buttons (Top-Right)

@Composable
internal fun WidgetActionButtons(isLoading: Boolean = false) {
    val setupIntent = SetupActivity.intent(LocalContext.current, UsageEvent.FROM_WIDGET)

    Box(
        modifier = GlanceModifier.fillMaxSize().padding(4.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(horizontalAlignment = Alignment.End) {
            if (isLoading) {
                // Same slot the refresh button sits in, so a refresh in flight reads as "in
                // progress" without the rest of the widget going blank to show it.
                Box(
                    modifier = GlanceModifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = GlanceModifier.size(20.dp),
                        color = ChromeColors.onButton
                    )
                }
            } else {
                RoundIconButton(
                    icon = R.drawable.ic_refresh,
                    contentDescription = LocalContext.current.getString(R.string.action_refresh),
                    onClickModifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetAction>())
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            RoundIconButton(
                icon = R.drawable.ic_edit,
                contentDescription = LocalContext.current.getString(R.string.action_open_settings),
                onClickModifier = GlanceModifier.clickable(actionStartActivity(setupIntent))
            )
        }
    }
}

@Composable
internal fun RoundIconButton(
    icon: Int,
    contentDescription: String,
    onClickModifier: GlanceModifier
) {
    Box(
        modifier = GlanceModifier
            .size(40.dp)
            .cornerRadius(20.dp)
            .background(ChromeColors.button)
            .then(onClickModifier),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(20.dp),
            colorFilter = ColorFilter.tint(ChromeColors.onButton)
        )
    }
}

// endregion

// region Bottom Bar (actions + sync status)

/** Translucent blue so the bar reads as a layer above the notes, not a solid block. */
internal val BAR_TINT = ColorProvider(Color(0x552196F3))

@Composable
internal fun BottomBar(
    vaultName: String?,
    noteFolderPath: String?,
    chip: BottomBarChip,
    backgroundPercent: Int,
    showAddButtons: Boolean
) {
    if (!showsBottomBar(showAddButtons, chip)) return
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAddButtons) {
            WidgetSurface(backgroundPercent, cornerRadius = 16.dp) {
                Row(
                    modifier = GlanceModifier
                        .cornerRadius(16.dp)
                        .background(ChromeColors.bar)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickActions(vaultName, noteFolderPath)
                }
            }
        }
        Box(modifier = GlanceModifier.defaultWeight()) {}
        when (chip) {
            BottomBarChip.None -> Unit
            is BottomBarChip.Sync -> SyncStatus(chip.label, backgroundPercent)
            is BottomBarChip.Rate -> RateChip(chip.alternate)
        }
    }
}

/** Explicit traffic-light colours: the theme's primary is not readable as "healthy". */
@Composable
private fun severityColor(severity: GitSyncLabel.Severity): ColorProvider = when (severity) {
    GitSyncLabel.Severity.Ok -> ColorProvider(Color(0xFF4CAF50))
    GitSyncLabel.Severity.Stale -> ColorProvider(Color(0xFFFFB300))
    GitSyncLabel.Severity.Problem -> ColorProvider(Color(0xFFE53935))
    GitSyncLabel.Severity.Off -> ChromeColors.onBar
}

@Composable
internal fun SyncStatus(label: GitSyncLabel.Label, backgroundPercent: Int) {
    val context = LocalContext.current
    // Tapping the chip opens settings, where the full story lives: what is wrong, when it
    // last synced, and the picker for the folder that holds .git.
    BarChip(
        onClick = actionStartActivity(SetupActivity.intent(context, UsageEvent.FROM_SYNC_CHIP)),
        dotColor = severityColor(label.severity),
        text = label.text(context),
        backgroundPercent = backgroundPercent
    )
}

/**
 * Tertiary rather than the amber of a stale sync, which it must not be mistaken for. The cross-fade
 * comes from a ViewFlipper, so it keeps running without the widget being redrawn.
 */
@Composable
private fun RateChip(alternate: RateAlternate) {
    val context = LocalContext.current
    val views = RemoteViews(context.packageName, R.layout.widget_rate_chip).apply {
        setOnClickPendingIntent(R.id.rate_chip_flipper, rateActivityPendingIntent(context))
        setContentDescription(R.id.rate_chip_flipper, context.getString(R.string.rate_chip))
        setDayNightTextColor(context, R.id.rate_chip_star, RATE_STAR_GOLD)
        setDayNightTextColor(context, R.id.rate_chip_label, GlanceTheme.colors.onTertiaryContainer)
        setDayNightTextColor(context, R.id.rate_chip_alternate_text, GlanceTheme.colors.onTertiaryContainer)
        when (alternate) {
            is RateAlternate.SyncLabel -> {
                setTextViewText(R.id.rate_chip_alternate_text, alternate.label.text(context))
                setViewVisibility(R.id.rate_chip_dot, View.VISIBLE)
            }
            RateAlternate.Hint -> setTextViewText(R.id.rate_chip_alternate_text, context.getString(R.string.rate_chip_hint))
        }
        if (alternate.startsWithRate) setDisplayedChild(R.id.rate_chip_flipper, RATE_CHILD_INDEX)
    }
    Box(
        modifier = GlanceModifier
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.tertiaryContainer)
    ) {
        AndroidRemoteViews(views)
    }
}

private const val RATE_CHILD_INDEX = 1
private const val RATE_CHIP_REQUEST_CODE = 0x7a7e

private val RATE_STAR_GOLD = dayNightColor(day = Color(0xFFF4B400), night = Color(0xFFFFD54F))

private fun rateActivityPendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    RATE_CHIP_REQUEST_CODE,
    Intent(context, RateActivity::class.java),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
)

/** Night colours apply by themselves from Android 12; before that the current mode is baked in. */
private fun RemoteViews.setDayNightTextColor(context: Context, viewId: Int, color: ColorProvider) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setColorInt(
            viewId,
            "setTextColor",
            color.getColor(context.withNightMode(false)).toArgb(),
            color.getColor(context.withNightMode(true)).toArgb()
        )
    } else {
        setTextColor(viewId, color.getColor(context).toArgb())
    }
}

private fun GitSyncLabel.Label.text(context: Context): String =
    arg?.let { context.getString(textRes, it) } ?: context.getString(textRes)

@Composable
private fun BarChip(onClick: Action, dotColor: ColorProvider, text: String, backgroundPercent: Int) {
    WidgetSurface(backgroundPercent, cornerRadius = 16.dp) {
        Row(
            modifier = GlanceModifier
                .cornerRadius(16.dp)
                .background(ChromeColors.bar)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .clickable(onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(8.dp)
                    .cornerRadius(4.dp)
                    .background(dotColor)
            ) {}
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = text,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ChromeColors.onBar
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun QuickActions(vaultName: String?, noteFolderPath: String?) {
    Box(
        modifier = GlanceModifier
            .size(44.dp)
            .cornerRadius(14.dp)
            .background(GlanceTheme.colors.primary)
            .clickable(
                actionStartActivity(
                    Intent(androidx.glance.LocalContext.current, AddNoteActivity::class.java)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_add),
            contentDescription = LocalContext.current.getString(R.string.action_add_note),
            modifier = GlanceModifier.size(24.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
        )
    }
    Spacer(modifier = GlanceModifier.width(8.dp))
    Box(
        modifier = GlanceModifier
            .size(44.dp)
            .cornerRadius(14.dp)
            .background(GlanceTheme.colors.primary)
            .clickable(
                actionStartActivity(
                    Intent(
                        androidx.glance.LocalContext.current,
                        FastNoteActivity::class.java
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_fast_note),
            contentDescription = LocalContext.current.getString(R.string.action_add_fast_note),
            modifier = GlanceModifier.size(22.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
        )
    }
}

// endregion


/**
 * Content on top, an opaque action bar at the bottom. The bar is a sibling of the content
 * (not an overlay), so the last note is never covered by the buttons.
 */
@Composable
internal fun ConfiguredScaffold(
    vaultName: String?,
    noteFolderPath: String?,
    bottomBarChip: BottomBarChip,
    backgroundPercent: Int,
    isLoading: Boolean = false,
    showAddButtons: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Box(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
            content()
            WidgetActionButtons(isLoading)
        }
        BottomBar(vaultName, noteFolderPath, bottomBarChip, backgroundPercent, showAddButtons)
    }
}

/** Obsidian wants the path from the vault root, not a bare file name. */
internal fun vaultRelativePath(noteFolderPath: String?, fileName: String): String =
    if (noteFolderPath.isNullOrEmpty()) fileName else "$noteFolderPath/$fileName"


// region Widget background

/** The user's widget colour; null while they keep the system one. */
private val LocalWidgetColors = staticCompositionLocalOf<WidgetColors?> { null }

/** The app's widget theme, plus the user's widget colour for everything in [ChromeColors]. */
@Composable
internal fun WidgetColorTheme(colors: WidgetColors?, content: @Composable () -> Unit) {
    WidgetTheme {
        CompositionLocalProvider(LocalWidgetColors provides colors, content = content)
    }
}

/**
 * Colours of what the widget draws on its own background. With the system colour they are the
 * theme's, exactly as before there was a choice; with a chosen one they come from [WidgetColors].
 */
internal object ChromeColors {
    val background: ColorProvider
        @Composable get() = fixed { it.background } ?: GlanceTheme.colors.widgetBackground
    val content: ColorProvider
        @Composable get() = fixed { it.content } ?: GlanceTheme.colors.onSurface
    val error: ColorProvider
        @Composable get() = fixed { it.error } ?: GlanceTheme.colors.error
    val progress: ColorProvider
        @Composable get() = fixed { it.content } ?: GlanceTheme.colors.primary
    val bar: ColorProvider
        @Composable get() = fixed { it.bar } ?: BAR_TINT
    val onBar: ColorProvider
        @Composable get() = fixed { it.onBar } ?: GlanceTheme.colors.onSurfaceVariant
    val button: ColorProvider
        @Composable get() = fixed { it.bar } ?: GlanceTheme.colors.widgetBackground
    val onButton: ColorProvider
        @Composable get() = fixed { it.onBar } ?: GlanceTheme.colors.onSurface

    @Composable
    private fun fixed(pick: (WidgetColors) -> Color): ColorProvider? =
        LocalWidgetColors.current?.let { ColorProvider(pick(it)) }
}

/**
 * The root's colour at the user's opacity. A chosen colour is one colour in both modes. The system
 * one, fully opaque, stays the theme's own provider, so the default widget is untouched. Otherwise
 * its day and night colours are resolved separately before fading, so a dark home screen keeps a
 * dark, see-through widget rather than a light one.
 */
@Composable
internal fun widgetBackground(percent: Int): ColorProvider {
    LocalWidgetColors.current?.let { chosen ->
        return ColorProvider(chosen.background.copy(alpha = WidgetBackground.alpha(percent)))
    }
    val base = GlanceTheme.colors.widgetBackground
    if (WidgetBackground.isOpaque(percent)) return base
    val context = LocalContext.current
    return remember(base, percent, context) {
        val alpha = WidgetBackground.alpha(percent)
        dayNightColor(
            day = base.getColor(context.withNightMode(false)).copy(alpha = alpha),
            night = base.getColor(context.withNightMode(true)).copy(alpha = alpha)
        )
    }
}

private fun Context.withNightMode(night: Boolean): Context {
    val configuration = Configuration(resources.configuration)
    val nightBits = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightBits
    return createConfigurationContext(configuration)
}

/**
 * On a see-through widget, puts the opaque widget background under [content], so text keeps the
 * backdrop it was designed for instead of landing on the wallpaper. On an opaque widget it adds
 * nothing at all — no extra view, no padding.
 */
@Composable
internal fun WidgetSurface(
    backgroundPercent: Int,
    cornerRadius: Dp,
    horizontalPadding: Dp = 0.dp,
    verticalPadding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    if (WidgetBackground.needsBacking(backgroundPercent)) {
        Box(
            modifier = GlanceModifier
                .cornerRadius(cornerRadius)
                .background(ChromeColors.background)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            content()
        }
    } else {
        content()
    }
}

// endregion

// region Note card colors (shared by both widget flavours)

@Composable
internal fun NoteCardAppearance.backgroundColor(): ColorProvider {
    val custom = if (color == NoteCardColor.Custom) {
        NoteCardPalette.parseCustom(customColorHex) ?: Color(0xFFE7E0EC)
    } else {
        null
    }
    val fixed = custom ?: NoteCardPalette.background(color)
    return fixed?.let { ColorProvider(it) } ?: GlanceTheme.colors.surfaceVariant
}

@Composable
internal fun NoteCardAppearance.contentColor(): ColorProvider {
    val custom = if (color == NoteCardColor.Custom) {
        NoteCardPalette.contentForCustom(customColorHex)
    } else {
        null
    }
    val fixed = custom ?: NoteCardPalette.content(color)
    return fixed?.let { ColorProvider(it) } ?: GlanceTheme.colors.onSurfaceVariant
}

// endregion
