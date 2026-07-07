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
        val host = uri.host ?: return null
        if (!host.contains("moneypilot.app") && !host.contains("github.io")) return null

        var pathSegments = uri.pathSegments
        if (host.contains("github.io") && pathSegments.isNotEmpty() && pathSegments[0].lowercase() == "moneypilot-legal") {
            pathSegments = pathSegments.drop(1)
        }

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
            "cosplit" -> {
                val subPath = if (pathSegments.size > 1) pathSegments[1].lowercase() else null
                if (subPath == "join") {
                    val groupId = uri.getQueryParameter("groupId")
                    val groupName = uri.getQueryParameter("name") ?: "Shared Group"
                    if (groupId != null) {
                        Destination.CoSplitJoin(groupId = groupId, groupName = groupName)
                    } else {
                        Destination.CoSplitGroups
                    }
                } else {
                    Destination.CoSplitGroups
                }
            }
            else -> null
        }
    }
}
