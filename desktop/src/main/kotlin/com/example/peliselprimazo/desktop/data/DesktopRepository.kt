package com.example.peliselprimazo.desktop.data

import com.example.peliselprimazo.data.model.*
import com.example.peliselprimazo.data.repository.helpers.FileNameParser
import com.example.peliselprimazo.domain.model.ContentType
import com.example.peliselprimazo.domain.model.Movie
import com.example.peliselprimazo.domain.model.ServerLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DesktopStreamtapeApi {
    @GET("file/listfolder")
    suspend fun listFolder(
        @Query("login") login: String,
        @Query("key") key: String,
        @Query("folder") folderId: String? = null
    ): StreamtapeResponse<StreamtapeFolderList>

    @GET("file/dlticket")
    suspend fun getDownloadTicket(
        @Query("file") fileId: String,
        @Query("login") login: String,
        @Query("key") key: String
    ): StreamtapeResponse<StreamtapeTicket>

    @GET("file/dl")
    suspend fun getDownloadLink(
        @Query("file") fileId: String,
        @Query("ticket") ticket: String
    ): StreamtapeResponse<StreamtapeDownloadLink>
}

interface DesktopTmdbApi {
    @GET("search/movie")
    suspend fun searchMovie(
        @Query("query") query: String,
        @Query("year") year: String? = null,
        @Query("language") language: String = "es-MX",
        @Query("api_key") apiKey: String = "4c4847173ec09e41bd12ce8654963e87"
    ): TmdbSearchResponse<TmdbMovieResult>

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Query("first_air_date_year") year: String? = null,
        @Query("language") language: String = "es-MX",
        @Query("api_key") apiKey: String = "4c4847173ec09e41bd12ce8654963e87"
    ): TmdbSearchResponse<TmdbTvResult>

    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getTvSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("language") language: String = "es-MX",
        @Query("api_key") apiKey: String = "4c4847173ec09e41bd12ce8654963e87"
    ): TmdbSeasonDetail
}

