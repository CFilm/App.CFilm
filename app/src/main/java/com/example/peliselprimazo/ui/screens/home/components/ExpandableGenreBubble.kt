package com.example.peliselprimazo.ui.screens.home.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableGenreBubble(
    selectedGenre: String?,
    onGenreSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val genres = listOf(
        "Acción", "Comedia", "Terror", "Ciencia Ficción", "Drama",
        "Suspenso", "Aventura", "Animación", "Fantasía", "Romance",
        "Crimen", "Misterio"
    )

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier
    ) {
        // Expandable genre popup menu
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Surface(
                modifier = Modifier
                    .width(270.dp)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1E1E).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                shadowElevation = 12.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Géneros de Contenido",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            Surface(
                                onClick = {
                                    onGenreSelected(null)
                                    isExpanded = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedGenre == null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "🎬 Todos los Géneros",
                                        color = if (selectedGenre == null) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        items(genres) { genre ->
                            val isSelected = selectedGenre == genre
                            Surface(
                                onClick = {
                                    onGenreSelected(if (isSelected) null else genre)
                                    isExpanded = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                    Text(
                                        text = genre,
                                        color = if (isSelected) Color.Black else Color.LightGray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Bubble Button with Icon
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            containerColor = if (selectedGenre != null) MaterialTheme.colorScheme.primary else Color(0xFF1E1E1E),
            contentColor = if (selectedGenre != null) Color.Black else Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(8.dp),
            modifier = Modifier
                .border(
                    BorderStroke(
                        1.5.dp,
                        if (selectedGenre != null) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    ),
                    CircleShape
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = "Géneros",
                    tint = if (selectedGenre != null) Color.Black else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = selectedGenre ?: "Géneros",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedGenre != null) Color.Black else Color.White
                )
            }
        }
    }
}
