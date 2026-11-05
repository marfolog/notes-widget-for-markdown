package com.marfolog.noteswidgetformarkdown.data.repository

import com.marfolog.noteswidgetformarkdown.domain.model.FolderDiagnosis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderDiagnosisCacheTest {

    private var now = 0L
    private val cache = FolderDiagnosisCache(ttlMillis = 30_000) { now }

    @Test
    fun `a diagnosis is remembered per folder`() {
        cache.put("a", FolderDiagnosis.EmptyFolder)
        assertEquals(FolderDiagnosis.EmptyFolder, cache.get("a"))
        assertNull(cache.get("b"))
    }

    @Test
    fun `it is forgotten after the time to live`() {
        cache.put("a", FolderDiagnosis.EmptyFolder)
        now = 30_000
        assertEquals(FolderDiagnosis.EmptyFolder, cache.get("a"))
        now = 30_001
        assertNull(cache.get("a"))
    }

    @Test
    fun `forgetting one folder leaves the others`() {
        cache.put("a", FolderDiagnosis.EmptyFolder)
        cache.put("b", FolderDiagnosis.NoMarkdown(1))
        cache.forget("a")
        assertNull(cache.get("a"))
        assertEquals(FolderDiagnosis.NoMarkdown(1), cache.get("b"))
    }

    @Test
    fun `many folders never grow it without bound`() {
        repeat(100) { cache.put("folder $it", FolderDiagnosis.EmptyFolder) }
        assertEquals(FolderDiagnosis.EmptyFolder, cache.get("folder 99"))
        assertNull(cache.get("folder 0"))
    }
}
