package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent

/**
 * The guide as the settings activity sees it: which step is showing, null when there is none.
 * Moving between steps and ending the guide go through here, so the activity holds no guide logic.
 */
internal class OnboardingSession(private val context: Context) {

    var step by mutableStateOf(
        Onboarding.stepOnOpen(OnboardingStore.savedStep(context), OnboardingStore.progress(context))
            ?.also { OnboardingStore.save(context, it) }
    )
        private set

    fun show(next: OnboardingStep) {
        OnboardingStore.save(context, next)
        step = next
    }

    fun finish(widgetsPlaced: Int) {
        if (widgetsPlaced == 0) {
            Telemetry.event(UsageEvent.SETUP_SKIPPED, UsageEvent.PARAM_STEP to UsageEvent.STEP_WIDGET)
        }
        Telemetry.event(UsageEvent.SETUP_STEP_DONE, UsageEvent.PARAM_STEP to UsageEvent.STEP_FINISHED)
        OnboardingStore.complete(context, widgetsPlaced)
        step = null
    }
}
