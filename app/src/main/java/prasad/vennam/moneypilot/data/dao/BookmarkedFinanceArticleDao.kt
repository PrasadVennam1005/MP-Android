package prasad.vennam.moneypilot.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.BookmarkedFinanceArticle

@Dao
interface BookmarkedFinanceArticleDao {
    @Query("SELECT * FROM bookmarked_finance_articles")
    fun getAllBookmarks(): Flow<List<BookmarkedFinanceArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkedFinanceArticle)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkedFinanceArticle)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_finance_articles WHERE articleId = :articleId)")
    fun isBookmarked(articleId: String): Flow<Boolean>
}
