package com.marfolog.noteswidgetformarkdown.data.repository

import java.io.FileNotFoundException
import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `isMissingFile` decides whether a failed delete should be reported as success (the file the
 * caller wanted gone is already gone) or as a real failure. Getting the "false" cases right
 * matters as much as the "true" ones — this must not turn a genuine problem into a fake success.
 */
class FileRepositoryImplTest {

    @Test
    fun `a bare FileNotFoundException is a missing file`() {
        assertTrue(FileNotFoundException("no such file").isMissingFile())
    }

    @Test
    fun `the real shape seen on device is recognised`() {
        // Exact wrapping DocumentsContract.deleteDocument throws when a SAF provider is asked
        // to delete a document that no longer exists — captured from a live device log.
        val wrapped = IllegalArgumentException(
            "Failed to determine if primary:Documents/mynotes/MobileQuickNote/Nová poznámka 16.md " +
                "is child of primary:Documents/mynotes/MobileQuickNote",
            FileNotFoundException(
                "Missing file for primary:Documents/mynotes/MobileQuickNote/Nová poznámka 16.md " +
                    "at /storage/emulated/0/Documents/mynotes/MobileQuickNote/Nová poznámka 16.md"
            )
        )
        assertTrue(wrapped.isMissingFile())
    }

    @Test
    fun `an unwrapped message mentioning a missing file still counts`() {
        // Some providers skip the FileNotFoundException cause and only put it in the message —
        // the fallback branch exists for exactly this.
        assertTrue(IllegalArgumentException("Missing file for some/path").isMissingFile())
    }

    @Test
    fun `a revoked permission is not a missing file`() {
        assertFalse(SecurityException("Permission Denial: reading not granted").isMissingFile())
    }

    @Test
    fun `an unrelated IllegalArgumentException is not a missing file`() {
        // Same exception type as the real case, but a different problem — must not be swallowed
        // just because the type matches.
        assertFalse(IllegalArgumentException("Invalid URI authority").isMissingFile())
    }

    @Test
    fun `a generic IOException without a FileNotFoundException cause is not a missing file`() {
        assertFalse(IOException("disk I/O error").isMissingFile())
    }

    @Test
    fun `an IllegalArgumentException wrapping an unrelated cause is not a missing file`() {
        val wrapped = IllegalArgumentException("Failed to open", IOException("disk full"))
        assertFalse(wrapped.isMissingFile())
    }
}
