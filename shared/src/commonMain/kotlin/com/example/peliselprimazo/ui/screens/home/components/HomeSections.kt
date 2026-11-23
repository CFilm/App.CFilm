package com.example.peliselprimazo.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.peliselprimazo.core.model.Movie
import com.example.peliselprimazo.ui.components.MovieCardItem
import com.example.peliselprimazo.ui.components.VerticalMovieGrid
import com.example.peliselprimazo.ui.components.WatchingCardItem
import com.example.peliselprimazo.ui.screens.home.HomeSection

@Composable
fun HomeSectionsContent(
    sections: List<HomeSection>,
    filteredList: List<Movie>,
    selectedGenre: String?,
    onMovieClick: (Int) -> Unit,
    onDirectPlayClick: (Movie) -> Unit,
    announcement: String? = null
) {
    var selectedSection by remember { mutableStateOf<HomeSection?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedSection != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(top = 105.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedSection = null },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = selectedSection!!.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                VerticalMovieGrid(
                    movies = selectedSection!!.items,
                    onMovieClick = onMovieClick,
                    topPadding = 0.dp
                )
            }
        } else if (selectedGenre != null) {
            VerticalMovieGrid(movies = filteredList, onMovieClick = onMovieClick)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp)) {
                if (!announcement.isNullOrBlank()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text(text = announcement, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                itemsIndexed(sections, key = { _, section -> section.title }) { _, section ->
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = section.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { selectedSection = section },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "Ver más",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(section.items, key = { it.id }) { movie -> 
                                if (section.isWatchingSection) {
                                    WatchingCardItem(movie) { onDirectPlayClick(movie) }
                                } else {
                                    MovieCardItem(movie, { onMovieClick(it) }, width = 130.dp)
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
fun CategoryTabContent(
    sections: List<HomeSection>,
    filteredList: List<Movie>,
    selectedGenre: String?,
    onMovieClick: (Int) -> Unit
) {
    var selectedSection by remember { mutableStateOf<HomeSection?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedSection != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(top = 105.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedSection = null },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = selectedSection!!.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                VerticalMovieGrid(
                    movies = selectedSection!!.items,
                    onMovieClick = onMovieClick,
                    topPadding = 0.dp
                )
            }
        } else if (selectedGenre != null) {
            VerticalMovieGrid(movies = filteredList, onMovieClick = onMovieClick)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp)) {
                itemsIndexed(sections, key = { _, section -> section.title }) { _, section ->
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = section.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { selectedSection = section },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "Ver más",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(section.items, key = { it.id }) { movie ->
                                MovieCardItem(movie, { onMovieClick(it) }, width = 130.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
