package com.kitconnvpn.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Отношение сторон полотна флага: круг показывает его центральную часть
private const val FLAG_RATIO = 1.5f

/**
 * Круглый флаг, нарисованный в Canvas. Код страны берётся из эмодзи-флага,
 * для стран без нарисованного флага остаётся эмодзи в круге.
 */
@Composable
fun CountryFlag(
    flagEmoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val code = flagEmoji.toCountryCode()
    val painter = code?.let { flagPainters[it] }

    if (painter == null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF232739)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = flagEmoji, fontSize = (size.value * 0.68f).sp)
        }
        return
    }

    Canvas(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    ) {
        val h = this.size.height
        val w = h * FLAG_RATIO
        // Сдвиг полотна: для большинства флагов — центр, у США слева, чтобы был виден синий угол
        val left = -(w - this.size.width) * painter.viewportStart
        painter.draw(this, left, w, h)
    }
}

private class FlagPainter(val viewportStart: Float = 0.5f, val draw: DrawScope.(Float, Float, Float) -> Unit)

private fun String.toCountryCode(): String? {
    val points = codePoints().toArray()
    if (points.size != 2) return null
    val chars = points.map { it - 0x1F1E6 + 'A'.code }
    if (chars.any { it !in 'A'.code..'Z'.code }) return null
    return chars.joinToString("") { it.toChar().toString() }
}

private fun DrawScope.horizontal(left: Float, w: Float, h: Float, colors: List<Color>) {
    val band = h / colors.size
    colors.forEachIndexed { i, c ->
        // +1 пиксель, чтобы между полосами не было щелей от округления
        drawRect(c, Offset(left, band * i), Size(w, band + 1f))
    }
}

private fun DrawScope.vertical(left: Float, w: Float, h: Float, colors: List<Color>) {
    val band = w / colors.size
    colors.forEachIndexed { i, c ->
        drawRect(c, Offset(left + band * i, 0f), Size(band + 1f, h))
    }
}

private val flagPainters: Map<String, FlagPainter> = mapOf(
    "DE" to FlagPainter { l, w, h ->
        horizontal(l, w, h, listOf(Color(0xFF000000), Color(0xFFDD0000), Color(0xFFFFCE00)))
    },
    "IT" to FlagPainter { l, w, h ->
        vertical(l, w, h, listOf(Color(0xFF009246), Color.White, Color(0xFFCE2B37)))
    },
    "PL" to FlagPainter { l, w, h ->
        horizontal(l, w, h, listOf(Color.White, Color(0xFFDC143C)))
    },
    "NL" to FlagPainter { l, w, h ->
        horizontal(l, w, h, listOf(Color(0xFFAE1C28), Color.White, Color(0xFF21468B)))
    },
    "FR" to FlagPainter { l, w, h ->
        vertical(l, w, h, listOf(Color(0xFF0055A4), Color.White, Color(0xFFEF4135)))
    },
    "RU" to FlagPainter { l, w, h ->
        horizontal(l, w, h, listOf(Color.White, Color(0xFF0039A6), Color(0xFFD52B1E)))
    },
    "US" to FlagPainter(viewportStart = 0f) { l, w, h ->
        val red = Color(0xFFB22234)
        val stripe = h / 13f
        drawRect(Color.White, Offset(l, 0f), Size(w, h))
        for (i in 0 until 13 step 2) {
            drawRect(red, Offset(l, stripe * i), Size(w, stripe + 1f))
        }
        val cantonW = w * 0.45f
        val cantonH = stripe * 7
        drawRect(Color(0xFF3C3B6E), Offset(l, 0f), Size(cantonW, cantonH))
        // Звёзды упрощены до точек: на таком размере звезда всё равно не читается
        val rows = 4
        val cols = 5
        val dot = stripe * 0.32f
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                drawCircle(
                    Color.White,
                    dot,
                    Offset(l + cantonW * (c + 0.5f) / cols, cantonH * (r + 0.5f) / rows)
                )
            }
        }
    }
)
