package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.dao.BookmarkedFinanceArticleDao
import prasad.vennam.moneypilot.data.entity.BookmarkedFinanceArticle
import prasad.vennam.moneypilot.data.model.FinanceArticle
import prasad.vennam.moneypilot.data.repository.LearnFinanceRepository
import javax.inject.Inject

data class LearnFinanceUiState(
    val articles: List<FinanceArticle> = emptyList(),
    val categories: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val bookmarkedIds: Set<String> = emptySet(),
    val showBookmarksOnly: Boolean = false
)

@HiltViewModel
class LearnFinanceViewModel @Inject constructor(
    private val repository: LearnFinanceRepository,
    private val bookmarkDao: BookmarkedFinanceArticleDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _showBookmarksOnly = MutableStateFlow(false)

    val bookmarkedIds: StateFlow<Set<String>> = bookmarkDao.getAllBookmarks()
        .map { bookmarks -> bookmarks.map { it.articleId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val uiState: StateFlow<LearnFinanceUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _showBookmarksOnly,
        bookmarkedIds
    ) { query, category, showBookmarks, bookmarks ->
        val filteredArticles = repository.getArticles().filter { article ->
            val matchesQuery = article.title.contains(query, ignoreCase = true) ||
                    article.content.contains(query, ignoreCase = true)
            val matchesCategory = category == null || article.category == category
            val matchesBookmark = !showBookmarks || bookmarks.contains(article.id)
            matchesQuery && matchesCategory && matchesBookmark
        }
        LearnFinanceUiState(
            articles = filteredArticles,
            categories = repository.getCategories(),
            searchQuery = query,
            selectedCategory = category,
            bookmarkedIds = bookmarks,
            showBookmarksOnly = showBookmarks
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LearnFinanceUiState())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleBookmark(articleId: String) {
        viewModelScope.launch {
            val isCurrentlyBookmarked = bookmarkedIds.value.contains(articleId)
            if (isCurrentlyBookmarked) {
                bookmarkDao.deleteBookmark(BookmarkedFinanceArticle(articleId))
            } else {
                bookmarkDao.insertBookmark(BookmarkedFinanceArticle(articleId))
            }
        }
    }

    fun toggleShowBookmarksOnly() {
        _showBookmarksOnly.value = !_showBookmarksOnly.value
    }

    fun getArticleById(id: String): FinanceArticle? {
        return repository.getArticleById(id)
    }
}
