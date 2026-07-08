package prasad.vennam.moneypilot.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarked_finance_articles")
data class BookmarkedFinanceArticle(
    @PrimaryKey val articleId: String,
    val bookmarkedAt: Long = System.currentTimeMillis(),
)
