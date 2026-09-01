package com.marfolog.noteswidgetformarkdown.worker

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import android.util.Log
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.presentation.widget.NotesWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Refreshes the widget as soon as the notes folder changes.
 *
 * Waiting for the 15-minute WorkManager cycle or a screen unlock means a note created in
 * Obsidian (or pulled by a sync client) shows up minutes late. JobScheduler content triggers
 * watch the SAF folder itself and start the app process when the provider reports a change.
 *
 * Content-trigger jobs are one-shot, so every run re-arms the next one.
 */
class NotesFolderObserverJob : JobService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            runCatching {
                if (notesChangedSinceLastRun(applicationContext)) {
                    NotesWidget.updateAll(applicationContext)
                }
            }.onFailure { Log.w(TAG, "Refresh on folder change failed", it) }
            schedule(applicationContext)
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    companion object {
        private const val TAG = "NotesFolderObserver"
        private const val JOB_ID = 4711

        /** Coalesce bursts — a sync client writing ten files should cause one refresh. */
        private const val UPDATE_DELAY_MILLIS = 2_000L
        private const val MAX_DELAY_MILLIS = 10_000L
        private const val KEY_FOLDER_FINGERPRINT = "notes_folder_fingerprint"

        /**
         * The MediaStore trigger fires for any file on the device, so check whether *our* notes
         * actually moved before redrawing. Listing a handful of files is far cheaper than a
         * needless widget update on every photo taken.
         */
        private fun notesChangedSinceLastRun(context: Context): Boolean {
            val prefs = context.getSharedPreferences(
                SetupActivity.PREFS_NAME,
                Context.MODE_PRIVATE
            )
            val folderUri = prefs.getString(SetupActivity.KEY_NOTES_URI, null)
                ?: prefs.getString(SetupActivity.KEY_VAULT_URI, null)
                ?: return false

            val fingerprint = runCatching {
                val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
                    ?: return@runCatching null
                val markdown = folder.listFiles()
                    .filter { it.isFile && it.name?.endsWith(".md", ignoreCase = true) == true }
                val newest = markdown.maxOfOrNull { it.lastModified() } ?: 0L
                "${markdown.size}:$newest"
            }.getOrNull() ?: return true

            if (fingerprint == prefs.getString(KEY_FOLDER_FINGERPRINT, null)) {
                return false
            }
            prefs.edit().putString(KEY_FOLDER_FINGERPRINT, fingerprint).apply()
            return true
        }

        fun schedule(context: Context) {
            val prefs = context.getSharedPreferences(
                SetupActivity.PREFS_NAME,
                Context.MODE_PRIVATE
            )
            val folderUri = prefs.getString(SetupActivity.KEY_NOTES_URI, null)
                ?: prefs.getString(SetupActivity.KEY_VAULT_URI, null)
                ?: return

            val childrenUri = runCatching {
                val treeUri = Uri.parse(folderUri)
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )
            }.getOrElse {
                Log.w(TAG, "Could not build observation URI for $folderUri", it)
                return
            }

            val job = JobInfo.Builder(
                JOB_ID,
                ComponentName(context, NotesFolderObserverJob::class.java)
            )
                .addTriggerContentUri(
                    JobInfo.TriggerContentUri(
                        childrenUri,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS
                    )
                )
                // Apps with all-files access (Obsidian, sync clients) write straight to the
                // filesystem, which the documents provider does not report. MediaStore does
                // see those writes, so it is the trigger that actually fires in practice.
                .addTriggerContentUri(
                    JobInfo.TriggerContentUri(
                        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS
                    )
                )
                .setTriggerContentUpdateDelay(UPDATE_DELAY_MILLIS)
                .setTriggerContentMaxDelay(MAX_DELAY_MILLIS)
                .build()

            runCatching {
                context.getSystemService(JobScheduler::class.java)?.schedule(job)
            }.onFailure { Log.w(TAG, "Could not schedule folder observer", it) }
        }
    }
}
