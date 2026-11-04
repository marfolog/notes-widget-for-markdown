package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardAppearance
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardSize
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardTextSize
import com.marfolog.noteswidgetformarkdown.util.KeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetCustomizationTest {

    private class InMemoryStore : KeyValueStore {
        val values = mutableMapOf<String, Any>()
        override fun contains(key: String) = key in values
        override fun getLong(key: String) = values[key] as Long?
        override fun putLong(key: String, value: Long) { values[key] = value }
        override fun getInt(key: String) = values[key] as Int?
        override fun putInt(key: String, value: Int) { values[key] = value }
        override fun getString(key: String) = values[key] as String?
        override fun putString(key: String, value: String) { values[key] = value }
        override fun getBoolean(key: String, default: Boolean) = values[key] as Boolean? ?: default
        override fun putBoolean(key: String, value: Boolean) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
    }

    private val store = InMemoryStore()

    @Test
    fun `add buttons stay on for everyone who never touched the switch`() {
        assertTrue(AddButtons.isShown(store))
    }

    @Test
    fun `switched off add buttons stay off`() {
        AddButtons.setShown(store, false)

        assertFalse(AddButtons.isShown(store))
        assertEquals(false, store.values["show_add_buttons"])
    }

    @Test
    fun `switched back on add buttons show again`() {
        AddButtons.setShown(store, false)
        AddButtons.setShown(store, true)

        assertTrue(AddButtons.isShown(store))
    }

    @Test
    fun `nobody has chosen the add buttons until something wrote the setting`() {
        assertNull(AddButtons.choice(store))
        assertTrue(AddButtons.isShown(store))
    }

    @Test
    fun `a choice to show them is a choice, not the default`() {
        AddButtons.setShown(store, true)

        assertEquals(true, AddButtons.choice(store))
    }

    @Test
    fun `a choice to hide them is read back`() {
        AddButtons.setShown(store, false)

        assertEquals(false, AddButtons.choice(store))
    }

    @Test
    fun `the guide, the dialog and settings all write the one setting`() {
        AddButtons.setShown(store, false)

        assertEquals(false, store.values["show_add_buttons"])
    }

    @Test
    fun `no default saved yet is the compact card the widget always had`() {
        assertEquals(NoteCardAppearance(), CardDefaults.get(store))
    }

    @Test
    fun `a saved default is read back as saved`() {
        val appearance = NoteCardAppearance(NoteCardSize.Large, NoteCardColor.Sky, NoteCardTextSize.ExtraLarge)

        CardDefaults.save(store, appearance)

        assertEquals(appearance, CardDefaults.get(store))
        assertEquals("large", store.values["default_card_size"])
        assertEquals("sky", store.values["default_card_color"])
        assertEquals("extra_large", store.values["default_card_text_size"])
    }

    @Test
    fun `a custom default colour keeps its hex`() {
        val appearance = NoteCardAppearance(color = NoteCardColor.Custom, customColorHex = "#D9E8C8")

        CardDefaults.save(store, appearance)

        assertEquals("#D9E8C8", CardDefaults.get(store).customColorHex)
    }

    @Test
    fun `no custom colour is stored blank and read as none`() {
        CardDefaults.save(store, NoteCardAppearance())

        assertEquals("", store.values["default_card_color_hex"])
        assertNull(CardDefaults.get(store).customColorHex)
    }

    @Test
    fun `values the app does not know fall back to the compact card`() {
        store.putString("default_card_size", "huge")
        store.putString("default_card_color", "neon")
        store.putString("default_card_text_size", "tiny")

        assertEquals(NoteCardAppearance(), CardDefaults.get(store))
    }
}
