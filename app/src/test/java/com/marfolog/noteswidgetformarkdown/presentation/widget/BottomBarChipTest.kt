package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.presentation.widget.GitSyncLabel.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomBarChipTest {

    private val synced = GitSyncLabel.Label(R.string.sync_synced, Severity.Ok, GitSyncLabel.Age.Clock("09:41"))
    private val stale = GitSyncLabel.Label(R.string.sync_synced, Severity.Stale, GitSyncLabel.Age.Days(3))
    private val stuck = GitSyncLabel.Label(R.string.sync_stuck, Severity.Problem)
    private val noInfo = GitSyncLabel.Label(R.string.sync_no_info, Severity.Off)

    @Test
    fun `stale sync keeps the sync chip and hides the rate chip`() {
        assertEquals(BottomBarChip.Sync(stale), bottomBarChip(rateWindowOpen = true, syncLabel = stale))
    }

    @Test
    fun `broken sync keeps the sync chip and hides the rate chip`() {
        assertEquals(BottomBarChip.Sync(stuck), bottomBarChip(rateWindowOpen = true, syncLabel = stuck))
    }

    @Test
    fun `healthy sync becomes a rate chip that takes turns with the synced label`() {
        assertEquals(
            BottomBarChip.Rate(RateAlternate.SyncLabel(synced)),
            bottomBarChip(rateWindowOpen = true, syncLabel = synced)
        )
    }

    @Test
    fun `sync without information gives way to a rate chip that takes turns with the hint`() {
        assertEquals(BottomBarChip.Rate(RateAlternate.Hint), bottomBarChip(rateWindowOpen = true, syncLabel = noInfo))
    }

    @Test
    fun `an unreadable sync state never shows its label inside the rate chip`() {
        val unknown = GitSyncLabel.Label(R.string.sync_unknown, Severity.Off)
        assertEquals(BottomBarChip.Rate(RateAlternate.Hint), bottomBarChip(rateWindowOpen = true, syncLabel = unknown))
    }

    @Test
    fun `sync chip switched off leaves the corner to a rate chip that takes turns with the hint`() {
        assertEquals(BottomBarChip.Rate(RateAlternate.Hint), bottomBarChip(rateWindowOpen = true, syncLabel = null))
    }

    @Test
    fun `outside a rate window the sync chip shows as it always did`() {
        listOf(synced, stale, stuck, noInfo).forEach { label ->
            assertEquals(BottomBarChip.Sync(label), bottomBarChip(rateWindowOpen = false, syncLabel = label))
        }
    }

    @Test
    fun `outside a rate window with the sync chip off the corner stays empty`() {
        assertEquals(BottomBarChip.None, bottomBarChip(rateWindowOpen = false, syncLabel = null))
    }

    @Test
    fun `the synced label shows first and the rate chip fades in after it`() {
        assertFalse(RateAlternate.SyncLabel(synced).startsWithRate)
    }

    @Test
    fun `the rate chip shows first and the hint fades in after it`() {
        assertTrue(RateAlternate.Hint.startsWithRate)
    }

    @Test
    fun `add buttons keep the bar even without a chip`() {
        assertTrue(showsBottomBar(addButtons = true, chip = BottomBarChip.None))
    }

    @Test
    fun `without add buttons the bar stays for a sync chip`() {
        assertTrue(showsBottomBar(addButtons = false, chip = BottomBarChip.Sync(synced)))
    }

    @Test
    fun `without add buttons the bar stays for a rate chip`() {
        assertTrue(showsBottomBar(addButtons = false, chip = BottomBarChip.Rate(RateAlternate.Hint)))
    }

    @Test
    fun `without add buttons and without a chip the bar is gone`() {
        assertFalse(showsBottomBar(addButtons = false, chip = BottomBarChip.None))
    }
}
