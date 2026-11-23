package com.example.peliselprimazo.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.peliselprimazo.core.model.CastMember
import com.example.peliselprimazo.core.model.ServerLink

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String?,
    val rating: Double,
    val contentType: String,
    val year: String?,
    val genres: List<String>,
    val serverLinks: List<ServerLink>,
    val trailerUrl: String?,
    val cast: List<CastMember>,
    val season: Int?,
    val episode: Int?,
    val duration: String? = null,
    val isLiked: Boolean = false,
    val isWatchLater: Boolean = false,
    val isWatching: Boolean = false,
    val isSaved: Boolean = false,
    val isFinished: Boolean = false,
    val lastPosition: Long = 0L,
    val totalDuration: Long = 0L,
    val timestamp: Long = 0L,
    val lastUpdated: Long = 0L
)
