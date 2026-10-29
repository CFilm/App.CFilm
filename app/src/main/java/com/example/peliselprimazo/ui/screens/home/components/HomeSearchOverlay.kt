package com.example.peliselprimazo.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.peliselprimazo.domain.model.Movie
import com.example.peliselprimazo.ui.components.SearchGrid
import com.example.peliselprimazo.ui.screens.home.HomeViewModel

@Composable
fun SearchContent(
    query: String,
    history: List<String>,
    results: List<Movie>,
    viewModel: HomeViewModel,
    onQueryChange: (String) -> Unit,
    onMovieClick: (Int) -> Unit,
    onClearHistory: () -> Unit
) {
    val searchType by viewModel.searchTypeFilter.collectAsState()
    val searchGenre by viewModel.searchGenreFilter.collectAsState()
    val searchRating by viewModel.searchRatingFilter.collectAsState()
    val searchYear by viewModel.searchYearFilter.collectAsState()

    val hasActiveFilters = searchType != "ALL" || searchGenre != null || searchRating != null || searchYear != null

    Column(modifier = Modifier.fillMaxSize().padding(top = 80.dp)) {
        // Barra de Filtros Avanzados
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = searchType == "ALL",
                    onClick = { viewModel.setSearchTypeFilter("ALL") },
                    label = { Text("Todos") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
            item {
                FilterChip(
                    selected = searchType == "MOVIE",
                    onClick = { viewModel.setSearchTypeFilter(if (searchType == "MOVIE") "ALL" else "MOVIE") },
                    label = { Text("🎬 Películas") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
            item {
                FilterChip(
                    selected = searchType == "TV",
                    onClick = { viewModel.setSearchTypeFilter(if (searchType == "TV") "ALL" else "TV") },
                    label = { Text("📺 Series") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.Black
                    )
                )
            }

            item {
                FilterChip(
                    selected = searchRating == 7.0,
                    onClick = { viewModel.setSearchRatingFilter(if (searchRating == 7.0) null else 7.0) },
                    label = { Text("⭐ 7.0+") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
            item {
                FilterChip(
                    selected = searchRating == 8.0,
                    onClick = { viewModel.setSearchRatingFilter(if (searchRating == 8.0) null else 8.0) },
                    label = { Text("⭐ 8.0+") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.Black
                    )
                )
            }

            val years = listOf("2024", "2023", "2022", "2021", "2020")
            items(years) { y ->
                FilterChip(
                    selected = searchYear == y,
                    onClick = { viewModel.setSearchYearFilter(if (searchYear == y) null else y) },
                    label = { Text(y) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.Black
                    )
                )
            }

            val genres = listOf("Acción", "Comedia", "Terror", "Ciencia Ficción", "Drama", "Suspenso", "Aventura", "Animación", "Fantasía")
            items(genres) { g ->
                FilterChip(
                    selected = searchGenre == g,
                    onClick = { viewModel.setSearchGenreFilter(if (searchGenre == g) null else g) },
                    label = { Text(g) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.Black
                    )
                )
            }

            if (hasActiveFilters) {
                item {
                    TextButton(onClick = { viewModel.resetSearchFilters() }) {
                        Text("Limpiar filtros", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (query.isBlank() && !hasActiveFilters) {
            if (history.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Búsquedas recientes", color = Color.White, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onClearHistory) {
                        Text("Limpiar", color = MaterialTheme.colorScheme.primary)
                    }
                }
                LazyColumn {
                    items(history) { item ->
                        ListItem(
                            headlineContent = { Text(item, color = Color.LightGray) },
                            leadingContent = { Icon(Icons.Rounded.History, contentDescription = null, tint = Color.Gray) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onQueryChange(item) }
                        )
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Busca tus películas o series favoritas por nombre, género o año", color = Color.Gray)
                }
            }
        } else {
            SearchGrid(results, onMovieClick)
        }
    }
}
