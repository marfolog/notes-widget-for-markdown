package com.marfolog.noteswidgetformarkdown.util

/** What, if anything, follows a saved note. */
enum class AfterNotePrompt {
    None, Consent, NativeReview, Rating;

    /** Whether something is drawn over the screen that the activity has to wait for the user to close. */
    val showsDialog: Boolean get() = this == Consent || this == Rating

    /** Whether it counts against [PromptBudget], so that settings keep their distance from it. */
    val countsAsAutomaticDialog: Boolean get() = this != None

    companion object {

        /**
         * One thing per save. The reporting question goes first and the rating asks wait for the next
         * save: a request only counts as made when it is, so nothing is lost by waiting.
         */
        fun choose(consentDue: Boolean, ask: RatingAsk): AfterNotePrompt = when {
            consentDue -> Consent
            ask == RatingAsk.Native -> NativeReview
            ask == RatingAsk.Dialog -> Rating
            else -> None
        }
    }
}
