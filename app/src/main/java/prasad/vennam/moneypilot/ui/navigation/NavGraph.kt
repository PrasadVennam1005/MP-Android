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
import prasad.vennam.moneypilot.ui.dashboard.DashboardScreen
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.emergencyfund.EmergencyFundScreen
import prasad.vennam.moneypilot.ui.faq.FaqScreen
import prasad.vennam.moneypilot.ui.investments.InvestmentScreen
import prasad.vennam.moneypilot.ui.learnfinance.ArticleDetailScreen
import prasad.vennam.moneypilot.ui.learnfinance.LearnFinanceScreen
import prasad.vennam.moneypilot.ui.loans.EmiCalculatorScreen
import prasad.vennam.moneypilot.ui.loans.LoanScreen
import prasad.vennam.moneypilot.ui.news.NewsScreen
import prasad.vennam.moneypilot.ui.news.NewsWebViewScreen
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

fun MoneyPilotNavEntry(
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
            NavEntry<NavKey>(key) {
                prasad.vennam.moneypilot.ui.login.AuthScreen(
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
            NavEntry<NavKey>(key) {
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
            NavEntry<NavKey>(key) {
                InsightsScreen(
                    userData = userData,
                    syncState = syncState,
                    onProfileClick = { backStack.add(Destination.Settings) },
                    onBackClick = onBack,
                    onNavigateToAiChat = { backStack.add(Destination.AiChat) },
                )
            }

        is Destination.AiChat ->
            NavEntry<NavKey>(key) {
                AiChatScreen(
                    onBackClick = onBack,
                )
            }

        is Destination.History ->
            NavEntry<NavKey>(key) {
                HistoryScreen(
                    viewModel = transactionViewModel,
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
            NavEntry<NavKey>(key) {
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
            NavEntry<NavKey>(key) {
                InvestmentScreen(
                    viewModel = investmentViewModel,
                    userData = userData,
                    syncState = syncState,
                    isPremium = isPremium,
                    onProfileClick = { backStack.add(Destination.Settings) },
                )
            }

        is Destination.Loans ->
            NavEntry<NavKey>(key) {
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
                )
            }

        is Destination.Reports ->
            NavEntry<NavKey>(key) {
                ReportsTabScreen(
                    budgetViewModel = budgetViewModel,
                    transactionViewModel = transactionViewModel,
                    analyticsViewModel = analyticsViewModel,
                    userData = userData,
                    syncState = syncState,
                    isPremium = isPremium,
                    onProfileClick = { backStack.add(Destination.Settings) },
                )
            }

        is Destination.Settings ->
            NavEntry<NavKey>(key) {
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
            NavEntry<NavKey>(key) {
                CategoryListScreen(
                    viewModel = transactionViewModel,
                    onNavigateBack = onBack,
                )
            }

        is Destination.ReceiptScanner ->
            NavEntry<NavKey>(key) {
                ReceiptScannerScreen(
                    onNavigateBack = onBack,
                    transactionViewModel = transactionViewModel,
                    analyticsHelper = analyticsHelper,
                )
            }

        is Destination.Notifications ->
            NavEntry<NavKey>(key) {
                prasad.vennam.moneypilot.ui.notifications.NotificationsScreen(
                    onNavigateBack = onBack,
                    onNavigateToWeb = { url, title ->
                        backStack.add(Destination.NewsWebFrame(url = url, title = title))
                    },
                )
            }

        is Destination.FAQ ->
            NavEntry<NavKey>(key) {
                FaqScreen(
                    onNavigateBack = onBack,
                )
            }

        is Destination.EmergencyFund ->
            NavEntry<NavKey>(key) {
                EmergencyFundScreen(
                    userPreferences = userPreferences,
                    onNavigateBack = onBack,
                )
            }

        is Destination.TermsOfService ->
            NavEntry<NavKey>(key) {
                NewsWebViewScreen(
                    url = AppLinks.TERMS,
                    title = "Terms of Service",
                    showBookmark = false,
                    onBack = onBack,
                )
            }

        is Destination.PrivacyPolicy ->
            NavEntry<NavKey>(key) {
                NewsWebViewScreen(
                    url = AppLinks.PRIVACY_POLICY,
                    title = "Privacy Policy",
                    showBookmark = false,
                    onBack = onBack,
                )
            }

        is Destination.FinancialNews ->
            NavEntry<NavKey>(key) {
                NewsScreen(
                    onBack = onBack,
                    onNavigateToWeb = { url, title ->
                        backStack.add(Destination.NewsWebFrame(url = url, title = title))
                    },
                )
            }

        is Destination.NewsWebFrame ->
            NavEntry<NavKey>(key) {
                NewsWebViewScreen(
                    url = key.url,
                    title = key.title,
                    onBack = onBack,
                )
            }

        is Destination.FinancialSandbox ->
            NavEntry<NavKey>(key) {
                prasad.vennam.moneypilot.ui.sandbox.FinancialSandboxScreen(
                    onBack = onBack,
                )
            }

        is Destination.EmiCalculator ->
            NavEntry<NavKey>(key) {
                EmiCalculatorScreen(
                    onBack = onBack,
                    isPremium = isPremium,
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
            NavEntry<NavKey>(key) {
                LearnFinanceScreen(
                    viewModel = learnFinanceViewModel,
                    onBack = onBack,
                    onArticleClick = { articleId ->
                        backStack.add(Destination.ArticleDetail(articleId))
                    },
                    isPremium = isPremium
                )
            }

        is Destination.ArticleDetail ->
            NavEntry<NavKey>(key) {
                ArticleDetailScreen(
                    articleId = key.articleId,
                    viewModel = learnFinanceViewModel,
                    onBack = onBack,
                    onArticleClick = { articleId ->
                        backStack.add(Destination.ArticleDetail(articleId))
                    }
                )
            }

        is Destination.PremiumScreen ->
            NavEntry<NavKey>(key) {
                prasad.vennam.moneypilot.ui.premium.PremiumScreen(
                    onBackClick = onBack,
                )
            }

        is Destination.CurrencyConverter ->
            NavEntry<NavKey>(key) {
                prasad.vennam.moneypilot.ui.currency.CurrencyConverterScreen(
                    onNavigateBack = onBack
                )
            }

        else -> error("Unknown destination: $key")
    }
}
