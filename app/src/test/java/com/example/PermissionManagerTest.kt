package com.example

import com.example.core.ui.AppPermissionGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionManagerTest {

    @Test
    fun appPermissionGroup_containsAllRequiredGroups() {
        val groups = AppPermissionGroup.entries
        assertEquals(4, groups.size)
        assertTrue(groups.contains(AppPermissionGroup.CAMERA))
        assertTrue(groups.contains(AppPermissionGroup.MICROPHONE))
        assertTrue(groups.contains(AppPermissionGroup.FOREGROUND_SERVICE))
        assertTrue(groups.contains(AppPermissionGroup.STORAGE))
    }

    @Test
    fun appPermissionGroup_returnsNonEmptyPermissionsArray() {
        AppPermissionGroup.entries.forEach { group ->
            val permissions = group.getPermissions()
            assertNotNull("Permissions array should not be null", permissions)
            assertTrue("Permissions array for ${group.name} should not be empty", permissions.isNotEmpty())
        }
    }

    @Test
    fun cameraAndMicrophone_returnExpectedAndroidPermissions() {
        val cameraPermissions = AppPermissionGroup.CAMERA.getPermissions()
        assertEquals(1, cameraPermissions.size)
        assertEquals("android.permission.CAMERA", cameraPermissions[0])

        val micPermissions = AppPermissionGroup.MICROPHONE.getPermissions()
        assertEquals(1, micPermissions.size)
        assertEquals("android.permission.RECORD_AUDIO", micPermissions[0])
    }
}
