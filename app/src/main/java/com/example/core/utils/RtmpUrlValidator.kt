package com.example.core.utils

/**
 * Utility for validating RTMP connection strings and stream keys
 * prior to initiating live streaming broadcasts.
 */
object RtmpUrlValidator {

    private val RTMP_URL_REGEX = Regex(
        pattern = "^rtmps?://[a-zA-Z0-9.-]+(:[0-9]{1,5})?(/.*)?$",
        option = RegexOption.IGNORE_CASE
    )

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val warningMessage: String? = null
    )

    /**
     * Validates an RTMP / RTMPS connection server URL.
     */
    fun validateRtmpUrl(url: String?): ValidationResult {
        if (url.isNullOrBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = "RTMP Server URL cannot be empty"
            )
        }

        val trimmedUrl = url.trim()

        if (trimmedUrl.startsWith("http://", ignoreCase = true) || trimmedUrl.startsWith("https://", ignoreCase = true)) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Invalid scheme: Must start with rtmp:// or rtmps://"
            )
        }

        if (!trimmedUrl.startsWith("rtmp://", ignoreCase = true) && !trimmedUrl.startsWith("rtmps://", ignoreCase = true)) {
            return ValidationResult(
                isValid = false,
                errorMessage = "URL must start with rtmp:// or rtmps://"
            )
        }

        if (!RTMP_URL_REGEX.matches(trimmedUrl)) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Invalid RTMP server hostname or format (e.g. rtmp://a.rtmp.youtube.com/live2)"
            )
        }

        val warning = when {
            trimmedUrl.startsWith("rtmp://", ignoreCase = true) -> "Unencrypted RTMP stream. Consider using RTMPS if supported by platform."
            else -> null
        }

        return ValidationResult(
            isValid = true,
            warningMessage = warning
        )
    }

    /**
     * Validates an RTMP stream key.
     */
    fun validateStreamKey(key: String?): ValidationResult {
        if (key.isNullOrBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Stream Key cannot be empty"
            )
        }

        val trimmedKey = key.trim()

        if (trimmedKey.contains(" ")) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Stream Key must not contain spaces"
            )
        }

        if (trimmedKey.length < 4) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Stream Key is too short (minimum 4 characters)"
            )
        }

        return ValidationResult(isValid = true)
    }

    /**
     * Validates both RTMP Server URL and Stream Key configuration.
     */
    fun validateConfiguration(rtmpUrl: String?, streamKey: String?): ValidationResult {
        val urlResult = validateRtmpUrl(rtmpUrl)
        if (!urlResult.isValid) return urlResult

        val keyResult = validateStreamKey(streamKey)
        if (!keyResult.isValid) return keyResult

        return ValidationResult(
            isValid = true,
            warningMessage = urlResult.warningMessage
        )
    }

    /**
     * Returns standard default RTMP URLs based on selected platform name.
     */
    fun getDefaultRtmpUrlForPlatform(platformName: String): String {
        return when (platformName.uppercase()) {
            "YOUTUBE" -> "rtmp://a.rtmp.youtube.com/live2"
            "TIKTOK" -> "rtmp://live-push.tiktok.com/live/"
            "FACEBOOK" -> "rtmps://live-api-s.facebook.com:443/rtmp/"
            "TWITCH" -> "rtmp://live.twitch.tv/app/"
            else -> "rtmp://a.rtmp.youtube.com/live2"
        }
    }
}
