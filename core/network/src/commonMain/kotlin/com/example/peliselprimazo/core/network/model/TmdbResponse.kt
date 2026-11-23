package com.example.peliselprimazo.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbSearchResponse<T>(
    @SerialName("results") val results: List<T>
)

@Serializable
data class TmdbMovieResult(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("original_title") val originalTitle: String?,
    @SerialName("overview") val overview: String?,
    @SerialName("poster_path") val posterPath: String?,
    @SerialName("backdrop_path") val backdropPath: String?,
    @SerialName("release_date") val releaseDate: String?,
    @SerialName("vote_average") val voteAverage: Double?
)

@Serializable
data class TmdbTvResult(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("original_name") val originalName: String?,
    @SerialName("overview") val overview: String?,
    @SerialName("poster_path") val posterPath: String?,
    @SerialName("backdrop_path") val backdropPath: String?,
    @SerialName("first_air_date") val firstAirDate: String?,
    @SerialName("vote_average") val voteAverage: Double?
)

@Serializable
data class TmdbAlternativeTitle(
    @SerialName("iso_3166_1") val isoCountry: String?,
    @SerialName("title") val title: String?,
    @SerialName("name") val name: String?,
    @SerialName("type") val type: String?
)

@Serializable
data class TmdbAlternativeTitlesResponse(
    @SerialName("results") val results: List<TmdbAlternativeTitle>?,
    @SerialName("titles") val titles: List<TmdbAlternativeTitle>?
)

@Serializable
data class TmdbMovieDetail(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("original_title") val originalTitle: String?,
    @SerialName("overview") val overview: String,
    @SerialName("poster_path") val posterPath: String?,
    @SerialName("backdrop_path") val backdropPath: String?,
    @SerialName("release_date") val releaseDate: String?,
    @SerialName("vote_average") val voteAverage: Double,
    @SerialName("genres") val genres: List<TmdbGenre>?,
    @SerialName("videos") val videos: TmdbVideoResponse?,
    @SerialName("credits") val credits: TmdbCredits?,
    @SerialName("alternative_titles") val alternativeTitles: TmdbAlternativeTitlesResponse?,
    @SerialName("runtime") val runtime: Int?
)

@Serializable
data class TmdbTvDetail(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("original_name") val originalName: String?,
    @SerialName("overview") val overview: String,
    @SerialName("poster_path") val posterPath: String?,
    @SerialName("backdrop_path") val backdropPath: String?,
    @SerialName("first_air_date") val firstAirDate: String?,
    @SerialName("vote_average") val voteAverage: Double,
    @SerialName("genres") val genres: List<TmdbGenre>?,
    @SerialName("videos") val videos: TmdbVideoResponse?,
    @SerialName("credits") val credits: TmdbCredits?,
    @SerialName("alternative_titles") val alternativeTitles: TmdbAlternativeTitlesResponse?,
    @SerialName("episode_run_time") val episodeRunTime: List<Int>?
)

@Serializable
data class TmdbEpisode(
    @SerialName("season_number") val seasonNumber: Int?,
    @SerialName("episode_number") val episodeNumber: Int?,
    @SerialName("name") val name: String?,
    @SerialName("still_path") val stillPath: String?,
    @SerialName("overview") val overview: String?
)

@Serializable
data class TmdbSeasonDetail(
    @SerialName("season_number") val seasonNumber: Int?,
    @SerialName("poster_path") val posterPath: String?,
    @SerialName("episodes") val episodes: List<TmdbEpisode>?
)

@Serializable
data class TmdbGenre(@SerialName("name") val name: String)

@Serializable
data class TmdbVideoResponse(@SerialName("results") val results: List<TmdbVideo>)
@Serializable
data class TmdbVideo(@SerialName("key") val key: String, @SerialName("site") val site: String, @SerialName("type") val type: String)

@Serializable
data class TmdbCredits(@SerialName("cast") val cast: List<TmdbCast>)
@Serializable
data class TmdbCast(
    @SerialName("name") val name: String,
    @SerialName("character") val character: String,
    @SerialName("profile_path") val profilePath: String?
)
