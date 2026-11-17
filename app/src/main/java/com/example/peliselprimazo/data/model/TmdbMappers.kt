package com.example.peliselprimazo.data.model

import com.example.peliselprimazo.core.model.CastMember
import com.example.peliselprimazo.core.model.ContentType
import com.example.peliselprimazo.core.model.Movie
import com.example.peliselprimazo.core.model.ServerLink

fun TmdbMovieDetail.toDomain(
    displayTitle: String,
    enrichedOverview: String,
    serverLinks: List<ServerLink> = emptyList()
): Movie {
    val poster = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    val backdrop = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
    val trailer = videos?.results?.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }?.let { "https://www.youtube.com/watch?v=${it.key}" }
    val castList = credits?.cast?.take(10)?.map { 
        CastMember(it.name, it.character, it.profilePath?.let { p -> "https://image.tmdb.org/t/p/w185$p" }) 
    } ?: emptyList()
    val dur = runtime?.let { "$it min" }

    return Movie(
        id = id,
        title = displayTitle,
        overview = enrichedOverview,
        posterUrl = poster,
        backdropUrl = backdrop,
        releaseDate = releaseDate,
        rating = voteAverage,
        serverLinks = serverLinks,
        contentType = ContentType.MOVIE,
        genres = genres?.map { it.name } ?: emptyList(),
        trailerUrl = trailer,
        cast = castList,
        duration = dur,
        year = releaseDate?.take(4)
    )
}

fun TmdbTvDetail.toDomain(
    displayTitle: String,
    enrichedOverview: String,
    seasonPosterUrl: String? = null,
    serverLinks: List<ServerLink> = emptyList()
): Movie {
    val poster = seasonPosterUrl ?: posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    val backdrop = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
    val trailer = videos?.results?.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }?.let { "https://www.youtube.com/watch?v=${it.key}" }
    val castList = credits?.cast?.take(10)?.map { 
        CastMember(it.name, it.character, it.profilePath?.let { p -> "https://image.tmdb.org/t/p/w185$p" }) 
    } ?: emptyList()
    val dur = episodeRunTime?.firstOrNull()?.let { "$it min" }

    return Movie(
        id = id,
        title = displayTitle,
        overview = enrichedOverview,
        posterUrl = poster,
        backdropUrl = backdrop,
        releaseDate = firstAirDate,
        rating = voteAverage,
        serverLinks = serverLinks,
        contentType = ContentType.TV,
        genres = genres?.map { it.name } ?: emptyList(),
        trailerUrl = trailer,
        cast = castList,
        duration = dur,
        year = firstAirDate?.take(4)
    )
}
