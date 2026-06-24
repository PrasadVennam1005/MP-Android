package prasad.vennam.moneypilot.ui.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.ads.InterstitialAdManager
import prasad.vennam.moneypilot.data.AppLinks
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.feature.ai.presentation.AiChatScreen
import prasad.vennam.moneypilot.ui.ai.InsightsScreen
import prasad.vennam.moneypilot.ui.budget.ReportsTabScreen
import prasad.vennam.moneypilot.ui.categories.CategoryListScreen
import prasad.vennam.moneypilot.ui.currency.CurrencyConverterScreen
import prasad.vennam.moneypilot.ui.dashboard.DashboardScreen
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.emergencyfund.EmergencyFundScreen
import prasad.vennam.moneypilot.ui.faq.FaqScreen
import prasad.vennam.moneypilot.ui.investments.InvestmentScreen
import prasad.vennam.moneypilot.ui.learnfinance.ArticleDetailScreen
import prasad.vennam.moneypilot.ui.learnfinance.LearnFinanceScreen
import prasad.vennam.moneypilot.ui.loans.EmiCalculatorScreen
import prasad.vennam.moneypilot.ui.loans.LoanScreen
import prasad.vennam.moneypilot.ui.login.AuthScreen
import prasad.vennam.moneypilot.ui.news.NewsScreen
import prasad.vennam.moneypilot.ui.news.NewsWebViewScreen
import prasad.vennam.moneypilot.ui.notifications.NotificationsScreen
import prasad.vennam.moneypilot.ui.premium.PremiumScreen
import prasad.vennam.moneypilot.ui.sandbox.FinancialSandboxScreen
import prasad.vennam.moneypilot.ui.scanner.ReceiptScannerScreen
import prasad.vennam.moneypilot.ui.settings.SettingsScreen
import prasad.vennam.moneypilot.ui.transactions.AddEditTransactionScreen
import prasad.vennam.moneypilot.ui.transactions.HistoryScreen
import prasad.vennam.moneypilot.ui.viewmodel.AnalyticsViewModel
import prasad.vennam.moneypilot.ui.viewmodel.BudgetViewModel
import prasad.vennam.moneypilot.ui.viewmodel.InvestmentViewModel
import prasad.vennam.moneypilot.ui.viewmodel.LearnFinanceViewModel
import prasad.vennam.moneypilot.ui.viewmodel.MainViewModel
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.util.AnalyticsHelper

