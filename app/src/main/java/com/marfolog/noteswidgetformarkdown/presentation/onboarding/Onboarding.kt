package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import com.marfolog.noteswidgetformarkdown.domain.model.FolderDiagnosis
import com.marfolog.noteswidgetformarkdown.util.UsageEvent

/**
 * The screens of the first-launch guide, in the order they come: this declaration is the one place
 * that order lives, so moving a step is moving its line. Consent exists only where reporting does.
 */
internal enum class OnboardingStep(val storageValue: String, val eventValue: String?) {

    /** Not reported: nothing may be sent before the reporting question has been answered. */
    Welcome("welcome", null),
    Consent("consent", UsageEvent.STEP_CONSENT),
    Folder("folder", UsageEvent.STEP_FOLDER),
    Buttons("buttons", UsageEvent.STEP_BUTTONS),
    Widget("widget", UsageEvent.STEP_WIDGET);

    companion object {
        fun fromStorage(value: String?): OnboardingStep? = entries.firstOrNull { it.storageValue == value }
    }
}

/**
 * What the guide knows about the user's answers so far. [consent] and [buttons] are null until the
 * user picks, since neither is preselected; true is "share stats" and "show the buttons".
 */
internal data class OnboardingProgress(
    val canReport: Boolean,
    val consent: Boolean?,
    val notesFolderChosen: Boolean,
    val buttons: Boolean?
)

/** The main button of the folder step: go on, pick one of the subfolders, or pick another folder. */
internal enum class FolderPrimary { Continue, PickSubfolder, ChooseFolder }

/** What the widget step offers: the pin buttons, a widget already in place, or how to add one by hand. */
internal enum class WidgetStepState { Offer, Placed, ManualHint }

/**
 * Who sees the first-launch guide and which step is next. Whoever has a notes folder never sees it,
 * which keeps it away from everyone who updated. Every step stores itself, so a rotation, the folder
 * picker or the process being killed in between all come back to the same step.
 */
internal object Onboarding {

    fun steps(canReport: Boolean): List<OnboardingStep> =
        OnboardingStep.entries.filter { it != OnboardingStep.Consent || canReport }

    /** Whether the user has done what [step] asks of them, so they may go on from it. */
    fun isDone(step: OnboardingStep, progress: OnboardingProgress): Boolean = when (step) {
        OnboardingStep.Welcome, OnboardingStep.Widget -> true
        OnboardingStep.Consent -> progress.consent != null
        OnboardingStep.Folder -> progress.notesFolderChosen
        OnboardingStep.Buttons -> progress.buttons != null
    }

    /**
     * Null: straight to settings. A stored step is only honoured when every step before it is done,
     * otherwise the guide goes back to the first one that is not; a stored step this build does not
     * have, such as consent on foss, restarts the guide.
     */
    fun stepOnOpen(savedStep: OnboardingStep?, progress: OnboardingProgress): OnboardingStep? {
        val steps = steps(progress.canReport)
        if (savedStep == null) return if (progress.notesFolderChosen) null else steps.first()
        val target = steps.indexOf(savedStep).coerceAtLeast(0)
        return steps.take(target).firstOrNull { !isDone(it, progress) } ?: steps[target]
    }

    /** Stays on [step] until it is done; null after the last one, when the guide is over. */
    fun next(step: OnboardingStep, progress: OnboardingProgress): OnboardingStep? {
        if (!isDone(step, progress)) return step
        val steps = steps(progress.canReport)
        return steps.getOrNull(steps.indexOf(step) + 1)
    }

    /** Null on the first step: back leaves the app, as it would from any first screen. */
    fun back(step: OnboardingStep, progress: OnboardingProgress): OnboardingStep? {
        val steps = steps(progress.canReport)
        return steps.getOrNull(steps.indexOf(step) - 1)
    }

    fun position(step: OnboardingStep, progress: OnboardingProgress): Int =
        steps(progress.canReport).indexOf(step) + 1

    fun count(progress: OnboardingProgress): Int = steps(progress.canReport).size

    /** The answer a card shows as picked: none until the question has been answered. */
    fun consentChoice(asked: Boolean, enabled: Boolean): Boolean? = if (asked) enabled else null

    /**
     * A widget placed by any route counts, also one put there before the guide opened. A launcher
     * that cannot pin, or a pin request that came back without a widget, gets the manual steps.
     */
    fun widgetStepState(widgetsPlaced: Int, pinSupported: Boolean, pinRequested: Boolean): WidgetStepState = when {
        widgetsPlaced > 0 -> WidgetStepState.Placed
        !pinSupported || pinRequested -> WidgetStepState.ManualHint
        else -> WidgetStepState.Offer
    }

    /**
     * What the folder step makes the main button. Nothing here stops anyone: an empty folder may be
     * meant for notes still to come, so continuing stays one tap away in every case.
     */
    fun folderPrimary(diagnosis: FolderDiagnosis?): FolderPrimary = when (diagnosis) {
        null, is FolderDiagnosis.HasNotes, FolderDiagnosis.EmptyFolder -> FolderPrimary.Continue
        is FolderDiagnosis.NotesInSubfolders -> FolderPrimary.PickSubfolder
        is FolderDiagnosis.NoMarkdown, FolderDiagnosis.NoAccess -> FolderPrimary.ChooseFolder
    }
}
