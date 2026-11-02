package com.example.peliselprimazo.desktop.data

import com.example.peliselprimazo.data.model.*
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

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "es-MX",
        @Query("api_key") apiKey: String = "4c4847173ec09e41bd12ce8654963e87"
    ): TmdbMovieDetail

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "es-MX",
        @Query("api_key") apiKey: String = "4c4847173ec09e41bd12ce8654963e87"
    ): TmdbTvDetail
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
        val rawFiles = mutableListOf<Triple<String, ServerLink, Boolean>>()

        suspend fun traverseFolder(folderId: String?, parentName: String?, isFolderTv: Boolean) {
            try {
                val response = streamtapeApi.listFolder(login, key, folderId)
                val subfolders = mutableListOf<Pair<String, String>>()

                val currentIsTv = isFolderTv || parentName?.lowercase()?.let {
                    it.contains("serie") || it.contains("anime") || it.contains("tv") || it.contains("temporada")
                } == true

                response.result?.files?.forEach { file ->
                    val fileName = file.name.lowercase()
                    if (videoExtensions.any { fileName.endsWith(it) }) {
                        val name = if (parentName != null && !parentName.equals("series", true) && !parentName.equals("anime", true) && !parentName.equals("peliculas", true)) {
                            "$parentName ${file.name}"
                        } else {
                            file.name
                        }
                        rawFiles.add(Triple(name, ServerLink("Streamtape", file.linkid, "HD"), currentIsTv))
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
                            traverseFolder(subId, subName, currentIsTv)
                        }
                    }
                    deferreds.awaitAll()
                }
            } catch (e: Exception) {
                println("Error traversing folder $folderId: ${e.message}")
            }
        }

        try {
            traverseFolder(null, null, false)

            val initialMovies = rawFiles.map { (rawName, link, isFolderTv) ->
                val cleanTitle = rawName.replace(Regex("\\.(mp4|mkv|avi|mov|wmv|flv|webm|m3u8)$", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("[._\\-/]"), " ")
                    .replace(Regex("(?i)1080p|720p|480p|h264|h265|latino|sub"), "")
                    .trim()

                val isNameTv = rawName.contains("season", true) || rawName.contains("temporada", true) || rawName.contains("s0", true) || rawName.contains("ep", true) || rawName.contains("capitulo", true)
                val contentType = if (isFolderTv || isNameTv) ContentType.TV else ContentType.MOVIE
                val id = (cleanTitle + link.fileId).hashCode()

                Movie(
                    id = id,
                    title = cleanTitle,
                    overview = "Cargando metadatos...",
                    posterUrl = null,
                    serverLinks = listOf(link),
                    contentType = contentType
                )
            }.distinctBy { it.id }

            // Enriquecer con TMDB en paralelo para máxima velocidad
            val enrichedMovies = initialMovies.map { movie ->
                async(Dispatchers.IO) {
                    try {
                        val tmdbResult = if (movie.contentType == ContentType.TV) {
                            tmdbApi.searchTv(movie.title).results.firstOrNull()
                        } else {
                            tmdbApi.searchMovie(movie.title).results.firstOrNull() ?: tmdbApi.searchTv(movie.title).results.firstOrNull()
                        }
                        if (tmdbResult != null) {
                            val posterPath = (tmdbResult as? TmdbMovieResult)?.posterPath ?: (tmdbResult as? TmdbTvResult)?.posterPath
                            val posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                            val overview = (tmdbResult as? TmdbMovieResult)?.overview ?: (tmdbResult as? TmdbTvResult)?.overview ?: movie.overview
                            val rating = (tmdbResult as? TmdbMovieResult)?.voteAverage ?: (tmdbResult as? TmdbTvResult)?.voteAverage ?: 0.0
                            movie.copy(posterUrl = posterUrl, overview = overview, rating = rating)
                        } else {
                            movie
                        }
                    } catch (_: Exception) {
                        movie
                    }
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
