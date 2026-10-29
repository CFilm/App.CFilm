package com.example.peliselprimazo.ui.screens.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.peliselprimazo.domain.model.Movie

@Composable
fun HeaderSection(movie: Movie, isServerOnline: Boolean, onPlayClick: () -> Unit) {
    Box(modifier = Modifier.height(300.dp).fillMaxWidth()) {
        AsyncImage(
            model = movie.backdropUrl ?: movie.posterUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black), startY = 300f)))
        
        if (isServerOnline) {
            Surface(
                modifier = Modifier.size(64.dp).align(Alignment.Center).clickable { onPlayClick() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Reproducir", modifier = Modifier.size(40.dp), tint = Color.Black)
                }
            }
        } else {
            Surface(
                modifier = Modifier.height(48.dp).padding(horizontal = 16.dp).align(Alignment.Center),
                shape = RoundedCornerShape(24.dp),
                color = Color.DarkGray.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, Color.Red)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                    Text(text = "Servidor no disponible", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun UserActionRow(
    isFavorito: Boolean,
    isViendo: Boolean,
    isTerminado: Boolean,
    isVerDespues: Boolean,
    onFavoritoToggle: () -> Unit,
    onViendoToggle: () -> Unit,
    onTerminadoToggle: () -> Unit,
    onVerDespuesToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DetailActionButton(
            icon = if (isFavorito) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, 
            label = "Favoritos", 
            selected = isFavorito, 
            selectedColor = Color(0xFFE50914),
            onClick = onFavoritoToggle
        )
        DetailActionButton(
            icon = Icons.Rounded.PlayCircle, 
            label = "Viendo", 
            selected = isViendo, 
            selectedColor = Color(0xFF2196F3),
            onClick = onViendoToggle
        )
        DetailActionButton(
            icon = Icons.Rounded.CheckCircle, 
            label = "Terminados", 
            selected = isTerminado, 
            selectedColor = Color(0xFF4CAF50),
            onClick = onTerminadoToggle
        )
        DetailActionButton(
            icon = if (isVerDespues) Icons.Rounded.WatchLater else Icons.Outlined.WatchLater, 
            label = "Ver Después", 
            selected = isVerDespues, 
            selectedColor = Color(0xFFFF9800),
            onClick = onVerDespuesToggle
        )
    }
}

@Composable
fun DetailActionButton(
    icon: ImageVector, 
    label: String, 
    selected: Boolean, 
    selectedColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, 
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = label, 
            tint = if (selected) selectedColor else Color.White, 
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(text = label, color = if (selected) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
