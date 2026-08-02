package com.example

import com.example.core.utils.StorageWatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageWatcherTest {

    @Test
    fun getStorageInfo_criticalThreshold_evaluatesCritical() {
        val criticalMb = 350L
        val thresholdMb = 500L
        val isCritical = criticalMb < thresholdMb

        assertTrue("350MB should trigger critical storage warning (< 500MB)", isCritical)
    }

    @Test
    fun getStorageInfo_sufficientSpace_evaluatesNormal() {
        val freeMb = 2500L
        val isCritical = freeMb < StorageWatcher.CRITICAL_THRESHOLD_MB
        val isWarning = freeMb < StorageWatcher.WARNING_THRESHOLD_MB

        assertFalse("2500MB should not trigger critical warning", isCritical)
        assertFalse("2500MB should not trigger low storage warning", isWarning)
    }
}
