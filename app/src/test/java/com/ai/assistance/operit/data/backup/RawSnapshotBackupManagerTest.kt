package com.ai.assistance.operit.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RawSnapshotBackupManagerTest {

    @Test
    fun snapshotPackageName_acceptsOnlyCurrentApplicationPackage() {
        assertTrue(
            isSupportedSnapshotPackageName(
                packageName = "com.zhixing.ai",
                expectedPackageName = "com.zhixing.ai",
            )
        )
    }

    @Test
    fun snapshotPackageName_rejectsOtherVariantsAndUpstreamPackage() {
        assertFalse(isSupportedSnapshotPackageName("com.zhixing.ai.internal", "com.zhixing.ai"))
        assertFalse(isSupportedSnapshotPackageName("com.zhixing.ai.debug", "com.zhixing.ai"))
        assertFalse(isSupportedSnapshotPackageName("com.ai.assistance.operit", "com.zhixing.ai"))
    }
}
