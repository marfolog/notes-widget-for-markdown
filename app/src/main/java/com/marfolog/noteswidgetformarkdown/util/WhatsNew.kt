package com.marfolog.noteswidgetformarkdown.util

import androidx.annotation.StringRes
import com.marfolog.noteswidgetformarkdown.R

enum class WhatsNewIcon { Palette, Checklist, Chat, Translate, Folder, Sync }

/** The pages of the dialog, in order: the question some releases open with, then the news. */
enum class WhatsNewPage(val eventValue: String) {
    ButtonsChoice(UsageEvent.WHATS_NEW_STEP_BUTTONS),
    News(UsageEvent.WHATS_NEW_STEP_NEWS)
}

data class WhatsNewItem(val icon: WhatsNewIcon, @StringRes val textRes: Int)

/**
 * The one-time "What's new" dialog after an update: what it lists for each release, and when
 * settings show it. A release without an entry here shows nothing, so a release adds its lines
 * to [RELEASES] and nothing else.
 */
object WhatsNew {

    internal val RELEASES: Map<String, List<WhatsNewItem>> = mapOf(
        "1.5.0" to listOf(
            WhatsNewItem(WhatsNewIcon.Folder, R.string.whats_new_150_folder),
            WhatsNewItem(WhatsNewIcon.Checklist, R.string.whats_new_150_fast_target),
            WhatsNewItem(WhatsNewIcon.Sync, R.string.whats_new_150_git)
        ),
        "1.4.0" to listOf(
            WhatsNewItem(WhatsNewIcon.Palette, R.string.whats_new_140_card_default)
        ),
        "1.3.0" to listOf(
            WhatsNewItem(WhatsNewIcon.Palette, R.string.whats_new_130_background),
            WhatsNewItem(WhatsNewIcon.Checklist, R.string.whats_new_130_fast_note),
            WhatsNewItem(WhatsNewIcon.Chat, R.string.whats_new_130_feedback),
            WhatsNewItem(WhatsNewIcon.Translate, R.string.whats_new_130_languages)
        )
    )

    /**
     * The release lines whose dialog opens by asking about the add buttons. 1.5 asks too, for
     * whoever skipped 1.4 and never chose; whoever chose already is not asked again.
     */
    private val BUTTONS_CHOICE_LINES = setOf("1.4", "1.5")

    /**
     * Whoever never set the add buttons, in settings or anywhere else, has to answer before the news
     * of 1.4 or 1.5: the buttons used to be fixed, and hiding them is something to decide, not to discover.
     */
    internal fun needsButtonsChoice(versionName: String, buttonsChoice: Boolean?): Boolean =
        line(versionName) in BUTTONS_CHOICE_LINES && buttonsChoice == null

    internal fun firstPage(versionName: String, buttonsChoice: Boolean?): WhatsNewPage =
        if (needsButtonsChoice(versionName, buttonsChoice)) WhatsNewPage.ButtonsChoice else WhatsNewPage.News

    /** "1.3.0-foss" and "1.3.0" are the same release. */
    fun release(versionName: String): String = versionName.substringBefore('-').trim()

    /** "1.3.1" and "1.3.0" share one dialog: a patch release brings no news of its own. */
    internal fun line(versionName: String): String = release(versionName).split('.').take(2).joinToString(".")

    fun itemsFor(versionName: String, releases: Map<String, List<WhatsNewItem>> = RELEASES): List<WhatsNewItem> =
        releases[release(versionName)]
            ?: releases.entries.firstOrNull { line(it.key) == line(versionName) }?.value
            ?: emptyList()

    /**
     * Whether this start brought a new release to someone who already had the app. With no
     * version recorded yet, data from before is what tells an update from a fresh install.
     */
    internal fun arrivedByUpdate(lastVersionSeen: String?, versionName: String, installedBefore: Boolean): Boolean =
        if (lastVersionSeen == null) installedBefore else line(lastVersionSeen) != line(versionName)

    /**
     * Once per release, for someone who updated to it, set up and not sent here to fix a failed
     * fast note. Fresh installs never see it: everything in it is new to them anyway.
     */
    internal fun shouldShow(
        versionName: String,
        updatedToVersion: String?,
        seenVersion: String?,
        openedFrom: String?,
        remoteEnabled: Boolean,
        setupComplete: Boolean,
        releases: Map<String, List<WhatsNewItem>> = RELEASES
    ): Boolean {
        if (!setupComplete || !remoteEnabled || openedFrom == UsageEvent.FROM_FAST_NOTE_ERROR) return false
        val current = line(versionName)
        if (itemsFor(versionName, releases).isEmpty()) return false
        if (updatedToVersion == null || line(updatedToVersion) != current) return false
        return seenVersion == null || line(seenVersion) != current
    }
}
