package com.example.core.utils

/**
 * Neutralizes user-supplied names before they reach a [java.io.File] constructor.
 *
 * Every export path in MediaProcessor builds its output as
 * `File(File(baseDir, folderName), "$name.$ext")`, where both `folderName` and `name` can
 * originate from free-text UI fields or from a WorkManager payload. Without sanitization a
 * name like `../../../shared/evil` escapes the intended directory on pre-Q devices, where
 * writes still land in public storage.
 *
 * Pure functions with no Android dependencies, so the traversal payloads are unit-testable.
 */
object SanitizationUtil {

    private val NULL_BYTE = Char.MIN_VALUE
    private const val SEPARATOR = '/'

    /** Illegal in a file name on FAT/exFAT volumes, which removable SD cards still use. */
    private val RESERVED_FILE_CHARS = charArrayOf(
        SEPARATOR, '\\', ':', '*', '?', '"', '<', '>', '|', NULL_BYTE
    )

    /**
     * Collapses [name] into a single path segment: no separator survives, so the result can
     * never redirect the write into another directory.
     *
     * @param fallback returned when nothing printable survives sanitization.
     */
    fun sanitizeFileName(name: String, fallback: String): String {
        val stripped = name.filterNot { it in RESERVED_FILE_CHARS }
        return stripped.trimPathNoise().ifBlank { fallback }
    }

    /**
     * Keeps [path] a relative multi-segment path. Forward slashes are preserved on purpose:
     * callers such as BatchColorGradingCard pass legitimate nested destinations like
     * `Movies/Graded`, so stripping separators would relocate their exports.
     *
     * Traversal is stopped by discarding `.` and `..` segments rather than by rejecting the
     * whole value, which keeps a partially malicious path usable instead of failing the export.
     * A leading separator is dropped as a side effect, so absolute paths become relative.
     *
     * @param fallback returned when no usable segment survives.
     */
    fun sanitizeFolderPath(path: String, fallback: String): String {
        val safeSegments = path
            .replace('\\', SEPARATOR)
            .split(SEPARATOR)
            .map { segment -> segment.filterNot { it in RESERVED_FILE_CHARS }.trimPathNoise() }
            .filter { segment -> segment.isNotBlank() }

        if (safeSegments.isEmpty()) return fallback
        return safeSegments.joinToString(SEPARATOR.toString())
    }

    /**
     * Strips surrounding whitespace and dots. Dots are removed at both ends because a leading
     * `..` is the traversal payload itself, while a trailing dot is silently truncated by
     * Windows and SMB targets. Interior dots survive, so `com.evil` and `clip.v2` stay intact.
     */
    private fun String.trimPathNoise(): String = trim { it.isWhitespace() || it == '.' }
}
