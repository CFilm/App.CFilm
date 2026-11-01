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

    // Generar partículas cuyas posiciones objetivo forman la geometría exacta del logo de CFilm
    val particles = remember {
        val list = mutableListOf<LogoParticle>()
        val rand = Random(42)

        // 1. Triángulo Exterior (Play Button Frame)
        for (i in 0..80) {
            val t = i / 80f
            val x: Float
            val y: Float
            if (t < 0.33f) {
                val subT = t * 3f
                x = -120f
                y = -130f + subT * 260f
            } else if (t < 0.66f) {
                val subT = (t - 0.33f) * 3f
                x = -120f + subT * 260f
                y = 130f - subT * 130f
            } else {
                val subT = (t - 0.66f) * 3f
                x = 140f - subT * 260f
                y = 0f - subT * 130f
            }
            addParticle(list, x, y, rand)
        }

        // 2. Letra 'C'
        for (i in 0..60) {
            val angle = (i / 60f) * 1.55f * Math.PI.toFloat() + 0.75f
            val radius = 68f
            val x = -40f + cos(angle) * radius
            val y = sin(angle) * radius
            addParticle(list, x, y, rand)
        }

        // 3. Play Interno dentro de la 'C'
        for (i in 0..35) {
            val t = i / 35f
            val x = -55f + t * 32f
            val y = -22f + t * 44f * (if (t < 0.5f) t * 2f else (1f - t) * 2f)
            addParticle(list, x, y, rand)
        }

        // 4. Letra 'F'
        for (i in 0..60) {
            val t = i / 60f
            val x: Float
            val y: Float
            if (t < 0.45f) {
                // Barra vertical de la F
                x = 38f
                y = -75f + (t / 0.45f) * 150f
            } else if (t < 0.75f) {
                // Barra superior de la F
                val subT = (t - 0.45f) / 0.3f
                x = 38f + subT * 60f
                y = -75f
            } else {
                // Barra del medio de la F
                val subT = (t - 0.75f) / 0.25f
                x = 38f + subT * 48f
                y = 0f
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

            // 1. Partículas convergiendo
            particles.forEach { p ->
                val currentX = centerX + p.startXOffset + (p.targetXOffset - p.startXOffset) * progress
                val currentY = centerY + p.startYOffset + (p.targetYOffset - p.startYOffset) * progress

                val alpha = (p.alphaFactor * (0.2f + 0.8f * progress)).coerceIn(0f, 1f)
                val currentSize = p.size * (1.1f - 0.1f * progress)

                drawCircle(
                    color = neonBlue.copy(alpha = alpha),
                    radius = currentSize,
                    center = Offset(currentX, currentY)
                )
            }

            // 2. Trazos vectoriales exactos del logo al completarse el ensamblaje
            if (progress > 0.85f) {
                val alpha = vectorGlowAlpha.value
                val strokeWidth = 5.5.dp.toPx()

                // Triángulo exterior
                val trianglePath = Path().apply {
                    moveTo(centerX - 120f, centerY - 130f)
                    lineTo(centerX - 120f, centerY + 130f)
                    lineTo(centerX + 140f, centerY)
                    close()
                }
                drawPath(
                    path = trianglePath,
                    color = neonBlue.copy(alpha = alpha),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Letra C
                val cPath = Path().apply {
                    addArc(
                        oval = androidx.compose.ui.geometry.Rect(centerX - 105f, centerY - 68f, centerX + 25f, centerY + 68f),
                        startAngleDegrees = 45f,
                        sweepAngleDegrees = 270f
                    )
                }
                drawPath(
                    path = cPath,
                    color = neonBlue.copy(alpha = alpha),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Play interno en la C
                val playPath = Path().apply {
                    moveTo(centerX - 55f, centerY - 22f)
                    lineTo(centerX - 55f, centerY + 22f)
                    lineTo(centerX - 20f, centerY)
                    close()
                }
                drawPath(
                    path = playPath,
                    color = neonBlue.copy(alpha = alpha),
                    style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Letra F
                val fPath = Path().apply {
                    moveTo(centerX + 38f, centerY - 75f)
                    lineTo(centerX + 38f, centerY + 75f)
                    moveTo(centerX + 38f, centerY - 75f)
                    lineTo(centerX + 98f, centerY - 75f)
                    moveTo(centerX + 38f, centerY)
                    lineTo(centerX + 86f, centerY)
                }
                drawPath(
                    path = fPath,
                    color = neonBlue.copy(alpha = alpha),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        // Resplandor ambiental
        val glowAlpha by animateFloatAsState(
            targetValue = if (assemblyProgress.value > 0.6f) 0.65f else 0f,
            animationSpec = tween(800),
            label = "ambient_glow"
        )

        Box(
            modifier = Modifier
                .size(380.dp)
                .alpha(glowAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            neonBlue.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    )
                )
                .blur(75.dp)
        )
    }
}

private fun addParticle(list: MutableList<LogoParticle>, targetX: Float, targetY: Float, rand: Random) {
    val angle = rand.nextFloat() * 2f * Math.PI.toFloat()
    val distance = rand.nextFloat() * 850f + 300f
    val startX = targetX + cos(angle) * distance
    val startY = targetY + sin(angle) * distance

    list.add(
        LogoParticle(
            startXOffset = startX - targetX,
            startYOffset = startY - targetY,
            targetXOffset = targetX,
            targetYOffset = targetY,
            size = rand.nextFloat() * 4f + 2f,
            alphaFactor = rand.nextFloat() * 0.4f + 0.6f
        )
    )
}
