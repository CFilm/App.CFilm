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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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

    // Generar partículas dispersas que convergen al centro
    val particles = remember {
        val list = mutableListOf<SplashParticle>()
        val rand = Random(42)
        for (i in 0..200) {
            val angle = rand.nextFloat() * 2f * Math.PI.toFloat()
            val distance = rand.nextFloat() * 900f + 300f
            val startX = cos(angle) * distance
            val startY = sin(angle) * distance

            val targetAngle = rand.nextFloat() * 2f * Math.PI.toFloat()
            val targetRadius = rand.nextFloat() * 40f
            val targetX = cos(targetAngle) * targetRadius
            val targetY = sin(targetAngle) * targetRadius

            list.add(
                SplashParticle(
                    startXOffset = startX,
                    startYOffset = startY,
                    targetXOffset = targetX,
                    targetYOffset = targetY,
                    size = rand.nextFloat() * 4.5f + 2f,
                    alphaFactor = rand.nextFloat() * 0.4f + 0.6f
                )
            )
        }
        list
    }

    // Fase 1: Ensamblaje de partículas (0s a 3.5s)
    val assemblyProgress = animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(
            durationMillis = 3500,
            easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
        ),
        label = "assembly"
    )

    // Fase 2: Explosión / Destello de luz al juntarse las partículas (3.5s a 4.0s)
    val flashProgress = animateFloatAsState(
        targetValue = if (assemblyProgress.value >= 0.95f) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "flash"
    )

    // Fase 3: Aparición del logotipo oficial tras la explosión (4.0s en adelante)
    val logoScale = animateFloatAsState(
        targetValue = if (flashProgress.value > 0.5f) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val logoAlpha = animateFloatAsState(
        targetValue = if (flashProgress.value > 0.3f) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "logo_alpha"
    )

    LaunchedEffect(Unit) {
        animationTriggered = true
        delay(6000) // Duración total exacta de 6 segundos
        onAnimationFinished()
    }

    val neonBlue = Color(0xFF00D2FF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Lienzo para las partículas y la explosión
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val progress = assemblyProgress.value
            val flash = flashProgress.value

            // 1. Dibujar partículas convergiendo
            if (progress < 1f) {
                particles.forEach { p ->
                    val currentX = centerX + p.startXOffset + (p.targetXOffset - p.startXOffset) * progress
                    val currentY = centerY + p.startYOffset + (p.targetYOffset - p.startYOffset) * progress
                    val alpha = (p.alphaFactor * (1f - progress * 0.3f)).coerceIn(0f, 1f)

                    drawCircle(
                        color = neonBlue.copy(alpha = alpha),
                        radius = p.size,
                        center = Offset(currentX, currentY)
                    )
                }
            }

            // 2. Onda expansiva de la explosión (Shockwave / Flash)
            if (flash > 0f) {
                val shockwaveRadius = flash * 500f
                val shockwaveAlpha = (1f - flash).coerceIn(0f, 1f)
                drawCircle(
                    color = Color.White.copy(alpha = shockwaveAlpha * 0.8f),
                    radius = shockwaveRadius * 0.5f,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = neonBlue.copy(alpha = shockwaveAlpha),
                    radius = shockwaveRadius,
                    center = Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12.dp.toPx() * (1f - flash))
                )
            }
        }

        // Resplandor ambiental posterior a la explosión
        val glowAlpha by animateFloatAsState(
            targetValue = if (logoAlpha.value > 0f) 0.7f else 0f,
            animationSpec = tween(800),
            label = "glow"
        )

        Box(
            modifier = Modifier
                .size(450.dp)
                .alpha(glowAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            neonBlue.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
                .blur(90.dp)
        )

        // Logotipo oficial que emerge de la explosión
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Logo CFilm",
            modifier = Modifier
                .size(260.dp)
                .scale(logoScale.value)
                .alpha(logoAlpha.value)
        )
    }
}
