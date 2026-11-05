package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.marfolog.noteswidgetformarkdown.presentation.setup.NotesFolderSelection
import com.marfolog.noteswidgetformarkdown.presentation.setup.launchSafely
import com.marfolog.noteswidgetformarkdown.presentation.setup.subfolderPickerStart
import com.marfolog.noteswidgetformarkdown.presentation.widget.AddButtons
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.ReportingConsent
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.TelemetrySwitch
import com.marfolog.noteswidgetformarkdown.util.UsageEvent

private const val AREA = "Onboarding"

/**
 * The first-launch guide: the header, the way back, and the step [session] says is showing. What
 * each step decides lives in [Onboarding]; the step files only draw.
 */
@Composable
internal fun OnboardingScreen(session: OnboardingSession, onLeave: () -> Unit) {
    val step = session.step ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf(OnboardingStore.progress(context)) }
    var notesUri by rememberSaveable { mutableStateOf(OnboardingStore.notesUri(context)) }

    fun refresh() {
        progress = OnboardingStore.progress(context)
        notesUri = OnboardingStore.notesUri(context)
    }

    // Registered on every step: after the process was killed behind the picker, the result is
    // delivered to whichever launcher sits here when the activity comes back.
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            NotesFolderSelection.select(context, uri, scope)
            refresh()
        }
    }

    fun goBack() {
        Onboarding.back(step, progress)?.let(session::show) ?: onLeave()
    }

    fun advance() {
        step.eventValue?.let { Telemetry.event(UsageEvent.SETUP_STEP_DONE, UsageEvent.PARAM_STEP to it) }
        Onboarding.next(step, progress)?.let(session::show)
    }

    /** One tap answers and moves on; the next step is worked out from the answer just stored. */
    fun answerConsent(agreed: Boolean) {
        ReportingConsent.answer(TelemetrySwitch(context), agreed)
        AppLog.i(AREA, "Reporting switched ${if (agreed) "on" else "off"} in the guide")
        refresh()
        step.eventValue?.let { Telemetry.event(UsageEvent.SETUP_STEP_DONE, UsageEvent.PARAM_STEP to it) }
        Onboarding.next(step, progress)?.let(session::show)
    }

    fun chooseButtons(shown: Boolean) {
        if (progress.buttons == shown) return
        AddButtons.setShown(context, shown)
        Telemetry.event(UsageEvent.SETTING_CHANGED, *UsageEvent.addButtonsChanged(shown, UsageEvent.FROM_ONBOARDING))
        refresh()
    }

    BackHandler(enabled = Onboarding.back(step, progress) != null) { goBack() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OnboardingHeader(position = Onboarding.position(step, progress), count = Onboarding.count(progress))
            Spacer(modifier = Modifier.height(24.dp))
            when (step) {
                OnboardingStep.Welcome -> WelcomeStep(onGetStarted = ::advance)
                OnboardingStep.Consent -> ConsentStep(onAnswer = ::answerConsent, onBack = ::goBack)
                OnboardingStep.Folder -> FolderStep(
                    folder = rememberFolderSummary(notesUri),
                    onPick = { folderPicker.launchSafely(null) },
                    // The same picker, opened inside the subfolder: confirming it makes it the folder.
                    onPickSubfolder = { subfolder -> folderPicker.launchSafely(subfolderPickerStart(notesUri, subfolder)) },
                    onBack = ::goBack,
                    onContinue = ::advance
                )
                OnboardingStep.Buttons -> ButtonsStep(
                    choice = progress.buttons,
                    onChoose = ::chooseButtons,
                    onBack = ::goBack,
                    onContinue = ::advance
                )
                OnboardingStep.Widget -> WidgetStepHost(
                    showButtons = progress.buttons != false,
                    onFinish = session::finish,
                    onBack = ::goBack
                )
            }
        }
    }
}
