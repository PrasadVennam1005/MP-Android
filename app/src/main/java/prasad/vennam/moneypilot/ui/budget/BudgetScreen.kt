package prasad.vennam.moneypilot.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.ui.viewmodel.BudgetViewModel
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import java.util.Calendar

data class BudgetItemState(
    val budget: Budget,
    val category: Category?,
    val spent: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgetViewModel: BudgetViewModel,
    transactionViewModel: TransactionViewModel,
) {
    val budgets by budgetViewModel.allBudgets.collectAsState()
    val categories by budgetViewModel.allCategories.collectAsState()
    val transactions by transactionViewModel.allTransactions.collectAsState()

    var showFormSheet by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<Budget?>(null) }

    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Budget Planner",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    budgetToEdit = null
                    showFormSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Add Budget",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { innerPadding ->
        val budgetItemStates = remember(budgets, transactions, categories, currentMonth, currentYear) {
            budgets.map { budget ->
                val category = categories.find { it.id == budget.categoryId }
                val spent = transactions.filter {
                    val transCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    it.categoryId == budget.categoryId &&
                            it.type == TransactionType.EXPENSE &&
                            transCal.get(Calendar.MONTH) == currentMonth &&
                            transCal.get(Calendar.YEAR) == currentYear
                }.sumOf { it.amount }
                BudgetItemState(budget, category, spent)
            }
        }

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                BudgetHeaderSection(budgets, transactions, currentMonth, currentYear)
            }

            item {
                Text(
                    "Monthly Budgets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (budgetItemStates.isEmpty()) {
                item {
                    EmptyBudgetState()
                }
            } else {
                items(budgetItemStates, key = { it.budget.id }) { itemState ->
                    PremiumBudgetCard(
                        category = itemState.category,
                        budget = itemState.budget,
                        spent = itemState.spent,
                        onEdit = {
                            budgetToEdit = itemState.budget
                            showFormSheet = true
                        },
                        onDelete = { budgetViewModel.deleteBudget(itemState.budget) }
                    )
                }
            }
        }
    }

    if (showFormSheet) {
        BudgetFormBottomSheet(
            initialBudget = budgetToEdit,
            categories = categories.filter { it.isExpense },
            onDismiss = {
                showFormSheet = false
                budgetToEdit = null
            },
            onSave = { catId, amount ->
                if (budgetToEdit == null) {
                    budgetViewModel.saveBudget(
                        Budget(
                            categoryId = catId,
                            amount = amount,
                            period = "Monthly"
                        )
                    )
                } else {
                    budgetViewModel.saveBudget(
                        budgetToEdit!!.copy(
                            categoryId = catId,
                            amount = amount
                        )
                    )
                }
                showFormSheet = false
                budgetToEdit = null
            }
        )
    }
}

@Composable
fun BudgetHeaderSection(
    budgets: List<Budget>,
    transactions: List<Transaction>,
    month: Int,
    year: Int,
) {
    val totalBudget = remember(budgets) { budgets.sumOf { it.amount } }
    val totalSpent = remember(transactions, budgets, month, year) {
        transactions.filter {
            val transCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            it.type == TransactionType.EXPENSE &&
                    transCal.get(Calendar.MONTH) == month &&
                    transCal.get(Calendar.YEAR) == year &&
                    budgets.any { b -> b.categoryId == it.categoryId }
        }.sumOf { it.amount }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                alpha = 0.3f
            )
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Budget Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Budget", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "₹${String.format("%,.0f", totalBudget)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Spent", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "₹${String.format("%,.0f", totalSpent)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumBudgetCard(
    category: Category?,
    budget: Budget,
    spent: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val remaining = (budget.amount - spent).coerceAtLeast(0.0)
    val progress = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f

    val progressColor = when {
        progress < 0.7f -> Color(0xFF4CAF50) // Green
        progress < 0.9f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = progressColor.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getCategoryIcon(category?.iconName),
                                contentDescription = null,
                                tint = progressColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        category?.name ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BudgetStat(label = "Budget", amount = budget.amount)
                BudgetStat(label = "Used", amount = spent, color = progressColor)
                BudgetStat(
                    label = "Remaining",
                    amount = remaining,
                    color = if (remaining > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            if (progress > 1f) {
                Text(
                    "Exceeded by ₹${String.format("%,.0f", spent - budget.amount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun BudgetStat(label: String, amount: Double, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "₹${String.format("%,.0f", amount)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetFormBottomSheet(
    initialBudget: Budget? = null,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Long, Double) -> Unit,
) {
    var selectedCategoryId by remember { mutableStateOf(initialBudget?.categoryId) }
    var amount by remember { mutableStateOf(initialBudget?.amount?.toString() ?: "") }
    var expanded by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Close Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialBudget == null) "Set New Budget" else "Edit Budget",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Category Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategoryId }?.name
                            ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Category,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expanded = true })
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getCategoryIcon(category.iconName),
                                        contentDescription = null,
                                        tint = Color(category.color),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    selectedCategoryId = category.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it
                    },
                    label = { Text("Monthly Budget Amount") },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Payments,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    prefix = {
                        Text(
                            "₹",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Save Button
                Button(
                    onClick = {
                        onSave(selectedCategoryId!!, amount.toDoubleOrNull() ?: 0.0)
                    },
                    enabled = selectedCategoryId != null && amount.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Budget", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun EmptyBudgetState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.AccountBalanceWallet,
            null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No budgets set yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

fun getCategoryIcon(name: String?): ImageVector {
    return when (name) {
        "restaurant" -> Icons.Rounded.Restaurant
        "directions_car" -> Icons.Rounded.DirectionsCar
        "shopping_cart" -> Icons.Rounded.ShoppingCart
        "movie" -> Icons.Rounded.Movie
        "medical_services" -> Icons.Rounded.MedicalServices
        "lightbulb" -> Icons.Rounded.Lightbulb
        "home" -> Icons.Rounded.Home
        "school" -> Icons.Rounded.School
        "card_giftcard" -> Icons.Rounded.CardGiftcard
        "flight" -> Icons.Rounded.Flight
        "security" -> Icons.Rounded.Security
        "receipt" -> Icons.Rounded.Receipt
        "payments" -> Icons.Rounded.Payments
        "work" -> Icons.Rounded.Work
        "trending_up" -> Icons.Rounded.TrendingUp
        "apartment" -> Icons.Rounded.Apartment
        "redeem" -> Icons.Rounded.Redeem
        "history" -> Icons.Rounded.History
        else -> Icons.Rounded.Category
    }
}
