package prasad.vennam.moneypilot.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.BookmarkedArticle

@Dao
interface BookmarkedArticleDao {
    @Query("SELECT * FROM bookmarked_articles ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkedArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkedArticle)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkedArticle)

    @Query("DELETE FROM bookmarked_articles WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    @Query("SELECT * FROM bookmarked_articles WHERE url = :url LIMIT 1")
    suspend fun getBookmarkByUrl(url: String): BookmarkedArticle?
}
