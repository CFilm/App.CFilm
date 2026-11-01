package com.example.peliselprimazo.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class LogoParticle(
    val startXOffset: Float,
    val startYOffset: Float,
    val targetXOffset: Float,
    val targetYOffset: Float,
    val size: Float,
    val alphaFactor: Float
)

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    var animationTriggered by remember { mutableStateOf(false) }

    // Generar partículas con alta densidad para formar exactamente el diseño robusto del logo
    val particles = remember {
        val list = mutableListOf<LogoParticle>()
        val rand = Random(42)

        // 1. Triángulo Exterior (Play Frame)
        for (i in 0..100) {
            val t = i / 100f
            val x: Float
            val y: Float
            if (t < 0.33f) {
                val subT = t * 3f
                x = -150f
                y = -150f + subT * 300f
            } else if (t < 0.66f) {
                val subT = (t - 0.33f) * 3f
                x = -150f + subT * 330f
                y = 150f - subT * 150f
            } else {
                val subT = (t - 0.66f) * 3f
                x = 180f - subT * 330f
                y = 0f - subT * 150f
            }
            addParticle(list, x, y, rand)
        }

        // 2. Letra 'C' gruesa
        for (i in 0..80) {
            val angle = (i / 80f) * 1.55f * Math.PI.toFloat() + 0.78f
            val radius = 75f
            val x = -45f + cos(angle) * radius
            val y = sin(angle) * radius
            addParticle(list, x, y, rand)
        }

        // 3. Play Interno dentro de la 'C'
        for (i in 0..40) {
            val t = i / 40f
            val x = -65f + t * 30f
            val y = -25f + t * 50f * (if (t < 0.5f) t * 2f else (1f - t) * 2f)
            addParticle(list, x, y, rand)
        }

        // 4. Letra 'F' robusta
        for (i in 0..80) {
            val t = i / 80f
            val x: Float
            val y: Float
            if (t < 0.45f) {
                // Vertical bar
                x = 35f
                y = -70f + (t / 0.45f) * 140f
            } else if (t < 0.75f) {
                // Top horizontal bar
                val subT = (t - 0.45f) / 0.3f
                x = 35f + subT * 70f
                y = -70f
            } else {
                // Middle horizontal bar
                val subT = (t - 0.75f) / 0.25f
                x = 35f + subT * 55f
                y = -5f
            }
            addParticle(list, x, y, rand)
        }

        list
    }

    val assemblyProgress = animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(
            durationMillis = 2600,
            easing = CubicBezierEasing(0.15f, 0.85f, 0.15f, 1f)
        ),
        label = "vector_assembly"
    )

    val vectorGlowAlpha = animateFloatAsState(
        targetValue = if (assemblyProgress.value > 0.8f) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "vector_glow"
    )

    LaunchedEffect(Unit) {
        animationTriggered = true
        delay(3200)
        onAnimationFinished()
    }

    val neonBlue = Color(0xFF00D2FF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f
            val progress = assemblyProgress.value

            // 1. Partículas convergiendo con mayor brillo y tamaño
            particles.forEach { p ->
                val currentX = centerX + p.startXOffset + (p.targetXOffset - p.startXOffset) * progress
                val currentY = centerY + p.startYOffset + (p.targetYOffset - p.startYOffset) * progress

                val alpha = (p.alphaFactor * (0.3f + 0.7f * progress)).coerceIn(0f, 1f)
                val currentSize = p.size * (1.2f - 0.2f * progress)

                drawCircle(
                    color = neonBlue.copy(alpha = alpha),
                    radius = currentSize,
                    center = Offset(currentX, currentY)
                )
            }

            // 2. Trazos vectoriales exactos, robustos y brillantes al completarse el ensamblaje
            if (progress > 0.85f) {
                val alpha = vectorGlowAlpha.value
                val strokeWidth = 10.dp.toPx() // Trazos gruesos idénticos a la expectativa

                // Triángulo Exterior
                val trianglePath = Path().apply {
                    moveTo(centerX - 150f, centerY - 150f)
                    lineTo(centerX - 150f, centerY + 150f)
                    lineTo(centerX + 180f, centerY)
                    close()
                }
                drawPath(
                    path = trianglePath,
                    color = neonBlue.copy(alpha = alpha),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Letra C gruesa
                val cPath = Path().apply {
                    addArc(
                        oval = androidx.compose.ui.geometry.Rect(centerX - 120f, centerY - 75f, centerX + 20f, centerY + 75f),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = 180f
                    )
                }
                drawPath(
                    path = cPath,
                    color = neonBlue.copy(alpha = alpha),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Play interno sólido en la C
                val playPath = Path().apply {
                    moveTo(centerX - 65f, centerY - 25f)
                    lineTo(centerX - 65f, centerY + 25f)
                    lineTo(centerX - 35f, centerY)
                    close()
                }
                drawPath(
                    path = playPath,
                    color = neonBlue.copy(alpha = alpha),
                    style = Stroke(width = strokeWidth * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Letra F robusta
                val fPath = Path().apply {
                    // Barra vertical
                    moveTo(centerX + 35f, centerY - 70f)
                    lineTo(centerX + 35f, centerY + 70f)
                    // Barra superior
                    moveTo(centerX + 35f, centerY - 70f)
                    lineTo(centerX + 105f, centerY - 70f)
                    // Barra del medio
                    moveTo(centerX + 35f, centerY - 5f)
                    lineTo(centerX + 90f, centerY - 5f)
                }
                drawPath(
                    path = fPath,
                    color = neonBlue.copy(alpha = alpha),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        // Resplandor ambiental de alta intensidad
        val glowAlpha by animateFloatAsState(
            targetValue = if (assemblyProgress.value > 0.6f) 0.8f else 0f,
            animationSpec = tween(800),
            label = "ambient_glow"
        )

        Box(
            modifier = Modifier
                .size(450.dp)
                .alpha(glowAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            neonBlue.copy(alpha = 0.65f),
                            Color.Transparent
                        )
                    )
                )
                .blur(80.dp)
        )
    }
}

private fun addParticle(list: MutableList<LogoParticle>, targetX: Float, targetY: Float, rand: Random) {
    val angle = rand.nextFloat() * 2f * Math.PI.toFloat()
    val distance = rand.nextFloat() * 900f + 350f
    val startX = targetX + cos(angle) * distance
    val startY = targetY + sin(angle) * distance

    list.add(
        LogoParticle(
            startXOffset = startX - targetX,
            startYOffset = startY - targetY,
            targetXOffset = targetX,
            targetYOffset = targetY,
            size = rand.nextFloat() * 5f + 2.5f,
            alphaFactor = rand.nextFloat() * 0.4f + 0.6f
        )
    )
}
