package com.example.peliselprimazo.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.peliselprimazo.R
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class SplashParticle(
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
    var isExiting by remember { mutableStateOf(false) }

    val particles = remember {
        val list = mutableListOf<SplashParticle>()
        val rand = Random(42)
        for (i in 0..240) {
            val angle = rand.nextFloat() * 2f * Math.PI.toFloat()
            val distance = rand.nextFloat() * 1100f + 450f
            val startX = cos(angle) * distance
            val startY = sin(angle) * distance

            val targetAngle = rand.nextFloat() * 2f * Math.PI.toFloat()
            val targetRadius = rand.nextFloat() * 45f
            val targetX = cos(targetAngle) * targetRadius
            val targetY = sin(targetAngle) * targetRadius

            list.add(
                SplashParticle(
                    startXOffset = startX,
                    startYOffset = startY,
                    targetXOffset = targetX,
                    targetYOffset = targetY,
                    size = rand.nextFloat() * 4f + 2f,
                    alphaFactor = rand.nextFloat() * 0.4f + 0.6f
                )
            )
        }
        list
    }

    // Fase 1: Ensamblaje de partículas más pausado y cinematográfico (4.2s)
    val assemblyProgress = animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(
            durationMillis = 4200,
            easing = CubicBezierEasing(0.25f, 0.75f, 0.25f, 1f)
        ),
        label = "assembly"
    )

    // Fase 2: Explosión / Onda expansiva (0.6s)
    val flashProgress = animateFloatAsState(
        targetValue = if (assemblyProgress.value >= 0.95f) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "flash"
    )

    // Fase 3: Logotipo visible (sostener 1.2s antes de salir)
    val logoScale = animateFloatAsState(
        targetValue = if (flashProgress.value > 0.4f) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val logoAlpha = animateFloatAsState(
        targetValue = if (flashProgress.value > 0.2f) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "logo_alpha"
    )

    // Animación de salida (0.5s final hacia el Home)
    val exitAlpha = animateFloatAsState(
        targetValue = if (isExiting) 0f else 1f,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "exit_alpha"
    )

    val exitScale = animateFloatAsState(
        targetValue = if (isExiting) 1.15f else 1f,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "exit_scale"
    )

    LaunchedEffect(Unit) {
        animationTriggered = true
        delay(6000) // 4.2s + 0.6s + 1.2s = 6.0s antes de iniciar el desvanecimiento
        isExiting = true
        delay(500) // 0.5s de salida = 6.5 segundos totales exactos
        onAnimationFinished()
    }

    val neonBlue = Color(0xFF00D2FF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(exitAlpha.value)
            .scale(exitScale.value),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val progress = assemblyProgress.value
            val flash = flashProgress.value

            if (progress < 1f) {
                particles.forEach { p ->
                    val currentX = centerX + p.startXOffset + (p.targetXOffset - p.startXOffset) * progress
                    val currentY = centerY + p.startYOffset + (p.targetYOffset - p.startYOffset) * progress
                    val alpha = (p.alphaFactor * (1f - progress * 0.2f)).coerceIn(0f, 1f)

                    drawCircle(
                        color = neonBlue.copy(alpha = alpha),
                        radius = p.size,
                        center = Offset(currentX, currentY)
                    )
                }
            }

            if (flash > 0f) {
                val shockwaveRadius = flash * Math.max(size.width, size.height) * 1.3f
                val shockwaveAlpha = (1f - flash).coerceIn(0f, 1f)
                drawCircle(
                    color = Color.White.copy(alpha = shockwaveAlpha * 0.95f),
                    radius = shockwaveRadius * 0.7f,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = neonBlue.copy(alpha = shockwaveAlpha),
                    radius = shockwaveRadius,
                    center = Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 24.dp.toPx() * (1f - flash))
                )
            }
        }

        Image(
            painter = painterResource(id = R.mipmap.app_logo),
            contentDescription = "Logo CFilm",
            modifier = Modifier
                .size(260.dp)
                .scale(logoScale.value)
                .alpha(logoAlpha.value)
        )
    }
}
