package com.example.peliselprimazo.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.peliselprimazo.desktop.data.DesktopRepository
import com.example.peliselprimazo.domain.model.ContentType
import com.example.peliselprimazo.domain.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AsyncImageDesktop(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val stream = java.net.URL(url).openStream()
                    bitmap = androidx.compose.ui.res.loadImageBitmap(stream)
                } catch (_: Exception) {}
            }
        }
    }

    Box(modifier = modifier.background(Color(0xFF18181B))) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF27272A)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
@Preview
fun DesktopApp() {
    val repository = remember { DesktopRepository() }
    val scope = rememberCoroutineScope()
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMovie by remember { mutableStateOf<Movie?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            movies = repository.fetchMovies()
            isLoading = false
        }
    }

    val filteredMovies = remember(movies, searchQuery, selectedTab) {
        val result = if (searchQuery.isBlank()) movies else movies.filter { it.title.contains(searchQuery, ignoreCase = true) }
        when (selectedTab) {
            1 -> result.filter { it.contentType == ContentType.MOVIE }
            2 -> result.filter { it.contentType == ContentType.TV }
            3 -> result.filter { it.contentType == ContentType.ANIME }
            else -> result
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF9333EA))) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF09090B)) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        color = Color(0xFF09090B),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CFilm",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )

                            // Search Field
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Buscar...", color = Color.Gray, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.width(320.dp).height(42.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF27272A),
                                    focusedContainerColor = Color(0xFF18181B),
                                    unfocusedContainerColor = Color(0xFF18181B)
                                )
                            )
                        }
                    }

                    // Navigation Tabs (Inicio, Películas, Series, Anime)
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF09090B),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    ) {
                        listOf("Inicio", "Películas", "Series", "Anime").forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 14.sp,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == index) Color.White else Color.Gray
                                    )
                                }
                            )
                        }
                    }

                    // Content Area
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (filteredMovies.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontró contenido disponible", color = Color.Gray, fontSize = 15.sp)
                        }
                    } else if (selectedTab == 0 && searchQuery.isBlank()) {
                        // Home View with Sections
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
                            val popularMovies = movies.filter { it.contentType == ContentType.MOVIE }.take(10)
                            val popularSeries = movies.filter { it.contentType == ContentType.TV || it.contentType == ContentType.ANIME }.take(10)

                            if (popularMovies.isNotEmpty()) {
                                Text(
                                    text = "Películas Populares",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(popularMovies) { movie ->
                                        DesktopMovieCard(movie = movie, onClick = { selectedMovie = movie }, width = 140.dp)
                                    }
                                }
                            }

                            if (popularSeries.isNotEmpty()) {
                                Text(
                                    text = "Series y Anime Destacados",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(popularSeries) { movie ->
                                        DesktopMovieCard(movie = movie, onClick = { selectedMovie = movie }, width = 140.dp)
                                    }
                                }
                            }
                        }
                    } else {
                        // Grid View for Tabs / Search
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredMovies, key = { it.id }) { movie ->
                                DesktopMovieCard(movie = movie, onClick = { selectedMovie = movie })
                            }
                        }
                    }
                }

                // Cinematic Detail Overlay
                selectedMovie?.let { movie ->
                    Surface(
                        modifier = Modifier.fillMaxSize().clickable { selectedMovie = null },
                        color = Color.Black.copy(alpha = 0.85f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Card(
                                modifier = Modifier
                                    .width(580.dp)
                                    .padding(24.dp)
                                    .clickable(enabled = false) {},
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                                        AsyncImageDesktop(
                                            url = movie.backdropUrl ?: movie.posterUrl,
                                            contentDescription = movie.title,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                                        colors = listOf(Color.Transparent, Color(0xFF18181B))
                                                    )
                                                )
                                        )
                                        IconButton(
                                            onClick = { selectedMovie = null },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(12.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                                        }
                                    }

                                    Column(modifier = Modifier.padding(24.dp)) {
                                        Text(
                                            text = movie.title,
                                            fontSize = 22.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = movie.overview,
                                            color = Color.LightGray,
                                            fontSize = 13.sp,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))

                                        Button(
                                            onClick = {
                                                val firstLink = movie.serverLinks.firstOrNull()
                                                if (firstLink != null) {
                                                    scope.launch {
                                                        val streamUrl = repository.getStreamUrl(firstLink.fileId)
                                                        if (streamUrl != null) {
                                                            try {
                                                                java.awt.Desktop.getDesktop().browse(java.net.URI(streamUrl))
                                                            } catch (_: Exception) {}
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(46.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Reproducir Ahora", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopMovieCard(movie: Movie, onClick: () -> Unit, width: androidx.compose.ui.unit.Dp? = null) {
    val modifier = if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()
    Card(
        modifier = modifier
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImageDesktop(
                url = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize()
            )
            if (movie.rating > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", movie.rating),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CFilm Player - Desktop PC",
        state = rememberWindowState(width = 1280.dp, height = 720.dp)
    ) {
        DesktopApp()
    }
}
