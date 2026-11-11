package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import android.net.Uri
import android.widget.Toast
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
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.presentation.setup.NotesFolderSelection
import com.marfolog.noteswidgetformarkdown.presentation.setup.StarterFolder
import com.marfolog.noteswidgetformarkdown.presentation.setup.launchSafely
import com.marfolog.noteswidgetformarkdown.presentation.setup.subfolderPickerStart
import com.marfolog.noteswidgetformarkdown.presentation.widget.AddButtons
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.ReportingConsent
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.TelemetrySwitch
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    // What the open picker was opened for, kept across the process dying behind it, so the answer is counted as it was asked.
    var pickerKind by rememberSaveable { mutableStateOf(UsageEvent.PICKER_KIND_PICK) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        Telemetry.event(
            UsageEvent.FOLDER_PICKER_CLOSED,
            *UsageEvent.folderPickerClosed(pickerKind, UsageEvent.FROM_ONBOARDING, picked = uri != null)
        )
        if (uri != null) {
            NotesFolderSelection.select(context, uri, scope)
            refresh()
        }
    }

    fun openPicker(kind: String, startAt: Uri?) {
        pickerKind = kind
        Telemetry.event(UsageEvent.FOLDER_PICKER_OPENED, *UsageEvent.folderPickerOpened(kind, UsageEvent.FROM_ONBOARDING))
        folderPicker.launchSafely(startAt)
    }

    /** Makes the example folder, then opens the picker in it; when that fails, in Documents with a word about it. */
    fun createStarterFolder() {
        scope.launch {
            val result = withContext(Dispatchers.IO) { StarterFolder.prepare(context, StarterFolder.noteText(context)) }
            Telemetry.event(UsageEvent.STARTER_FOLDER, *UsageEvent.starterFolder(result))
            if (result == UsageEvent.STARTER_FAILED) {
                Toast.makeText(context, R.string.onboarding_starter_failed, Toast.LENGTH_LONG).show()
            }
            openPicker(
                UsageEvent.PICKER_KIND_STARTER,
                if (result == UsageEvent.STARTER_FAILED) StarterFolder.fallbackPickerStart() else StarterFolder.pickerStart()
            )
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
                    onPick = { openPicker(UsageEvent.PICKER_KIND_PICK, null) },
                    // The same picker, opened inside the subfolder: confirming it makes it the folder.
                    onPickSubfolder = { subfolder ->
                        openPicker(UsageEvent.PICKER_KIND_SUBFOLDER, subfolderPickerStart(notesUri, subfolder))
                    },
                    onStarterOffered = { Telemetry.event(UsageEvent.STARTER_OFFER_TAPPED) },
                    onCreateStarter = ::createStarterFolder,
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
