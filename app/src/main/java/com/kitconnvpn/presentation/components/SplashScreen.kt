package com.kitconnvpn.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CatColor = Color(0xFF00E5FF)
private val BackgroundColor = Color(0xFF0C0D14)
private val CatCenter = Offset(54f, 52.5f)
private const val CAT_UNITS = 60f
private const val EYE_Y = 57f
private const val EYE_R = 2.8f
private const val LEFT_EYE_X = 45.2f
private const val RIGHT_EYE_X = 62.8f

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    val wink = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    delayMillis = 350,
                    easing = LinearEasing
                )
            )
        }
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        delay(750)
        while (true) {
            wink.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    delayMillis = 110,
                    easing = FastOutSlowInEasing
                )
            )
            delay(140)
            wink.animateTo(
                targetValue =0f,
                animationSpec = tween(
                    delayMillis = 160,
                    easing = FastOutSlowInEasing
                )
            )
            delay(1900)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
        ) {
            drawCat(wink.value)
        }
    }
}

private fun DrawScope.drawCat(wink: Float) {
    withTransform(transformBlock = {
        translate(
            left = size.width / 2f,
            top = size.height / 2f
        )
        scale(
            scaleX = size.minDimension / CAT_UNITS,
            scaleY = size.minDimension / CAT_UNITS,
            pivot = Offset.Zero
        )
        translate(
            left = -CatCenter.x,
            top = -CatCenter.y
        )
    }) {
        val ears = Path().apply {
            moveTo(x = 31f, y = 52f);
            lineTo(x = 29f, y = 27f);
            lineTo(x = 49f, y = 37f); close()
            moveTo(x = 77f, y = 52f);
            lineTo(x = 79f, y = 27f);
            lineTo(x = 59f, y = 37f); close()
        }
        drawPath(
            path = ears,
            color = CatColor
        )
        drawOval(
            color = CatColor,
            topLeft = Offset(
                x = 30f,
                y = 36f
            ),
            size = Size(width = 48f, height = 42f)
        )
        drawEye(
            cx = LEFT_EYE_X,
            openness = 1f
        )
        drawEye(
            cx = RIGHT_EYE_X,
            openness = 1f - 0.85f * wink
        )
    }
}

private fun DrawScope.drawEye(cx: Float, openness: Float) {
    val h = EYE_R * openness
    drawOval(
        color = BackgroundColor,
        topLeft = Offset(cx - EYE_R, EYE_Y - h), Size(EYE_R * 2, h * 2)
    )
}
