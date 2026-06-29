package prasad.vennam.moneypilot.ui.budget.components

import prasad.vennam.moneypilot.ui.components.BaseBottomSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.ui.budget.utils.getCategoryIcon
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import prasad.vennam.moneypilot.util.toMajorUnit
import java.util.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetFormBottomSheet(
    initialBudget: Budget? = null,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Long, Double) -> Unit,
) {
    var selectedCategoryId by remember { mutableStateOf(initialBudget?.categoryId) }
    var amount by remember {
        mutableStateOf(
            initialBudget
                ?.amount
                ?.toMajorUnit
                ?.toString()
                ?.removeSuffix(".0") ?: "",
        )
    }
    var expanded by remember { mutableStateOf(false) }
    val currencyCode = LocalCurrencyCode.current
    val currencySymbol = remember(currencyCode) { Currency.getInstance(currencyCode).symbol }

    val amountVal = amount.toDoubleOrNull()
    val isAmountError = amount.isNotEmpty() && (amountVal == null || amountVal <= 0.0 || amountVal > 100000000.0)

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current


    BaseBottomSheet(
        onDismissRequest = onDismiss,
        title = if (initialBudget == null) stringResource(R.string.set_new_budget) else stringResource(R.string.edit_budget),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
        ) {

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Category Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value =
                            categories.find { it.id == selectedCategoryId }?.name
                                ?: stringResource(R.string.select_category),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category)) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Category,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    )
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .clickable { expanded = true },
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f),
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getCategoryIcon(category.iconName),
                                        contentDescription = null,
                                        tint = Color(category.color),
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                                onClick = {
                                    selectedCategoryId = category.id
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it
                    },
                    label = { Text(stringResource(R.string.monthly_budget_amount)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Payments,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    prefix = {
                        Text(
                            currencySymbol,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    isError = isAmountError,
                    supportingText =
                        if (isAmountError) {
                            {
                                val text =
                                    when {
                                        amountVal == null -> stringResource(R.string.invalid_format)
                                        amountVal <= 0.0 -> stringResource(R.string.budget_limit_error_desc)
                                        else -> stringResource(R.string.amount_cannot_exceed)
                                    }
                                Text(text)
                            }
                        } else {
                            null
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Save Button
                Button(
                    onClick = {
                        selectedCategoryId?.let { id ->
                            onSave(id, amount.toDoubleOrNull() ?: 0.0)
                        }
                    },
                    enabled = selectedCategoryId != null && amount.isNotBlank() && !isAmountError,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.set_budget), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
