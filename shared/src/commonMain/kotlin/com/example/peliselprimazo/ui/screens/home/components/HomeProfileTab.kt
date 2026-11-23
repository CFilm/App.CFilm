package com.example.peliselprimazo.ui.screens.home.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.peliselprimazo.core.model.Movie
import com.example.peliselprimazo.core.model.User
import com.example.peliselprimazo.ui.components.MovieCardItem
import com.example.peliselprimazo.ui.components.PremiumBenefitsDialog
import com.example.peliselprimazo.ui.screens.home.HomeViewModel

@Composable
fun ProfileScreenContent(
    user: User?,
    watching: List<Movie>,
    saved: List<Movie>,
    liked: List<Movie>,
    watchLater: List<Movie>,
    finished: List<Movie>,
    totalHours: Long,
    viewModel: HomeViewModel,
    onMovieClick: (Int) -> Unit,
    onDirectPlayClick: (Movie) -> Unit,
    cambiosStatus: String,
    onSupportClick: () -> Unit
) {
    var selectedListCategory by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedListCategory,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "ProfileContentTransition"
        ) { category ->
            if (category == null) {
                ProfileMainContent(
                    user = user,
                    watchingCount = watching.size,
                    likedCount = liked.size,
                    finishedCount = finished.size,
                    watchLaterCount = watchLater.size,
                    totalCount = watching.size + liked.size + finished.size + watchLater.size,
                    totalHours = totalHours,
                    onCategoryClick = { selectedListCategory = it },
                    viewModel = viewModel,
                    cambiosStatus = cambiosStatus,
                    onSupportClick = onSupportClick
                )
            } else {
                val listToShow = when(category) {
                    "Favoritos" -> liked
                    "Viendo" -> watching
                    "Terminados" -> finished
                    "Ver Después" -> watchLater
                    "Guardados" -> saved
                    else -> emptyList()
                }
                ProfileListDetail(
                    title = category,
                    items = listToShow,
                    onBack = { selectedListCategory = null },
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}

@Composable
fun ProfileMainContent(
    user: User?,
    watchingCount: Int,
    likedCount: Int,
    finishedCount: Int,
    watchLaterCount: Int,
    totalCount: Int,
    totalHours: Long,
    onCategoryClick: (String) -> Unit,
    viewModel: HomeViewModel,
    cambiosStatus: String,
    onSupportClick: () -> Unit
) {
    var showPremiumDialog by remember { mutableStateOf(false) }

    if (showPremiumDialog) {
        PremiumBenefitsDialog(
            onDismiss = { showPremiumDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(modifier = Modifier.size(100.dp).clip(CircleShape), color = Color.DarkGray) {
                        if (user?.profilePictureUri != null) {
                            AsyncImage(
                                model = user.profilePictureUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(20.dp), tint = Color.White)
                        }
                    }
                }
                Text(
                    text = user?.username ?: "Usuario CFilm",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 16.dp),
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    modifier = Modifier.padding(top = 8.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "Plan Gratuito (General)",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Mis Listas", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 16.dp))
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileGridItem(title = "Favoritos", count = likedCount, icon = Icons.Rounded.Favorite, iconColor = Color(0xFFE50914), modifier = Modifier.weight(1f), onClick = { onCategoryClick("Favoritos") })
                    ProfileGridItem(title = "Viendo", count = watchingCount, icon = Icons.Rounded.PlayCircle, iconColor = Color(0xFF2196F3), modifier = Modifier.weight(1f), onClick = { onCategoryClick("Viendo") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileGridItem(title = "Terminados", count = finishedCount, icon = Icons.Rounded.CheckCircle, iconColor = Color(0xFF4CAF50), modifier = Modifier.weight(1f), onClick = { onCategoryClick("Terminados") })
                    ProfileGridItem(title = "Ver Después", count = watchLaterCount, icon = Icons.Rounded.WatchLater, iconColor = Color(0xFFFF9800), modifier = Modifier.weight(1f), onClick = { onCategoryClick("Ver Después") })
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1A1A1A), shape = RoundedCornerShape(20.dp)) {
                Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = totalCount.toString(), color = Color(0xFFE50914), fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(text = "Total Contenido", color = Color.Gray, fontSize = 12.sp)
                    }
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.DarkGray).align(Alignment.CenterVertically))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "~$totalHours", color = Color(0xFFE50914), fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(text = "Horas Aprox.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Ajustes", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            ProfileSettingItem(title = "Soporte y Reportar Falla", icon = Icons.Default.SupportAgent, onClick = onSupportClick)
            ProfileSettingItem(title = "Limpiar Caché", icon = Icons.Default.Delete, onClick = { viewModel.clearCache() })
            if (user?.isLoggedIn == true) {
                ProfileSettingItem(title = "Cerrar Sesión", icon = Icons.AutoMirrored.Filled.ExitToApp, iconColor = Color.Red, textColor = Color.Red, onClick = { viewModel.logout() })
            }
        }
    }
}

@Composable
fun ProfileGridItem(title: String, count: Int, icon: ImageVector, iconColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.height(120.dp).clickable { onClick() }, color = Color(0xFF1A1A1A), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = count.toString(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Text(text = title, color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ProfileSettingItem(title: String, icon: ImageVector, iconColor: Color = Color.Gray, textColor: Color = Color.White, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(title, color = textColor) }, leadingContent = { Icon(icon, contentDescription = null, tint = iconColor) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent), modifier = Modifier.clickable { onClick() })
}

@Composable
fun ProfileListDetail(title: String, items: List<Movie>, onBack: () -> Unit, onMovieClick: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(modifier = Modifier.statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) }
            Spacer(Modifier.width(8.dp))
            Text(text = title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        }
        if (items.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = "No hay nada aquí todavía", color = Color.Gray) } } else {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.id }) { movie -> MovieCardItem(movie, onClick = onMovieClick) }
            }
        }
    }
}
