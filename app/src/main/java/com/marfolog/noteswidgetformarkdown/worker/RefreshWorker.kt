package com.marfolog.noteswidgetformarkdown.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.marfolog.noteswidgetformarkdown.presentation.widget.NotesWidget
import com.marfolog.noteswidgetformarkdown.util.AppLog

class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        AppLog.d("15min", "cycle fired at ${System.currentTimeMillis()}")
        NotesWidget.updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "notes_widget_refresh"
    }
}
