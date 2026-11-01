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

    // Generar partículas cuyas posiciones objetivo forman la geometría exacta del logo (Triángulo, C, F, Play)
    val particles = remember {
        val list = mutableListOf<LogoParticle>()
        val rand = Random(42) // Semilla fija para consistencia en la silueta

        // 1. Puntos objetivo para el Triángulo exterior
        for (i in 0..70) {
            val t = i / 70f
            val x: Float
            val y: Float
            if (t < 0.33f) {
                val subT = t * 3f
                x = -110f + subT * 0f
                y = -110f + subT * 220f
            } else if (t < 0.66f) {
                val subT = (t - 0.33f) * 3f
                x = -110f + subT * 240f
                y = 110f - subT * 110f
            } else {
                val subT = (t - 0.66f) * 3f
                x = 130f - subT * 240f
                y = 0f - subT * 110f
            }
            addParticle(list, x, y, rand)
        }

        // 2. Puntos objetivo para la letra 'C'
        for (i in 0..55) {
            val angle = (i / 55f) * 1.6f * Math.PI.toFloat() + 0.7f
            val radius = 60f
            val x = -35f + cos(angle) * radius
            val y = sin(angle) * radius
            addParticle(list, x, y, rand)
        }

        // 3. Puntos objetivo para el Play interno en la C
        for (i in 0..30) {
            val t = i / 30f
            val x = -48f + t * 25f
            val y = -18f + t * 36f * (if (t < 0.5f) t * 2f else (1f - t) * 2f)
            addParticle(list, x, y, rand)
        }

        // 4. Puntos objetivo para la letra 'F'
        for (i in 0..55) {
            val t = i / 55f
            val x: Float
            val y: Float
            if (t < 0.4f) {
                // Vertical bar
                x = 35f
                y = -65f + (t / 0.4f) * 130f
            } else if (t < 0.7f) {
                // Top horizontal bar
                val subT = (t - 0.4f) / 0.3f
                x = 35f + subT * 55f
                y = -65f
            } else {
                // Middle horizontal bar
                val subT = (t - 0.7f) / 0.3f
                x = 35f + subT * 42f
                y = 0f
            }
            addParticle(list, x, y, rand)
        }

        list
    }

    // Progreso de ensamblaje (0f = dispersas por el espacio, 1f = forman el vector completo)
    val assemblyProgress = animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(
            durationMillis = 2600,
            easing = CubicBezierEasing(0.15f, 0.85f, 0.15f, 1f)
        ),
        label = "vector_assembly"
    )

    // Brillo e intensidad del vector una vez formado
    val vectorGlowAlpha = animateFloatAsState(
        targetValue = if (assemblyProgress.value > 0.8f) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "vector_glow"
    )

    LaunchedEffect(Unit) {
        animationTriggered = true
        delay(3200) // Duración total antes de pasar al Home
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

            // 1. Dibujar las partículas convergiendo
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

            // 2. Una vez ensamblado (progress >= 0.85f), dibujar los trazos vectoriales limpios del logo exacto
            if (progress > 0.85f) {
                val alpha = vectorGlowAlpha.value
                val strokeWidth = 5.dp.toPx()

                // Path del Triángulo Exterior
                val trianglePath = Path().apply {
                    moveTo(centerX - 110f, centerY - 110f)
                    lineTo(centerX - 110f, centerY + 110f)
                    lineTo(centerX + 130f, centerY)
                    close()
                }
                drawPath(
                    path = trianglePath,
                    color = neonBlue.copy(alpha = alpha),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Path de la C
                val cPath = Path().apply {
                    addArc(
                        oval = androidx.compose.ui.geometry.Rect(centerX - 95f, centerY - 60f, centerX + 25f, centerY + 60f),
                        startAngleDegrees = 45f,
                        sweepAngleDegrees = 270f
                    )
                }
                drawPath(
                    path = cPath,
                    color = neonBlue.copy(alpha = alpha),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Path del Play interno
                val playPath = Path().apply {
                    moveTo(centerX - 48f, centerY - 20f)
                    lineTo(centerX - 48f, centerY + 20f)
                    lineTo(centerX - 18f, centerY)
                    close()
                }
                drawPath(
                    path = playPath,
                    color = neonBlue.copy(alpha = alpha),
                    style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Path de la F
                val fPath = Path().apply {
                    // Vertical
                    moveTo(centerX + 35f, centerY - 65f)
                    lineTo(centerX + 35f, centerY + 65f)
                    // Top bar
                    moveTo(centerX + 35f, centerY - 65f)
                    lineTo(centerX + 90f, centerY - 65f)
                    // Middle bar
                    moveTo(centerX + 35f, centerY)
                    lineTo(centerX + 77f, centerY)
                }
                drawPath(
                    path = fPath,
                    color = neonBlue.copy(alpha = alpha),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        // Resplandor ambiental de luz detrás del logo vectorizado
        val glowAlpha by animateFloatAsState(
            targetValue = if (assemblyProgress.value > 0.6f) 0.6f else 0f,
            animationSpec = tween(800),
            label = "ambient_glow"
        )

        Box(
            modifier = Modifier
                .size(350.dp)
                .alpha(glowAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            neonBlue.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
                .blur(70.dp)
        )
    }
}

private fun addParticle(list: MutableList<LogoParticle>, targetX: Float, targetY: Float, rand: Random) {
    val angle = rand.nextFloat() * 2f * Math.PI.toFloat()
    val distance = rand.nextFloat() * 800f + 300f
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
