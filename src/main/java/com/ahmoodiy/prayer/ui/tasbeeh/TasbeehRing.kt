package com.ahmoodiy.prayer.ui.tasbeeh

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ahmoodiy.prayer.ui.components.goldGradientBrush
import com.ahmoodiy.prayer.ui.theme.EmeraldOutline
import com.ahmoodiy.prayer.ui.theme.GoldPrimary
import com.ahmoodiy.prayer.ui.theme.GoldShimmer
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The signature rosary ring: a gold progress arc, a ring of beads that light up as the user
 * counts, and a tappable centre that hosts the counter content.
 *
 * A tap anywhere inside the ring increments the count; a long press requests a reset.
 */
@Composable
fun TasbeehRing(
    progress: Float,
    target: Int,
    diameter: Dp,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 380f),
        label = "rosary-progress",
    )

    val beadSlots = when {
        target <= 0 -> 33
        target <= 34 -> target
        target <= 100 -> 36
        else -> 48
    }
    val filledBeads = (animatedProgress * beadSlots).roundToInt()

    Box(
        modifier = modifier
            .size(diameter)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val beadRadius = 3.4.dp.toPx()
            val beadOrbit = 15.dp.toPx()

            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val trackCenter = Offset(size.width / 2f, size.height / 2f)

            // Track
            drawArc(
                color = EmeraldOutline,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )

            // Progress sweep
            if (animatedProgress > 0f) {
                drawArc(
                    brush = goldGradientBrush(),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }

            // Beads
            val beadOrbitRadius = (min(size.width, size.height) / 2f) - stroke - beadOrbit
            for (index in 0 until beadSlots) {
                val angle = (-90.0 + index * 360.0 / beadSlots) * (Math.PI / 180.0)
                val position = Offset(
                    x = trackCenter.x + (beadOrbitRadius * cos(angle)).toFloat(),
                    y = trackCenter.y + (beadOrbitRadius * sin(angle)).toFloat(),
                )
                val isFilled = index < filledBeads
                drawCircle(
                    color = if (isFilled) GoldPrimary else EmeraldOutline,
                    radius = if (isFilled) beadRadius * 1.25f else beadRadius,
                    center = position,
                )
                if (isFilled) {
                    drawCircle(
                        color = GoldShimmer.copy(alpha = 0.35f),
                        radius = beadRadius * 2.1f,
                        center = position,
                    )
                }
            }
        }

        content()
    }
}
