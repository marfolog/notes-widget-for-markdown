package com.marfolog.noteswidgetformarkdown

import android.app.Application
import com.marfolog.noteswidgetformarkdown.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NotesWidgetApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@NotesWidgetApp)
            modules(appModule)
        }
    }
}
