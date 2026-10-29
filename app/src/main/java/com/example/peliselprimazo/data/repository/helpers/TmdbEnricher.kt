package com.example.peliselprimazo.data.repository.helpers

import android.util.Log
import com.example.peliselprimazo.data.model.TmdbAlternativeTitlesResponse
import com.example.peliselprimazo.data.remote.TmdbApi
import com.example.peliselprimazo.domain.model.CastMember
import javax.inject.Inject

data class TmdbData(
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String?,
    val rating: Double,
    val genres: List<String>,
    val trailerUrl: String?,
    val cast: List<CastMember>,
    val duration: String?
)

class TmdbEnricher @Inject constructor(
    private val tmdbApi: TmdbApi
) {
    suspend fun fetchTmdbData(info: InternalFileInfo, maxSeason: Int? = null): TmdbData? {
        return try {
            val id = if (info.isTv || info.isAnime) {
                tmdbApi.searchTv(info.cleanTitle, info.year, "es-MX").results.firstOrNull()?.id
                    ?: tmdbApi.searchTv(info.cleanTitle, info.year, "en-US").results.firstOrNull()?.id
                    ?: tmdbApi.searchTv(info.cleanTitle, info.year, "ja-JP").results.firstOrNull()?.id
            } else {
                tmdbApi.searchMovie(info.cleanTitle, info.year, "es-MX").results.firstOrNull()?.id
                    ?: tmdbApi.searchMovie(info.cleanTitle, info.year, "en-US").results.firstOrNull()?.id
                    ?: tmdbApi.searchMovie(info.cleanTitle, info.year, "ja-JP").results.firstOrNull()?.id
            } ?: return null

            if (info.isTv || info.isAnime) {
                val details = tmdbApi.getTvDetails(id)
                val displayTitle = if (info.isAnime) {
                    selectRomajiOrBestTitle(details.name, details.originalName, details.alternativeTitles, info.cleanTitle, true)
                } else {
                    if (isAuthorizedTitle(details.name, false)) {
                        details.name
                    } else {
                        info.cleanTitle.capitalizeWords()
                    }
                }

                var seasonPosterUrl: String? = null
                if (maxSeason != null && maxSeason > 0) {
                    try {
                        val seasonDetail = tmdbApi.getTvSeasonDetails(id, maxSeason)
                        seasonPosterUrl = seasonDetail.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                    } catch (_: Exception) {}
                }

                val posterUrl = seasonPosterUrl ?: details.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                val altTitlesList = (details.alternativeTitles?.results ?: emptyList()) + (details.alternativeTitles?.titles ?: emptyList())
                val altNamesString = altTitlesList.mapNotNull { it.title ?: it.name }.distinct().take(10).joinToString(", ")
                val enrichedOverview = if (altNamesString.isNotBlank()) {
                    "${details.overview}\n\nTítulos alternativos: $altNamesString, ${details.name}, ${details.originalName ?: ""}, ${info.cleanTitle}"
                } else {
                    details.overview
                }

                TmdbData(
                    title = displayTitle,
                    overview = enrichedOverview,
                    posterUrl = details.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                    backdropUrl = details.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" },
                    releaseDate = details.firstAirDate,
                    rating = details.voteAverage,
                    genres = details.genres?.map { it.name } ?: emptyList(),
                    trailerUrl = details.videos?.results?.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }?.let { "https://www.youtube.com/watch?v=${it.key}" },
                    cast = details.credits?.cast?.take(10)?.map { CastMember(it.name, it.character, it.profilePath?.let { p -> "https://image.tmdb.org/t/p/w185$p" }) } ?: emptyList(),
                    duration = details.episodeRunTime?.firstOrNull()?.let { "$it min" }
                )
            } else {
                val details = tmdbApi.getMovieDetails(id)
                val displayTitle = if (isAuthorizedTitle(details.title, false)) {
                    details.title
                } else {
                    info.cleanTitle.capitalizeWords()
                }
                val altTitlesList = (details.alternativeTitles?.results ?: emptyList()) + (details.alternativeTitles?.titles ?: emptyList())
                val altNamesString = altTitlesList.mapNotNull { it.title ?: it.name }.distinct().take(10).joinToString(", ")
                val enrichedOverview = if (altNamesString.isNotBlank()) {
                    "${details.overview}\n\nTítulos alternativos: $altNamesString, ${details.title}, ${details.originalTitle ?: ""}, ${info.cleanTitle}"
                } else {
                    details.overview
                }

                TmdbData(
                    title = displayTitle,
                    overview = enrichedOverview,
                    posterUrl = details.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                    backdropUrl = details.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" },
                    releaseDate = details.releaseDate,
                    rating = details.voteAverage,
                    genres = details.genres?.map { it.name } ?: emptyList(),
                    trailerUrl = details.videos?.results?.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }?.let { "https://www.youtube.com/watch?v=${it.key}" },
                    cast = details.credits?.cast?.take(10)?.map { CastMember(it.name, it.character, it.profilePath?.let { p -> "https://image.tmdb.org/t/p/w185$p" }) } ?: emptyList(),
                    duration = details.runtime?.let { "$it min" }
                )
            }
        } catch (e: Exception) {
            Log.e("TMDB", "Error fetching data for ${info.cleanTitle}: ${e.message}")
            null
        }
    }

    private fun containsJapaneseChars(text: String): Boolean {
        val japaneseRegex = Regex("[\\u3000-\\u303f\\u3040-\\u309f\\u30a0-\\u30ff\\uff00-\\uffef\\u4e00-\\u9faf]")
        return japaneseRegex.containsMatchIn(text)
    }

    private fun selectRomajiOrBestTitle(
        primaryTitle: String,
        originalTitle: String?,
        altTitles: TmdbAlternativeTitlesResponse?,
        fallbackTitle: String,
        isAnime: Boolean
    ): String {
        val allAlts = (altTitles?.results ?: emptyList()) + (altTitles?.titles ?: emptyList())

        val romaji = allAlts.find { it.type?.equals("Romaji", ignoreCase = true) == true && !it.title.isNullOrBlank() && !containsJapaneseChars(it.title) }?.title
            ?: allAlts.find { it.type?.equals("Romaji", ignoreCase = true) == true && !it.name.isNullOrBlank() && !containsJapaneseChars(it.name) }?.name
            ?: allAlts.find { (it.isoCountry?.equals("US", ignoreCase = true) == true || it.isoCountry?.equals("JP", ignoreCase = true) == true) && !it.title.isNullOrBlank() && !containsJapaneseChars(it.title) }?.title
            ?: allAlts.find { !it.title.isNullOrBlank() && !containsJapaneseChars(it.title) }?.title

        if (!romaji.isNullOrBlank() && !containsJapaneseChars(romaji)) {
            return romaji
        }

        if (isAnime && !fallbackTitle.isBlank() && !containsJapaneseChars(fallbackTitle)) {
            return fallbackTitle
        }

        if (!containsJapaneseChars(primaryTitle)) {
            return primaryTitle
        }
        if (originalTitle != null && !containsJapaneseChars(originalTitle)) {
            return originalTitle
        }
        if (!containsJapaneseChars(fallbackTitle)) {
            return fallbackTitle
        }
        return if (!fallbackTitle.isBlank()) fallbackTitle else primaryTitle
    }

    private fun isAuthorizedTitle(text: String, isAnime: Boolean): Boolean {
        if (text.isBlank()) return false
        val forbiddenRegex = Regex("[\\u0600-\\u06ff\\u0400-\\u04ff\\uac00-\\ud7af]")
        if (forbiddenRegex.containsMatchIn(text)) return false
        if (isAnime && containsJapaneseChars(text)) return false
        return true
    }

    private fun String.capitalizeWords() = split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
