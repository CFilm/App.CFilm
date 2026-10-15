package com.example.peliselprimazo.data.model

import com.google.gson.annotations.SerializedName

data class TmdbSearchResponse<T>(
    @SerializedName("results") val results: List<T>
)

data class TmdbMovieResult(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("original_title") val originalTitle: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?
)

data class TmdbTvResult(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?
)

data class TmdbAlternativeTitle(
    @SerializedName("iso_3166_1") val isoCountry: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("type") val type: String?
)

data class TmdbAlternativeTitlesResponse(
    @SerializedName("results") val results: List<TmdbAlternativeTitle>?,
    @SerializedName("titles") val titles: List<TmdbAlternativeTitle>?
)

data class TmdbMovieDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("original_title") val originalTitle: String?,
    @SerializedName("overview") val overview: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("genres") val genres: List<TmdbGenre>?,
    @SerializedName("videos") val videos: TmdbVideoResponse?,
    @SerializedName("credits") val credits: TmdbCredits?,
    @SerializedName("alternative_titles") val alternativeTitles: TmdbAlternativeTitlesResponse?,
    @SerializedName("runtime") val runtime: Int?
)

data class TmdbTvDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("overview") val overview: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("genres") val genres: List<TmdbGenre>?,
    @SerializedName("videos") val videos: TmdbVideoResponse?,
    @SerializedName("credits") val credits: TmdbCredits?,
    @SerializedName("alternative_titles") val alternativeTitles: TmdbAlternativeTitlesResponse?,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>?
)

data class TmdbEpisode(
    @SerializedName("season_number") val seasonNumber: Int?,
    @SerializedName("episode_number") val episodeNumber: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("still_path") val stillPath: String?,
    @SerializedName("overview") val overview: String?
)

data class TmdbSeasonDetail(
    @SerializedName("season_number") val seasonNumber: Int?,
    @SerializedName("episodes") val episodes: List<TmdbEpisode>?
)

data class TmdbGenre(val name: String)

data class TmdbVideoResponse(val results: List<TmdbVideo>)
data class TmdbVideo(val key: String, val site: String, val type: String)

data class TmdbCredits(val cast: List<TmdbCast>)
data class TmdbCast(
    @SerializedName("name") val name: String,
    @SerializedName("character") val character: String,
    @SerializedName("profile_path") val profilePath: String?
)
