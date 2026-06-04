package prasad.vennam.moneypilot.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.FirebaseAnalyticsHelper
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsHelper(
        firebaseAnalyticsHelper: FirebaseAnalyticsHelper
    ): AnalyticsHelper
}
