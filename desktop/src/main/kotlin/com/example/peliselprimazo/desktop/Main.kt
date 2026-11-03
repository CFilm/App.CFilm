package com.example.peliselprimazo.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.peliselprimazo.desktop.data.DesktopRepository
import com.example.peliselprimazo.domain.model.ContentType
import com.example.peliselprimazo.domain.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    var hasError by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val stream = java.net.URL(url).openStream()
                    bitmap = androidx.compose.ui.res.loadImageBitmap(stream)
                } catch (_: Exception) {
                    hasError = true
                }
            }
        } else {
            hasError = true
        }
    }

    Box(modifier = modifier.background(Color(0xFF18181B)), contentAlignment = Alignment.Center) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else if (hasError) {
            Text(
                text = contentDescription ?: "CFilm",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
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

    // Estado del anuncio y reproducción
    var pendingFileId by remember { mutableStateOf<String?>(null) }
    var adCountdown by remember { mutableIntStateOf(0) }
    var preloadedStreamUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            movies = repository.fetchMovies()
            isLoading = false
        }
    }

    // Manejo del temporizador del anuncio y precarga simultánea en segundo plano
    LaunchedEffect(pendingFileId) {
        val fileId = pendingFileId
        if (fileId != null) {
            adCountdown = 5
            preloadedStreamUrl = null

            // 1. Iniciar extracción en segundo plano mientras corre el anuncio
            scope.launch {
                preloadedStreamUrl = repository.getStreamUrl(fileId)
            }

            // 2. Cuenta regresiva del anuncio de 5 segundos
            while (adCountdown > 0) {
                delay(1000)
                adCountdown--
            }

            // 3. Abrir video al terminar el anuncio (con fallback garantizado)
            val urlToOpen = preloadedStreamUrl 
                ?: repository.getStreamUrl(fileId) 
                ?: "https://streamtape.com/v/$fileId"

            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI(urlToOpen))
            } catch (e: Exception) {
                println("Error al abrir navegador/reproductor: ${e.message}")
            }
            pendingFileId = null
        }
    }

    val filteredMovies = remember(movies, searchQuery, selectedTab) {
        val result = if (searchQuery.isBlank()) movies else movies.filter { it.title.contains(searchQuery, ignoreCase = true) }
        when (selectedTab) {
            1 -> result.filter { it.contentType == ContentType.MOVIE }
            2 -> result.filter { it.contentType == ContentType.TV || it.contentType == ContentType.ANIME }
            else -> result
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFE50914))) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF000000)) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar (CFilm + Search)
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
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black
                            )

                            // Search Field
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("¿Qué quieres ver hoy?", color = Color.Gray, fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.width(360.dp).heightIn(min = 46.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = Color.White),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF27272A),
                                    focusedContainerColor = Color(0xFF18181B),
                                    unfocusedContainerColor = Color(0xFF18181B)
                                )
                            )
                        }
                    }

                    // Content Area
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text("Cargando contenido...", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    } else if (searchQuery.isNotBlank()) {
                        // Search Results Grid
                        if (filteredMovies.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No se encontraron resultados para \"$searchQuery\"", color = Color.Gray, fontSize = 15.sp)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 160.dp),
                                modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 90.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filteredMovies, key = { "${it.id}_${it.serverLinks.firstOrNull()?.fileId ?: ""}" }) { movie ->
                                    DesktopMovieCard(movie = movie, onClick = { selectedMovie = movie })
                                }
                            }
                        }
                    } else {
                        when (selectedTab) {
                            0 -> {
                                // Home View (Inicio) con múltiples secciones ricas
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 90.dp),
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    val trending = movies.sortedByDescending { it.rating }.take(12)
                                    val moviesOnly = movies.filter { it.contentType == ContentType.MOVIE }
                                    val seriesOnly = movies.filter { it.contentType == ContentType.TV || it.contentType == ContentType.ANIME }
                                    val topRated = movies.sortedByDescending { it.rating }.drop(3).take(12)

                                    if (trending.isNotEmpty()) {
                                        item {
                                            DesktopSectionRow(title = "🔥 Tendencias Globales", items = trending, onMovieClick = { selectedMovie = it })
                                        }
                                    }
                                    if (moviesOnly.isNotEmpty()) {
                                        item {
                                            DesktopSectionRow(title = "🎬 Películas Populares", items = moviesOnly, onMovieClick = { selectedMovie = it })
                                        }
                                    }
                                    if (seriesOnly.isNotEmpty()) {
                                        item {
                                            DesktopSectionRow(title = "📺 Series & Anime Destacados", items = seriesOnly, onMovieClick = { selectedMovie = it })
                                        }
                                    }
                                    if (topRated.isNotEmpty()) {
                                        item {
                                            DesktopSectionRow(title = "⭐ Los Más Valorados", items = topRated, onMovieClick = { selectedMovie = it })
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // Películas Grid
                                val moviesList = movies.filter { it.contentType == ContentType.MOVIE }
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 160.dp),
                                    modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 90.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(moviesList, key = { "${it.id}_${it.serverLinks.firstOrNull()?.fileId ?: ""}" }) { movie ->
                                        DesktopMovieCard(movie = movie, onClick = { selectedMovie = movie })
                                    }
                                }
                            }
                            2 -> {
                                // Series Grid
                                val seriesList = movies.filter { it.contentType == ContentType.TV || it.contentType == ContentType.ANIME }
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 160.dp),
                                    modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 90.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(seriesList, key = { "${it.id}_${it.serverLinks.firstOrNull()?.fileId ?: ""}" }) { movie ->
                                        DesktopMovieCard(movie = movie, onClick = { selectedMovie = movie })
                                    }
                                }
                            }
                            3 -> {
                                // Perfil View
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 100.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Card(
                                        modifier = Modifier.width(600.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(96.dp),
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(52.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "Usuario CFilm Desktop",
                                                color = Color.White,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Sincronizado con tu cuenta multimedia",
                                                color = Color.Gray,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(32.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceAround
                                            ) {
                                                ProfileStatItem(label = "Contenido Total", value = "${movies.size}")
                                                ProfileStatItem(label = "Películas", value = "${movies.count { it.contentType == ContentType.MOVIE }}")
                                                ProfileStatItem(label = "Series / Anime", value = "${movies.count { it.contentType == ContentType.TV || it.contentType == ContentType.ANIME }}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Bottom Navigation Bubble
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .zIndex(20f),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    shadowElevation = 16.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val navItems = listOf(
                            Triple("Inicio", Icons.Default.Home, 0),
                            Triple("Películas", Icons.Default.Movie, 1),
                            Triple("Series", Icons.Default.Tv, 2),
                            Triple("Perfil", Icons.Default.Person, 3)
                        )
                        navItems.forEach { (title, icon, index) ->
                            val isSelected = selectedTab == index
                            Surface(
                                onClick = { selectedTab = index },
                                shape = RoundedCornerShape(24.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = title,
                                        tint = if (isSelected) Color.White else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Overlay de Anuncio Recompensado con cuenta regresiva y precarga en paralelo
                if (pendingFileId != null) {
                    Surface(
                        modifier = Modifier.fillMaxSize().zIndex(100f),
                        color = Color.Black.copy(alpha = 0.92f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Card(
                                modifier = Modifier.width(420.dp).padding(24.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(56.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 4.dp
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Anuncio Recompensado CFilm",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "El contenido comenzará automáticamente al finalizar el anuncio.",
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "Iniciando en $adCountdown s...",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Cinematic Detail Overlay
                selectedMovie?.let { movie ->
                    val seasons = remember(movie) {
                        val detected = movie.serverLinks.mapNotNull { it.season }.distinct().sorted()
                        if (detected.isNotEmpty()) detected else listOf(1)
                    }
                    var selectedSeason by remember(movie) { mutableIntStateOf(seasons.first()) }

                    val episodes = remember(movie, selectedSeason) {
                        movie.serverLinks
                            .filter { (it.season ?: 1) == selectedSeason }
                            .distinctBy { it.episode ?: it.fileId }
                            .sortedBy { it.episode ?: 0 }
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize().clickable { selectedMovie = null }.zIndex(50f),
                        color = Color.Black.copy(alpha = 0.85f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Card(
                                modifier = Modifier
                                    .width(680.dp)
                                    .heightIn(max = 650.dp)
                                    .padding(24.dp)
                                    .clickable(enabled = false) {},
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
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
                                                .padding(16.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                                        }
                                    }

                                    Column(modifier = Modifier.padding(24.dp).weight(1f)) {
                                        Text(
                                            text = movie.title,
                                            fontSize = 24.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (movie.rating > 0) {
                                                Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp)) {
                                                    Text(text = " ★ ${String.format("%.1f", movie.rating)} ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                            Text(text = if (movie.contentType == ContentType.TV) "Serie / Anime" else "Película", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = movie.overview,
                                            color = Color.LightGray,
                                            fontSize = 13.sp,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        if (movie.contentType == ContentType.TV || movie.contentType == ContentType.ANIME) {
                                            // Temporadas Tabs
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(seasons) { season ->
                                                    val isSelected = selectedSeason == season
                                                    Surface(
                                                        onClick = { selectedSeason = season },
                                                        shape = RoundedCornerShape(20.dp),
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF27272A)
                                                    ) {
                                                        Text(
                                                            text = "Temporada $season",
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Lista de Episodios
                                            LazyColumn(
                                                modifier = Modifier.fillMaxWidth().weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(episodes, key = { "${it.season}_${it.episode}_${it.fileId}" }) { ep ->
                                                    Surface(
                                                        modifier = Modifier.fillMaxWidth().clickable {
                                                            pendingFileId = ep.fileId
                                                        },
                                                        color = Color(0xFF27272A),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Box(
                                                                modifier = Modifier.width(100.dp).height(60.dp).clip(RoundedCornerShape(8.dp))
                                                            ) {
                                                                AsyncImageDesktop(
                                                                    url = ep.thumbnailUri ?: movie.backdropUrl ?: movie.posterUrl,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.fillMaxSize()
                                                                )
                                                                Box(
                                                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = "Episodio ${ep.episode ?: 1}${if (!ep.episodeTitle.isNullOrBlank()) ": ${ep.episodeTitle}" else ""}",
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 13.sp,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                if (!ep.overview.isNullOrBlank()) {
                                                                    Spacer(modifier = Modifier.height(2.dp))
                                                                    Text(
                                                                        text = ep.overview!!,
                                                                        color = Color.LightGray.copy(alpha = 0.8f),
                                                                        fontSize = 11.sp,
                                                                        maxLines = 2,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // Botón de Reproducir para Películas
                                            Button(
                                                onClick = {
                                                    val firstLink = movie.serverLinks.firstOrNull()
                                                    if (firstLink != null) {
                                                        pendingFileId = firstLink.fileId
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Reproducir Ahora", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
}

@Composable
fun DesktopSectionRow(title: String, items: List<Movie>, onMovieClick: (Movie) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(items, key = { "${it.id}_${it.serverLinks.firstOrNull()?.fileId ?: ""}" }) { movie ->
                DesktopMovieCard(movie = movie, onClick = { onMovieClick(movie) }, width = 160.dp)
            }
        }
    }
}

@Composable
fun ProfileStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 13.sp
        )
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
