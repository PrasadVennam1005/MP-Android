package prasad.vennam.moneypilot.util

import android.net.Uri
import prasad.vennam.moneypilot.ui.navigation.Destination

/**
 * Maps incoming App Link URIs to typed Navigation3 Destinations.
 *
 * Example URLs:
 *  - https://moneypilot.app/dashboard
 *  - https://moneypilot.app/article/budgeting-101
 *  - https://moneypilot.app/history
 */
object DeepLinkMapper {
    fun fromUri(uri: Uri): Destination? {
        if (uri.host?.contains("moneypilot.app") == false) return null

        val pathSegments = uri.pathSegments
        if (pathSegments.isEmpty()) return Destination.Dashboard

        return when (pathSegments[0].lowercase()) {
            "dashboard" -> Destination.Dashboard
            "insights" -> Destination.Insights
            "history" -> Destination.History
            "premium" -> Destination.PremiumScreen
            "learn" -> Destination.LearnFinance
            "article" -> {
                val articleId = if (pathSegments.size > 1) pathSegments[1] else null
                if (articleId != null) {
                    Destination.ArticleDetail(articleId = articleId)
                } else {
                    Destination.LearnFinance
                }
            }
            else -> null
        }
    }
}
