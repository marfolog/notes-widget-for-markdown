package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import java.util.concurrent.TimeUnit

/** Dialogs settings open on their own, highest priority first. */
enum class AutoPrompt { Consent, WhatsNew }

/**
 * How often settings may interrupt with a dialog nobody asked for: one per visit at most, and
 * three days of quiet between any two. When several are due the first in [AutoPrompt] wins and
 * the others wait. Dialogs the user opens themselves are not counted.
 *
 * A clock earlier than the last automatic dialog reads as "not yet".
 *
 * A dialog the user has to answer, such as the add buttons question of 1.4, is not held back by
 * the quiet days, only by the one-per-visit rule: waiting three days for a required choice would
 * leave it unanswered for three days.
 */
object PromptBudget {

    val MIN_GAP_MILLIS = TimeUnit.DAYS.toMillis(3)

    private const val KEY_LAST_AUTO_PROMPT_AT = "last_auto_prompt_at"

    /**
     * Whoever taps the sync chip came to find out why it is amber or red. No dialog may cover
     * that answer, so such a visit gets no automatic dialog at all.
     */
    fun isQuietVisit(openedFrom: String?): Boolean = openedFrom == UsageEvent.FROM_SYNC_CHIP

    internal fun pick(
        due: Set<AutoPrompt>,
        nowMillis: Long,
        lastAutoPromptAtMillis: Long?,
        promptedThisVisit: Boolean,
        exemptFromGap: Set<AutoPrompt> = emptySet()
    ): AutoPrompt? {
        if (promptedThisVisit) return null
        val quiet = lastAutoPromptAtMillis != null && nowMillis - lastAutoPromptAtMillis < MIN_GAP_MILLIS
        val candidates = if (quiet) due.intersect(exemptFromGap) else due
        return AutoPrompt.entries.firstOrNull { it in candidates }
    }

    fun pick(
        context: Context,
        due: Set<AutoPrompt>,
        nowMillis: Long,
        promptedThisVisit: Boolean,
        exemptFromGap: Set<AutoPrompt> = emptySet()
    ): AutoPrompt? = pick(store(context), due, nowMillis, promptedThisVisit, exemptFromGap)

    internal fun pick(
        store: KeyValueStore,
        due: Set<AutoPrompt>,
        nowMillis: Long,
        promptedThisVisit: Boolean,
        exemptFromGap: Set<AutoPrompt> = emptySet()
    ) = pick(due, nowMillis, store.getLong(KEY_LAST_AUTO_PROMPT_AT), promptedThisVisit, exemptFromGap)

    fun markShown(context: Context, nowMillis: Long) = markShown(store(context), nowMillis)

    internal fun markShown(store: KeyValueStore, nowMillis: Long) = store.putLong(KEY_LAST_AUTO_PROMPT_AT, nowMillis)

    private fun store(context: Context): KeyValueStore = SharedPreferencesStore(
        context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
    )
}
