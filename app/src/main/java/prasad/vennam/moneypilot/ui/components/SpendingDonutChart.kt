package prasad.vennam.moneypilot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun SpendingDonutChart(
    sortedSpending: List<Pair<String, Double>>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val totalSpending = sortedSpending.sumOf { it.second }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f

            if (totalSpending == 0.0) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 40f, cap = StrokeCap.Round),
                )
            } else {
                sortedSpending.forEachIndexed { index, pair ->
                    val sweepAngle = (pair.second / totalSpending * 360).toFloat()
                    val color = colors.getOrElse(index) { Color.Gray }

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 40f, cap = StrokeCap.Round),
                    )
                    startAngle += sweepAngle
                }
            }
        }
    }
}
