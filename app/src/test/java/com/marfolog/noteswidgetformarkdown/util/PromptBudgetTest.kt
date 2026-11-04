package com.marfolog.noteswidgetformarkdown.util

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PromptBudgetTest {

    private class InMemoryStore : KeyValueStore {
        val values = mutableMapOf<String, Any>()
        override fun contains(key: String) = key in values
        override fun getLong(key: String) = values[key] as Long?
        override fun putLong(key: String, value: Long) { values[key] = value }
        override fun getInt(key: String) = values[key] as Int?
        override fun putInt(key: String, value: Int) { values[key] = value }
        override fun getString(key: String) = values[key] as String?
        override fun putString(key: String, value: String) { values[key] = value }
        override fun getBoolean(key: String, default: Boolean) = values[key] as Boolean? ?: default
        override fun putBoolean(key: String, value: Boolean) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
    }

    private val day = TimeUnit.DAYS.toMillis(1)
    private val openedAt = 1_760_000_000_000L
    private val everything = setOf(AutoPrompt.Consent, AutoPrompt.WhatsNew)

    private fun pick(
        due: Set<AutoPrompt>,
        nowMillis: Long = openedAt,
        lastAutoPromptAtMillis: Long? = null,
        promptedThisVisit: Boolean = false,
        exemptFromGap: Set<AutoPrompt> = emptySet()
    ) = PromptBudget.pick(due, nowMillis, lastAutoPromptAtMillis, promptedThisVisit, exemptFromGap)

    @Test
    fun `nothing due means no dialog`() {
        assertNull(pick(emptySet()))
    }

    @Test
    fun `the only dialog due is shown when none came before`() {
        assertEquals(AutoPrompt.Consent, pick(setOf(AutoPrompt.Consent)))
        assertEquals(AutoPrompt.WhatsNew, pick(setOf(AutoPrompt.WhatsNew)))
    }

    @Test
    fun `the reporting question goes before everything else`() {
        assertEquals(AutoPrompt.Consent, pick(everything))
    }

    @Test
    fun `a second dialog in the same visit is refused, rotation included`() {
        assertNull(pick(setOf(AutoPrompt.WhatsNew), promptedThisVisit = true))
        assertNull(pick(everything, promptedThisVisit = true))
    }

    @Test
    fun `three days of quiet after any automatic dialog, from exactly three days on it is allowed`() {
        val lastAt = openedAt - 3 * day
        assertNull(pick(setOf(AutoPrompt.WhatsNew), nowMillis = lastAt + day, lastAutoPromptAtMillis = lastAt))
        assertNull(pick(setOf(AutoPrompt.WhatsNew), nowMillis = lastAt + 3 * day - 1, lastAutoPromptAtMillis = lastAt))
        assertEquals(
            AutoPrompt.WhatsNew,
            pick(setOf(AutoPrompt.WhatsNew), nowMillis = lastAt + 3 * day, lastAutoPromptAtMillis = lastAt)
        )
    }

    @Test
    fun `the quiet days hold back even the reporting question`() {
        assertNull(pick(everything, nowMillis = openedAt + day, lastAutoPromptAtMillis = openedAt))
    }

    @Test
    fun `a clock set before the last dialog reads as not yet`() {
        assertNull(pick(setOf(AutoPrompt.WhatsNew), nowMillis = openedAt - 10 * day, lastAutoPromptAtMillis = openedAt))
    }

    @Test
    fun `a dialog months ago no longer holds anything back`() {
        assertEquals(
            AutoPrompt.WhatsNew,
            pick(setOf(AutoPrompt.WhatsNew), nowMillis = openedAt + 90 * day, lastAutoPromptAtMillis = openedAt)
        )
    }

    @Test
    fun `what's new today makes the reporting question wait three days`() {
        val store = InMemoryStore()

        assertEquals(AutoPrompt.WhatsNew, PromptBudget.pick(store, setOf(AutoPrompt.WhatsNew), openedAt, promptedThisVisit = false))
        PromptBudget.markShown(store, openedAt)

        val nextVisit = openedAt + day
        assertNull(PromptBudget.pick(store, setOf(AutoPrompt.Consent), nextVisit, promptedThisVisit = false))
        assertEquals(
            AutoPrompt.Consent,
            PromptBudget.pick(store, setOf(AutoPrompt.Consent), openedAt + 3 * day, promptedThisVisit = false)
        )
    }

    @Test
    fun `the time of the last automatic dialog is what gets stored`() {
        val store = InMemoryStore()
        PromptBudget.markShown(store, openedAt)

        assertEquals(openedAt, store.values["last_auto_prompt_at"])
    }

    @Test
    fun `a fresh store holds nothing back`() {
        assertEquals(AutoPrompt.WhatsNew, PromptBudget.pick(InMemoryStore(), setOf(AutoPrompt.WhatsNew), openedAt, false))
    }

    @Test
    fun `a required dialog is not held back by the quiet days`() {
        assertEquals(
            AutoPrompt.WhatsNew,
            pick(
                due = setOf(AutoPrompt.WhatsNew),
                lastAutoPromptAtMillis = openedAt - day,
                exemptFromGap = setOf(AutoPrompt.WhatsNew)
            )
        )
    }

    @Test
    fun `a required dialog still waits for the next visit when another one was shown in this one`() {
        assertNull(
            pick(
                due = setOf(AutoPrompt.WhatsNew),
                promptedThisVisit = true,
                exemptFromGap = setOf(AutoPrompt.WhatsNew)
            )
        )
    }

    @Test
    fun `during the quiet days only the required dialog may come, the others keep waiting`() {
        assertEquals(
            AutoPrompt.WhatsNew,
            pick(
                due = everything,
                lastAutoPromptAtMillis = openedAt - day,
                exemptFromGap = setOf(AutoPrompt.WhatsNew)
            )
        )
        assertNull(pick(due = setOf(AutoPrompt.Consent), lastAutoPromptAtMillis = openedAt - day, exemptFromGap = setOf(AutoPrompt.WhatsNew)))
    }

    @Test
    fun `the reporting question still goes first when no quiet days hold it back`() {
        assertEquals(
            AutoPrompt.Consent,
            pick(due = everything, exemptFromGap = setOf(AutoPrompt.WhatsNew))
        )
    }

    @Test
    fun `right after the reporting question the news come on the next visit, not three days later`() {
        val store = InMemoryStore()
        val first = PromptBudget.pick(store, everything, openedAt, promptedThisVisit = false, exemptFromGap = setOf(AutoPrompt.WhatsNew))
        PromptBudget.markShown(store, openedAt)

        val second = PromptBudget.pick(
            store,
            setOf(AutoPrompt.WhatsNew),
            openedAt + 60_000,
            promptedThisVisit = false,
            exemptFromGap = setOf(AutoPrompt.WhatsNew)
        )

        assertEquals(AutoPrompt.Consent, first)
        assertEquals(AutoPrompt.WhatsNew, second)
    }
}
