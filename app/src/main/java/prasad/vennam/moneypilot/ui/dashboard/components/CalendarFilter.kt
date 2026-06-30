package prasad.vennam.moneypilot.ui.dashboard.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.TimeFrame
import java.text.DateFormatSymbols
import java.util.Calendar

@Composable
fun CalendarFilterRow(
    selectedTimeFrame: TimeFrame,
    selectedMonth: Int,
    selectedYear: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClickedFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filterLabel = when (selectedTimeFrame) {
        TimeFrame.MONTHLY -> {
            val monthName = DateFormatSymbols.getInstance().months[selectedMonth]
            "$monthName $selectedYear"
        }
        TimeFrame.QUARTERLY -> {
            val quarterNum = (selectedMonth / 3) + 1
            "Q$quarterNum $selectedYear"
        }
        TimeFrame.YEARLY -> {
            "$selectedYear"
        }
    }

    val now = Calendar.getInstance()
    val maxYear = now.get(Calendar.YEAR)
    val maxMonth = now.get(Calendar.MONTH)

    val isNextEnabled = when (selectedTimeFrame) {
        TimeFrame.MONTHLY -> {
            selectedYear < maxYear || (selectedYear == maxYear && selectedMonth < maxMonth)
        }
        TimeFrame.QUARTERLY -> {
            val currentQuarterMonthStart = (selectedMonth / 3) * 3
            val maxQuarterMonthStart = (maxMonth / 3) * 3
            selectedYear < maxYear || (selectedYear == maxYear && currentQuarterMonthStart < maxQuarterMonthStart)
        }
        TimeFrame.YEARLY -> {
            selectedYear < maxYear
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous period button
        Surface(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .clickable { onPrevious() },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "Previous Period",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Selected filter pill
        Surface(
            modifier = Modifier
                .height(38.dp)
                .clip(CircleShape)
                .clickable { onClickedFilter() },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = filterLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Next period button
        Surface(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .clickable(enabled = isNextEnabled) { if (isNextEnabled) onNext() },
            color = if (isNextEnabled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            },
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = if (isNextEnabled) 0.08f else 0.03f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Next Period",
                    tint = if (isNextEnabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarFilterBottomSheet(
    timeFrame: TimeFrame,
    selectedMonth: Int,
    selectedYear: Int,
    onDismiss: () -> Unit,
    onSelected: (month: Int, year: Int) -> Unit
) {
    var pickerYear by remember { mutableStateOf(selectedYear) }
    val now = Calendar.getInstance()
    val maxYear = now.get(Calendar.YEAR)
    val maxMonth = now.get(Calendar.MONTH)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 36.dp, start = 20.dp, end = 20.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Time Period",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Year selector header (only relevant for Monthly and Quarterly)
            if (timeFrame != TimeFrame.YEARLY) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { pickerYear -= 1 }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "Previous Year",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "$pickerYear",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    val isNextYearEnabled = pickerYear < maxYear
                    IconButton(
                        onClick = { if (isNextYearEnabled) pickerYear += 1 },
                        enabled = isNextYearEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Next Year",
                            tint = if (isNextYearEnabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (timeFrame) {
                TimeFrame.MONTHLY -> {
                    val months = DateFormatSymbols.getInstance().months
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(months.take(12)) { index, month ->
                            val isSelected = index == selectedMonth && pickerYear == selectedYear
                            val isFuture = pickerYear == maxYear && index > maxMonth

                            Surface(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isFuture) {
                                        onSelected(index, pickerYear)
                                    },
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = month.take(3),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                TimeFrame.QUARTERLY -> {
                    val quarters = listOf("Quarter 1", "Quarter 2", "Quarter 3", "Quarter 4")
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(quarters) { index, quarter ->
                            val quarterStartMonth = index * 3
                            val isSelected = (selectedMonth / 3) == index && pickerYear == selectedYear
                            val isFuture = pickerYear == maxYear && quarterStartMonth > maxMonth

                            Surface(
                                modifier = Modifier
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isFuture) {
                                        onSelected(quarterStartMonth, pickerYear)
                                    },
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = quarter,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                TimeFrame.YEARLY -> {
                    val years = (maxYear - 5..maxYear).toList().reversed()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(years) { _, year ->
                            val isSelected = year == selectedYear
                            Surface(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSelected(0, year)
                                    },
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "$year",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
