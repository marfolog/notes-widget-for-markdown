package com.marfolog.noteswidgetformarkdown.util.prompt

import android.content.Context
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.AppClock
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.SharedPreferencesStore
import com.marfolog.noteswidgetformarkdown.util.UsageEvent

/**
 * What a surface calls: it hands over its facts and gets the one prompt to show, if any, which is
 * then recorded as shown. [peek] asks the same question without recording, for a surface that has
 * to prepare something ahead of time.
 */
object PromptSurfaces {

    fun history(context: Context): PromptHistory = PromptHistory(
        SharedPreferencesStore(context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE))
    )

    fun decide(
        context: Context,
        surface: Surface,
        facts: PromptFacts,
        visit: PromptCoordinator.Visit = PromptCoordinator.Visit()
    ): PromptDef? {
        val history = history(context)
        val now = AppClock.now()
        val chosen = PromptCoordinator.decide(PROMPTS, PromptContext(now, surface, facts, history), visit)
        if (chosen != null) {
            PromptCoordinator.markShown(history, chosen, now)
            AppLog.d("Prompts", "${surface.name}: showing ${chosen.id}")
        }
        return chosen
    }

    fun peek(context: Context, surface: Surface, facts: PromptFacts): PromptDef? =
        PromptCoordinator.decide(PROMPTS, PromptContext(AppClock.now(), surface, facts, history(context)))

    /** Whoever tapped the sync chip came to read why it is amber or red; nothing may cover that. */
    fun isQuietVisit(openedFrom: String?): Boolean = openedFrom == UsageEvent.FROM_SYNC_CHIP

    /** A passive or informational prompt that was on screen, which is not chosen but still written down. */
    fun recordShown(context: Context, id: String) {
        val def = PROMPTS.firstOrNull { it.id == id } ?: return
        PromptCoordinator.markShown(history(context), def, AppClock.now())
    }

    /** An answer, so that "shown" and "answered" can be told apart. */
    fun answered(context: Context, id: String) =
        PromptCoordinator.markAnswered(history(context), id, AppClock.now())
}
