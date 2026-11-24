package com.example.peliselprimazo.ui.screens.detail

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.peliselprimazo.domain.model.ContentType
import com.example.peliselprimazo.domain.model.Movie
import com.example.peliselprimazo.ui.components.AuthDialog
import com.example.peliselprimazo.ui.components.ParticleLoading
import com.example.peliselprimazo.ui.screens.detail.components.*

import android.app.Activity
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.peliselprimazo.ui.components.SupportDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onPlay: (String, String) -> Unit,
    onMovieClick: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPreloading by viewModel.preloadingVideo.collectAsState()
    val downloadAdState by viewModel.downloadAdState.collectAsState()
    
    val isFavorito by viewModel.isFavorito.collectAsState()
    val isViendo by viewModel.isViendo.collectAsState()
    val isTerminado by viewModel.isTerminado.collectAsState()
    val isVerDespues by viewModel.isVerDespues.collectAsState()
    val similarContent by viewModel.similarContent.collectAsState()
    
    val user by viewModel.user.collectAsState()
    
    var showEpisodeSheet by remember { mutableStateOf(false) }
    var showDownloadEpisodeSheet by remember { mutableStateOf(false) }
    var showNoServerDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var currentTrailerUrl by remember { mutableStateOf<String?>(null) }
    
    var showAuthDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val error by viewModel.error.collectAsState()
    val isLoadingAuth by viewModel.isLoadingAuth.collectAsState()
    val isServerOnline by viewModel.isServerOnline.collectAsState()

    val handleActionWithAuth = { action: () -> Unit ->
        if (user?.isLoggedIn == true || viewModel.isGuestMode()) {
            action()
        } else {
            pendingAction = action
            showAuthDialog = true
        }
    }

    val context = LocalContext.current

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
                actions = {
                    IconButton(
                        onClick = {
                            handleActionWithAuth {
                                val successState = uiState as? DetailUiState.Success
                                if (successState != null) {
                                    if (successState.movie.serverLinks.isEmpty()) {
                                        showNoServerDialog = true
                                    } else if (successState.movie.contentType == ContentType.MOVIE) {
                                        val firstLink = successState.movie.serverLinks.firstOrNull()
                                        if (firstLink != null) {
                                            viewModel.prepareDownload(successState.movie.title, firstLink, isEpisode = false)
                                        }
                                    } else {
                                        showDownloadEpisodeSheet = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(4.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Descargar", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { showSupportDialog = true },
                        modifier = Modifier.padding(4.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = "Soporte", tint = MaterialTheme.colorScheme.primary)
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
                        similarContent = similarContent,
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
                        },
                        onMovieClick = onMovieClick
                    )

                    if (showEpisodeSheet) {
                        Dialog(
                            onDismissRequest = { showEpisodeSheet = false },
                            properties = DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .fillMaxHeight(0.82f)
                                    .clip(RoundedCornerShape(28.dp)),
                                color = Color(0xFF121215),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                                shadowElevation = 16.dp
                            ) {
                                EpisodeSelectionSheet(
                                    movie = state.movie,
                                    sheetTitle = "Reproducir Episodio",
                                    onClose = { showEpisodeSheet = false },
                                    onEpisodeSelected = { ep ->
                                        showEpisodeSheet = false
                                        onPlay(ep.serverName, ep.fileId)
                                    }
                                )
                            }
                        }
                    }

                    if (showDownloadEpisodeSheet) {
                        Dialog(
                            onDismissRequest = { showDownloadEpisodeSheet = false },
                            properties = DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .fillMaxHeight(0.82f)
                                    .clip(RoundedCornerShape(28.dp)),
                                color = Color(0xFF121215),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                                shadowElevation = 16.dp
                            ) {
                                EpisodeSelectionSheet(
                                    movie = state.movie,
                                    sheetTitle = "Descargar Episodio",
                                    onClose = { showDownloadEpisodeSheet = false },
                                    onEpisodeSelected = { ep ->
                                        showDownloadEpisodeSheet = false
                                        viewModel.prepareDownload(
                                            movieTitle = state.movie.title,
                                            serverLink = ep,
                                            isEpisode = true
                                        )
                                    }
                                )
                            }
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
                    onGuestLogin = {
                        viewModel.continueAsGuest {
                            showAuthDialog = false
                            pendingAction?.invoke()
                            pendingAction = null
                        }
                    },
                    onResetPassword = { email, callback ->
                        viewModel.sendPasswordResetEmail(email, callback)
                    },
                    onDismiss = { showAuthDialog = false }
                )
            }

            if (showSupportDialog) {
                SupportDialog(
                    onDismiss = { showSupportDialog = false }
                )
            }

            val downloadState = downloadAdState
            if (downloadState != null) {
                DownloadAdDialog(
                    state = downloadState,
                    onDismiss = { viewModel.dismissDownloadDialog() },
                    onWatchAd = { viewModel.watchNextAd(context as Activity) },
                    onStartDownload = { viewModel.startFileDownload(context as Activity) }
                )
            }
        }
    }
}

@Composable
fun DetailContent(
    movie: Movie,
    similarContent: List<Movie>,
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
    onPlayButtonClick: () -> Unit,
    onMovieClick: (Int) -> Unit
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

        if (similarContent.isNotEmpty()) {
            item { SimilarContentSection(similarContent, onMovieClick) }
        }
        
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun DownloadAdDialog(
    state: DownloadAdState,
    onDismiss: () -> Unit,
    onWatchAd: () -> Unit,
    onStartDownload: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!state.isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF16161A),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (state.isEpisode) "Descargar Episodio" else "Descargar Película",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !state.isProcessing,
                        modifier = Modifier.size(28.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF222228),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = state.movieTitle,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (state.isEpisode) {
                            val epText = buildString {
                                append("Temporada ${state.season ?: 1} - Episodio ${state.episode ?: 1}")
                                if (!state.episodeTitle.isNullOrBlank()) {
                                    append(": ${state.episodeTitle}")
                                }
                            }
                            Text(
                                text = epText,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Para desbloquear la descarga debes ver 2 anuncios en video. Al completarlos, la descarga se activará.",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdBadgeItem(adNumber = 1, isWatched = state.adsWatched >= 1)
                    AdBadgeItem(adNumber = 2, isWatched = state.adsWatched >= 2)
                }

                Spacer(Modifier.height(16.dp))

                if (state.statusMessage != null) {
                    Text(
                        text = state.statusMessage,
                        color = if (state.statusMessage.contains("Error")) Color.Red else MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (state.adsWatched < 2) {
                    Button(
                        onClick = onWatchAd,
                        enabled = !state.isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Ver Anuncio ${state.adsWatched + 1} de 2",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onStartDownload,
                        enabled = !state.isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (state.isCompleted) "Descargar Nuevamente" else "Iniciar Descarga Directa",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdBadgeItem(adNumber: Int, isWatched: Boolean) {
    Surface(
        color = if (isWatched) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (isWatched) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isWatched) Icons.Default.CheckCircle else Icons.Default.OndemandVideo,
                contentDescription = null,
                tint = if (isWatched) Color(0xFF4CAF50) else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Anuncio $adNumber ${if (isWatched) "✓" else ""}",
                color = if (isWatched) Color(0xFF4CAF50) else Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
