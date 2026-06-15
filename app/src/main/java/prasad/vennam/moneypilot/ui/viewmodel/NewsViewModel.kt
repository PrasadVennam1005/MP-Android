package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.BookmarkedArticle
import prasad.vennam.moneypilot.domain.usecase.AddBookmarkUseCase
import prasad.vennam.moneypilot.domain.usecase.GetBookmarksUseCase
import prasad.vennam.moneypilot.domain.usecase.RemoveBookmarkUseCase
import javax.inject.Inject

data class NewsPortal(
    val name: String,
    val url: String,
    val description: String,
    val category: String, // "General", "Markets", "Personal Finance"
)

data class NewsUiState(
    val selectedCurrency: String = "INR",
    val portals: List<NewsPortal> = emptyList(),
    val bookmarks: List<BookmarkedArticle> = emptyList(),
)

@HiltViewModel
class NewsViewModel
    @Inject
    constructor(
        private val getBookmarksUseCase: GetBookmarksUseCase,
        private val addBookmarkUseCase: AddBookmarkUseCase,
        private val removeBookmarkUseCase: RemoveBookmarkUseCase,
        private val userPreferences: UserPreferences,
    ) : ViewModel() {
        val uiState: StateFlow<NewsUiState> =
            combine(
                userPreferences.currency,
                getBookmarksUseCase(),
            ) { currency, bookmarks ->
                NewsUiState(
                    selectedCurrency = currency,
                    portals = getPortalsForCurrency(currency),
                    bookmarks = bookmarks,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = NewsUiState(),
            )

        fun addBookmark(
            title: String,
            url: String,
            currencyCode: String,
        ) {
            viewModelScope.launch {
                addBookmarkUseCase(title, url, currencyCode)
            }
        }

        fun removeBookmarkByUrl(url: String) {
            viewModelScope.launch {
                removeBookmarkUseCase(url)
            }
        }

        private fun getPortalsForCurrency(currency: String): List<NewsPortal> =
            when (currency.uppercase()) {
                "INR" ->
                    listOf(
                        NewsPortal(
                            name = "Moneycontrol",
                            url = "https://www.moneycontrol.com",
                            description = "Indian stock market news, financial news, economy, mutual funds & analysis.",
                            category = "Markets",
                        ),
                        NewsPortal(
                            name = "Economic Times",
                            url = "https://economictimes.indiatimes.com",
                            description = "Latest business & financial news, economy, share market updates.",
                            category = "General",
                        ),
                        NewsPortal(
                            name = "Livemint",
                            url = "https://www.livemint.com",
                            description = "Stock market news, business news, personal finance, economy.",
                            category = "Personal Finance",
                        ),
                        NewsPortal(
                            name = "Business Standard",
                            url = "https://www.business-standard.com",
                            description = "Indian economy, trade, global affairs & market analysis.",
                            category = "General",
                        ),
                    )
                "USD" ->
                    listOf(
                        NewsPortal(
                            name = "Bloomberg",
                            url = "https://www.bloomberg.com",
                            description = "Global business, financial market news, economic analysis.",
                            category = "Markets",
                        ),
                        NewsPortal(
                            name = "CNBC",
                            url = "https://www.cnbc.com",
                            description = "Stock market news, business updates, earnings report, personal finance.",
                            category = "Personal Finance",
                        ),
                        NewsPortal(
                            name = "Wall Street Journal",
                            url = "https://www.wsj.com",
                            description = "Leading source of financial news, global business reporting.",
                            category = "General",
                        ),
                        NewsPortal(
                            name = "Yahoo Finance",
                            url = "https://finance.yahoo.com",
                            description = "Stock quotes, financial portfolios, market news, trends.",
                            category = "Markets",
                        ),
                    )
                "EUR", "GBP" ->
                    listOf(
                        NewsPortal(
                            name = "Reuters Business",
                            url = "https://www.reuters.com/business",
                            description = "International business, markets, macroeconomics coverage.",
                            category = "General",
                        ),
                        NewsPortal(
                            name = "Financial Times",
                            url = "https://www.ft.com",
                            description = "Global financial news, economic analysis, business policy.",
                            category = "General",
                        ),
                        NewsPortal(
                            name = "Bloomberg UK",
                            url = "https://www.bloomberg.com/europe",
                            description = "European business, markets, currency exchange news.",
                            category = "Markets",
                        ),
                        NewsPortal(
                            name = "The Economist",
                            url = "https://www.economist.com",
                            description = "Insights and analysis on international news, politics, business, finance.",
                            category = "Personal Finance",
                        ),
                    )
                else ->
                    listOf(
                        NewsPortal(
                            name = "Reuters Business",
                            url = "https://www.reuters.com/business",
                            description = "International business, markets, macroeconomics coverage.",
                            category = "General",
                        ),
                        NewsPortal(
                            name = "Yahoo Finance",
                            url = "https://finance.yahoo.com",
                            description = "Stock quotes, financial portfolios, market news, trends.",
                            category = "Markets",
                        ),
                    )
            }
    }
