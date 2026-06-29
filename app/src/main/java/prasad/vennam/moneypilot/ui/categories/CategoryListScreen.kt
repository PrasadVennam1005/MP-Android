package prasad.vennam.moneypilot.ui.categories

import prasad.vennam.moneypilot.ui.components.BaseBottomSheet
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.ui.budget.utils.getCategoryIcon
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.TrackScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    viewModel: TransactionViewModel,
    analyticsHelper: AnalyticsHelper,
    onNavigateBack: () -> Unit,
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.MANAGE_CATEGORIES)
    val categories by viewModel.allCategories.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.manage_categories),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = Color.Unspecified,
                        navigationIconContentColor = Color.Unspecified,
                        titleContentColor = Color.Unspecified,
                        actionIconContentColor = Color.Unspecified,
                    ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCategory = null
                    showAddSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_category))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val expenseCategories = categories.filter { it.isExpense }
            val incomeCategories = categories.filter { !it.isExpense }

            if (expenseCategories.isNotEmpty()) {
                item {
                    CategoryHeader(stringResource(R.string.expenses))
                }
                items(expenseCategories, key = { it.id }) { category ->
                    CategoryItem(
                        category = category,
                        onClick = {
                            editingCategory = it
                            showAddSheet = true
                        },
                        onDelete = { categoryToDelete = it },
                    )
                }
            }

            if (incomeCategories.isNotEmpty()) {
                item {
                    CategoryHeader(stringResource(R.string.income))
                }
                items(incomeCategories, key = { it.id }) { category ->
                    CategoryItem(
                        category = category,
                        onClick = {
                            editingCategory = it
                            showAddSheet = true
                        },
                        onDelete = { categoryToDelete = it },
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        BaseBottomSheet(
            onDismissRequest = {
                showAddSheet = false
                editingCategory = null
            },
            title = if (editingCategory == null) stringResource(R.string.new_category) else stringResource(R.string.edit_category),
        ) {
            AddCategorySheetContent(
                initialCategory = editingCategory,
                onSave = { newCategory ->
                    analyticsHelper.logEvent(
                        AnalyticsConstants.Event.CATEGORY_SAVED,
                        mapOf(
                            AnalyticsConstants.Param.IS_EDIT to (editingCategory != null),
                            AnalyticsConstants.Param.IS_EXPENSE to newCategory.isExpense,
                        ),
                    )
                    viewModel.saveCategory(newCategory)
                    showAddSheet = false
                    editingCategory = null
                },
                onCancel = {
                    showAddSheet = false
                    editingCategory = null
                },
            )
        }
    }

    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text(stringResource(R.string.delete_category)) },
            text = {
                Text(
                    stringResource(R.string.delete_category_confirm, cat.name),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        analyticsHelper.logEvent(
                            AnalyticsConstants.Event.CATEGORY_DELETED,
                            mapOf(AnalyticsConstants.Param.NAME to cat.name),
                        )
                        viewModel.deleteCategory(cat)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
    )
}

@Composable
fun CategoryItem(
    category: Category,
    onClick: (Category) -> Unit,
    onDelete: (Category) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(category) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = Color(category.color).copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getCategoryIcon(category.iconName),
                        contentDescription = null,
                        tint = Color(category.color),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onDelete(category) }) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
fun AddCategorySheetContent(
    initialCategory: Category?,
    onSave: (Category) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember(initialCategory) { mutableStateOf(initialCategory?.name ?: "") }
    var isExpense by remember(initialCategory) { mutableStateOf(initialCategory?.isExpense ?: true) }

    val availableColors =
        listOf(
            0xFFF44336,
            0xFFE91E63,
            0xFF9C27B0,
            0xFF673AB7,
            0xFF3F51B5,
            0xFF2196F3,
            0xFF03A9F4,
            0xFF00BCD4,
            0xFF009688,
            0xFF4CAF50,
            0xFF8BC34A,
            0xFFCDDC39,
            0xFFFFEB3B,
            0xFFFFC107,
            0xFFFF9800,
            0xFFFF5722,
            0xFF795548,
            0xFF9E9E9E,
            0xFF607D8B,
        )
    var selectedColor by remember(initialCategory) { mutableLongStateOf(initialCategory?.color ?: availableColors[0]) }

    val availableIcons =
        listOf(
            "restaurant",
            "directions_car",
            "shopping_cart",
            "movie",
            "medical_services",
            "lightbulb",
            "home",
            "school",
            "card_giftcard",
            "flight",
            "security",
            "receipt",
            "payments",
            "work",
            "trending_up",
            "apartment",
            "redeem",
            "history",
            "category",
        )
    var selectedIcon by remember(initialCategory) { mutableStateOf(initialCategory?.iconName ?: availableIcons[0]) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 24.dp),
    ) {


        // Type selection
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { isExpense = true },
                modifier = Modifier.weight(1f),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isExpense) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        contentColor = if (isExpense) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                Text(stringResource(R.string.expense))
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { isExpense = false },
                modifier = Modifier.weight(1f),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = if (!isExpense) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        contentColor = if (!isExpense) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                Text(stringResource(R.string.income))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val isNameError = name.isNotEmpty() && name.trim().isEmpty()

        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.category_name)) },
            modifier = Modifier.fillMaxWidth(),
            isError = isNameError,
            supportingText =
                if (isNameError) {
                    { Text(stringResource(R.string.category_name_error_desc)) }
                } else {
                    null
                },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (name.isNotBlank()) {
                        onSave(
                            Category(
                                id = initialCategory?.id ?: 0,
                                name = name.trim(),
                                iconName = selectedIcon,
                                color = selectedColor,
                                isExpense = isExpense,
                            )
                        )
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.color), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Color Picker
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(availableColors, key = { it }) { color ->
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .clickable { selectedColor = color },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selectedColor == color) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.save),
                            tint = Color.White,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.icon), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Icon Picker
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(availableIcons, key = { it }) { iconName ->
                Surface(
                    color =
                        if (selectedIcon ==
                            iconName
                        ) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    shape = CircleShape,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clickable { selectedIcon = iconName },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getCategoryIcon(iconName),
                            contentDescription = null,
                            tint =
                                if (selectedIcon ==
                                    iconName
                                ) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            Category(
                                id = initialCategory?.id ?: 0,
                                name = name.trim(),
                                iconName = selectedIcon,
                                color = selectedColor,
                                isExpense = isExpense,
                            ),
                        )
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
