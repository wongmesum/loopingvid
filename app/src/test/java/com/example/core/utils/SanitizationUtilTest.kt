package com.example.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/** Guards user-supplied export paths before they reach MediaProcessor file construction. */
class SanitizationUtilTest {

    @Test
    fun `sanitizeFileName keeps an ordinary name untouched`() {
        assertEquals("My Loop 01", SanitizationUtil.sanitizeFileName("My Loop 01", FALLBACK_FILE))
    }

    @Test
    fun `sanitizeFileName strips traversal sequences and separators`() {
        assertEquals(
            "etcpasswd",
            SanitizationUtil.sanitizeFileName("../../../etc/passwd", FALLBACK_FILE)
        )
    }

    @Test
    fun `sanitizeFileName strips windows style separators`() {
        assertEquals(
            "windowssystem32",
            SanitizationUtil.sanitizeFileName("..\\..\\windows\\system32", FALLBACK_FILE)
        )
    }

    @Test
    fun `sanitizeFileName removes embedded null bytes`() {
        val nameWithNullByte = "nul" + Char.MIN_VALUE + "byte"
        assertEquals("nulbyte", SanitizationUtil.sanitizeFileName(nameWithNullByte, FALLBACK_FILE))
    }

    @Test
    fun `sanitizeFileName removes characters reserved by filesystems`() {
        assertEquals(
            "abcdefgh",
            SanitizationUtil.sanitizeFileName("a:b*c?d\"e<f>g|h", FALLBACK_FILE)
        )
    }

    @Test
    fun `sanitizeFileName trims surrounding whitespace and trailing dots`() {
        assertEquals("name", SanitizationUtil.sanitizeFileName("  name.  ", FALLBACK_FILE))
    }

    @Test
    fun `sanitizeFileName falls back when nothing survives sanitization`() {
        assertEquals(FALLBACK_FILE, SanitizationUtil.sanitizeFileName("../..", FALLBACK_FILE))
        assertEquals(FALLBACK_FILE, SanitizationUtil.sanitizeFileName("///", FALLBACK_FILE))
    }

    @Test
    fun `sanitizeFileName falls back on blank input`() {
        assertEquals(FALLBACK_FILE, SanitizationUtil.sanitizeFileName("", FALLBACK_FILE))
        assertEquals(FALLBACK_FILE, SanitizationUtil.sanitizeFileName("   ", FALLBACK_FILE))
    }

    @Test
    fun `sanitizeFolderPath preserves nested destinations already used by editor cards`() {
        assertEquals("Movies/Graded", SanitizationUtil.sanitizeFolderPath("Movies/Graded", FALLBACK_DIR))
        assertEquals("Movies/Retime", SanitizationUtil.sanitizeFolderPath("Movies/Retime", FALLBACK_DIR))
        assertEquals("Movies/Templates", SanitizationUtil.sanitizeFolderPath("Movies/Templates", FALLBACK_DIR))
        assertEquals("Movies/Transitions", SanitizationUtil.sanitizeFolderPath("Movies/Transitions", FALLBACK_DIR))
    }

    @Test
    fun `sanitizeFolderPath preserves the flat dropdown destinations`() {
        listOf("Downloads", "Movies", "Music", "Documents").forEach { destination ->
            assertEquals(destination, SanitizationUtil.sanitizeFolderPath(destination, FALLBACK_DIR))
        }
    }

    @Test
    fun `sanitizeFolderPath drops parent directory segments`() {
        assertEquals("etc", SanitizationUtil.sanitizeFolderPath("../../etc", FALLBACK_DIR))
        assertEquals("Movies/etc", SanitizationUtil.sanitizeFolderPath("Movies/../../../etc", FALLBACK_DIR))
    }

    @Test
    fun `sanitizeFolderPath makes absolute paths relative`() {
        assertEquals(
            "data/data/com.evil",
            SanitizationUtil.sanitizeFolderPath("/data/data/com.evil", FALLBACK_DIR)
        )
    }

    @Test
    fun `sanitizeFolderPath normalizes backslashes into forward separators`() {
        assertEquals(
            "Movies/Graded",
            SanitizationUtil.sanitizeFolderPath("Movies\\Graded", FALLBACK_DIR)
        )
    }

    @Test
    fun `sanitizeFolderPath collapses repeated separators`() {
        assertEquals("Movies/Graded", SanitizationUtil.sanitizeFolderPath("Movies///Graded", FALLBACK_DIR))
    }

    @Test
    fun `sanitizeFolderPath removes embedded null bytes`() {
        val pathWithNullByte = "Mov" + Char.MIN_VALUE + "ies"
        assertEquals("Movies", SanitizationUtil.sanitizeFolderPath(pathWithNullByte, FALLBACK_DIR))
    }

    @Test
    fun `sanitizeFolderPath falls back when nothing survives sanitization`() {
        assertEquals(FALLBACK_DIR, SanitizationUtil.sanitizeFolderPath("../..", FALLBACK_DIR))
        assertEquals(FALLBACK_DIR, SanitizationUtil.sanitizeFolderPath("/", FALLBACK_DIR))
        assertEquals(FALLBACK_DIR, SanitizationUtil.sanitizeFolderPath("   ", FALLBACK_DIR))
    }

    private companion object {
        const val FALLBACK_FILE = "Export_fallback"
        const val FALLBACK_DIR = "RenderOutput"
    }
}
