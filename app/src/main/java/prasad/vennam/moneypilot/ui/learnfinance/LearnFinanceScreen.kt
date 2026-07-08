package prasad.vennam.moneypilot.ui.learnfinance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.model.FinanceArticle
import prasad.vennam.moneypilot.ui.components.AdBannerView
import prasad.vennam.moneypilot.ui.viewmodel.LearnFinanceViewModel
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.TrackScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnFinanceScreen(
    viewModel: LearnFinanceViewModel,
    analyticsHelper: AnalyticsHelper,
    onBack: () -> Unit,
    onArticleClick: (String) -> Unit,
    isPremium: Boolean = false,
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.LEARN_FINANCE)
    val uiState by viewModel.uiState.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    if (isSearchActive) {
        BackHandler {
            viewModel.onSearchQueryChanged("")
            isSearchActive = false
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text(stringResource(R.string.search_articles)) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                            colors =
                                TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.onSearchQueryChanged("")
                                    isSearchActive = false
                                }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.close_search),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            },
                        )
                    } else {
                        Text(
                            stringResource(R.string.learn_finance),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search))
                        }
                        IconButton(onClick = { viewModel.toggleShowBookmarksOnly() }) {
                            Icon(
                                if (uiState.showBookmarksOnly) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = stringResource(R.string.bookmarks),
                                tint = if (uiState.showBookmarksOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val featuredArticles =
            remember(uiState.articles, uiState.selectedCategory) {
                uiState.articles.filter { it.featured && (uiState.selectedCategory == null || it.category == uiState.selectedCategory) }
            }

        Column(modifier = Modifier.padding(padding)) {
            if (isExpanded) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Pane: Navigation & Categories
                    Surface(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.category),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            CategorySidebarItem(
                                label = stringResource(R.string.all),
                                isSelected = uiState.selectedCategory == null,
                                onClick = { viewModel.onCategorySelected(null) }
                            )
                            
                            uiState.categories.forEach { category ->
                                CategorySidebarItem(
                                    label = category,
                                    isSelected = uiState.selectedCategory == category,
                                    onClick = { viewModel.onCategorySelected(category) }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Ad banner in sidebar for tablet
                            AdBannerView(
                                isPremium = isPremium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Right Pane: Article Content Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = lazyGridState,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (featuredArticles.isNotEmpty() && uiState.searchQuery.isEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                Text(
                                    text = stringResource(R.string.featured),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            items(featuredArticles, key = { "featured_${it.id}" }) { article ->
                                FeaturedArticleCard(
                                    article = article,
                                    onClick = { onArticleClick(article.id) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item(span = { GridItemSpan(2) }) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty()) stringResource(R.string.search_articles) else stringResource(R.string.articles),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        if (uiState.articles.isEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(stringResource(R.string.no_articles_found), style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        } else {
                            items(uiState.articles, key = { it.id }) { article ->
                                ArticleItem(
                                    article = article,
                                    isBookmarked = uiState.bookmarkedIds.contains(article.id),
                                    onBookmarkClick = { viewModel.toggleBookmark(article.id) },
                                    onClick = { onArticleClick(article.id) },
                                )
                            }
                        }
                    }
                }
            } else {
                // Mobile Layout: Vertical
                CategoryTabs(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { viewModel.onCategorySelected(it) },
                )

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (featuredArticles.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.featured),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(featuredArticles, key = { "featured_${it.id}" }) { article ->
                                    FeaturedArticleCard(
                                        article = article,
                                        onClick = { onArticleClick(article.id) },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // AdMob banner
                        item {
                            AdBannerView(
                                isPremium = isPremium,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                            )
                        }
                    }

                    item {
                        Text(
                            text = stringResource(R.string.articles),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    if (uiState.articles.isEmpty()) {
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(stringResource(R.string.no_articles_found), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    } else {
                        items(uiState.articles, key = { it.id }) { article ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                ArticleItem(
                                    article = article,
                                    isBookmarked = uiState.bookmarkedIds.contains(article.id),
                                    onBookmarkClick = { viewModel.toggleBookmark(article.id) },
                                    onClick = { onArticleClick(article.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySidebarItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FeaturedArticleCard(
    article: FinanceArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .widthIn(max = 400.dp)
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                ),
                        ),
                    ).padding(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = article.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    )
                    LevelBadge(level = article.level, isDarkBackground = true)
                }

                Column {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (article.quiz.enabled) stringResource(R.string.read_time_minutes_quiz, article.readTimeMinutes) else stringResource(R.string.read_time_minutes, article.readTimeMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryTabs(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
) {
    LazyRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text(stringResource(R.string.all)) },
            )
        }
        items(categories, key = { it }) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
            )
        }
    }
}

@Composable
fun LevelBadge(
    level: String,
    isDarkBackground: Boolean = false,
) {
    val containerColor =
        when {
            isDarkBackground -> Color.White.copy(alpha = 0.2f)
            level.equals("Beginner", ignoreCase = true) -> Color(0xFFE8F5E9)
            level.equals("Intermediate", ignoreCase = true) -> Color(0xFFFFF8E1)
            else -> Color(0xFFF3E5F5)
        }

    val contentColor =
        when {
            isDarkBackground -> Color.White
            level.equals("Beginner", ignoreCase = true) -> Color(0xFF2E7D32)
            level.equals("Intermediate", ignoreCase = true) -> Color(0xFFF57F17)
            else -> Color(0xFF7B1FA2)
        }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.padding(horizontal = 4.dp),
    ) {
        Text(
            text = level.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun ArticleItem(
    article: FinanceArticle,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = article.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (article.subcategory.isNotEmpty()) {
                        Text(
                            text = " • ${article.subcategory.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LevelBadge(level = article.level)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.read_time_minutes, article.readTimeMinutes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (article.quiz.enabled) {
                        Surface(
                            color = Color(0xFFE0F7FA),
                            contentColor = Color(0xFF006064),
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(
                                text = stringResource(R.string.quiz_label),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = stringResource(R.string.bookmark),
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
