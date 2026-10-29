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
        val moviesList = mutableListOf<Movie>()
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

            rawFiles.forEach { (rawName, link) ->
                val cleanTitle = rawName.replace(Regex("\\.(mp4|mkv|avi|mov|wmv|flv|webm|m3u8)$", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("[._\\-/]"), " ")
                    .replace(Regex("(?i)1080p|720p|480p|h264|h265|latino|sub"), "")
                    .trim()

                val id = cleanTitle.hashCode()

                var posterUrl: String? = null
                var overview: String = "Contenido disponible."
                try {
                    val tmdbResult = tmdbApi.searchMovie(cleanTitle).results.firstOrNull()
                        ?: tmdbApi.searchTv(cleanTitle).results.firstOrNull()
                    if (tmdbResult != null) {
                        val posterPath = (tmdbResult as? TmdbMovieResult)?.posterPath ?: (tmdbResult as? TmdbTvResult)?.posterPath
                        posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                        overview = (tmdbResult as? TmdbMovieResult)?.overview ?: (tmdbResult as? TmdbTvResult)?.overview ?: overview
                    }
                } catch (_: Exception) {}

                moviesList.add(
                    Movie(
                        id = id,
                        title = cleanTitle,
                        overview = overview,
                        posterUrl = posterUrl,
                        serverLinks = listOf(link),
                        contentType = ContentType.MOVIE
                    )
                )
            }
        } catch (e: Exception) {
            println("Error fetching desktop files: ${e.message}")
        }
        moviesList
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
