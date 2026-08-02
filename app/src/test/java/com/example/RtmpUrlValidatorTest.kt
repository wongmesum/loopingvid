package com.example

import com.example.core.utils.RtmpUrlValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RtmpUrlValidatorTest {

    @Test
    fun validRtmpUrl_returnsSuccess() {
        val youtubeUrl = "rtmp://a.rtmp.youtube.com/live2"
        val result = RtmpUrlValidator.validateRtmpUrl(youtubeUrl)
        assertTrue(result.isValid)
    }

    @Test
    fun validRtmpsUrl_returnsSuccess() {
        val facebookUrl = "rtmps://live-api-s.facebook.com:443/rtmp/"
        val result = RtmpUrlValidator.validateRtmpUrl(facebookUrl)
        assertTrue(result.isValid)
    }

    @Test
    fun invalidSchemeHttp_returnsError() {
        val httpUrl = "http://a.rtmp.youtube.com/live2"
        val result = RtmpUrlValidator.validateRtmpUrl(httpUrl)
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun emptyUrl_returnsError() {
        val result = RtmpUrlValidator.validateRtmpUrl("")
        assertFalse(result.isValid)
    }

    @Test
    fun validStreamKey_returnsSuccess() {
        val key = "live_123456789_abcdef"
        val result = RtmpUrlValidator.validateStreamKey(key)
        assertTrue(result.isValid)
    }

    @Test
    fun streamKeyWithSpaces_returnsError() {
        val key = "live 123 456"
        val result = RtmpUrlValidator.validateStreamKey(key)
        assertFalse(result.isValid)
    }

    @Test
    fun shortStreamKey_returnsError() {
        val key = "12"
        val result = RtmpUrlValidator.validateStreamKey(key)
        assertFalse(result.isValid)
    }

    @Test
    fun fullConfigurationValidation_works() {
        val validResult = RtmpUrlValidator.validateConfiguration(
            rtmpUrl = "rtmps://live-push.tiktok.com/live/",
            streamKey = "stream_key_test_123"
        )
        assertTrue(validResult.isValid)

        val invalidResult = RtmpUrlValidator.validateConfiguration(
            rtmpUrl = "invalid_protocol",
            streamKey = "stream_key_test_123"
        )
        assertFalse(invalidResult.isValid)
    }
}
