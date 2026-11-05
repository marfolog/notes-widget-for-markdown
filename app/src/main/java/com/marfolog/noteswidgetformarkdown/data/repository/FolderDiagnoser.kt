package com.marfolog.noteswidgetformarkdown.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.marfolog.noteswidgetformarkdown.domain.model.FolderDiagnosis
import com.marfolog.noteswidgetformarkdown.domain.model.FolderScan
import com.marfolog.noteswidgetformarkdown.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Diagnoses kept for a short while per folder, so the guide, settings and an empty widget asking
 * within seconds of each other search the tree once. Short on purpose: someone who just moved a
 * file into the folder expects the next look to see it.
 */
internal class FolderDiagnosisCache(
    private val ttlMillis: Long = TTL_MILLIS,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private class Entry(val diagnosis: FolderDiagnosis, val at: Long)

    private val entries = HashMap<String, Entry>()

    @Synchronized
    fun get(folderUri: String): FolderDiagnosis? {
        val entry = entries[folderUri] ?: return null
        if (clock() - entry.at > ttlMillis) {
            entries.remove(folderUri)
            return null
        }
        return entry.diagnosis
    }

    @Synchronized
    fun put(folderUri: String, diagnosis: FolderDiagnosis) {
        // A handful of folders at most, but never let a long-lived process collect them forever.
        if (entries.size >= MAX_ENTRIES) entries.clear()
        entries[folderUri] = Entry(diagnosis, clock())
    }

    @Synchronized
    fun forget(folderUri: String) {
        entries.remove(folderUri)
    }

    private companion object {
        const val TTL_MILLIS = 30_000L
        const val MAX_ENTRIES = 8
    }
}

/**
 * Looks into the picked notes folder and its subfolders through `DocumentsContract` queries, which
 * is one query per folder instead of one per file. Never throws: whatever the provider does, the
 * answer is a [FolderDiagnosis], [FolderDiagnosis.NoAccess] when the folder cannot be read at all.
 */
class FolderDiagnoser(context: Context) {

    private val appContext = context.applicationContext

    suspend fun diagnose(folderUri: String, refresh: Boolean = false): FolderDiagnosis =
        withContext(Dispatchers.IO) {
            // One search at a time: the guide and the selection that just happened both ask for the
            // same folder, and the second should find the first one's answer instead of searching again.
            searching.withLock {
            if (refresh) cache.forget(folderUri)
            cache.get(folderUri)?.let { return@withLock it }
            val diagnosis = runCatching { FolderDiagnosis.of(scan(folderUri)) }
                .getOrElse { error ->
                    AppLog.w(AREA, "Could not look into the notes folder", error)
                    FolderDiagnosis.NoAccess
                }
            // A folder that cannot be read right now may be fine in a moment, so do not remember that.
            if (diagnosis != FolderDiagnosis.NoAccess) cache.put(folderUri, diagnosis)
            AppLog.d(AREA, "Folder diagnosed as ${diagnosis::class.simpleName}")
            diagnosis
            }
        }

    private fun scan(folderUri: String): FolderScan {
        val treeUri = Uri.parse(folderUri)
        val hasGrant = appContext.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission
        }
        if (!hasGrant) return FolderScan.Unreadable
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootName = queryName(appContext.contentResolver, treeUri, rootId)
        return FolderScanner.scan(rootId, rootName, ContentResolverLister(appContext.contentResolver, treeUri))
    }

    private fun queryName(resolver: ContentResolver, treeUri: Uri, documentId: String): String? = try {
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        resolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val AREA = "Folder"
        private val cache = FolderDiagnosisCache()
        private val searching = Mutex()

        /** After the folder changed from the app's own side, such as a note created in it. */
        fun forget(folderUri: String) = cache.forget(folderUri)
    }
}

private class ContentResolverLister(
    private val resolver: ContentResolver,
    private val treeUri: Uri
) : DirectoryLister {

    override fun list(directoryId: String): List<DocEntry>? = try {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, directoryId)
        resolver.query(children, PROJECTION, null, null, null)?.use { cursor ->
            buildList {
                // A folder with tens of thousands of entries is not read to the end: the scan stops at 2,000 anyway.
                while (cursor.moveToNext() && size < MAX_ROWS) {
                    if (cursor.isNull(0)) continue
                    add(
                        DocEntry(
                            id = cursor.getString(0),
                            name = if (cursor.isNull(1)) null else cursor.getString(1),
                            isDirectory = !cursor.isNull(2) &&
                                cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        // SecurityException, a folder deleted in the middle, a provider that died, a bad cursor:
        // all the same to the caller, which treats this folder as not readable.
        null
    }

    private companion object {
        const val MAX_ROWS = 2_100
        val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
    }
}
