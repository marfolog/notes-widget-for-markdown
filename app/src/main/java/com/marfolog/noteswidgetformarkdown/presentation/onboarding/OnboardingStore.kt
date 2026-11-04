package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import android.content.Context
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.presentation.widget.AddButtons
import com.marfolog.noteswidgetformarkdown.util.KeyValueStore
import com.marfolog.noteswidgetformarkdown.util.SharedPreferencesStore
import com.marfolog.noteswidgetformarkdown.util.Telemetry

/** What the guide remembers between process starts, and where it reads the user's answers from. */
internal object OnboardingStore {

    private const val KEY_STEP = "onboarding_step"

    fun progress(context: Context): OnboardingProgress = OnboardingProgress(
        canReport = Telemetry.ENABLED,
        consent = Onboarding.consentChoice(Telemetry.hasBeenAsked(context), Telemetry.isEnabled(context)),
        notesFolderChosen = notesUri(context) != null,
        buttons = AddButtons.choice(context)
    )

    fun notesUri(context: Context): String? = notesUri(store(context))

    /**
     * The folder the widget reads, which falls back to the vault root for whoever only ever picked
     * that, exactly as the widget does: such a user is set up and never sees the guide.
     */
    internal fun notesUri(store: KeyValueStore): String? =
        store.getString(SetupActivity.KEY_NOTES_URI) ?: store.getString(SetupActivity.KEY_VAULT_URI)

    fun savedStep(context: Context): OnboardingStep? = savedStep(store(context))

    internal fun savedStep(store: KeyValueStore): OnboardingStep? = OnboardingStep.fromStorage(store.getString(KEY_STEP))

    fun save(context: Context, step: OnboardingStep) = save(store(context), step)

    internal fun save(store: KeyValueStore, step: OnboardingStep) = store.putString(KEY_STEP, step.storageValue)

    /**
     * Ends the guide for good. Without a widget it counts as skipped, the same flag the old setup
     * banner set, so whatever reads that flag keeps meaning what it meant.
     */
    fun complete(context: Context, widgetsPlaced: Int) = complete(store(context), widgetsPlaced)

    internal fun complete(store: KeyValueStore, widgetsPlaced: Int) {
        store.remove(KEY_STEP)
        store.putBoolean(SetupActivity.KEY_GUIDE_DONE, true)
        if (widgetsPlaced == 0) store.putBoolean(SetupActivity.KEY_GUIDE_SKIPPED, true)
    }

    private fun store(context: Context): KeyValueStore = SharedPreferencesStore(
        context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
    )
}
