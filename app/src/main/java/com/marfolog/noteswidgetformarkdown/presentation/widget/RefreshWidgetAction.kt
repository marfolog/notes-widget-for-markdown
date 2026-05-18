package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val startedAt = System.currentTimeMillis()
        NotesWidget().update(context, glanceId)
        Log.d(TAG, "Manual refresh finished in ${System.currentTimeMillis() - startedAt}ms")
    }

    companion object {
        private const val TAG = "RefreshWidgetAction"
    }
}
