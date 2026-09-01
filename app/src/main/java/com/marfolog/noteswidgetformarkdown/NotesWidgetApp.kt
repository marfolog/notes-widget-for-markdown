package com.marfolog.noteswidgetformarkdown

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.marfolog.noteswidgetformarkdown.di.appModule
import com.marfolog.noteswidgetformarkdown.presentation.widget.NotesWidget
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.worker.NotesFolderObserverJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NotesWidgetApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Refresh the widget whenever the user unlocks the phone, so notes edited
    // elsewhere (Obsidian / GitSync pull) show up without waiting for the 15-min
    // WorkManager cycle. ACTION_USER_PRESENT can't be manifest-declared on
    // Android 8+, so it's registered dynamically here; it stays active while the
    // app process is alive (WorkManager covers the process-killed case).
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_USER_PRESENT) return
            val pending = goAsync()
            appScope.launch {
                try {
                    NotesWidget.updateAll(context.applicationContext)
                } catch (t: Throwable) {
                    Log.w(TAG, "Widget refresh on unlock failed", t)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Telemetry.init(this)
        AppLog.i("App", "Started, telemetry=${Telemetry.ENABLED}")
        startKoin {
            androidContext(this@NotesWidgetApp)
            modules(appModule)
        }
        NotesFolderObserverJob.schedule(this)
        ContextCompat.registerReceiver(
            this,
            unlockReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    companion object {
        private const val TAG = "NotesWidgetApp"
    }
}
