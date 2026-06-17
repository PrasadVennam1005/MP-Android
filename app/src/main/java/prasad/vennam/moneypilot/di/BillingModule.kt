package prasad.vennam.moneypilot.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import prasad.vennam.moneypilot.billing.BillingManager
import prasad.vennam.moneypilot.data.UserPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {
    @Provides
    @Singleton
    fun provideBillingManager(
        @ApplicationContext context: Context,
        userPreferences: UserPreferences,
    ): BillingManager {
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        return BillingManager(context, userPreferences, applicationScope)
    }
}
