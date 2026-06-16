package prasad.vennam.moneypilot.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import prasad.vennam.moneypilot.data.repository.FinanceRepositoryImpl
import prasad.vennam.moneypilot.domain.repository.FinanceRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindFinanceRepository(
        impl: FinanceRepositoryImpl,
    ): FinanceRepository
}
