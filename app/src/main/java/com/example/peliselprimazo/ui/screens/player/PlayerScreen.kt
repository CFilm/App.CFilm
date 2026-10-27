package com.example.peliselprimazo.ui.screens.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.mediarouter.app.MediaRouteButton
import com.example.peliselprimazo.data.AdsManager
import com.example.peliselprimazo.domain.model.ContentType
import com.example.peliselprimazo.domain.model.ServerLink
import com.example.peliselprimazo.ui.components.ParticleLoading
import com.google.android.gms.cast.framework.CastButtonFactory
import kotlinx.coroutines.delay

@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    adsManager: AdsManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        activity?.let { act ->
            val window = act.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            view.keepScreenOn = true
        }
        
        onDispose {
            activity?.let { act ->
                val window = act.window
                WindowCompat.setDecorFitsSystemWindows(window, true)
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                view.keepScreenOn = false
            }
        }
    }

    BackHandler { onBack() }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ParticleLoading(size = 150.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Preparando cine...",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            is PlayerUiState.Success -> {
                val isSeries = state.movie.contentType == ContentType.TV || 
                               (state.currentLink?.season != null && state.currentLink?.episode != null)

                Box(modifier = Modifier.fillMaxSize()) {
                    NativePlayer(
                        movieTitle = state.movie.title,
                        currentLink = state.currentLink,
                        allMovieLinks = state.movie.serverLinks,
                        isSeries = isSeries,
                        videoUrl = state.videoUrl,
                        headers = state.headers,
                        resumePosition = state.resumePosition,
                        onBack = onBack,
                        onNext = state.nextLink?.let { next ->
                            {
                                activity?.let { act ->
                                    adsManager.showRewardedVideo(act) {
                                        viewModel.loadVideo(next.serverName, next.fileId, state.movie.id)
                                    }
                                } ?: viewModel.loadVideo(next.serverName, next.fileId, state.movie.id)
                            }
                        },
                        onPrev = state.prevLink?.let { prev ->
                            {
                                activity?.let { act ->
                                    adsManager.showRewardedVideo(act) {
                                        viewModel.loadVideo(prev.serverName, prev.fileId, state.movie.id)
                                    }
                                } ?: viewModel.loadVideo(prev.serverName, prev.fileId, state.movie.id)
                            }
                        },
                        onSelectLanguage = { selectedLink ->
                            viewModel.loadVideo(selectedLink.serverName, selectedLink.fileId, state.movie.id)
                        },
                        onProgress = { pos, dur ->
                            viewModel.updateProgress(pos, dur)
                        },
                        onError = {
                            viewModel.reportError()
                            viewModel.onPlaybackError()
                        }
                    )
                    
                    if (state.isChangingEpisode) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                ParticleLoading(size = 150.dp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Preparando cine...",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            is PlayerUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Rounded.Error, null, tint = Color.Red, modifier = Modifier.size(56.dp))
                        Text(state.message, color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { 
                                Text("Volver", color = Color.White) 
                            }
                            Button(onClick = { /* reload */ }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { 
                                Text("Inténtalo de nuevo", color = Color.White, fontWeight = FontWeight.Bold) 
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = Color.White.copy(alpha = 0.2f)
) {
    Box(
        modifier = modifier
            .width(8.dp)
            .clip(CircleShape)
            .background(trackColor),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(progress.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(color)
        )
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun NativePlayer(
    movieTitle: String,
    currentLink: ServerLink?,
    allMovieLinks: List<ServerLink> = emptyList(),
    isSeries: Boolean,
    videoUrl: String,
    headers: Map<String, String>,
    resumePosition: Long,
    onBack: () -> Unit,
    onNext: (() -> Unit)? = null,
    onPrev: (() -> Unit)? = null,
    onSelectLanguage: ((ServerLink) -> Unit)? = null,
    onProgress: (position: Long, duration: Long) -> Unit,
    onError: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }

    var currentVolume by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    var currentBrightness by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness.takeIf { it != null && it >= 0f } ?: 0.5f
        )
    }

    var showVolumeOverlay by remember { mutableStateOf(false) }
    var showBrightnessOverlay by remember { mutableStateOf(false) }
    var isDraggingLeft by remember { mutableStateOf(false) }
    var isDraggingRight by remember { mutableStateOf(false) }

    var autoNextCanceled by remember { mutableStateOf(false) }
    var autoNextTriggered by remember { mutableStateOf(false) }
    var autoNextCountdown by remember { mutableIntStateOf(6) }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(videoUrl) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        currentPosition = 0L
        duration = 0L
        autoNextCanceled = false
        autoNextTriggered = false
        autoNextCountdown = 6
        val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
            if (headers.isNotEmpty()) {
                setDefaultRequestProperties(headers)
            }
        }
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        if (resumePosition > 0) {
            exoPlayer.seekTo(resumePosition)
        }
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    LaunchedEffect(showVolumeOverlay) {
        if (showVolumeOverlay) {
            delay(2000)
            showVolumeOverlay = false
        }
    }

    LaunchedEffect(showBrightnessOverlay) {
        if (showBrightnessOverlay) {
            delay(2000)
            showBrightnessOverlay = false
        }
    }

    // Detectar últimos 90 segundos (1.5 min) donde típicamente inician los créditos
    val remainingMillis = if (duration > 0) duration - currentPosition else Long.MAX_VALUE
    val isNearEnd = isSeries && onNext != null && duration > 60000L && remainingMillis in 1L..90000L && !autoNextCanceled

    LaunchedEffect(isNearEnd) {
        if (isNearEnd && !autoNextTriggered && !autoNextCanceled) {
            autoNextCountdown = 6
            while (autoNextCountdown > 0 && !autoNextCanceled && !autoNextTriggered) {
                delay(1000)
                autoNextCountdown--
            }
            if (autoNextCountdown <= 0 && !autoNextCanceled && !autoNextTriggered) {
                autoNextTriggered = true
                exoPlayer.stop()
                onNext?.invoke()
            }
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                super.onPlayerError(error)
                Log.e("NativePlayer", "Error de reproducción: ${error.message}")
                onError()
            }
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)

        val handler = Handler(Looper.getMainLooper())
        val updateRunnable = object : Runnable {
            override fun run() {
                if (exoPlayer.isPlaying) {
                    currentPosition = exoPlayer.currentPosition
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                    onProgress(currentPosition, duration)
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(updateRunnable, 1000)

        onDispose {
            handler.removeCallbacks(updateRunnable)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val isLeft = offset.x < size.width / 2f
                        isDraggingLeft = isLeft
                        isDraggingRight = !isLeft
                    },
                    onDragEnd = {
                        isDraggingLeft = false
                        isDraggingRight = false
                    },
                    onDragCancel = {
                        isDraggingLeft = false
                        isDraggingRight = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val deltaY = -dragAmount.y / size.height.toFloat()

                        if (isDraggingLeft) {
                            currentBrightness = (currentBrightness + deltaY * 1.5f).coerceIn(0.01f, 1.0f)
                            activity?.window?.let { window ->
                                val lp = window.attributes
                                lp.screenBrightness = currentBrightness
                                window.attributes = lp
                            }
                            showBrightnessOverlay = true
                        } else if (isDraggingRight) {
                            currentVolume = (currentVolume + deltaY * maxVolume.toFloat() * 1.5f).coerceIn(0f, maxVolume.toFloat())
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume.toInt(), 0)
                            showVolumeOverlay = true
                        }
                    }
                )
            }
            .clickable { showControls = !showControls }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Brillo Vertical (Lado Izquierdo)
        AnimatedVisibility(
            visible = showBrightnessOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (currentBrightness > 0.5f) Icons.Rounded.WbSunny else Icons.Rounded.BrightnessMedium,
                        contentDescription = "Brillo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(currentBrightness * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    VerticalProgressBar(
                        progress = currentBrightness,
                        modifier = Modifier.height(100.dp)
                    )
                }
            }
        }

        // Overlay Volumen Vertical (Lado Derecho)
        AnimatedVisibility(
            visible = showVolumeOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                val volumePercent = currentVolume / maxVolume.toFloat()
                val volumeIcon = when {
                    volumePercent <= 0f -> Icons.Rounded.VolumeOff
                    volumePercent < 0.5f -> Icons.Rounded.VolumeDown
                    else -> Icons.Rounded.VolumeUp
                }

                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = volumeIcon,
                        contentDescription = "Volumen",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(volumePercent * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    VerticalProgressBar(
                        progress = volumePercent,
                        modifier = Modifier.height(100.dp)
                    )
                }
            }
        }

        // Card Saltar Créditos / Autoplay Siguiente Episodio
        AnimatedVisibility(
            visible = isNearEnd && !autoNextTriggered,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 24.dp)
        ) {
            Surface(
                color = Color(0xFF1A1A1A).copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Siguiente episodio $autoNextCountdown s...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                autoNextTriggered = true
                                exoPlayer.stop()
                                onNext?.invoke()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Saltar Créditos", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        TextButton(
                            onClick = { autoNextCanceled = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("Cancelar", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Animated Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            exoPlayer.stop()
                            onBack()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    val season = currentLink?.season
                    val episode = currentLink?.episode
                    val titleInfo = if (season != null && episode != null) {
                        "$movieTitle  •  Temp $season  •  Ep $episode"
                    } else {
                        movieTitle
                    }

                    Text(
                        text = titleInfo,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Botón Minimizar (Modo PiP)
                    val activity = LocalContext.current as? Activity
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        Surface(
                            onClick = {
                                try {
                                    val params = android.app.PictureInPictureParams.Builder().build()
                                    activity?.enterPictureInPictureMode(params)
                                } catch (e: Exception) {
                                    Log.e("PiP", "Error entering PiP: ${e.message}")
                                }
                            },
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AspectRatio,
                                    contentDescription = "Minimizar",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Minimizar",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // 1. Selector de Velocidad de Reproducción
                    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
                    var showSpeedMenu by remember { mutableStateOf(false) }

                    Box {
                        Surface(
                            onClick = { showSpeedMenu = true },
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = "Velocidad",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${currentSpeed}x",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false },
                            modifier = Modifier.background(Color(0xFF1A1A1A))
                        ) {
                            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            speeds.forEach { speed ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                                            color = if (speed == currentSpeed) MaterialTheme.colorScheme.primary else Color.White,
                                            fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        showSpeedMenu = false
                                        currentSpeed = speed
                                        exoPlayer.setPlaybackSpeed(speed)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 2. Botón Transmitir a TV (Google Cast)
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                try {
                                    MediaRouteButton(ctx).apply {
                                        try {
                                            CastButtonFactory.setUpMediaRouteButton(ctx, this)
                                        } catch (e: Exception) {
                                            Log.e("GoogleCast", "Error setting up Cast button: ${e.message}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("GoogleCast", "Error creating MediaRouteButton: ${e.message}")
                                    android.view.View(ctx)
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Selector de Idiomas
                    val availableLanguages = remember(currentLink, allMovieLinks) {
                        val filtered = if (isSeries && currentLink?.season != null && currentLink.episode != null) {
                            allMovieLinks.filter { it.season == currentLink.season && it.episode == currentLink.episode }
                        } else {
                            allMovieLinks
                        }
                        filtered.distinctBy { it.language }
                    }

                    var showLanguageMenu by remember { mutableStateOf(false) }

                    if (availableLanguages.size > 1) {
                        Box {
                            Surface(
                                onClick = { showLanguageMenu = true },
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Translate,
                                        contentDescription = "Idioma",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentLink?.language ?: "Idioma",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showLanguageMenu,
                                onDismissRequest = { showLanguageMenu = false },
                                modifier = Modifier.background(Color(0xFF1A1A1A))
                            ) {
                                availableLanguages.forEach { link ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = link.language,
                                                color = if (link.language == currentLink?.language) MaterialTheme.colorScheme.primary else Color.White,
                                                fontWeight = if (link.language == currentLink?.language) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            showLanguageMenu = false
                                            if (link.fileId != currentLink?.fileId) {
                                                exoPlayer.stop()
                                                onSelectLanguage?.invoke(link)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Center Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.FastRewind, contentDescription = "-10s", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.FastForward, contentDescription = "+10s", tint = Color.White)
                    }
                }

                // Bottom Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatMillis(currentPosition),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { value ->
                                val target = (value * duration).toLong()
                                currentPosition = target
                                exoPlayer.seekTo(target)
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = formatMillis(duration),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (isSeries) {
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    exoPlayer.stop()
                                    onPrev?.invoke()
                                },
                                enabled = onPrev != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    disabledContainerColor = Color.White.copy(alpha = 0.05f)
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = if (onPrev != null) Color.White else Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ant", color = if (onPrev != null) Color.White else Color.Gray, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    exoPlayer.stop()
                                    onNext?.invoke()
                                },
                                enabled = onNext != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = Color.White.copy(alpha = 0.05f)
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("Sig", color = if (onNext != null) Color.Black else Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = if (onNext != null) Color.Black else Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatMillis(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
