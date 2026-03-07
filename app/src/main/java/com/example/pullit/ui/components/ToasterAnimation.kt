package com.example.pullit.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.example.pullit.ui.theme.Primary
import com.example.pullit.ui.theme.PrimaryLight
import kotlinx.coroutines.delay

private enum class ToasterPhase { PRESSING, WAITING, POPPING, FLOATING }

// Toaster colors (light / dark)
private data class ToasterColors(
    val body: Color,
    val front: Color,
    val slot: Color,
    val slotInner: Color,
    val steam: Color,
    val highlight: Color,
    val shadow: Color
)

private val LightColors = ToasterColors(
    body = Color(0xFFF0EBE4),
    front = Color(0xFFE8E2D8),
    slot = Color(0xFF3D2C1E),
    slotInner = Color(0xFF2A1F14),
    steam = Color(0xFFD4C4B0),
    highlight = Color.White.copy(alpha = 0.3f),
    shadow = Color.Black.copy(alpha = 0.08f)
)

private val DarkColors = ToasterColors(
    body = Color(0xFF3D3530),
    front = Color(0xFF332C27),
    slot = Color(0xFF181210),
    slotInner = Color(0xFF0F0C08),
    steam = Color(0xFF5A4F44),
    highlight = Color.White.copy(alpha = 0.08f),
    shadow = Color.Black.copy(alpha = 0.12f)
)

@Composable
fun MiniToasterView(modifier: Modifier = Modifier) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    val cardColor = Primary
    val cardLight = PrimaryLight

    var phase by remember { mutableStateOf(ToasterPhase.PRESSING) }

    // Phase cycle
    LaunchedEffect(Unit) {
        while (true) {
            phase = ToasterPhase.PRESSING
            delay(600)
            phase = ToasterPhase.WAITING
            delay(4000)
            phase = ToasterPhase.POPPING
            delay(600)
            phase = ToasterPhase.FLOATING
            delay(2500)
        }
    }

    val leverDown = phase == ToasterPhase.PRESSING || phase == ToasterPhase.WAITING
    val cardOut = phase == ToasterPhase.POPPING || phase == ToasterPhase.FLOATING

    // Animated properties
    val leverY by animateFloatAsState(
        targetValue = if (leverDown) 8f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "lever"
    )
    val cardY by animateFloatAsState(
        targetValue = if (cardOut) -46f else -8f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "cardY"
    )
    val cardScaleVal by animateFloatAsState(
        targetValue = if (cardOut) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "cardScale"
    )
    val cardRot by animateFloatAsState(
        targetValue = if (cardOut) 0f else -4f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "cardRot"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (cardOut) 1f else 0f,
        animationSpec = tween(300),
        label = "cardAlpha"
    )

    // Gentle bounce during waiting
    val infiniteTransition = rememberInfiniteTransition(label = "toaster")
    val rawBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    val bounceY = if (phase == ToasterPhase.WAITING) rawBounce else 0f

    // Steam timer (continuously increasing, wraps every 6s)
    val steamTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "steam"
    )
    val showSteam = cardOut
    val steamAlpha = if (phase == ToasterPhase.FLOATING) 0.6f else 0.3f

    Canvas(modifier = modifier) {
        val s = size.minDimension / 200f
        val cx = size.width / 2f
        val cy = size.height / 2f

        translate(cx, cy) {
            scale(s, s, pivot = Offset.Zero) {
                // Ground shadow
                drawOval(
                    color = colors.shadow,
                    topLeft = Offset(-60f, 50f),
                    size = Size(120f, 16f)
                )

                // Toaster body group (with bounce)
                translate(0f, bounceY) {
                    drawToasterBody(colors, leverY)
                }

                // Recipe card (animated)
                if (cardAlpha > 0.01f) {
                    translate(0f, cardY) {
                        scale(cardScaleVal, cardScaleVal, pivot = Offset.Zero) {
                            rotate(cardRot, pivot = Offset.Zero) {
                                drawRecipeCard(cardColor, cardLight, cardAlpha)
                            }
                        }
                    }
                }

                // Steam
                if (showSteam) {
                    drawSteamWisp(colors.steam, -18f, steamTime, 0f, steamAlpha)
                    drawSteamWisp(colors.steam, 0f, steamTime, 0.7f, steamAlpha)
                    drawSteamWisp(colors.steam, 18f, steamTime, 1.4f, steamAlpha)
                }
            }
        }
    }
}

