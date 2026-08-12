package com.example.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the MediaStore MIME labelling. Publishing a WAV or M4A as "audio/mpeg" makes the
 * gallery advertise a codec the file does not contain, which breaks playback in some players.
 */
class MediaMimeTypesTest {

    @Test
    fun `resolveAudioMimeType maps each supported export format to its real container type`() {
        assertEquals("audio/mpeg", MediaMimeTypes.resolveAudioMimeType("mp3"))
        assertEquals("audio/wav", MediaMimeTypes.resolveAudioMimeType("wav"))
        assertEquals("audio/mp4", MediaMimeTypes.resolveAudioMimeType("m4a"))
        assertEquals("audio/aac", MediaMimeTypes.resolveAudioMimeType("aac"))
        assertEquals("audio/flac", MediaMimeTypes.resolveAudioMimeType("flac"))
        assertEquals("audio/ogg", MediaMimeTypes.resolveAudioMimeType("ogg"))
    }

    @Test
    fun `resolveAudioMimeType ignores extension casing and leading dot`() {
        assertEquals("audio/wav", MediaMimeTypes.resolveAudioMimeType("WAV"))
        assertEquals("audio/mp4", MediaMimeTypes.resolveAudioMimeType(".M4a"))
    }

    @Test
    fun `resolveAudioMimeType falls back to mpeg for unknown or blank extensions`() {
        assertEquals("audio/mpeg", MediaMimeTypes.resolveAudioMimeType(""))
        assertEquals("audio/mpeg", MediaMimeTypes.resolveAudioMimeType("   "))
        assertEquals("audio/mpeg", MediaMimeTypes.resolveAudioMimeType("xyz"))
    }

    @Test
    fun `resolveVideoMimeType maps supported containers and falls back to mp4`() {
        assertEquals("video/mp4", MediaMimeTypes.resolveVideoMimeType("mp4"))
        assertEquals("video/x-matroska", MediaMimeTypes.resolveVideoMimeType("mkv"))
        assertEquals("video/quicktime", MediaMimeTypes.resolveVideoMimeType("mov"))
        assertEquals("video/webm", MediaMimeTypes.resolveVideoMimeType("webm"))
        assertEquals("video/mp4", MediaMimeTypes.resolveVideoMimeType("unknown"))
    }

    @Test
    fun `resolveAudioMimeTypeForFile derives the type from the file name extension`() {
        assertEquals("audio/wav", MediaMimeTypes.resolveAudioMimeTypeForFile("mastered_track.wav"))
        assertEquals("audio/mp4", MediaMimeTypes.resolveAudioMimeTypeForFile("/cache/mastering/out.m4a"))
        assertEquals("audio/mpeg", MediaMimeTypes.resolveAudioMimeTypeForFile("no_extension"))
    }
}
