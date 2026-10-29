package com.example.peliselprimazo.ui.screens.detail.components

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.peliselprimazo.domain.model.Movie
import com.example.peliselprimazo.domain.model.ServerLink

@Composable
fun EpisodeSelectionSheet(
    movie: Movie,
    onClose: () -> Unit = {},
    onEpisodeSelected: (String, String) -> Unit
) {
    val seasons = remember(movie) {
        val detected = movie.serverLinks.mapNotNull { it.season }.distinct().sorted()
        if (detected.isNotEmpty()) detected else listOf(1)
    }
    var selectedSeason by remember(movie) { mutableIntStateOf(seasons.first()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(seasons) { season ->
                val isSelected = selectedSeason == season
                Surface(
                    onClick = { selectedSeason = season },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1A1A1A),
                    border = BorderStroke(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "Temporada $season",
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        val episodes = remember(movie, selectedSeason) {
            movie.serverLinks
                .filter { (it.season ?: 1) == selectedSeason }
                .distinctBy { it.episode ?: it.fileId }
                .sortedBy { it.episode ?: 0 }
        }

        if (episodes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay episodios disponibles para la Temporada $selectedSeason", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(episodes.size) { index ->
                    val ep = episodes[index]
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable { onEpisodeSelected(ep.serverName, ep.fileId) },
                        color = Color(0xFF1A1A1E),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(110.dp).height(70.dp).clip(RoundedCornerShape(10.dp))) {
                                AsyncImage(
                                    model = ep.thumbnailUri ?: movie.backdropUrl ?: movie.posterUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                var isEpOverviewExpanded by remember { mutableStateOf(false) }
                                val epName = ep.episodeTitle
                                val isGeneric = epName.isNullOrBlank() || 
                                                epName.equals("episodio ${ep.episode}", ignoreCase = true) || 
                                                epName.equals("episode ${ep.episode}", ignoreCase = true) ||
                                                epName.equals("episodio", ignoreCase = true)

                                val epTitleText = if (!isGeneric) {
                                    "Episodio ${ep.episode ?: 1}: $epName"
                                } else {
                                    "Episodio ${ep.episode ?: 1}"
                                }

                                Text(
                                    text = epTitleText,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val overviewText = ep.overview?.ifBlank { null }
                                if (!overviewText.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = overviewText,
                                        color = Color.LightGray.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        maxLines = if (isEpOverviewExpanded) 8 else 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.animateContentSize()
                                    )
                                    if (overviewText.length > 50) {
                                        Text(
                                            text = if (isEpOverviewExpanded) "Ver menos" else "Leer más...",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .clickable { isEpOverviewExpanded = !isEpOverviewExpanded }
                                        )
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
                                    WebView(ctx).apply {
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
                                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                        }
                                        setBackgroundColor(android.graphics.Color.BLACK)
                                        webChromeClient = WebChromeClient()
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
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
