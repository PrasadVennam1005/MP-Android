package prasad.vennam.moneypilot.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.util.inRupees
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import prasad.vennam.moneypilot.R
import androidx.compose.ui.res.stringResource
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.ui.components.ProfileIconButton
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.dashboard.SyncStatusIndicator
import androidx.compose.material3.TopAppBar

data class TransactionItemState(
    val transaction: Transaction,
    val category: Category?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: TransactionViewModel,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    userData: UserPreferences.UserData?,
    syncState: SyncState?,
    onProfileClick: () -> Unit,
    fixedType: TransactionType? = null,
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedPaymentMode by remember { mutableStateOf<String?>(null) }
    var selectedDateRange by remember { mutableStateOf<Long?>(null) } // Simplified for now

    var showFilterSheet by remember { mutableStateOf(false) }

    val filteredTransactions =
        remember(transactions, searchQuery, selectedCategoryId, selectedPaymentMode, fixedType) {
            transactions.filter { transaction ->
                val matchesType = fixedType == null || transaction.type == fixedType
                val matchesSearch = transaction.note.contains(searchQuery, ignoreCase = true) ||
                        categories.find { it.id == transaction.categoryId }?.name?.contains(
                            searchQuery,
                            ignoreCase = true
                        ) == true
                val matchesCategory =
                    selectedCategoryId == null || transaction.categoryId == selectedCategoryId
                val matchesPayment =
                    selectedPaymentMode == null || transaction.paymentMode == selectedPaymentMode

                matchesType && matchesSearch && matchesCategory && matchesPayment
            }
        }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        Text(
                            when (fixedType) {
                                TransactionType.INCOME -> stringResource(R.string.income)
                                TransactionType.EXPENSE -> stringResource(R.string.expenses)
                                else -> stringResource(R.string.history)
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    actions = {
                        if (syncState != null) {
                            SyncStatusIndicator(syncState)
                        }
                        ProfileIconButton(userData = userData, onClick = onProfileClick)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = Color.Unspecified,
                        navigationIconContentColor = Color.Unspecified,
                        titleContentColor = Color.Unspecified,
                        actionIconContentColor = Color.Unspecified
                    )
                )

                // Search Bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onFilterClick = { showFilterSheet = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add))
            }
        }
    ) { innerPadding ->
        val transactionItemStates = remember(filteredTransactions, categories) {
            filteredTransactions.map { transaction ->
                TransactionItemState(transaction, categories.find { it.id == transaction.categoryId })
            }
        }

        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            if (transactionItemStates.isEmpty()) {
                EmptyState(searchQuery.isNotEmpty())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(transactionItemStates, key = { it.transaction.id }) { itemState ->
                        SwipeableTransactionCard(
                            transaction = itemState.transaction,
                            category = itemState.category,
                            onEdit = { onEditTransaction(itemState.transaction.id) },
                            onDelete = { viewModel.deleteTransaction(itemState.transaction) }
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            categories = categories.filter { fixedType == null || it.isExpense == (fixedType == TransactionType.EXPENSE) },
            selectedCategoryId = selectedCategoryId,
            selectedPaymentMode = selectedPaymentMode,
            onCategorySelect = { selectedCategoryId = it },
            onPaymentModeSelect = { selectedPaymentMode = it },
            onReset = {
                selectedCategoryId = null
                selectedPaymentMode = null
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = { Text(stringResource(R.string.search_transactions)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search)) },
        trailingIcon = {
            IconButton(onClick = onFilterClick) {
                Icon(
                    Icons.Rounded.FilterList,
                    contentDescription = stringResource(R.string.filter),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionCard(
    transaction: Transaction,
    category: Category?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
                dismissState.reset()
            }

            SwipeToDismissBoxValue.StartToEnd -> {
                onEdit()
                dismissState.reset()
            }

            else -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Rounded.Edit
                SwipeToDismissBoxValue.EndToStart -> Icons.Rounded.Delete
                else -> Icons.Rounded.Delete
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Icon(icon, contentDescription = null)
            }
        },
        content = {
            FintechTransactionCard(transaction, category, onClick = onEdit)
        }
    )
}

@Composable
fun FintechTransactionCard(
    transaction: Transaction,
    category: Category?,
    onClick: () -> Unit,
) {
    val locale = androidx.compose.ui.platform.LocalLocale.current.platformLocale
    val dateFormatter = remember(locale) { SimpleDateFormat("dd MMM, yyyy", locale) }
    val currencyCode = LocalCurrencyCode.current

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Surface(
                color = if (category != null) Color(category.color).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = remember(category?.iconName) {
                        when (category?.iconName) {
                            "restaurant" -> Icons.Rounded.Restaurant
                            "directions_car" -> Icons.Rounded.DirectionsCar
                            "shopping_cart" -> Icons.Rounded.ShoppingCart
                            "movie" -> Icons.Rounded.Movie
                            "payments" -> Icons.Rounded.Payments
                            "work" -> Icons.Rounded.Work
                            "medical_services" -> Icons.Rounded.MedicalServices
                            "home" -> Icons.Rounded.Home
                            "school" -> Icons.Rounded.School
                            "flight" -> Icons.Rounded.Flight
                            "receipt" -> Icons.Rounded.Receipt
                            "trending_up" -> Icons.AutoMirrored.Rounded.TrendingUp
                            else -> Icons.Rounded.Category
                        }
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (category != null) Color(category.color) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note.ifBlank { category?.name ?: stringResource(R.string.transaction) },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${category?.name ?: stringResource(R.string.general)} • ${dateFormatter.format(Date(transaction.timestamp))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.paymentMode.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            transaction.paymentMode,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val formattedAmount = CurrencyFormatter.format(transaction.amount.inRupees, transaction.currencyCode)
                val sign = if (transaction.type == TransactionType.INCOME) "+" else "-"
                Text(
                    text = "$sign$formattedAmount",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    categories: List<Category>,
    selectedCategoryId: Long?,
    selectedPaymentMode: String?,
    onCategorySelect: (Long?) -> Unit,
    onPaymentModeSelect: (String?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.filters),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onReset) { Text(stringResource(R.string.reset)) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                stringResource(R.string.category),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { onCategorySelect(null) },
                        label = { Text(stringResource(R.string.all)) }
                    )
                }
                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onCategorySelect(category.id) },
                        label = { Text(category.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.payment_mode),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            val modes = listOf("Cash", "UPI", "Bank Transfer", "Credit Card", "Debit Card")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedPaymentMode == null,
                        onClick = { onPaymentModeSelect(null) },
                        label = { Text(stringResource(R.string.all)) }
                    )
                }
                items(modes, key = { it }) { mode ->
                    FilterChip(
                        selected = selectedPaymentMode == mode,
                        onClick = { onPaymentModeSelect(mode) },
                        label = { Text(mode) }
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.apply_filters))
            }
        }
    }
}

@Composable
fun EmptyState(isSearching: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (isSearching) Icons.Rounded.SearchOff else Icons.Rounded.History,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (isSearching) stringResource(R.string.no_results_found) else stringResource(R.string.no_transactions_yet),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
