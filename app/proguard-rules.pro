# Glance builds its widgets through RemoteViews and looks up the receiver and the widget class by
# name, so R8 must not rename or remove them.
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.action.ActionCallback { *; }

# Entry points the system instantiates by name from the manifest.
-keep class com.marfolog.noteswidgetformarkdown.NotesWidgetApp { *; }
-keep class com.marfolog.noteswidgetformarkdown.worker.NotesFolderObserverJob { *; }

# Koin resolves these by class reference at runtime.
-keep class com.marfolog.noteswidgetformarkdown.domain.usecase.** { *; }
-keep class com.marfolog.noteswidgetformarkdown.domain.repository.** { *; }

# Crashlytics stack traces are worth reading; keep line numbers and original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# These names end up in logs and crash reports ("state=Success", "git=Tracked"). Obfuscated to
# s0 and c they say nothing, and a bug report is all the visibility the foss build has.
-keepnames class com.marfolog.noteswidgetformarkdown.domain.model.** { *; }
-keepnames class com.marfolog.noteswidgetformarkdown.presentation.widget.WidgetState** { *; }
