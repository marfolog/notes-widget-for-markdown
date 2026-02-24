package com.marfolog.noteswidgetformarkdown.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.marfolog.noteswidgetformarkdown.presentation.widget.NotesWidget

class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        NotesWidget.updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "notes_widget_refresh"
    }
}
