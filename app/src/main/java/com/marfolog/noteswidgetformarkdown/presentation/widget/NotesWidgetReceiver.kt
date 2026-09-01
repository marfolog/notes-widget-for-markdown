package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.marfolog.noteswidgetformarkdown.worker.RefreshWorker
import java.util.concurrent.TimeUnit

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesWidget()

    // No onUpdate override: GlanceAppWidgetReceiver already recomposes the widget here.
    // Calling NotesWidget.updateAll() from it would bounce back through the fallback
    // broadcast that updateAll sends, and the two would keep triggering each other.

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork(RefreshWorker.WORK_NAME)
    }
}
