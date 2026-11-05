package com.marfolog.noteswidgetformarkdown.util.prompt

import com.marfolog.noteswidgetformarkdown.util.KeyValueStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * When prompts were shown and answered, in one place. "Shown" and "answered" are kept apart: a
 * dialog can be shown many times before it is answered, and a banner closed without an answer is
 * still an answer of a kind. New keys are `prompt.<id>.*`; the times older versions wrote under
 * their own names are only read, through [LEGACY_SHOWN_AT], and never migrated.
 *
 * A "day" is the calendar day in [zone]; gaps between prompts are plain milliseconds.
 */
class PromptHistory internal constructor(
    private val store: KeyValueStore,
    private val zone: ZoneId = ZoneId.systemDefault()
) {

    fun shownAt(id: String): Long? =
        store.time(key(id, "shown_at")) ?: LEGACY_SHOWN_AT[id]?.let { store.time(it) }

    fun answeredAt(id: String): Long? = store.time(key(id, "answered_at"))

    /** The last time anything that interrupts was shown, older versions' dialogs included. */
    fun lastInterruptingAt(): Long? = listOfNotNull(
        store.time(KEY_LAST_INTERRUPTING),
        store.time(LEGACY_LAST_DIALOG)
    ).maxOrNull()

    fun lastShownOn(kind: PromptKind): Long? = store.time("$PREFIX.kind.${kind.name.lowercase()}.shown_at")

    fun wasShownOn(id: String, day: LocalDate): Boolean =
        shownAt(id)?.let { dayOf(it) == day } == true

    fun dayOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    internal fun markShown(def: PromptDef, nowMillis: Long) {
        store.putLong(key(def.id, "shown_at"), nowMillis)
        store.putLong("$PREFIX.kind.${def.kind.name.lowercase()}.shown_at", nowMillis)
        if (def.kind == PromptKind.Interrupting) store.putLong(KEY_LAST_INTERRUPTING, nowMillis)
    }

    internal fun markAnswered(id: String, nowMillis: Long) = store.putLong(key(id, "answered_at"), nowMillis)

    /** Forgets everything the coordinator wrote, for testing; the older keys are left alone. */
    internal fun clearAll(ids: Collection<String>) {
        ids.forEach { id ->
            store.remove(key(id, "shown_at"))
            store.remove(key(id, "answered_at"))
        }
        PromptKind.entries.forEach { store.remove("$PREFIX.kind.${it.name.lowercase()}.shown_at") }
        store.remove(KEY_LAST_INTERRUPTING)
    }

    private fun KeyValueStore.time(key: String): Long? = getLong(key)?.takeIf { it >= 0 }

    companion object {
        private const val PREFIX = "prompt"
        private const val KEY_LAST_INTERRUPTING = "$PREFIX.last_interrupting_at"

        /** Where the old PromptBudget kept the last dialog's time. */
        internal const val LEGACY_LAST_DIALOG = "last_auto_prompt_at"

        /** Prompt id to the key an older version stored its shown time under. */
        internal val LEGACY_SHOWN_AT = mapOf(
            "whats_new" to "whats_new_shown_at",
            "rating_native" to "after_note_rating_native_at"
        )

        internal fun key(id: String, field: String) = "$PREFIX.$id.$field"
    }
}
