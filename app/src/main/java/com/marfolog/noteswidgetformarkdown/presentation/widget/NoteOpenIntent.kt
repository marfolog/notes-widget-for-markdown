package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity

/**
 * How a tapped note gets opened.
 *
 * Obsidian is the default because that is what this started as, but the widget reads a folder of
 * ordinary Markdown files and there is no reason the folder has to belong to Obsidian.
 */
enum class OpenWith(val storageValue: String) {

    /** `obsidian://open?vault=…&file=…`. Needs the vault name worked out correctly. */
    Obsidian("obsidian"),

    /** Hand the file itself to whatever the user picked as their default for Markdown. */
    DefaultApp("default"),

    /** Same, but let Android ask every time. */
    AskEveryTime("ask");

    companion object {
        fun from(value: String?): OpenWith =
            entries.firstOrNull { it.storageValue == value } ?: Obsidian

        fun current(context: Context): OpenWith = from(
            context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(SetupActivity.KEY_OPEN_WITH, null)
        )
    }
}

/**
 * The intent behind tapping a note.
 *
 * For anything other than Obsidian we pass the document URI along with read and write permission.
 * Write matters: without it the other editor can show the note but not save it back, which is
 * worse than not opening it at all.
 */
internal fun noteOpenIntent(
    context: Context,
    note: NoteSummary,
    vaultName: String?,
    noteFolderPath: String?
): Intent = when (OpenWith.current(context)) {

    // CLEAR_TOP matters when Obsidian is already running: without it, some launches just bring
    // its existing window forward without delivering the new file — the app opens, but stays on
    // whatever note it already had up, which reads as "the wrong note opened".
    OpenWith.Obsidian -> Intent(
        Intent.ACTION_VIEW,
        Uri.parse(obsidianDeepLink(vaultName, noteFolderPath, note.fileName))
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    OpenWith.DefaultApp -> fileIntent(note)

    OpenWith.AskEveryTime -> Intent.createChooser(
        fileIntent(note),
        context.getString(R.string.open_note_with)
    )
}

private fun fileIntent(note: NoteSummary): Intent =
    Intent(Intent.ACTION_VIEW).apply {
        // Markdown has no universally recognised type on Android; text/markdown is what editors
        // register for, and the extension carries the rest.
        setDataAndType(Uri.parse(note.fileUri), "text/markdown")
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_ACTIVITY_NEW_TASK
        )
    }

internal fun obsidianDeepLink(
    vaultName: String?,
    noteFolderPath: String?,
    fileName: String
): String = buildString {
    append("obsidian://open?")
    if (!vaultName.isNullOrEmpty()) {
        append("vault=${Uri.encode(vaultName)}&")
    }
    append("file=${Uri.encode(vaultRelativePath(noteFolderPath, fileName))}")
}
