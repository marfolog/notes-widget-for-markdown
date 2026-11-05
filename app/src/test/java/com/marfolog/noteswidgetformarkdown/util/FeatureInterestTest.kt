package com.marfolog.noteswidgetformarkdown.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureInterestTest {

    private val now = 1_800_000_000_000L
    private val feature = UsageEvent.FEATURE_MULTI_FOLDER_WIDGETS

    private fun due(answered: Boolean = false, guideDone: Boolean = true, visits: Int = 10, shows: Int = 0) =
        FeatureInterest.isDue(answered, guideDone, visits, shows)

    @Test
    fun `the tenth visit is the first one that makes it due`() {
        assertFalse(due(visits = 1))
        assertFalse(due(visits = 3))
        assertFalse(due(visits = 9))
        assertTrue(due(visits = 10))
        assertTrue(due(visits = 25))
    }

    @Test
    fun `never before the guide is finished`() {
        assertFalse(due(guideDone = false))
    }

    @Test
    fun `never again after any answer or the close button`() {
        assertFalse(due(answered = true))
        assertFalse(due(answered = true, visits = 99))
    }

    @Test
    fun `a banner nobody reacts to is shown on three visits and then never again`() {
        assertTrue(due(shows = 0))
        assertTrue(due(shows = 1))
        assertTrue(due(shows = 2))
        assertFalse(due(shows = 3))
        assertFalse(due(shows = 8, visits = 99))
    }

    @Test
    fun `an ignored banner is reported once, after its third showing`() {
        assertFalse(FeatureInterest.isIgnored(answered = false, bannerShows = 2, alreadyReported = false))
        assertTrue(FeatureInterest.isIgnored(answered = false, bannerShows = 3, alreadyReported = false))
        assertFalse(FeatureInterest.isIgnored(answered = false, bannerShows = 3, alreadyReported = true))
    }

    @Test
    fun `an answered banner is never reported as ignored`() {
        assertFalse(FeatureInterest.isIgnored(answered = true, bannerShows = 3, alreadyReported = false))
    }

    @Test
    fun `the showings are counted per feature`() {
        val store = InMemoryKeyValueStore()
        FeatureInterest.recordBannerShown(store, feature)
        FeatureInterest.recordBannerShown(store, feature)
        assertEquals(2, store.getInt("feature_interest_banner_shows_$feature"))
        assertEquals(null, store.getInt("feature_interest_banner_shows_other"))
    }

    @Test
    fun `the line under the subfolder list needs two folders with notes and no answer yet`() {
        assertFalse(FeatureInterest.shouldShowListRow(answered = false, subfoldersWithNotes = 1, asked = true))
        assertTrue(FeatureInterest.shouldShowListRow(answered = false, subfoldersWithNotes = 2, asked = true))
        assertFalse(FeatureInterest.shouldShowListRow(answered = true, subfoldersWithNotes = 5, asked = true))
    }

    @Test
    fun `a build that cannot count the answer never shows the line`() {
        assertFalse(FeatureInterest.shouldShowListRow(answered = false, subfoldersWithNotes = 9, asked = false))
    }

    @Test
    fun `an answer anywhere ends the question everywhere`() {
        val store = InMemoryKeyValueStore()
        assertFalse(FeatureInterest.isAnswered(store, feature))
        FeatureInterest.markAnswered(store, feature)
        assertTrue(FeatureInterest.isAnswered(store, feature))
        // Judged by the same store the banner and the list read, so the list row also ends it.
        assertFalse(FeatureInterest.shouldShowListRow(FeatureInterest.isAnswered(store, feature), 4, asked = true))
    }

    @Test
    fun `another feature is asked separately`() {
        val store = InMemoryKeyValueStore()
        FeatureInterest.markAnswered(store, feature)
        assertFalse(FeatureInterest.isAnswered(store, "some_other_feature"))
    }

    @Test
    fun `the visit counter counts fresh starts and survives a bad value`() {
        val store = InMemoryKeyValueStore()
        assertEquals(0, SettingsVisits.count(store))
        assertEquals(1, SettingsVisits.record(store))
        assertEquals(2, SettingsVisits.record(store))
        assertEquals(2, SettingsVisits.count(store))
        store.putInt("settings_visits", -7)
        assertEquals(1, SettingsVisits.record(store))
    }

    @Test
    fun `the events carry a fixed feature, a fixed answer and the place`() {
        assertEquals(
            listOf("feature" to "multi_folder_widgets", "from" to "settings_banner"),
            UsageEvent.featureInterestShown(feature, UsageEvent.FROM_SETTINGS_BANNER).toList()
        )
        assertEquals(
            listOf("feature" to "multi_folder_widgets", "answer" to "yes", "from" to "subfolder_list"),
            UsageEvent.featureInterest(feature, UsageEvent.ANSWER_YES, UsageEvent.FROM_SUBFOLDER_LIST).toList()
        )
        assertEquals("feature_interest", UsageEvent.FEATURE_INTEREST)
        assertEquals("feature_interest_shown", UsageEvent.FEATURE_INTEREST_SHOWN)
        assertEquals(
            listOf("yes", "no", "dismissed", "ignored"),
            listOf(UsageEvent.ANSWER_YES, UsageEvent.ANSWER_NO, UsageEvent.ANSWER_DISMISSED, UsageEvent.ANSWER_IGNORED)
        )
    }

    @Test
    fun `quick notes and folder events use fixed words`() {
        assertEquals(listOf("kind" to "existing"), UsageEvent.fastTargetSet(UsageEvent.TARGET_KIND_EXISTING).toList())
        assertEquals(listOf("kind" to "created"), UsageEvent.fastTargetSet(UsageEvent.TARGET_KIND_CREATED).toList())
        assertEquals(listOf("kind" to "cleared"), UsageEvent.fastTargetSet(UsageEvent.TARGET_KIND_CLEARED).toList())
        assertEquals(
            listOf("result" to "in_subfolders"),
            UsageEvent.folderSelected(com.marfolog.noteswidgetformarkdown.domain.model.FolderDiagnosis.NotesInSubfolders(1, emptyList(), false)).toList()
        )
    }
}
