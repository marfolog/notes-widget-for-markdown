package com.marfolog.noteswidgetformarkdown.util

/** foss flavour: has no network and no question to answer, so there is nothing to send. */
internal object InterestSender {
    @Suppress("UNUSED_PARAMETER")
    fun send(feature: String, answer: String) = Unit
}
