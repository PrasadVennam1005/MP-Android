package prasad.vennam.moneypilot.data.repository

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.dao.BookmarkedArticleDao
import prasad.vennam.moneypilot.data.entity.BookmarkedArticle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository
    @Inject
    constructor(
        private val bookmarkedArticleDao: BookmarkedArticleDao,
    ) {
        val allBookmarks: Flow<List<BookmarkedArticle>> = bookmarkedArticleDao.getAllBookmarks()

        suspend fun insertBookmark(bookmark: BookmarkedArticle) = bookmarkedArticleDao.insertBookmark(bookmark)

        suspend fun deleteBookmark(bookmark: BookmarkedArticle) = bookmarkedArticleDao.deleteBookmark(bookmark)

        suspend fun deleteBookmarkByUrl(url: String) = bookmarkedArticleDao.deleteBookmarkByUrl(url)

        suspend fun getBookmarkByUrl(url: String): BookmarkedArticle? = bookmarkedArticleDao.getBookmarkByUrl(url)
    }
