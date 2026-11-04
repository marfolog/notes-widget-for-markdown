package com.marfolog.noteswidgetformarkdown.util

import android.content.Context

/** The two things an answer to the reporting question changes. */
internal interface ReportingSwitch {
    fun setEnabled(enabled: Boolean)
    fun markAsked()
}

internal class TelemetrySwitch(private val context: Context) : ReportingSwitch {
    override fun setEnabled(enabled: Boolean) = Telemetry.setEnabled(context, enabled)
    override fun markAsked() = Telemetry.markAsked(context)
}

/**
 * The answer to the reporting question, wherever it is asked: the first-launch guide, the dialog in
 * settings, the one after a saved note. All of them end up here, so an answer has the same effect
 * from each — and once it is given, none of them asks again.
 */
internal object ReportingConsent {

    fun answer(switch: ReportingSwitch, agreed: Boolean) {
        switch.setEnabled(agreed)
        switch.markAsked()
    }
}
