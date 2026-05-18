package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.worker.RefreshWorker
import java.util.concurrent.TimeUnit

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val syncUri = context
            .getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(SetupActivity.KEY_SYNC_URI, null)
            ?.trim()

        if (!syncUri.isNullOrEmpty()) {
            runCatching {
                val intent = Intent(context, TrampolineActivity::class.java)
                    .putExtra(TrampolineActivity.EXTRA_URI, syncUri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

                val refreshRequest = OneTimeWorkRequestBuilder<RefreshWorker>()
                    .setInitialDelay(SYNC_REFRESH_DELAY_SECONDS, TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    SYNC_REFRESH_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    refreshRequest
                )
            }.getOrElse {
                NotesWidget().update(context, glanceId)
            }
            return
        }

        val startedAt = System.currentTimeMillis()
        NotesWidget().update(context, glanceId)
        Log.d(TAG, "Manual refresh finished in ${System.currentTimeMillis() - startedAt}ms")
    }

    companion object {
        private const val TAG = "RefreshWidgetAction"
        private const val SYNC_REFRESH_DELAY_SECONDS = 20L
        private const val SYNC_REFRESH_WORK_NAME = "notes_widget_sync_refresh"
    }
}
