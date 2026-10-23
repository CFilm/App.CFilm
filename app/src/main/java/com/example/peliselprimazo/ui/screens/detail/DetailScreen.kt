package com.example.peliselprimazo.ui.screens.detail

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.peliselprimazo.domain.model.CastMember
import com.example.peliselprimazo.domain.model.ContentType
import com.example.peliselprimazo.domain.model.Movie
import com.example.peliselprimazo.ui.components.AuthDialog
import com.example.peliselprimazo.ui.components.ParticleLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onPlay: (String, String) -> Unit // Simplificado: solo necesitamos server y fileId
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPreloading by viewModel.preloadingVideo.collectAsState()
    
    val isFavorito by viewModel.isFavorito.collectAsState()
    val isViendo by viewModel.isViendo.collectAsState()
    val isTerminado by viewModel.isTerminado.collectAsState()
    val isVerDespues by viewModel.isVerDespues.collectAsState()
    
    val user by viewModel.user.collectAsState()
    
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var showEpisodeSheet by remember { mutableStateOf(false) }
    var showNoServerDialog by remember { mutableStateOf(false) }
    var currentTrailerUrl by remember { mutableStateOf<String?>(null) }
    
    var showAuthDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val error by viewModel.error.collectAsState()
    val isLoadingAuth by viewModel.isLoadingAuth.collectAsState()
    val isServerOnline by viewModel.isServerOnline.collectAsState()

    val handleActionWithAuth = { action: () -> Unit ->
        if (user?.isLoggedIn == true) {
            action()
        } else {
            pendingAction = action
            showAuthDialog = true
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is DetailUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ParticleLoading()
                    }
                }
                is DetailUiState.Success -> {
                    DetailContent(
                        movie = state.movie,
                        isServerOnline = isServerOnline,
                        isFavorito = isFavorito,
                        isViendo = isViendo,
                        isTerminado = isTerminado,
                        isVerDespues = isVerDespues,
                        onFavoritoToggle = { handleActionWithAuth { viewModel.toggleFavorito() } },
                        onViendoToggle = { handleActionWithAuth { viewModel.toggleViendo() } },
                        onTerminadoToggle = { handleActionWithAuth { viewModel.toggleTerminado() } },
                        onVerDespuesToggle = { handleActionWithAuth { viewModel.toggleVerDespues() } },
                        onTrailerClick = { url ->
                            currentTrailerUrl = url
                        },
                        onPlayButtonClick = {
                            if (state.movie.serverLinks.isEmpty()) {
                                showNoServerDialog = true
                            } else if (state.movie.contentType == ContentType.MOVIE) {
                                val firstLink = state.movie.serverLinks.firstOrNull()
                                if (firstLink != null) {
                                    handleActionWithAuth {
                                        onPlay(firstLink.serverName, firstLink.fileId)
                                    }
                                }
                            } else {
                                handleActionWithAuth {
                                    showEpisodeSheet = true
                                }
                            }
                        }
                    )

                    if (showEpisodeSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showEpisodeSheet = false },
                            sheetState = sheetState,
                            containerColor = Color(0xFF121212),
                            contentColor = Color.White,
                            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
                        ) {
                            EpisodeSelectionSheet(
                                movie = state.movie,
                                onEpisodeSelected = { server, fileId ->
                                    showEpisodeSheet = false
                                    onPlay(server, fileId)
                                }
                            )
                        }
                    }

                    if (showNoServerDialog) {
                        AlertDialog(
                            onDismissRequest = { showNoServerDialog = false },
                            title = { Text("Sin Servidor Disponible", fontWeight = FontWeight.Bold, color = Color.White) },
                            text = { Text("Este contenido actualmente no cuenta con un servidor activo. Estará disponible muy pronto cuando re-suban el servidor.", color = Color.LightGray) },
                            confirmButton = {
                                Button(
                                    onClick = { showNoServerDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Aceptar", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            },
                            containerColor = Color(0xFF1A1A1A),
                            titleContentColor = Color.White,
                            textContentColor = Color.LightGray
                        )
                    }

                    if (currentTrailerUrl != null) {
                        TrailerDialog(trailerUrl = currentTrailerUrl!!) {
                            currentTrailerUrl = null
                        }
                    }
                }
                is DetailUiState.Error -> {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Error,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "Error de carga: ${state.message}",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Button(
                                onClick = { viewModel.loadMovieDetail() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Inténtalo de nuevo", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Overlay de carga (ahora manejado por MainActivity si prefieres, o lo dejamos aquí para feedback visual)
            AnimatedVisibility(
                visible = isPreloading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.zIndex(100f)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                    ParticleLoading(size = 150.dp)
                }
            }

            if (showAuthDialog) {
                AuthDialog(
                    isLoading = isLoadingAuth,
                    error = error,
                    onLogin = { e, p -> 
                        viewModel.login(e, p) {
                            showAuthDialog = false
                            pendingAction?.invoke()
                            pendingAction = null
                        }
                    },
                    onRegister = { u, e, p ->
                        viewModel.register(u, e, p) {
                            showAuthDialog = false
                            pendingAction?.invoke()
                            pendingAction = null
                        }
                    },
                    onDismiss = { showAuthDialog = false }
                )
            }
        }
    }
}

@Composable
fun DetailContent(
    movie: Movie,
    isServerOnline: Boolean,
    isFavorito: Boolean,
    isViendo: Boolean,
    isTerminado: Boolean,
    isVerDespues: Boolean,
    onFavoritoToggle: () -> Unit,
    onViendoToggle: () -> Unit,
    onTerminadoToggle: () -> Unit,
    onVerDespuesToggle: () -> Unit,
    onTrailerClick: (String) -> Unit,
    onPlayButtonClick: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { HeaderSection(movie, isServerOnline, onPlayButtonClick) }
        item { 
            UserActionRow(
                isFavorito = isFavorito,
                isViendo = isViendo,
                isTerminado = isTerminado,
                isVerDespues = isVerDespues,
                onFavoritoToggle = onFavoritoToggle,
                onViendoToggle = onViendoToggle,
                onTerminadoToggle = onTerminadoToggle,
                onVerDespuesToggle = onVerDespuesToggle
            )
        }
        
        item { InfoSection(movie, onTrailerClick) }
        
        if (movie.cast.isNotEmpty()) {
            item { CastSection(movie.cast) }
        }
        
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

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
fun InfoSection(movie: Movie, onTrailerClick: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = movie.title, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (movie.rating > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    Text(text = String.format("%.1f", movie.rating), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 2.dp))
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
            maxLines = if (isExpanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { isExpanded = !isExpanded }.animateContentSize()
        )
        
        if (movie.overview.length > 150) {
            Text(
                text = if (isExpanded) "Ver menos" else "Leer más...",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
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

@Composable
fun EpisodeSelectionSheet(movie: Movie, onEpisodeSelected: (String, String) -> Unit) {
    var selectedSeason by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(movie) { 
        val seasons = movie.serverLinks.mapNotNull { it.season }.distinct().sorted()
        selectedSeason = seasons.firstOrNull() ?: 1 
    }
    
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f).padding(bottom = 16.dp)) {
        Text(text = movie.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(16.dp))
        
        val seasons = movie.serverLinks.mapNotNull { it.season }.distinct().sorted()
        if (seasons.size > 1) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                items(seasons) { season ->
                    FilterChip(
                        selected = selectedSeason == season,
                        onClick = { selectedSeason = season },
                        label = { Text("Temp $season") },
                        colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF1A1A1A), labelColor = Color.Gray, selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.Black)
                    )
                }
            }
        }
        
        val episodes = movie.serverLinks
            .filter { it.season == selectedSeason }
            .distinctBy { it.episode }
            .sortedBy { it.episode ?: 0 }
            
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(episodes) { ep ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onEpisodeSelected(ep.serverName, ep.fileId) },
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.height(70.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(120.dp).fillMaxHeight()) {
                            AsyncImage(
                                model = ep.thumbnailUri ?: movie.backdropUrl ?: movie.posterUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.padding(end = 12.dp).weight(1f)) {
                            val epName = ep.episodeTitle
                            val isGeneric = epName.isNullOrBlank() || 
                                            epName.equals("episodio ${ep.episode}", ignoreCase = true) || 
                                            epName.equals("episode ${ep.episode}", ignoreCase = true) ||
                                            epName.equals("episodio", ignoreCase = true)

                            val epTitleText = if (!isGeneric) {
                                "Episodio ${ep.episode}: $epName"
                            } else {
                                "Episodio ${ep.episode ?: "?"}"
                            }
                            Text(text = epTitleText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = "Temporada ${ep.season ?: "?"}", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

fun extractYouTubeId(url: String): String? {
    return try {
        val uri = Uri.parse(url)
        when {
            uri.host?.contains("youtu.be") == true -> uri.lastPathSegment
            uri.host?.contains("youtube.com") == true -> uri.getQueryParameter("v")
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
fun TrailerDialog(trailerUrl: String, onDismiss: () -> Unit) {
    val videoId = remember(trailerUrl) { extractYouTubeId(trailerUrl) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF121212),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tráiler Oficial", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (videoId != null) {
                            AndroidView(
                                factory = { ctx ->
                                    android.webkit.WebView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            mediaPlaybackRequiresUserGesture = false
                                            loadWithOverviewMode = true
                                            useWideViewPort = true
                                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                        }
                                        setBackgroundColor(android.graphics.Color.BLACK)
                                        webChromeClient = android.webkit.WebChromeClient()
                                        webViewClient = object : android.webkit.WebViewClient() {
                                            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                                return false
                                            }
                                        }
                                        loadUrl("https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&modestbranding=1&rel=0")
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("No se pudo cargar el tráiler", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "¿El video muestra error de reproducción o restricción?",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl)).apply {
                                        setPackage("com.google.android.youtube")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver directamente en YouTube", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
