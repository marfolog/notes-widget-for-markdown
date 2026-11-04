package com.marfolog.noteswidgetformarkdown.util

import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteStyle
import com.marfolog.noteswidgetformarkdown.domain.model.WidgetColor
import com.marfolog.noteswidgetformarkdown.presentation.widget.OpenWith
import com.marfolog.noteswidgetformarkdown.presentation.widget.WidgetBackground
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageEventTest {

    @Test
    fun `widget type tells cards, list and both apart`() {
        assertEquals("cards", UsageEvent.widgetType(cardWidgets = 1, listWidgets = 0))
        assertEquals("list", UsageEvent.widgetType(cardWidgets = 0, listWidgets = 2))
        assertEquals("both", UsageEvent.widgetType(cardWidgets = 3, listWidgets = 1))
        assertEquals("none", UsageEvent.widgetType(cardWidgets = 0, listWidgets = 0))
    }

    @Test
    fun `settings opened from a known entry point keeps it`() {
        assertEquals("widget", UsageEvent.openedFrom("widget"))
        assertEquals("sync_chip", UsageEvent.openedFrom("sync_chip"))
        assertEquals("fast_note_error", UsageEvent.openedFrom("fast_note_error"))
    }

    @Test
    fun `settings opened without an extra came from the launcher`() {
        assertEquals("launcher", UsageEvent.openedFrom(null))
    }

    @Test
    fun `an unexpected extra is never sent as is`() {
        assertEquals("launcher", UsageEvent.openedFrom("Groceries.md"))
    }

    @Test
    fun `how long google's review card took goes into three buckets`() {
        assertEquals("lt1s", UsageEvent.nativeDuration(0))
        assertEquals("lt1s", UsageEvent.nativeDuration(999))
        assertEquals("1to5s", UsageEvent.nativeDuration(1_000))
        assertEquals("1to5s", UsageEvent.nativeDuration(5_000))
        assertEquals("gt5s", UsageEvent.nativeDuration(5_001))
        assertEquals("gt5s", UsageEvent.nativeDuration(120_000))
    }

    @Test
    fun `the rating events come from three places`() {
        assertEquals("after_fast_note", UsageEvent.FROM_AFTER_FAST_NOTE)
        assertEquals("after_new_note", UsageEvent.FROM_AFTER_NEW_NOTE)
        assertEquals("rate_chip", UsageEvent.FROM_RATE_CHIP)
        assertEquals("rate_native_requested", UsageEvent.RATE_NATIVE_REQUESTED)
        assertEquals("rate_native_finished", UsageEvent.RATE_NATIVE_FINISHED)
    }

    @Test
    fun `the add buttons choice is one setting told apart by where it was made`() {
        assertEquals(
            listOf("setting" to "add_buttons", "value" to "on", "from" to "onboarding"),
            UsageEvent.addButtonsChanged(shown = true, from = UsageEvent.FROM_ONBOARDING).toList()
        )
        assertEquals(
            listOf("setting" to "add_buttons", "value" to "off", "from" to "whats_new"),
            UsageEvent.addButtonsChanged(shown = false, from = UsageEvent.FROM_WHATS_NEW).toList()
        )
        assertEquals(
            listOf("setting" to "add_buttons", "value" to "off", "from" to "settings"),
            UsageEvent.addButtonsChanged(shown = false, from = UsageEvent.FROM_SETTINGS).toList()
        )
    }

    @Test
    fun `switches and flags map to fixed words`() {
        assertEquals("on", UsageEvent.onOff(true))
        assertEquals("off", UsageEvent.onOff(false))
        assertEquals("yes", UsageEvent.yesNo(true))
        assertEquals("no", UsageEvent.yesNo(false))
    }

    @Test
    fun `user properties describe the setup in fixed values`() {
        assertEquals(
            listOf(
                "widget_type" to "both",
                "sync_mode" to "git",
                "open_with" to "default",
                "has_extra_notes" to "yes",
                "fast_note_style" to "checkbox",
                "widget_alpha" to "30-70",
                "widget_color" to "graphite"
            ),
            UsageProperties.of(
                cardWidgets = 1,
                listWidgets = 1,
                syncMode = "git",
                openWith = OpenWith.DefaultApp,
                hasExtraNotes = true,
                fastNoteStyle = FastNoteStyle.Checkbox,
                widgetBackgroundPercent = 40,
                widgetColor = WidgetColor.Graphite
            )
        )
    }

    @Test
    fun `an unknown sync mode in prefs is reported as none`() {
        val properties = UsageProperties.of(0, 0, "syncthing", OpenWith.Obsidian, false, FastNoteStyle.Bullet, 100, WidgetColor.System).toMap()
        assertEquals("none", properties["sync_mode"])
        assertEquals("none", properties["widget_type"])
        assertEquals("no", properties["has_extra_notes"])
        assertEquals("100", properties["widget_alpha"])
        assertEquals("system", properties["widget_color"])
    }

    @Test
    fun `user property names and values stay within the Firebase limits`() {
        OpenWith.entries.forEach { openWith ->
            FastNoteStyle.entries.forEach { style ->
                (0..100).forEach { percent ->
                    WidgetColor.entries.forEach { color ->
                        UsageProperties.of(1, 1, "other", openWith, true, style, percent, color).forEach { (name, value) ->
                            assertTrue(name, name.length <= 24)
                            assertTrue(value, value.length <= 36)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `widget opacity is reported in four buckets with exact boundaries`() {
        assertEquals("100", UsageEvent.widgetAlpha(100))
        assertEquals("75-95", UsageEvent.widgetAlpha(95))
        assertEquals("75-95", UsageEvent.widgetAlpha(75))
        assertEquals("30-70", UsageEvent.widgetAlpha(70))
        assertEquals("30-70", UsageEvent.widgetAlpha(30))
        assertEquals("0-25", UsageEvent.widgetAlpha(25))
        assertEquals("0-25", UsageEvent.widgetAlpha(0))
    }

    @Test
    fun `widget opacity between steps or out of range lands in the bucket of its step`() {
        assertEquals("100", UsageEvent.widgetAlpha(98))
        assertEquals("75-95", UsageEvent.widgetAlpha(73))
        assertEquals("30-70", UsageEvent.widgetAlpha(72))
        assertEquals("30-70", UsageEvent.widgetAlpha(28))
        assertEquals("0-25", UsageEvent.widgetAlpha(27))
        assertEquals("100", UsageEvent.widgetAlpha(400))
        assertEquals("0-25", UsageEvent.widgetAlpha(-1))
    }

    @Test
    fun `every slider stop is reported as one of the four buckets`() {
        val buckets = (0..100 step WidgetBackground.STEP_PERCENT).map(UsageEvent::widgetAlpha).toSet()
        assertEquals(setOf("100", "75-95", "30-70", "0-25"), buckets)
    }

    @Test
    fun `event and parameter names stay within the Firebase limit`() {
        val names = UsageEvent::class.java.declaredFields
            .filter { it.type == String::class.java && java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.isAccessible = true; it.get(null) as String }
        assertTrue(names.isNotEmpty())
        names.forEach { name ->
            assertTrue(name, name.length <= 40)
            assertTrue(name, name.matches(Regex("[a-z][a-z0-9_]*")))
        }
    }

    @Test
    fun `a sent feedback counts the choice and the thumb`() {
        val form = FeedbackForm(
            thumbs = FeedbackThumbs.Down,
            text = "Search my Groceries list"
        )

        assertEquals(
            listOf(
                "feedback_dialog_choice" to ("choice" to "sent"),
                "feedback_thumbs" to ("value" to "down")
            ),
            UsageEvent.feedbackSent(form)
        )
    }

    @Test
    fun `a text only feedback is counted as sent and nothing else`() {
        assertEquals(
            listOf("feedback_dialog_choice" to ("choice" to "sent")),
            UsageEvent.feedbackSent(FeedbackForm(text = "Thanks"))
        )
    }
}
