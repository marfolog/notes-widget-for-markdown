package com.marfolog.noteswidgetformarkdown.data.preferences

import android.content.Context
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardAppearance
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardSize
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardTextSize
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import org.json.JSONObject

class NoteCardSettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): Map<String, NoteCardAppearance> {
        val raw = prefs.getString(KEY_CARD_SETTINGS_JSON, null) ?: return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            val result = mutableMapOf<String, NoteCardAppearance>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val noteId = keys.next()
                val item = root.optJSONObject(noteId) ?: continue
                result[noteId] = NoteCardAppearance(
                    size = NoteCardSize.fromStorage(item.optString(KEY_SIZE)),
                    color = NoteCardColor.fromStorage(item.optString(KEY_COLOR)),
                    textSize = NoteCardTextSize.fromStorage(item.optString(KEY_TEXT_SIZE)),
                    customColorHex = item.optString(KEY_CUSTOM_COLOR_HEX).takeIf { it.isNotBlank() }
                )
            }
            result
        }.getOrDefault(emptyMap())
    }

    fun get(noteId: String): NoteCardAppearance {
        return getAll()[noteId] ?: NoteCardAppearance()
    }

    fun save(noteId: String, appearance: NoteCardAppearance) {
        val root = JSONObject(prefs.getString(KEY_CARD_SETTINGS_JSON, "{}") ?: "{}")
        val item = JSONObject()
            .put(KEY_SIZE, appearance.size.storageValue)
            .put(KEY_COLOR, appearance.color.storageValue)
            .put(KEY_TEXT_SIZE, appearance.textSize.storageValue)
            .put(KEY_CUSTOM_COLOR_HEX, appearance.customColorHex.orEmpty())
        root.put(noteId, item)
        prefs.edit()
            .putString(KEY_CARD_SETTINGS_JSON, root.toString())
            .apply()
    }

    companion object {
        const val KEY_CARD_SETTINGS_JSON = "note_card_settings_json"
        private const val KEY_SIZE = "size"
        private const val KEY_COLOR = "color"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_CUSTOM_COLOR_HEX = "custom_color_hex"
    }
}
