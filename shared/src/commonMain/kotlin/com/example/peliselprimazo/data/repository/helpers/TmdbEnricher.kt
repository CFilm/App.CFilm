package com.example.peliselprimazo.data.repository.helpers

import com.example.peliselprimazo.core.network.model.TmdbAlternativeTitlesResponse
import com.example.peliselprimazo.core.network.api.TmdbApi
import com.example.peliselprimazo.core.model.CastMember

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

class TmdbEnricher(
    private val tmdbApi: TmdbApi
) {
    suspend fun fetchTmdbData(info: InternalFileInfo, maxSeason: Int? = null): TmdbData? {
        return try {
            val query = cleanQuery(info.cleanTitle)
            val year = info.year

            val searchId: Int? = if (info.isTv || info.isAnime) {
                // Pass 1: Search TV with year and Spanish/English/Japanese locales
                tmdbApi.searchTv(query, year, "es-MX").results.firstOrNull()?.id
                    ?: tmdbApi.searchTv(query, year, "en-US").results.firstOrNull()?.id
                    ?: tmdbApi.searchTv(query, year, "ja-JP").results.firstOrNull()?.id
                    // Pass 2: Search TV without year
                    ?: tmdbApi.searchTv(query, null, "es-MX").results.firstOrNull()?.id
                    ?: tmdbApi.searchTv(query, null, "en-US").results.firstOrNull()?.id
            } else {
                // Pass 1: Search Movie with year and Spanish/English/Japanese locales
                tmdbApi.searchMovie(query, year, "es-MX").results.firstOrNull()?.id
                    ?: tmdbApi.searchMovie(query, year, "en-US").results.firstOrNull()?.id
                    ?: tmdbApi.searchMovie(query, year, "ja-JP").results.firstOrNull()?.id
                    // Pass 2: Search Movie without year
                    ?: tmdbApi.searchMovie(query, null, "es-MX").results.firstOrNull()?.id
                    ?: tmdbApi.searchMovie(query, null, "en-US").results.firstOrNull()?.id
            }

            val id = searchId ?: return null

            if (info.isTv || info.isAnime) {
                val details = tmdbApi.getTvDetails(id)
                val displayTitle = if (info.isAnime) {
                    selectRomajiOrBestTitle(details.name, details.originalName, details.alternativeTitles, query, true)
                } else {
                    if (isAuthorizedTitle(details.name, false)) {
                        details.name
                    } else {
                        query.capitalizeWords()
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
                    "${details.overview}\n\nTítulos alternativos: $altNamesString, ${details.name}, ${details.originalName ?: ""}, $query"
                } else {
                    details.overview
                }

                TmdbData(
                    title = displayTitle,
                    overview = enrichedOverview,
                    posterUrl = posterUrl,
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
                    query.capitalizeWords()
                }
                val altTitlesList = (details.alternativeTitles?.results ?: emptyList()) + (details.alternativeTitles?.titles ?: emptyList())
                val altNamesString = altTitlesList.mapNotNull { it.title ?: it.name }.distinct().take(10).joinToString(", ")
                val enrichedOverview = if (altNamesString.isNotBlank()) {
                    "${details.overview}\n\nTítulos alternativos: $altNamesString, ${details.title}, ${details.originalTitle ?: ""}, $query"
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
            println("TMDB Enricher Error for ${info.cleanTitle}: ${e.message}")
            null
        }
    }

    private fun cleanQuery(title: String): String {
        return title
            .replace(Regex("(?i)\\b(temporada|temp|season|capitulo|cap|ep|episode)\\b.*"), "")
            .replace(Regex("[_\\-.()+/]+"), " ")
            .trim()
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

        val romaji = allAlts.find { alt ->
            val t = alt.title
            val n = alt.name
            val type = alt.type
            val country = alt.isoCountry
            val isRomaji = type?.equals("Romaji", ignoreCase = true) == true
            val isUSorJP = country?.equals("US", ignoreCase = true) == true || country?.equals("JP", ignoreCase = true) == true

            (isRomaji && !t.isNullOrBlank() && !containsJapaneseChars(t)) ||
            (isRomaji && !n.isNullOrBlank() && !containsJapaneseChars(n)) ||
            (isUSorJP && !t.isNullOrBlank() && !containsJapaneseChars(t)) ||
            (!t.isNullOrBlank() && !containsJapaneseChars(t))
        }?.let { alt ->
            val t = alt.title
            val n = alt.name
            if (alt.type?.equals("Romaji", ignoreCase = true) == true && !t.isNullOrBlank() && !containsJapaneseChars(t)) t
            else if (alt.type?.equals("Romaji", ignoreCase = true) == true && !n.isNullOrBlank() && !containsJapaneseChars(n)) n
            else if (!t.isNullOrBlank() && !containsJapaneseChars(t)) t
            else n
        }

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
