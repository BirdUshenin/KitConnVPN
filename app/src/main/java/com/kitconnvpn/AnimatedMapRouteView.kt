package com.kitconnvpn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnimatedMapRouteView(
    originCity: String = "Моя локация (RU)",
    destCountry: String,
    destFlag: String,
    progress: Float,
    isSuccessFlash: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = Color(0xFF0C0D14)
    val cyan = Color(0xFF00E5FF)
    val purple = Color(0xFF7C4DFF)
    val pink = Color(0xFFFF007F)
    val successGreen = Color(0xFF00E676)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val gridStep = 48.dp.toPx()
            val gridColor = Color(0xFF1B1E2E).copy(alpha = 0.5f)

            var x = 0f
            while (x < w) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1.dp.toPx()
                )
                x += gridStep
            }

            var y = 0f
            while (y < h) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += gridStep
            }

            // 2. Map node points
            val p1 = Offset(w * 0.22f, h * 0.28f)
            val p2 = Offset(w * 0.45f, h * 0.42f)
            val p3 = Offset(w * 0.38f, h * 0.60f)
            val p4 = Offset(w * 0.78f, h * 0.72f)

            // Construct smooth route path
            val routePath = Path().apply {
                moveTo(p1.x, p1.y)
                cubicTo(p1.x + 80f, p1.y + 120f, p2.x - 60f, p2.y - 40f, p2.x, p2.y)
                cubicTo(p2.x + 60f, p2.y + 60f, p3.x - 80f, p3.y - 20f, p3.x, p3.y)
                cubicTo(p3.x + 120f, p3.y + 80f, p4.x - 100f, p4.y - 60f, p4.x, p4.y)
            }

            val pathMeasure = PathMeasure()
            pathMeasure.setPath(routePath, false)
            val totalLength = pathMeasure.length

            drawPath(
                path = routePath,
                color = Color(0xFF262B3F),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            if (progress > 0f) {
                val animatedSegment = Path()
                pathMeasure.getSegment(0f, totalLength * progress, animatedSegment, true)

                val routeColor = if (isSuccessFlash) {
                    successGreen
                } else {
                    cyan
                }

                val strokeBrush = if (isSuccessFlash) {
                    Brush.linearGradient(listOf(successGreen, successGreen))
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(cyan, purple, pink),
                        startX = 0f,
                        endX = w
                    )
                }

                drawPath(
                    path = animatedSegment,
                    brush = strokeBrush,
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )

                if (progress < 1f || isSuccessFlash) {
                    val currentPos = pathMeasure.getPosition(totalLength * progress)
                    drawCircle(
                        color = routeColor.copy(alpha = 0.35f),
                        radius = 16.dp.toPx(),
                        center = currentPos
                    )
                    drawCircle(
                        color = routeColor,
                        radius = 7.dp.toPx(),
                        center = currentPos
                    )
                }
            }

            val nodes = listOf(p1, p2, p3, p4)
            nodes.forEachIndexed { idx, point ->
                val nodeReached = progress >= (idx.toFloat() / (nodes.size - 1))
                val nodeColor = when {
                    isSuccessFlash -> successGreen
                    nodeReached -> cyan
                    else -> Color(0xFF262B3F)
                }

                drawCircle(
                    color = nodeColor.copy(alpha = 0.3f),
                    radius = 10.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = nodeColor,
                    radius = 5.dp.toPx(),
                    center = point
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF161824).copy(alpha = 0.92f),
                border = BorderStroke(
                    1.dp,
                    if (isSuccessFlash) successGreen else Color(0xFF262A3E)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isSuccessFlash) "Маршрут установлен!" else "Построение маршрута...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccessFlash) successGreen else Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$originCity  ➔  $destFlag $destCountry",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A93A6)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isSuccessFlash) successGreen else cyan,
                                shape = CircleShape
                            )
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF161824).copy(alpha = 0.95f),
                border = BorderStroke(
                    1.dp,
                    if (isSuccessFlash) successGreen.copy(alpha = 0.5f) else Color(0xFF262A3E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSuccessFlash) "ПОДКЛЮЧЕНО" else "СОЕДИНЕНИЕ С СЕРВЕРОМ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (isSuccessFlash) successGreen else cyan
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color(0xFF222638), shape = CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(8.dp)
                                .background(
                                    brush = if (isSuccessFlash) {
                                        Brush.linearGradient(listOf(successGreen, successGreen))
                                    } else {
                                        Brush.horizontalGradient(listOf(cyan, purple, pink))
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}