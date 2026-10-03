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

// Геометрия кота в тех же координатах, что и в векторной иконке (viewport 108)
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
    // 0 — глаз открыт, 1 — прикрыт
    val wink = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            alpha.animateTo(1f, tween(350, easing = LinearEasing))
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
        // Экран живёт, пока грузятся серверы, поэтому подмигиваем по кругу
        while (true) {
            wink.animateTo(1f, tween(110, easing = FastOutSlowInEasing))
            delay(140)
            wink.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
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
    val unit = size.minDimension / CAT_UNITS

    withTransform({
        translate(size.width / 2f, size.height / 2f)
        scale(unit, unit, pivot = Offset.Zero)
        translate(-CatCenter.x, -CatCenter.y)
    }) {
        val ears = Path().apply {
            moveTo(31f, 52f); lineTo(29f, 27f); lineTo(49f, 37f); close()
            moveTo(77f, 52f); lineTo(79f, 27f); lineTo(59f, 37f); close()
        }
        drawPath(ears, CatColor)
        drawOval(CatColor, Offset(30f, 36f), Size(48f, 42f))

        // Левый глаз всегда открыт, правый подмигивает: сплющивается по вертикали
        drawEye(LEFT_EYE_X, 1f)
        drawEye(RIGHT_EYE_X, 1f - 0.85f * wink)
    }
}

private fun DrawScope.drawEye(cx: Float, openness: Float) {
    val h = EYE_R * openness
    drawOval(BackgroundColor, Offset(cx - EYE_R, EYE_Y - h), Size(EYE_R * 2, h * 2))
}
