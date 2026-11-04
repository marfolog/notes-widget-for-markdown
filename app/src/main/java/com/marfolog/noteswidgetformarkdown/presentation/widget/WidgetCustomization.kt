package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardAppearance
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardSize
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardTextSize
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.KeyValueStore
import com.marfolog.noteswidgetformarkdown.util.SharedPreferencesStore

/** The add and fast note buttons in the widget's bottom bar. On unless switched off. */
internal object AddButtons {

    fun isShown(context: Context): Boolean = isShown(store(context))

    /**
     * What someone chose, in settings, the first-launch guide or the "What's new" dialog; null until
     * they did, and the buttons are shown until then. The one place that tells a choice from a default.
     */
    fun choice(context: Context): Boolean? = choice(store(context))

    internal fun choice(store: KeyValueStore): Boolean? =
        if (store.contains(SetupActivity.KEY_SHOW_ADD_BUTTONS)) isShown(store) else null

    internal fun isShown(store: KeyValueStore): Boolean =
        store.getBoolean(SetupActivity.KEY_SHOW_ADD_BUTTONS, true)

    fun setShown(context: Context, shown: Boolean) = setShown(store(context), shown)

    internal fun setShown(store: KeyValueStore, shown: Boolean) =
        store.putBoolean(SetupActivity.KEY_SHOW_ADD_BUTTONS, shown)
}

/**
 * The look of every card nobody set up on its own. Unset, it is the compact card the widget has
 * always had, so an update changes nothing on anyone's home screen.
 */
internal object CardDefaults {

    fun get(context: Context): NoteCardAppearance = get(store(context))

    internal fun get(store: KeyValueStore): NoteCardAppearance = NoteCardAppearance(
        size = NoteCardSize.fromStorage(store.getString(SetupActivity.KEY_DEFAULT_CARD_SIZE)),
        color = NoteCardColor.fromStorage(store.getString(SetupActivity.KEY_DEFAULT_CARD_COLOR)),
        textSize = NoteCardTextSize.fromStorage(store.getString(SetupActivity.KEY_DEFAULT_CARD_TEXT_SIZE)),
        customColorHex = store.getString(SetupActivity.KEY_DEFAULT_CARD_COLOR_HEX)?.takeIf { it.isNotBlank() }
    )

    fun save(context: Context, appearance: NoteCardAppearance) = save(store(context), appearance)

    internal fun save(store: KeyValueStore, appearance: NoteCardAppearance) {
        store.putString(SetupActivity.KEY_DEFAULT_CARD_SIZE, appearance.size.storageValue)
        store.putString(SetupActivity.KEY_DEFAULT_CARD_COLOR, appearance.color.storageValue)
        store.putString(SetupActivity.KEY_DEFAULT_CARD_TEXT_SIZE, appearance.textSize.storageValue)
        store.putString(SetupActivity.KEY_DEFAULT_CARD_COLOR_HEX, appearance.customColorHex.orEmpty())
    }
}

private fun store(context: Context): KeyValueStore = SharedPreferencesStore(
    context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
)
