package com.marfolog.noteswidgetformarkdown.util.prompt

/**
 * The one place that decides which prompt may be shown now, so that no prompt has to know about any
 * other. It is given what is due and what has happened and returns at most one prompt; it knows no
 * prompt by name.
 *
 *  1. At most one intrusive prompt (dialog, native review, banner) per visit and surface.
 *  2. The lower [PromptDef.priority] goes first.
 *  3. Dialogs keep [PromptDef.gapAfterInterruptingMillis] from the last dialog, unless the prompt
 *     [PromptDef.ignoresGap]; a banner keeps a day from it.
 *  4. A banner is never shown in the visit of a dialog.
 *  5. Prompts named in [PromptDef.noSameDayAs] are not shown on the same day.
 *  6. A passive prompt that is due keeps the prompts that list it in [PromptDef.suppressedBy] away.
 *  7. So does an informational one, such as a problem card.
 *
 * A clock earlier than the last dialog reads as "not yet".
 */
object PromptCoordinator {

    /**
     * One visit to a surface: [shown] are the prompts it already showed, [quiet] a visit somebody
     * came to read an answer in, which no prompt may cover.
     */
    data class Visit(val shown: List<PromptDef> = emptyList(), val quiet: Boolean = false)

    fun decide(registry: List<PromptDef>, context: PromptContext, visit: Visit = Visit()): PromptDef? {
        if (visit.quiet) return null
        if (visit.shown.any { it.kind.isIntrusive }) return null
        return registry
            .filter { it.kind.isIntrusive && context.surface in it.surfaces }
            .filter { it.isDue(context) }
            .filter { keepsItsDistance(it, context) }
            .filter { !sharesADayWith(it, context) }
            .filter { !suppressed(it, registry, context) }
            .minByOrNull { it.priority }
    }

    private fun keepsItsDistance(def: PromptDef, context: PromptContext): Boolean {
        if (def.ignoresGap(context)) return true
        val last = context.history.lastInterruptingAt() ?: return true
        return context.nowMillis - last >= def.gapAfterInterruptingMillis
    }

    private fun sharesADayWith(def: PromptDef, context: PromptContext): Boolean {
        if (def.noSameDayAs.isEmpty()) return false
        val today = context.history.dayOf(context.nowMillis)
        return def.noSameDayAs.any { context.history.wasShownOn(it, today) }
    }

    private fun suppressed(def: PromptDef, registry: List<PromptDef>, context: PromptContext): Boolean =
        def.suppressedBy.any { id -> registry.any { it.id == id && !it.kind.isIntrusive && it.isDue(context) } }

    /** Written by the coordinator and nobody else, so the whole picture is in one place. */
    fun markShown(history: PromptHistory, def: PromptDef, nowMillis: Long) = history.markShown(def, nowMillis)

    fun markAnswered(history: PromptHistory, id: String, nowMillis: Long) = history.markAnswered(id, nowMillis)
}
