package prasad.vennam.moneypilot.ui.loans.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.ui.viewmodel.EmiCalculatorUiState
import prasad.vennam.moneypilot.ui.viewmodel.EmiCalculatorViewModel
import prasad.vennam.moneypilot.util.CurrencyFormatter
import java.util.Locale

@Composable
fun DetailedReportScreen(
    state: EmiCalculatorUiState,
    viewModel: EmiCalculatorViewModel,
    currencyCode: String
) {
    val pageSize = 12
    val activeSchedule = if (state.isMonthlyView) state.monthlySchedule else state.yearlySchedule

    val filteredSchedule = remember(activeSchedule, state.searchQuery) {
        if (state.searchQuery.isBlank()) {
            activeSchedule
        } else {
            activeSchedule.filter {
                it.dateLabel.contains(state.searchQuery, ignoreCase = true) ||
                        String.format(Locale.getDefault(), "%.2f", it.principalPaid).contains(state.searchQuery) ||
                        String.format(Locale.getDefault(), "%.2f", it.interestPaid).contains(state.searchQuery)
            }
        }
    }

    val maxPages = (filteredSchedule.size + pageSize - 1) / pageSize
    val displayedItems = filteredSchedule.drop(state.pageIndex * pageSize).take(pageSize)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SummaryChartCard(
                principal = state.amountInput.toDoubleOrNull() ?: 0.0,
                totalInterest = state.emiResult.totalInterest,
                totalPayable = state.emiResult.totalPayable,
                currencyCode = currencyCode
            )
        }

        item {
            MetricsGrid(state, currencyCode)
        }

        item {
            ScheduleControls(
                isMonthlyView = state.isMonthlyView,
                onViewToggle = { viewModel.updateIsMonthlyView(it) },
                searchQuery = state.searchQuery,
                onSearchChange = { viewModel.updateSearchQuery(it) }
            )
        }

        item {
            AmortizationTable(
                installments = displayedItems,
                currencyCode = currencyCode
            )
        }

        if (filteredSchedule.isNotEmpty()) {
            item {
                PaginationControl(
                    pageIndex = state.pageIndex,
                    maxPages = maxPages,
                    onPageChange = { viewModel.updatePageIndex(it) },
                    startRange = (state.pageIndex * pageSize) + 1,
                    endRange = minOf((state.pageIndex + 1) * pageSize, filteredSchedule.size),
                    totalCount = filteredSchedule.size
                )
            }
        }
        
//        item {
//            ExportActions(onExport = { /* TODO: Implement Export */ })
//        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun SummaryChartCard(
    principal: Double,
    totalInterest: Double,
    totalPayable: Double,
    currencyCode: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.total_interest_vs_principal),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(20.dp))

            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp),
            ) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val strokeWidth = 16.dp.toPx()
                    val principalSweep = if (totalPayable > 0) (principal / totalPayable * 360).toFloat() else 0f
                    val interestSweep = 360f - principalSweep

                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = principalSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = tertiaryColor,
                        startAngle = -90f + principalSweep,
                        sweepAngle = interestSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = CurrencyFormatter.format(totalPayable, currencyCode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                LegendIndicator(
                    color = primaryColor,
                    label = "Principal",
                    value = CurrencyFormatter.format(principal, currencyCode)
                )
                LegendIndicator(
                    color = tertiaryColor,
                    label = "Interest",
                    value = CurrencyFormatter.format(totalInterest, currencyCode)
                )
            }
        }
    }
}

@Composable
private fun LegendIndicator(color: androidx.compose.ui.graphics.Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetricsGrid(state: EmiCalculatorUiState, currencyCode: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                icon = Icons.Rounded.CreditCard,
                label = stringResource(R.string.monthly_emi),
                value = state.formattedMonthlyEmi,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Rounded.CalendarMonth,
                label = stringResource(R.string.tenure),
                value = "${state.tenureInput} ${if (state.isTenureInYears) "Years" else "Months"}",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                icon = Icons.Rounded.Percent,
                label = "Interest Rate",
                value = "${state.rateInput}% p.a.",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Rounded.Event,
                label = stringResource(R.string.completion_date),
                value = state.emiResult.loanEndDate,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ScheduleControls(
    isMonthlyView: Boolean,
    onViewToggle: (Boolean) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = isMonthlyView,
                    onClick = { onViewToggle(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text(stringResource(R.string.monthly)) }
                )
                SegmentedButton(
                    selected = !isMonthlyView,
                    onClick = { onViewToggle(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text(stringResource(R.string.yearly_label)) }
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_installments)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, null)
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )
        }
    }
}

@Composable
private fun PaginationControl(
    pageIndex: Int,
    maxPages: Int,
    onPageChange: (Int) -> Unit,
    startRange: Int,
    endRange: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    R.string.showing_installment_range,
                    startRange,
                    endRange,
                    totalCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    enabled = pageIndex > 0,
                    onClick = { onPageChange(pageIndex - 1) },
                ) {
                    Icon(Icons.Rounded.ChevronLeft, null)
                }
                IconButton(
                    enabled = pageIndex < maxPages - 1,
                    onClick = { onPageChange(pageIndex + 1) },
                ) {
                    Icon(Icons.Rounded.ChevronRight, null)
                }
            }
        }
    }
}

@Composable
private fun ExportActions(onExport: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = { onExport("PDF") },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Rounded.PictureAsPdf, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("PDF")
        }
        OutlinedButton(
            onClick = { onExport("CSV") },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Rounded.TableChart, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("CSV")
        }
    }
}
