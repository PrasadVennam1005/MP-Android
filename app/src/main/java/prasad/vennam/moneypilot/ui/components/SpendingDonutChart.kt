package prasad.vennam.moneypilot.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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

    // Animatable progress from 0f to 1f
    val transitionProgress = remember { Animatable(0f) }
    LaunchedEffect(sortedSpending) {
        transitionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        )
    }

    val donutStroke = remember { Stroke(width = 40f, cap = StrokeCap.Round) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f

            if (totalSpending == 0.0) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = donutStroke,
                )
            } else {
                sortedSpending.forEachIndexed { index, pair ->
                    // Multiply the final sweep angle by our animation progress
                    val sweepAngle = (pair.second / totalSpending * 360).toFloat() * transitionProgress.value
                    val color = colors.getOrElse(index) { Color.Gray }

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = donutStroke,
                    )
                    startAngle += sweepAngle
                }
            }
        }
    }
}
