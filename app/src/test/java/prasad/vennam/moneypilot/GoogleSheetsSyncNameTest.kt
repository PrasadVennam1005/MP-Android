package prasad.vennam.moneypilot

import org.junit.Assert.assertEquals
import org.junit.Test
import prasad.vennam.moneypilot.util.GoogleSheetsSyncHelper
import prasad.vennam.moneypilot.util.WorkManagerSyncScheduler

class GoogleSheetsSyncNameTest {
    @Test
    fun guarantee_uniqueWorkNamesAreAligned() {
        // Enforce that scheduling name matches UI observer name to guarantee synchronization tracking works
        assertEquals(
            "WorkManager scheduler unique name must match the name observed by the UI flow.",
            GoogleSheetsSyncHelper.SYNC_WORK_NAME,
            WorkManagerSyncScheduler.UNIQUE_WORK_NAME,
        )
    }
}
