package com.example.peliselprimazo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.peliselprimazo.domain.model.Movie
import java.util.Locale

@Composable
fun WatchingCardItem(movie: Movie, onClick: () -> Unit) {
    Box(modifier = Modifier.width(200.dp).clickable { onClick() }) {
        Card(modifier = Modifier.height(110.dp).fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
            Box {
                AsyncImage(model = movie.backdropUrl ?: movie.posterUrl, contentDescription = movie.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                Icon(imageVector = Icons.Default.PlayCircleOutline, contentDescription = null, modifier = Modifier.size(36.dp).align(Alignment.Center), tint = Color.White)
                if (movie.totalDuration > 0) {
                    val progressRatio = movie.lastPosition.toFloat() / movie.totalDuration.toFloat()
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.Gray.copy(alpha = 0.5f)).align(Alignment.BottomStart)) {
                        Box(modifier = Modifier.fillMaxWidth(progressRatio.coerceIn(0f, 1f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalMovieGrid(movies: List<Movie>, onMovieClick: (Int) -> Unit, topPadding: Dp = 80.dp) {
    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = topPadding, start = 8.dp, end = 8.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(movies, key = { it.id }) { movie -> MovieCardItem(movie, { onMovieClick(it) }) }
    }
}

@Composable
fun SearchGrid(movies: List<Movie>, onMovieClick: (Int) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 16.dp, start = 8.dp, end = 8.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(movies, key = { it.id }) { movie -> MovieCardItem(movie, { onMovieClick(it) }) }
    }
}

@Composable
fun MovieCardItem(movie: Movie, onClick: (Int) -> Unit, width: Dp? = null) {
    val modifier = if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()
    val context = LocalContext.current
    Box(modifier = modifier.clickable { onClick(movie.id) }) {
        Card(modifier = Modifier.aspectRatio(0.7f), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
            Box {
                AsyncImage(model = ImageRequest.Builder(context).data(movie.posterUrl).crossfade(true).diskCachePolicy(CachePolicy.ENABLED).memoryCachePolicy(CachePolicy.ENABLED).build(), contentDescription = movie.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                if (movie.rating > 0) { Surface(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp), color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp)) { Text(text = String.format(Locale.US, "%.1f", movie.rating), color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) } }
                if (movie.totalDuration > 0 && movie.lastPosition > 0) {
                    val progressRatio = (movie.lastPosition.toFloat() / movie.totalDuration.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.Gray.copy(alpha = 0.5f))
                            .align(Alignment.BottomStart)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressRatio)
                                .fillMaxHeight()
                                .background(Color.Red)
                        )
                    }
                }
            }
        }
    }
}
