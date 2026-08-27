package com.ai.assistance.operit.ui.features.permission

import com.ai.assistance.operit.core.tools.system.AndroidPermissionLevel
import com.ai.assistance.operit.ui.features.permission.viewmodel.PermissionGuideViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionGuideDefaultsTest {
    @Test
    fun firstRun_recommendsAccessibilityInsteadOfRoot() {
        val initialState = PermissionGuideViewModel.UiState()

        assertEquals(
            AndroidPermissionLevel.ACCESSIBILITY,
            initialState.selectedPermissionLevel
        )
    }
}