class DesktopRepository {
    private val login = "cc8de27b7d3b3bffdc5c"
    private val key = "8VeOQJod1lCokDg"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .build()
            chain.proceed(request)
        }
        .build()

    private val streamtapeApi = Retrofit.Builder()
        .baseUrl("https://api.streamtape.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DesktopStreamtapeApi::class.java)

    private val tmdbApi = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DesktopTmdbApi::class.java)

    suspend fun fetchMovies(): List<Movie> = withContext(Dispatchers.IO) {
        val videoExtensions = listOf(".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m3u8")
        val rawFiles = mutableListOf<Pair<String, ServerLink>>()

        suspend fun traverseFolder(folderId: String?, parentName: String?) {
            try {
                val response = streamtapeApi.listFolder(login, key, folderId)
                val subfolders = mutableListOf<Pair<String, String>>()

                response.result?.files?.forEach { file ->
                    val fileName = file.name.lowercase()
                    if (videoExtensions.any { fileName.endsWith(it) }) {
                        val name = if (parentName != null && !parentName.equals("series", true) && !parentName.equals("anime", true) && !parentName.equals("peliculas", true)) {
                            "$parentName ${file.name}"
                        } else {
                            file.name
                        }
                        rawFiles.add(name to ServerLink("Streamtape", file.linkid, "HD"))
                    }
                }

                response.result?.folders?.forEach { folder ->
                    val folderName = folder.name.lowercase()
                    if (!folderName.contains("thumbnail") && !folderName.startsWith(".")) {
                        subfolders.add(folder.id to folder.name)
                    }
                }

                if (subfolders.isNotEmpty()) {
                    val deferreds = subfolders.map { (subId, subName) ->
                        async(Dispatchers.IO) {
                            traverseFolder(subId, subName)
                        }
                    }
                    deferreds.awaitAll()
                }
            } catch (e: Exception) {
                println("Error traversing folder $folderId: ${e.message}")
            }
        }

        try {
            traverseFolder(null, null)

            // Parsear cada archivo con FileNameParser
            val parsedFiles = rawFiles.map { (rawName, link) ->
                val parsed = FileNameParser.parseFileName(rawName)
                val updatedLink = link.copy(
                    season = parsed.season,
                    episode = parsed.episode,
                    language = parsed.language
                )
                Triple(parsed, updatedLink, rawName)
            }

            // Agrupar archivos por el título limpio de la serie/película
            val groupedByTitle = parsedFiles.groupBy { (parsed, _, _) ->
                val titleLower = parsed.cleanTitle.lowercase()
                    .replace(Regex("[-–—:]"), " ")
                    .replace(Regex("\\b(season|s\\d+|hen|part|cour|arc|ova|special|movie|uncut|season\\s*\\d+|temporada\\s*\\d+|capitulo\\s*\\d+|ep\\s*\\d+|vol\\s*\\d+)\\b.*"), "")
                    .replace(Regex("[^a-z0-9]"), " ")
                    .trim()
                val words = titleLower.split(Regex("\\s+")).filter { it.isNotBlank() }
                val baseKey = words.take(2).joinToString(" ").ifBlank { titleLower }
                if (parsed.isTv || parsed.isAnime) "tv|$baseKey" else "movie|$baseKey|${parsed.year ?: "unknown"}"
            }

            // Procesar cada grupo para crear 1 SOLO Movie por serie/película
            val enrichedMovies = groupedByTitle.values.map { items ->
                async(Dispatchers.IO) {
                    val firstParsed = items.first().first
                    val allLinks = items.map { it.second }.distinctBy { it.fileId }
                    val isTv = firstParsed.isTv || firstParsed.isAnime
                    val contentType = if (isTv) ContentType.TV else ContentType.MOVIE
                    val id = (firstParsed.cleanTitle + (firstParsed.year ?: "")).hashCode()

                    var title = firstParsed.cleanTitle.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    var overview = "Contenido disponible."
                    var posterUrl: String? = null
                    var backdropUrl: String? = null
                    var rating = 0.0
                    var finalLinks = allLinks

                    try {
                        if (isTv) {
                            val tvResult = tmdbApi.searchTv(firstParsed.cleanTitle, firstParsed.year).results.firstOrNull()
                                ?: tmdbApi.searchTv(firstParsed.cleanTitle).results.firstOrNull()

                            if (tvResult != null) {
                                title = tvResult.name
                                overview = tvResult.overview?.ifBlank { overview } ?: overview
                                posterUrl = tvResult.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                                backdropUrl = tvResult.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
                                rating = tvResult.voteAverage ?: 0.0

                                // Obtener detalles de episodios para miniaturas y títulos
                                val seasons = allLinks.mapNotNull { it.season }.distinct()
                                val epThumbnails = mutableMapOf<Pair<Int, Int>, Triple<String?, String?, String?>>()

                                seasons.forEach { seasonNum ->
                                    try {
                                        val seasonDetail = tmdbApi.getTvSeasonDetails(tvResult.id, seasonNum)
                                        seasonDetail.episodes?.forEach { ep ->
                                            if (ep.seasonNumber != null && ep.episodeNumber != null) {
                                                val thumb = ep.stillPath?.let { "https://image.tmdb.org/t/p/w300$it" }
                                                epThumbnails[ep.seasonNumber to ep.episodeNumber] = Triple(thumb, ep.name, ep.overview)
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }

                                finalLinks = allLinks.map { link ->
                                    val s = link.season ?: 1
                                    val e = link.episode ?: 1
                                    val epInfo = epThumbnails[s to e]
                                    if (epInfo != null) {
                                        link.copy(
                                            thumbnailUri = epInfo.first,
                                            episodeTitle = epInfo.second,
                                            overview = epInfo.third
                                        )
                                    } else {
                                        link
                                    }
                                }
                            }
                        } else {
                            val movieResult = tmdbApi.searchMovie(firstParsed.cleanTitle, firstParsed.year).results.firstOrNull()
                                ?: tmdbApi.searchMovie(firstParsed.cleanTitle).results.firstOrNull()

                            if (movieResult != null) {
                                title = movieResult.title
                                overview = movieResult.overview?.ifBlank { overview } ?: overview
                                posterUrl = movieResult.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                                backdropUrl = movieResult.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
                                rating = movieResult.voteAverage ?: 0.0
                            }
                        }
                    } catch (_: Exception) {}

                    Movie(
                        id = id,
                        title = title,
                        overview = overview,
                        posterUrl = posterUrl,
                        backdropUrl = backdropUrl,
                        rating = rating,
                        serverLinks = finalLinks,
                        contentType = contentType
                    )
                }
            }.awaitAll()

            return@withContext enrichedMovies
        } catch (e: Exception) {
            println("Error fetching desktop files: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun getStreamUrl(fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            val tResponse = streamtapeApi.getDownloadTicket(fileId, login, key)
            val t = tResponse.result ?: return@withContext null
            val urlResponse = streamtapeApi.getDownloadLink(fileId, t.ticket)
            val url = urlResponse.result?.url
            if (url != null) {
                return@withContext if (url.startsWith("//")) "https:$url" else if (url.startsWith("/")) "https://streamtape.com$url" else url
            }
        } catch (e: Exception) {
            println("Error getting stream url: ${e.message}")
        }
        null
    }
}
