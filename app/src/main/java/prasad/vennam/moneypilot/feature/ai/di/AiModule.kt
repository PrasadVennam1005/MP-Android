package prasad.vennam.moneypilot.feature.ai.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import prasad.vennam.moneypilot.feature.ai.data.AiRepositoryImpl
import prasad.vennam.moneypilot.feature.ai.domain.AiRepository
import prasad.vennam.moneypilot.feature.ai.service.LlmService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideLlmService(@ApplicationContext context: Context): LlmService {
        return LlmService(context)
    }

    @Provides
    @Singleton
    fun provideAiRepository(
        @ApplicationContext context: Context,
        llmService: LlmService
    ): AiRepository {
        return AiRepositoryImpl(context, llmService)
    }
}
