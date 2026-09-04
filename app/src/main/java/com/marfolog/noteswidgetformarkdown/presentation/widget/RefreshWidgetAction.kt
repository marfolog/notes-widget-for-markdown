package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.marfolog.noteswidgetformarkdown.util.AppLog

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val startedAt = System.currentTimeMillis()
        AppLog.d("Refresh", "Manual tap at $startedAt")
        // A single update(glanceId) call has no defence against the Glance session race — the
        // button was the one caller that skipped the retry every other trigger already gets.
        // Route it through the same path so a tap gets the same second attempt.
        NotesWidget.updateAll(context)
        AppLog.d("Refresh", "Manual refresh finished in ${System.currentTimeMillis() - startedAt}ms")
    }

}
