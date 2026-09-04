package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.WorkManager
import androidx.work.await
import com.marfolog.noteswidgetformarkdown.util.AppLog

/**
 * Glance drives each widget's recomposition through a WorkManager job it manages internally.
 * `GlanceAppWidget.update()` first checks whether that job is still running — and when it is,
 * update() just pokes the existing session instead of starting a new one. On device that check
 * has come back "running" for over a minute at a time while nothing was actually happening: the
 * widget stops redrawing until something else — so far only leaving it alone for a while —
 * frees it. See issue #4.
 *
 * The job's name is deterministic and public knowledge (`"appWidget-<id>"`, see Glance's
 * `AppWidgetUtils.createUniqueRemoteUiName`), so it can be cancelled through WorkManager's own
 * public API without touching any Glance internals. Cancelling it first means the "is it
 * running" check always comes back false, so `update()` is forced to start a fresh session
 * instead of trusting one that might be wedged.
 */
internal suspend fun forceFreshGlanceSession(context: Context, manager: GlanceAppWidgetManager, id: GlanceId) {
    val appWidgetId = runCatching { manager.getAppWidgetId(id) }.getOrNull() ?: return
    runCatching {
        WorkManager.getInstance(context)
            .cancelUniqueWork("appWidget-$appWidgetId")
            .await()
    }.onFailure { AppLog.w("Render", "Could not cancel session work for widget $appWidgetId", it) }
}
