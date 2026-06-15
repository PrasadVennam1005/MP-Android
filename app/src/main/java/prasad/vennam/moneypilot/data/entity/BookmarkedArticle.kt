package prasad.vennam.moneypilot.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarked_articles")
data class BookmarkedArticle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long,
    val currencyCode: String,
)
