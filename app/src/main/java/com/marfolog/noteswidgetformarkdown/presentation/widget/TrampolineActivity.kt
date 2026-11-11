package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent

/** A widget tap lands here first, so the app can count it before [noteOpenIntent] opens the note. */
class TrampolineActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openNote()
        finish()
    }

    private fun openNote() {
        val request = NoteOpenRequest.fromExtras(
            string = intent::getStringExtra,
            boolean = { intent.getBooleanExtra(it, false) }
        ) ?: run {
            AppLog.w(AREA, "Started without a note, nothing to open")
            return
        }
        Telemetry.event(UsageEvent.NOTE_OPENED, noteOpenedEvent(OpenWith.current(this)))
        // Without a handler for obsidian:// startActivity throws and would take the app down with it.
        var step: OpenStep? = openStepFor(OpenWith.current(this), request.note, request.noteFolderPath)
        while (step != null) {
            try {
                startActivity(noteOpenIntent(this, step, request.note, request.vaultName, request.noteFolderPath))
                AppLog.d(AREA, "Opened ${AppLog.redactFileName(request.note.fileUri)} as $step")
                return
            } catch (e: ActivityNotFoundException) {
                AppLog.w(AREA, "No app handles $step", e)
                step = nextOpenStep(step)
            }
        }
        Toast.makeText(this, R.string.no_app_opens_notes, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val AREA = "OpenNote"

        fun intent(
            context: Context,
            note: NoteSummary,
            vaultName: String?,
            noteFolderPath: String?
        ): Intent = Intent(context, TrampolineActivity::class.java)
            .putExtras(bundleOf(*NoteOpenRequest(note, vaultName, noteFolderPath).toExtras().toTypedArray()))
    }
}
