package com.marfolog.noteswidgetformarkdown.util

import com.marfolog.noteswidgetformarkdown.util.prompt.PromptIds

/** What, if anything, follows a saved note. Which one is for the prompt coordinator to say. */
enum class AfterNotePrompt {
    None, Consent, NativeReview, Rating;

    /** Whether something is drawn over the screen that the activity has to wait for the user to close. */
    val showsDialog: Boolean get() = this == Consent || this == Rating

    companion object {
        /** The prompt the coordinator chose, by its id; anything else shows nothing. */
        fun of(promptId: String?): AfterNotePrompt = when (promptId) {
            PromptIds.CONSENT -> Consent
            PromptIds.RATING_NATIVE -> NativeReview
            PromptIds.RATING_DIALOG -> Rating
            else -> None
        }
    }
}