private fun DrawScope.drawToasterBody(c: ToasterColors, leverY: Float) {
    // Body
    drawRoundRect(
        color = c.body,
        topLeft = Offset(-60f, -15f),
        size = Size(120f, 70f),
        cornerRadius = CornerRadius(16f)
    )
    // Front face
    drawRoundRect(
        color = c.front,
        topLeft = Offset(-60f, -5f),
        size = Size(120f, 60f),
        cornerRadius = CornerRadius(14f)
    )
    // Top surface
    drawRoundRect(
        color = c.body,
        topLeft = Offset(-58f, -15f),
        size = Size(116f, 14f),
        cornerRadius = CornerRadius(7f)
    )
    // Slot
    drawRoundRect(
        color = c.slot,
        topLeft = Offset(-35f, -18f),
        size = Size(70f, 10f),
        cornerRadius = CornerRadius(5f)
    )
    // Slot inner
    drawRoundRect(
        color = c.slotInner,
        topLeft = Offset(-32f, -16f),
        size = Size(64f, 6f),
        cornerRadius = CornerRadius(3f)
    )
    // Highlight
    drawRoundRect(
        color = c.highlight,
        topLeft = Offset(-50f, -10.5f),
        size = Size(80f, 3f),
        cornerRadius = CornerRadius(1.5f)
    )
    // Lever
    translate(0f, leverY) {
        // Arm
        drawRoundRect(
            color = c.steam,
            topLeft = Offset(58f, 0f),
            size = Size(6f, 16f),
            cornerRadius = CornerRadius(3f)
        )
        // Knob
        drawCircle(
            color = c.body,
            radius = 4f,
            center = Offset(61f, 0f)
        )
        drawCircle(
            color = c.steam,
            radius = 4f,
            center = Offset(61f, 0f),
            style = Stroke(width = 1.5f)
        )
    }
}

private fun DrawScope.drawRecipeCard(cardColor: Color, cardLight: Color, alpha: Float) {
    // Card shadow
    drawRoundRect(
        color = cardColor.copy(alpha = 0.15f * alpha),
        topLeft = Offset(-26f, -48f),
        size = Size(56f, 52f),
        cornerRadius = CornerRadius(6f)
    )
    // Card body
    drawRoundRect(
        color = cardColor.copy(alpha = alpha),
        topLeft = Offset(-28f, -50f),
        size = Size(56f, 52f),
        cornerRadius = CornerRadius(6f)
    )
    // Light overlay (top half)
    drawRoundRect(
        color = cardLight.copy(alpha = 0.4f * alpha),
        topLeft = Offset(-28f, -50f),
        size = Size(56f, 26f),
        cornerRadius = CornerRadius(6f)
    )
    // Text lines
    val lineColor = Color.White.copy(alpha = 0.45f * alpha)
    val lineWidths = listOf(36f, 32f, 28f, 34f)
    lineWidths.forEachIndexed { i, w ->
        drawRoundRect(
            color = lineColor,
            topLeft = Offset(-w / 2f, -35f + i * 8f),
            size = Size(w, 2f),
            cornerRadius = CornerRadius(1f)
        )
    }
}

private fun DrawScope.drawSteamWisp(
    steamColor: Color, baseX: Float, time: Float, delay: Float, maxOpacity: Float
) {
    val cycle = ((time + delay) % 2f) / 2f
    val yShift = -cycle * 24f
    val alpha = when {
        cycle < 0.2f -> cycle * 2.5f
        cycle < 0.5f -> 0.5f - cycle * 0.6f
        else -> maxOf(0f, (1f - cycle) * 0.15f)
    } * maxOpacity
    if (alpha < 0.01f) return

    translate(0f, yShift) {
        val path = Path().apply {
            moveTo(baseX, -48f)
            quadraticTo(baseX - 4f, -58f, baseX + 2f, -68f)
            quadraticTo(baseX + 6f, -78f, baseX - 1f, -88f)
        }
        drawPath(
            path = path,
            color = steamColor.copy(alpha = alpha),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
    }
}
