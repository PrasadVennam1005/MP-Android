package prasad.vennam.moneypilot.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenever
import prasad.vennam.moneypilot.data.dao.BookmarkedFinanceArticleDao
import prasad.vennam.moneypilot.data.model.FinanceArticle
import prasad.vennam.moneypilot.data.repository.LearnFinanceRepository

@OptIn(ExperimentalCoroutinesApi::class)
class LearnFinanceViewModelTest {

    private val repository = mock(LearnFinanceRepository::class.java)
    private val bookmarkDao = mock(BookmarkedFinanceArticleDao::class.java)
    private val testDispatcher = UnconfinedTestDispatcher()

    private val sampleArticles = listOf(
        FinanceArticle(
            id = "1",
            title = "Title One",
            category = "Budgeting",
            content = "Content One",
            description = "Desc One",
            subcategory = "Basics",
            tags = listOf("budget", "basics")
        ),
        FinanceArticle(
            id = "2",
            title = "Title Two",
            category = "Loans",
            content = "Content Two",
            description = "Desc Two",
            subcategory = "Advanced",
            tags = listOf("loans")
        ),
        FinanceArticle(
            id = "3",
            title = "Unmatched Title",
            category = "Savings",
            content = "Unmatched Content",
            description = "Unmatched Desc",
            subcategory = "Other",
            tags = listOf("savings")
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(repository.getArticles()).thenReturn(flowOf(sampleArticles))
        whenever(repository.getCategories()).thenReturn(listOf("Budgeting", "Loans", "Savings"))
        whenever(bookmarkDao.getAllBookmarks()).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSearchQueryFiltersArticles() = runTest {
        val viewModel = LearnFinanceViewModel(repository, bookmarkDao)
        
        // Start collection to activate StateFlow
        val collectJob = launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        // Wait for initial load
        waitForCondition { viewModel.uiState.value.articles.size == 3 }
        assertEquals(3, viewModel.uiState.value.articles.size)

        // Set search query by title
        viewModel.onSearchQueryChanged("One")
        waitForCondition { viewModel.uiState.value.articles.size == 1 }
        assertEquals(1, viewModel.uiState.value.articles.size)
        assertEquals("1", viewModel.uiState.value.articles.first().id)

        // Set search query by subcategory
        viewModel.onSearchQueryChanged("Basics")
        waitForCondition { viewModel.uiState.value.articles.size == 1 }
        assertEquals(1, viewModel.uiState.value.articles.size)
        assertEquals("1", viewModel.uiState.value.articles.first().id)

        // Set search query by tags
        viewModel.onSearchQueryChanged("budget")
        waitForCondition { viewModel.uiState.value.articles.size == 1 }
        assertEquals(1, viewModel.uiState.value.articles.size)
        assertEquals("1", viewModel.uiState.value.articles.first().id)

        collectJob.cancel()
    }

    private suspend fun waitForCondition(timeoutMs: Long = 2000, condition: () -> Boolean) {
        val startTime = System.currentTimeMillis()
        while (!condition() && System.currentTimeMillis() - startTime < timeoutMs) {
            delay(10)
        }
    }
}
