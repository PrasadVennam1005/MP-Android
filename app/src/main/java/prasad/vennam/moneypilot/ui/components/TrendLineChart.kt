package prasad.vennam.moneypilot.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import prasad.vennam.moneypilot.ui.viewmodel.TrendPoint

@Composable
fun TrendLineChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    incomeColor: Color = Color(0xFF10B981), // Premium Green
    expenseColor: Color = Color(0xFF2563EB), // Premium Blue
    gridColor: Color = Color.LightGray.copy(alpha = 0.2f),
    labelColor: Color = Color.Gray,
    currencySymbol: String = "₹",
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("No data available for this range", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val maxVal =
        remember(points) {
            val maxIn = points.maxOfOrNull { it.income } ?: 0.0
            val maxExp = points.maxOfOrNull { it.expense } ?: 0.0
            maxOf(maxIn, maxExp, 1000.0) * 1.15 // 15% padding at top
        }

    val minVal = 0.0

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        )
    }

    // Reusable Path and Stroke structures to avoid allocations on every draw frame
    val trendLinePath = remember { Path() }
    val trendFillPath = remember { Path() }
    val trendStroke = remember { Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val selectorStroke = remember { Stroke(width = 4f) }
    val dashPathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }

    // Pre-measure X and Y labels so expensive text layout does not run on every frame
    val gridLinesCount = 4
    val yLabelResults = remember(points, maxVal, textMeasurer, labelColor, currencySymbol) {
        (0..gridLinesCount).map { i ->
            val ratio = i.toFloat() / gridLinesCount
            val value = minVal + (ratio * (maxVal - minVal))
            val label = "$currencySymbol${String.format("%,.0f", value)}"
            textMeasurer.measure(
                text = label,
                style = TextStyle(fontSize = 10.sp, color = labelColor),
            )
        }
    }

    val xLabelResults = remember(points, textMeasurer, labelColor) {
        val labelStep = (points.size / 6).coerceAtLeast(1)
        points.mapIndexed { index, point ->
            if (index % labelStep == 0 || index == points.size - 1) {
                textMeasurer.measure(
                    text = point.label,
                    style = TextStyle(fontSize = 10.sp, color = labelColor),
                )
            } else {
                null
            }
        }
    }

    Box(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(16.dp),
    ) {
        Column {
            // Header showing values
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val activePoint = selectedIndex?.let { points.getOrNull(it) }
                if (activePoint != null) {
                    Column {
                        Text(
                            text = "Date: ${activePoint.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = labelColor,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(8.dp)
                                            .background(incomeColor, RoundedCornerShape(50)),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Inc: $currencySymbol${String.format("%,.0f", activePoint.income)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = incomeColor,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(8.dp)
                                            .background(expenseColor, RoundedCornerShape(50)),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Exp: $currencySymbol${String.format("%,.0f", activePoint.expense)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = expenseColor,
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(8.dp)
                                        .background(incomeColor, RoundedCornerShape(50)),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Income",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(8.dp)
                                        .background(expenseColor, RoundedCornerShape(50)),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Expense",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Canvas
            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(points) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val canvasWidth = size.width
                                    val stepX = canvasWidth / (points.size - 1).coerceAtLeast(1)
                                    val index = (offset.x / stepX).plus(0.5f).toInt().coerceIn(0, points.size - 1)
                                    selectedIndex = index
                                    tryAwaitRelease()
                                    selectedIndex = null
                                },
                            )
                        }.pointerInput(points) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val canvasWidth = size.width
                                    val stepX = canvasWidth / (points.size - 1).coerceAtLeast(1)
                                    selectedIndex = (offset.x / stepX).plus(0.5f).toInt().coerceIn(0, points.size - 1)
                                },
                                onDrag = { change, _ ->
                                    val canvasWidth = size.width
                                    val stepX = canvasWidth / (points.size - 1).coerceAtLeast(1)
                                    selectedIndex = (change.position.x / stepX).plus(0.5f).toInt().coerceIn(0, points.size - 1)
                                },
                                onDragEnd = { selectedIndex = null },
                                onDragCancel = { selectedIndex = null },
                            )
                        },
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val paddingBottom = 40f
                val paddingTop = 20f
                val chartHeight = canvasHeight - paddingBottom - paddingTop

                val stepX = canvasWidth / (points.size - 1).coerceAtLeast(1)

                // Helpers to compute coordinates
                fun getX(index: Int): Float = index * stepX

                fun getY(value: Double): Float {
                    val ratio = ((value - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)
                    return canvasHeight - paddingBottom - (ratio * chartHeight * animationProgress.value)
                }

                // Draw horizontal grid lines
                for (i in 0..gridLinesCount) {
                    val ratio = i.toFloat() / gridLinesCount
                    val y = canvasHeight - paddingBottom - (ratio * chartHeight)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 2f,
                    )
                    // Draw Y labels
                    val textLayoutResult = yLabelResults[i]
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(4f, y - textLayoutResult.size.height),
                    )
                }

                // Draw X Labels
                points.forEachIndexed { index, _ ->
                    val textLayoutResult = xLabelResults[index]
                    if (textLayoutResult != null) {
                        val x = getX(index)
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(x - textLayoutResult.size.width / 2, canvasHeight - paddingBottom + 8f),
                        )
                    }
                }

                // Draw lines with gradients
                fun drawTrendLine(
                    color: Color,
                    extractor: (TrendPoint) -> Double,
                ) {
                    if (points.size < 2) return

                    trendLinePath.reset()
                    trendLinePath.moveTo(getX(0), getY(extractor(points[0])))
                    for (i in 0 until points.size - 1) {
                        val x1 = getX(i)
                        val y1 = getY(extractor(points[i]))
                        val x2 = getX(i + 1)
                        val y2 = getY(extractor(points[i + 1]))

                        val cx1 = x1 + (x2 - x1) / 2
                        val cy1 = y1
                        val cx2 = x1 + (x2 - x1) / 2
                        val cy2 = y2

                        trendLinePath.cubicTo(cx1, cy1, cx2, cy2, x2, y2)
                    }

                    // Fill Gradient
                    trendFillPath.reset()
                    trendFillPath.addPath(trendLinePath)
                    trendFillPath.lineTo(getX(points.size - 1), canvasHeight - paddingBottom)
                    trendFillPath.lineTo(getX(0), canvasHeight - paddingBottom)
                    trendFillPath.close()

                    drawPath(
                        path = trendFillPath,
                        brush =
                            Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                                startY = getY(maxVal),
                                endY = canvasHeight - paddingBottom,
                            ),
                    )

                    drawPath(
                        path = trendLinePath,
                        color = color,
                        style = trendStroke,
                    )
                }

                // Render Income Line
                drawTrendLine(incomeColor) { it.income }

                // Render Expense Line
                drawTrendLine(expenseColor) { it.expense }

                // Highlight interactive selection
                selectedIndex?.let { index ->
                    val x = getX(index)
                    drawLine(
                        color = labelColor.copy(alpha = 0.4f),
                        start = Offset(x, paddingTop),
                        end = Offset(x, canvasHeight - paddingBottom),
                        strokeWidth = 3f,
                        pathEffect = dashPathEffect,
                    )

                    // Draw circles on both lines
                    val incY = getY(points[index].income)
                    val expY = getY(points[index].expense)

                    drawCircle(
                        color = Color.White,
                        radius = 12f,
                        center = Offset(x, incY),
                    )
                    drawCircle(
                        color = incomeColor,
                        radius = 8f,
                        center = Offset(x, incY),
                        style = selectorStroke,
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 12f,
                        center = Offset(x, expY),
                    )
                    drawCircle(
                        color = expenseColor,
                        radius = 8f,
                        center = Offset(x, expY),
                        style = selectorStroke,
                    )
                }
            }
        }
    }
}
