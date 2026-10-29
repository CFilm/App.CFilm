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
    
    val isFavorito by viewModel.isFavorito.collectAsState()
    val isViendo by viewModel.isViendo.collectAsState()
    val isTerminado by viewModel.isTerminado.collectAsState()
    val isVerDespues by viewModel.isVerDespues.collectAsState()
    val similarContent by viewModel.similarContent.collectAsState()
    
    val user by viewModel.user.collectAsState()
    
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
                                    onClose = { showEpisodeSheet = false },
                                    onEpisodeSelected = { server, fileId ->
                                        showEpisodeSheet = false
                                        onPlay(server, fileId)
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
                    onDismiss = { showAuthDialog = false }
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
