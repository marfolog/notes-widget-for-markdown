package com.marfolog.noteswidgetformarkdown.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppLogTest {

    @Test
    fun `strips a storage uri from a line that could reach a crash report`() {
        val line = "Notes folder granted: content://com.android.externalstorage.documents/" +
            "tree/primary%3ADocuments%2FPrivateVault"

        val redacted = AppLog.redactLocations(line)

        assertEquals("Notes folder granted: <storage location>", redacted)
        assertFalse(redacted.contains("PrivateVault"))
    }

    @Test
    fun `leaves ordinary messages untouched`() {
        val line = "Refresh finished in 12ms"

        assertEquals(line, AppLog.redactLocations(line))
    }

    @Test
    fun `strips file uris too`() {
        val redacted = AppLog.redactLocations("Could not read file:///sdcard/Notes/secret.md")

        assertFalse(redacted.contains("secret.md"))
    }
}
