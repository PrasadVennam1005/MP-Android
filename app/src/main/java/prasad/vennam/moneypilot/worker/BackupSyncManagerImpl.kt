package prasad.vennam.moneypilot.worker

import android.content.Context
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.repository.DataManagementRepository
import prasad.vennam.moneypilot.domain.usecase.BackupSyncManager
import prasad.vennam.moneypilot.util.GoogleSheetsSyncHelper
import prasad.vennam.moneypilot.util.SyncResult

class BackupSyncManagerImpl(
    private val repository: DataManagementRepository,
    private val userPreferences: UserPreferences,
) : BackupSyncManager {
    override suspend fun performTwoWaySync(
        context: Context,
        email: String,
        spreadsheetId: String?,
        isRestore: Boolean,
        onSpreadsheetIdFound: suspend (String) -> Unit,
    ): SyncResult =
        GoogleSheetsSyncHelper.performTwoWaySync(
            context = context,
            email = email,
            repository = repository,
            userPreferences = userPreferences,
            spreadsheetId = spreadsheetId,
            isRestore = isRestore,
            onSpreadsheetIdFound = onSpreadsheetIdFound,
        )

    override suspend fun deleteSpreadsheetFile(
        context: Context,
        email: String,
        spreadsheetId: String,
    ): Boolean =
        GoogleSheetsSyncHelper.deleteSpreadsheetFile(
            context = context,
            email = email,
            spreadsheetId = spreadsheetId,
        )
}
