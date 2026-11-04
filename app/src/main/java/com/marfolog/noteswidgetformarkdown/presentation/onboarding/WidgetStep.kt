package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.presentation.widget.WidgetInventory
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import kotlinx.coroutines.delay

/** How long a launcher may take to bind a pinned widget after its dialog closes. */
private const val PIN_SETTLE_MILLIS = 1_000L

/** Asking the launcher to pin a widget, and counting what is on the home screen. */
internal object WidgetPinning {

    fun isSupported(context: Context): Boolean = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    fun request(context: Context, kind: WidgetInventory.Kind): Boolean = runCatching {
        AppWidgetManager.getInstance(context).requestPinAppWidget(ComponentName(context, kind.receiver), null, null)
    }.onFailure { AppLog.w(SetupActivity.AREA, "Pinning a widget failed", it) }.getOrDefault(false)

    fun placed(context: Context): Int = WidgetInventory.Kind.entries.sumOf { WidgetInventory.count(context, it) }
}

/**
 * The launcher's pin dialog covers this screen, so coming back is when a new widget can be seen;
 * it is looked at once more after a moment, in case the launcher binds the widget a beat later.
 */
@Composable
private fun rememberPlacedWidgets(): Int {
    val context = LocalContext.current
    var placed by remember { mutableIntStateOf(WidgetPinning.placed(context)) }
    var resumes by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resumes++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(resumes) {
        placed = WidgetPinning.placed(context)
        delay(PIN_SETTLE_MILLIS)
        placed = WidgetPinning.placed(context)
    }
    return placed
}

/** Holds what the step needs to know about the launcher and renders it; [showButtons] is the earlier answer. */
@Composable
internal fun ColumnScope.WidgetStepHost(showButtons: Boolean, onFinish: (widgetsPlaced: Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val pinSupported = remember { WidgetPinning.isSupported(context) }
    var pinRequested by rememberSaveable { mutableStateOf(false) }
    var reportedPlaced by rememberSaveable { mutableStateOf(false) }
    val placed = rememberPlacedWidgets()
    val state = Onboarding.widgetStepState(placed, pinSupported, pinRequested)

    LaunchedEffect(state) {
        if (state == WidgetStepState.Placed && !reportedPlaced) {
            reportedPlaced = true
            Telemetry.event(UsageEvent.SETUP_STEP_DONE, UsageEvent.PARAM_STEP to UsageEvent.STEP_WIDGET)
        }
    }

    WidgetStep(
        state = state,
        showButtons = showButtons,
        pinSupported = pinSupported,
        onPin = { kind ->
            pinRequested = true
            if (!WidgetPinning.request(context, kind)) {
                AppLog.i(SetupActivity.AREA, "Launcher refused to pin a ${kind.eventValue} widget")
            }
        },
        onFinish = { onFinish(placed) },
        onBack = onBack
    )
}

@Composable
private fun ColumnScope.WidgetStep(
    state: WidgetStepState,
    showButtons: Boolean,
    pinSupported: Boolean,
    onPin: (WidgetInventory.Kind) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val previewDescription = stringResource(R.string.onboarding_welcome_preview)
    StepLayout(
        titleRes = R.string.onboarding_widget_title,
        bodyRes = R.string.onboarding_widget_body,
        content = {
            when (state) {
                WidgetStepState.Placed -> StatusCard(
                    icon = Icons.Outlined.CheckCircle,
                    title = null,
                    text = stringResource(R.string.onboarding_widget_placed)
                )
                WidgetStepState.ManualHint -> StatusCard(
                    icon = Icons.Outlined.Widgets,
                    title = null,
                    text = stringResource(R.string.onboarding_widget_manual)
                )
                WidgetStepState.Offer -> MiniWidget(
                    showButtons = showButtons,
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .align(Alignment.CenterHorizontally)
                        .semantics { contentDescription = previewDescription }
                )
            }
        },
        buttons = {
            if (state == WidgetStepState.Placed) {
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onboarding_finish))
                }
            } else {
                if (pinSupported) {
                    Button(onClick = { onPin(WidgetInventory.Kind.Cards) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onboarding_widget_add_cards))
                    }
                    TextButton(
                        onClick = { onPin(WidgetInventory.Kind.List) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(R.string.onboarding_widget_add_list))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onboarding_widget_later))
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                Text(stringResource(R.string.onboarding_back))
            }
        }
    )
}
