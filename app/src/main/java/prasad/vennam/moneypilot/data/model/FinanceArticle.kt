package prasad.vennam.moneypilot.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArticleQuiz(
    val enabled: Boolean = false,
    val question: String? = null,
    val options: List<String>? = null,
    val correctAnswer: Int? = null,
    val explanation: String? = null,
)

@JsonClass(generateAdapter = true)
data class FinanceArticle(
    val id: String,
    val title: String,
    val category: String,
    val subcategory: String = "",
    val description: String = "",
    val content: String,
    val level: String = "Beginner",
    val readTimeMinutes: Int = 3,
    val featured: Boolean = false,
    val isPremium: Boolean = false,
    val thumbnailUrl: String = "",
    val bannerUrl: String = "",
    val youtubeUrl: String = "",
    val tags: List<String> = emptyList(),
    val relatedArticles: List<String> = emptyList(),
    val recommendedFor: List<String> = emptyList(),
    val quiz: ArticleQuiz = ArticleQuiz(),
    val publishedAt: String = "",
    val lastUpdatedAt: String = "",
)