fun moneyPilotNavEntry(
    key: NavKey,
    backStack: NavBackStack<NavKey>,
    analyticsHelper: AnalyticsHelper,
    mainViewModel: MainViewModel,
    transactionViewModel: TransactionViewModel,
    budgetViewModel: BudgetViewModel,
    investmentViewModel: InvestmentViewModel,
    analyticsViewModel: AnalyticsViewModel,
    learnFinanceViewModel: LearnFinanceViewModel,
    userPreferences: UserPreferences,
    isPremium: Boolean,
    userData: UserPreferences.UserData?,
    syncState: SyncState?,
    interstitialAdManager: InterstitialAdManager,
    onBack: () -> Unit
): NavEntry<NavKey> {
    return when (key) {
        is Destination.Auth ->
            NavEntry(key) {
                AuthScreen(
                    mainViewModel = mainViewModel,
                    analyticsHelper = analyticsHelper,
                    skipSplash = key.skipSplash,
                    onNavigateToTerms = {
                        backStack.add(Destination.TermsOfService)
                    },
                    onNavigateToPrivacy = {
                        backStack.add(Destination.PrivacyPolicy)
                    },
                    onAuthSuccess = {
                        backStack.clear()
                        backStack.add(Destination.Dashboard)
                    },
                )
            }

        is Destination.Dashboard ->
            NavEntry(key) {
                val context = LocalContext.current
                DashboardScreen(
                    mainViewModel = mainViewModel,
                    onNavigateToAddTransaction = { type ->
                        backStack.add(Destination.AddEditTransaction(initialType = type))
                    },
                    onNavigateToAddInvestment = {
                        backStack.add(Destination.Investments)
                    },
                    onNavigateToHistory = {
                        backStack.add(Destination.History)
                    },
                    onNavigateToBudgets = {
                        backStack.add(Destination.Reports)
                    },
                    onNavigateToSettings = {
                        backStack.add(Destination.Settings)
                    },
                    onNavigateToScanner = {
                        backStack.add(Destination.ReceiptScanner)
                    },
                    onNavigateToNotifications = {
                        backStack.add(Destination.Notifications)
                    },
                    onNavigateToLoans = {
                        backStack.add(Destination.Loans())
                    },
                    onNavigateToInsights = {
                        backStack.add(Destination.Insights)
                    },
                    onNavigateToAiChat = {
                        backStack.add(Destination.AiChat)
                    },
                    onNavigateToEmergencyFund = {
                        backStack.add(Destination.EmergencyFund)
                    },
                    onNavigateToNews = {
                        backStack.add(Destination.FinancialNews)
                    },
                    onNavigateToSandbox = {
                        backStack.add(Destination.FinancialSandbox)
                    },
                    onNavigateToEmiCalculator = {
                        if (isPremium) {
                            backStack.add(Destination.EmiCalculator)
                        } else {
                            (context as? Activity)?.let { activity ->
                                interstitialAdManager.showAd(activity) {
                                    backStack.add(Destination.EmiCalculator)
                                }
                            } ?: backStack.add(Destination.EmiCalculator)
                        }
                    },
                    onNavigateToLearnFinance = {
                        backStack.add(Destination.LearnFinance)
                    },
                    onNavigateToCurrencyConverter = {
                        backStack.add(Destination.CurrencyConverter)
                    },
                    analyticsHelper = analyticsHelper,
                )
            }

        is Destination.Insights ->
            NavEntry(key) {
                InsightsScreen(
                    userData = userData,
                    syncState = syncState,
                    analyticsHelper = analyticsHelper,
                    onProfileClick = { backStack.add(Destination.Settings) },
                    onBackClick = onBack,
                    onNavigateToAiChat = { backStack.add(Destination.AiChat) },
                )
            }

        is Destination.AiChat ->
            NavEntry(key) {
                AiChatScreen(
                    analyticsHelper = analyticsHelper,
                    onBackClick = onBack,
                )
            }

        is Destination.History ->
            NavEntry(key) {
                HistoryScreen(
                    viewModel = transactionViewModel,
                    analyticsHelper = analyticsHelper,
                    onAddTransaction = { type ->
                        backStack.add(Destination.AddEditTransaction(initialType = type))
                    },
                    onEditTransaction = { id, type ->
                        backStack.add(Destination.AddEditTransaction(id, type))
                    },
                    userData = userData,
                    syncState = syncState,
                    isPremium = isPremium,
                    onProfileClick = { backStack.add(Destination.Settings) },
                    fixedType = null,
                )
            }

        is Destination.AddEditTransaction ->
            NavEntry(key) {
                AddEditTransactionScreen(
                    transactionId = key.transactionId,
                    initialType = key.initialType,
                    viewModel = transactionViewModel,
                    analyticsHelper = analyticsHelper,
                    interstitialAdManager = interstitialAdManager,
                    isPremium = isPremium,
                    onNavigateBack = onBack,
                )
            }

        is Destination.Investments ->
            NavEntry(key) {
                InvestmentScreen(
                    viewModel = investmentViewModel,
                    userData = userData,
                    syncState = syncState,
                    isPremium = isPremium,
                    onProfileClick = { backStack.add(Destination.Settings) },
                    analyticsHelper = analyticsHelper,
                )
            }

        is Destination.Loans ->
            NavEntry(key) {
                val context = LocalContext.current
                LoanScreen(
                    userData = userData,
                    syncState = syncState,
                    isPremium = isPremium,
                    onProfileClick = { backStack.add(Destination.Settings) },
                    onNavigateToEmiCalculator = {
                        if (isPremium) {
                            backStack.add(Destination.EmiCalculator)
                        } else {
                            (context as? Activity)?.let { activity ->
                                interstitialAdManager.showAd(activity) {
                                    backStack.add(Destination.EmiCalculator)
                                }
                            } ?: backStack.add(Destination.EmiCalculator)
                        }
                    },
                    prefillAmount = key.prefillAmount,
                    prefillRate = key.prefillRate,
                    prefillTenureMonths = key.prefillTenureMonths,
                    prefillEmi = key.prefillEmi,
                    analyticsHelper = analyticsHelper
                )
            }

        is Destination.Reports ->
            NavEntry(key) {
                ReportsTabScreen(
                    budgetViewModel = budgetViewModel,
                    transactionViewModel = transactionViewModel,
                    analyticsViewModel = analyticsViewModel,
                    userData = userData,
                    syncState = syncState,
                    isPremium = isPremium,
                    onProfileClick = { backStack.add(Destination.Settings) },
                    analyticsHelper = analyticsHelper
                )
            }

        is Destination.Settings ->
            NavEntry(key) {
                SettingsScreen(
                    transactionViewModel = transactionViewModel,
                    mainViewModel = mainViewModel,
                    analyticsHelper = analyticsHelper,
                    onLogout = {
                        backStack.clear()
                        backStack.add(Destination.Auth(skipSplash = true))
                    },
                    onNavigateToCategories = {
                        backStack.add(Destination.ManageCategories)
                    },
                    onNavigateToNotifications = {
                        backStack.add(Destination.Notifications)
                    },
                    onNavigateToFAQ = {
                        backStack.add(Destination.FAQ)
                    },
                    onNavigateToTerms = {
                        backStack.add(Destination.TermsOfService)
                    },
                    onNavigateToPrivacy = {
                        backStack.add(Destination.PrivacyPolicy)
                    },
                    onNavigateToPremium = {
                        backStack.add(Destination.PremiumScreen)
                    },
                    onAccountDeleted = {
                        backStack.clear()
                        backStack.add(Destination.Auth(skipSplash = false))
                    },
                )
            }

        is Destination.ManageCategories ->
            NavEntry(key) {
                CategoryListScreen(
                    viewModel = transactionViewModel,
                    analyticsHelper = analyticsHelper,
                    onNavigateBack = onBack,
                )
            }

        is Destination.ReceiptScanner ->
            NavEntry(key) {
                ReceiptScannerScreen(
                    onNavigateBack = onBack,
                    transactionViewModel = transactionViewModel,
                    analyticsHelper = analyticsHelper,
                )
            }

        is Destination.Notifications ->
            NavEntry(key) {
                NotificationsScreen(
                    onNavigateBack = onBack,
                    onNavigateToWeb = { url, title ->
                        backStack.add(Destination.NewsWebFrame(url = url, title = title))
                    },
                    analyticsHelper = analyticsHelper,
                )
            }

        is Destination.FAQ ->
            NavEntry(key) {
                FaqScreen(
                    analyticsHelper = analyticsHelper,
                    onNavigateBack = onBack,
                )
            }

        is Destination.EmergencyFund ->
            NavEntry(key) {
                EmergencyFundScreen(
                    userPreferences = userPreferences,
                    analyticsHelper = analyticsHelper,
                    onNavigateBack = onBack,
                )
            }

        is Destination.TermsOfService ->
            NavEntry(key) {
                NewsWebViewScreen(
                    url = AppLinks.TERMS,
                    title = "Terms of Service",
                    analyticsHelper = analyticsHelper,
                    showBookmark = false,
                    screenName = "TermsOfService",
                    onBack = onBack,
                )
            }

        is Destination.PrivacyPolicy ->
            NavEntry(key) {
                NewsWebViewScreen(
                    url = AppLinks.PRIVACY_POLICY,
                    title = "Privacy Policy",
                    analyticsHelper = analyticsHelper,
                    showBookmark = false,
                    screenName = "PrivacyPolicy",
                    onBack = onBack,
                )
            }

        is Destination.FinancialNews ->
            NavEntry(key) {
                NewsScreen(
                    onBack = onBack,
                    onNavigateToWeb = { url, title ->
                        backStack.add(Destination.NewsWebFrame(url = url, title = title))
                    },
                    analyticsHelper = analyticsHelper,
                )
            }

        is Destination.NewsWebFrame ->
            NavEntry(key) {
                NewsWebViewScreen(
                    url = key.url,
                    title = key.title,
                    analyticsHelper = analyticsHelper,
                    onBack = onBack,
                )
            }

        is Destination.FinancialSandbox ->
            NavEntry(key) {
                FinancialSandboxScreen(
                    onBack = onBack,
                    analyticsHelper = analyticsHelper,
                )
            }

        is Destination.EmiCalculator ->
            NavEntry(key) {
                EmiCalculatorScreen(
                    onBack = onBack,
                    isPremium = isPremium,
                    analyticsHelper = analyticsHelper,
                    onNavigateToSaveLoan = { amount, rate, months, emi ->
                        backStack.add(
                            Destination.Loans(
                                prefillAmount = amount,
                                prefillRate = rate,
                                prefillTenureMonths = months,
                                prefillEmi = emi,
                            ),
                        )
                    },
                    onNavigateToCompare = {
                    },
                )
            }

        is Destination.LearnFinance ->
            NavEntry(key) {
                LearnFinanceScreen(
                    viewModel = learnFinanceViewModel,
                    analyticsHelper = analyticsHelper,
                    onBack = onBack,
                    onArticleClick = { articleId ->
                        backStack.add(Destination.ArticleDetail(articleId))
                    },
                    isPremium = isPremium
                )
            }

        is Destination.ArticleDetail ->
            NavEntry(key) {
                ArticleDetailScreen(
                    articleId = key.articleId,
                    viewModel = learnFinanceViewModel,
                    analyticsHelper = analyticsHelper,
                    onBack = onBack,
                    onArticleClick = { articleId ->
                        backStack.add(Destination.ArticleDetail(articleId))
                    }
                )
            }

        is Destination.PremiumScreen ->
            NavEntry(key) {
                PremiumScreen(
                    onBackClick = onBack,
                    analyticsHelper = analyticsHelper,
                )
            }

        is Destination.CurrencyConverter ->
            NavEntry(key) {
                CurrencyConverterScreen(
                    onNavigateBack = onBack,
                    analyticsHelper = analyticsHelper
                )
            }

        else -> error("Unknown destination: $key")
    }
}
