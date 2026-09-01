package com.marfolog.noteswidgetformarkdown.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Finds the real Obsidian vault among the folders the user granted us.
 *
 * `obsidian://open` needs the vault name and a path relative to the vault root. Users often
 * point the widget at a subfolder, whose name is not a vault — Obsidian then silently opens
 * whatever note was last open. The vault root is the folder holding `.obsidian`, so we look
 * for that instead of trusting the picked folder's name.
 */
class ObsidianVaultLocator(private val context: Context) {

    data class Vault(val name: String, val documentId: String)

    suspend fun locate(): Vault? = withContext(Dispatchers.IO) {
        context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .firstNotNullOfOrNull { permission ->
                runCatching {
                    val tree = DocumentFile.fromTreeUri(context, permission.uri)
                        ?: return@runCatching null
                    val holdsVault = tree.findFile(OBSIDIAN_DIR)?.isDirectory == true
                    val name = tree.name
                    if (holdsVault && !name.isNullOrEmpty()) {
                        Vault(
                            name = name,
                            documentId = DocumentsContract.getTreeDocumentId(permission.uri)
                        )
                    } else {
                        null
                    }
                }.getOrElse { e ->
                    Log.w(TAG, "Could not inspect ${permission.uri}", e)
                    null
                }
            }
    }

    companion object {
        private const val TAG = "ObsidianVaultLocator"
        private const val OBSIDIAN_DIR = ".obsidian"

        /**
         * Path of the notes folder relative to the vault root, e.g. `Projects`.
         * Empty when the notes folder *is* the vault root; null when it lives outside it.
         */
        fun relativePath(vaultDocumentId: String, notesFolderUri: String?): String? {
            val notesDocId = runCatching {
                DocumentsContract.getTreeDocumentId(Uri.parse(notesFolderUri ?: return null))
            }.getOrNull() ?: return null

            if (notesDocId == vaultDocumentId) {
                return ""
            }
            if (!notesDocId.startsWith("$vaultDocumentId/")) {
                return null
            }
            return notesDocId.removePrefix("$vaultDocumentId/")
        }
    }
}
