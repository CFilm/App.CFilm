package com.example.peliselprimazo.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
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

    // Generar partículas una sola vez con posiciones relativas al centro
    val particles = remember {
        List(180) { i ->
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val distance = Random.nextFloat() * 800f + 300f
            val startOffsetX = cos(angle) * distance
            val startOffsetY = sin(angle) * distance

            val targetAngle = (i.toFloat() / 180) * 2f * Math.PI.toFloat()
            val targetRadius = Random.nextFloat() * 110f + 25f
            val targetOffsetX = cos(targetAngle) * targetRadius
            val targetOffsetY = sin(targetAngle) * targetRadius

            SplashParticle(
                startXOffset = startOffsetX,
                startYOffset = startOffsetY,
                targetXOffset = targetOffsetX,
                targetYOffset = targetOffsetY,
                size = Random.nextFloat() * 4.5f + 2f,
                alphaFactor = Random.nextFloat() * 0.5f + 0.5f
            )
        }
    }

    // Progreso de ensamblaje de partículas (0f = dispersas por la pantalla, 1f = unidas formando el logo)
    val assemblyProgress = animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(
            durationMillis = 2400,
            easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
        ),
        label = "particle_assembly"
    )

    // Aparición y escala final del logotipo definitivo una vez ensamblado
    val logoScale = animateFloatAsState(
        targetValue = if (assemblyProgress.value > 0.8f) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_final_scale"
    )

    val logoAlpha = animateFloatAsState(
        targetValue = if (assemblyProgress.value > 0.7f) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "logo_final_alpha"
    )

    LaunchedEffect(Unit) {
        animationTriggered = true
        delay(3000) // Tiempo total de intro cinemática
        onAnimationFinished()
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Lienzo para simular las partículas magnetizándose hacia el centro
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f
            val progress = assemblyProgress.value

            particles.forEach { p ->
                val currentX = centerX + p.startXOffset + (p.targetXOffset - p.startXOffset) * progress
                val currentY = centerY + p.startYOffset + (p.targetYOffset - p.startYOffset) * progress
                
                val currentAlpha = (p.alphaFactor * (0.3f + 0.7f * progress)).coerceIn(0f, 1f)
                val currentSize = p.size * (1.2f - 0.2f * progress)

                drawCircle(
                    color = primaryColor.copy(alpha = currentAlpha),
                    radius = currentSize,
                    center = Offset(currentX, currentY)
                )
            }
        }

        // Resplandor ambiental central cuando las partículas convergen
        val glowAlpha by animateFloatAsState(
            targetValue = if (assemblyProgress.value > 0.5f) 0.75f else 0f,
            animationSpec = tween(800),
            label = "glow_alpha"
        )

        Box(
            modifier = Modifier
                .size(400.dp)
                .alpha(glowAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
                .blur(80.dp)
        )

        // Logotipo final que aparece resplandeciente cuando las partículas terminan de unirse
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Logo CFilm",
            modifier = Modifier
                .size(220.dp)
                .scale(logoScale.value)
                .alpha(logoAlpha.value)
        )
    }
}
