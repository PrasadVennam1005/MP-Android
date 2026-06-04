package prasad.vennam.moneypilot.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.ui.budget.getCategoryIcon
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val categories by viewModel.allCategories.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Manage Categories", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Category")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val expenseCategories = categories.filter { it.isExpense }
            val incomeCategories = categories.filter { !it.isExpense }

            if (expenseCategories.isNotEmpty()) {
                item {
                    CategoryHeader("Expenses")
                }
                items(expenseCategories) { category ->
                    CategoryItem(category) { categoryToDelete = it }
                }
            }

            if (incomeCategories.isNotEmpty()) {
                item {
                    CategoryHeader("Income")
                }
                items(incomeCategories) { category ->
                    CategoryItem(category) { categoryToDelete = it }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AddCategorySheetContent(
                onSave = { newCategory ->
                    viewModel.saveCategory(newCategory)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showAddSheet = false
                        }
                    }
                },
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showAddSheet = false
                        }
                    }
                }
            )
        }
    }

    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete '${cat.name}'? Transactions using this category will remain, but the category won't be available for new transactions.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(cat)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
fun CategoryItem(category: Category, onDelete: (Category) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(category.color).copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getCategoryIcon(category.iconName),
                        contentDescription = null,
                        tint = Color(category.color),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onDelete(category) }) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AddCategorySheetContent(
    onSave: (Category) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    
    val availableColors = listOf(
        0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 
        0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4, 
        0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39, 
        0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722, 
        0xFF795548, 0xFF9E9E9E, 0xFF607D8B
    )
    var selectedColor by remember { mutableStateOf(availableColors[0]) }

    val availableIcons = listOf(
        "restaurant", "directions_car", "shopping_cart", "movie", 
        "medical_services", "lightbulb", "home", "school", 
        "card_giftcard", "flight", "security", "receipt", 
        "payments", "work", "trending_up", "apartment", 
        "redeem", "history", "category"
    )
    var selectedIcon by remember { mutableStateOf(availableIcons[0]) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "New Category",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Type selection
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { isExpense = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isExpense) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (isExpense) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Expense")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { isExpense = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (!isExpense) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (!isExpense) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Income")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Category Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Color", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        
        // Color Picker
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(availableColors) { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .clickable { selectedColor = color },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedColor == color) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Icon", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        
        // Icon Picker
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(availableIcons) { iconName ->
                Surface(
                    color = if (selectedIcon == iconName) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { selectedIcon = iconName }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getCategoryIcon(iconName),
                            contentDescription = null,
                            tint = if (selectedIcon == iconName) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(Category(name = name.trim(), iconName = selectedIcon, color = selectedColor, isExpense = isExpense))
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
}
