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

/** One way of opening a tapped note; [nextOpenStep] says what to try when it finds no app. */
internal enum class OpenStep { ObsidianLink, File, FileChooser }

/**
 * The first way to try. An extra note outside the vault has no path Obsidian could resolve — a deep
 * link would open whatever note Obsidian had up last. The file itself can still be opened, so do that.
 */
internal fun openStepFor(openWith: OpenWith, note: NoteSummary, noteFolderPath: String?): OpenStep = when (openWith) {
    OpenWith.Obsidian -> if (note.isExtra && noteFolderPath == null) OpenStep.File else OpenStep.ObsidianLink
    OpenWith.DefaultApp -> OpenStep.File
    OpenWith.AskEveryTime -> OpenStep.FileChooser
}

/**
 * What to try after [failed] found no app to start, or null when nothing is left. Without Obsidian
 * the file itself goes to whatever opens Markdown; Android asks when there are several. The setting
 * is never changed on the way: it says what the user wants, not what this phone could do today.
 */
internal fun nextOpenStep(failed: OpenStep): OpenStep? = when (failed) {
    OpenStep.ObsidianLink -> OpenStep.File
    OpenStep.File, OpenStep.FileChooser -> null
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
): Intent = noteOpenIntent(
    context, openStepFor(OpenWith.current(context), note, noteFolderPath), note, vaultName, noteFolderPath
)

internal fun noteOpenIntent(
    context: Context,
    step: OpenStep,
    note: NoteSummary,
    vaultName: String?,
    noteFolderPath: String?
): Intent = when (step) {

    // CLEAR_TOP matters when Obsidian is already running: without it, some launches just bring
    // its existing window forward without delivering the new file — the app opens, but stays on
    // whatever note it already had up, which reads as "the wrong note opened".
    OpenStep.ObsidianLink -> Intent(
        Intent.ACTION_VIEW,
        Uri.parse(obsidianDeepLink(vaultName, noteFolderPath, note.fileName))
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    OpenStep.File -> fileIntent(note)

    OpenStep.FileChooser -> Intent.createChooser(
        fileIntent(note),
        context.getString(R.string.open_note_with)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/** A tap on the widget goes through [TrampolineActivity], which then opens [noteOpenIntent]. */
internal fun widgetNoteOpenIntent(
    context: Context,
    note: NoteSummary,
    vaultName: String?,
    noteFolderPath: String?
): Intent = TrampolineActivity.intent(context, note, vaultName, noteFolderPath)

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

/**
 * The folder to hand to [noteOpenIntent]. Folder notes share the notes folder's path; an extra
 * note has its own, and none at all when it lives outside the vault.
 */
internal fun noteFolderPathFor(
    note: NoteSummary,
    noteFolderPath: String?,
    extraNoteFolders: Map<String, String>
): String? = if (note.isExtra) extraNoteFolders[note.fileUri] else noteFolderPath

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
