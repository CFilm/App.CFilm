package com.example.peliselprimazo.ui.screens.detail.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.peliselprimazo.domain.model.CastMember
import com.example.peliselprimazo.domain.model.Movie
import com.example.peliselprimazo.ui.components.MovieCardItem
import java.util.Locale

@Composable
fun InfoSection(movie: Movie, onTrailerClick: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = movie.title, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (movie.rating > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    Text(text = String.format(Locale.US, "%.1f", movie.rating), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 2.dp))
                }
            }
            Text(text = movie.releaseDate?.take(4) ?: movie.year ?: "", color = Color.Gray, fontSize = 14.sp)
            
            if (!movie.duration.isNullOrBlank()) {
                Text(text = "•", color = Color.Gray)
                Text(text = movie.duration, color = Color.Gray, fontSize = 14.sp)
            }

            if (movie.trailerUrl != null) {
                Surface(
                    onClick = { onTrailerClick(movie.trailerUrl) },
                    modifier = Modifier.height(24.dp),
                    color = Color.Red.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("TRÁILER", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        if (movie.serverLinks.isEmpty()) {
            Surface(
                color = Color.Red.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sin Servidor Disponible", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (movie.genres.isNotEmpty()) {
            Text(text = movie.genres.joinToString(" • "), color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        Text(
            text = movie.overview,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray,
            lineHeight = 22.sp,
            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { isExpanded = !isExpanded }.animateContentSize()
        )
        
        if (movie.overview.length > 100) {
            Text(
                text = if (isExpanded) "Mostrar menos" else "Leer más",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp).clickable { isExpanded = !isExpanded }
            )
        }
    }
}

@Composable
fun CastSection(cast: List<CastMember>) {
    Column {
        Text("Reparto Principal", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(cast) { actor ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                    AsyncImage(
                        model = actor.profilePath,
                        contentDescription = actor.name,
                        modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.DarkGray),
                        contentScale = ContentScale.Crop
                    )
                    Text(text = actor.name, color = Color.White, fontSize = 10.sp, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
fun SimilarContentSection(movies: List<Movie>, onMovieClick: (Int) -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text("Contenido Similar", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(movies, key = { it.id }) { movie ->
                MovieCardItem(movie = movie, onClick = onMovieClick, width = 110.dp)
            }
        }
    }
}
