package prasad.vennam.moneypilot.domain.usecase

import android.content.Context
import prasad.vennam.moneypilot.util.SyncResult

interface BackupSyncManager {
    suspend fun performTwoWaySync(
        context: Context,
        email: String,
        spreadsheetId: String?,
        isRestore: Boolean,
        onSpreadsheetIdFound: suspend (String) -> Unit,
    ): SyncResult

    suspend fun deleteSpreadsheetFile(
        context: Context,
        email: String,
        spreadsheetId: String,
    ): Boolean
}
