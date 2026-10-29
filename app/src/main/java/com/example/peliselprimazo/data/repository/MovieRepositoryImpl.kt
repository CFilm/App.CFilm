package com.example.peliselprimazo.data.repository

import android.util.Log
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.peliselprimazo.BuildConfig
import com.example.peliselprimazo.data.local.dao.MovieDao
import com.example.peliselprimazo.data.local.dao.SearchHistoryDao
import com.example.peliselprimazo.data.local.entities.MovieEntity
import com.example.peliselprimazo.data.local.entities.SearchHistoryEntity
import com.example.peliselprimazo.data.model.*
import com.example.peliselprimazo.data.remote.StreamtapeApi
import com.example.peliselprimazo.data.remote.TmdbApi
import com.example.peliselprimazo.data.repository.helpers.FileNameParser
import com.example.peliselprimazo.data.repository.helpers.TmdbEnricher
import com.example.peliselprimazo.domain.model.ContentType
import com.example.peliselprimazo.domain.model.Movie
import com.example.peliselprimazo.domain.model.ServerLink
import com.example.peliselprimazo.domain.model.User
import com.example.peliselprimazo.domain.repository.MovieRepository
import com.example.peliselprimazo.ui.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class RemoteUserData(
    val liked: List<Int> = emptyList(),
    val saved: List<Int> = emptyList(),
    val watchLater: List<Int> = emptyList(),
    val finished: List<Int> = emptyList(),
    val watching: Map<Int, RemoteWatchingProgress> = emptyMap()
)

data class RemoteWatchingProgress(
    val position: Long,
    val duration: Long,
    val timestamp: Long
)

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val streamtapeApi: StreamtapeApi,
    private val tmdbApi: TmdbApi,
    private val tmdbEnricher: TmdbEnricher,
    private val movieDao: MovieDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val notificationHelper: NotificationHelper
) : MovieRepository {

    private val streamtapeLogin = BuildConfig.STREAMTAPE_LOGIN
    private val streamtapeKey = BuildConfig.STREAMTAPE_KEY
    private val tmdbApiKey = BuildConfig.TMDB_API_KEY

    private val parallelSemaphore = Semaphore(10)

    private val _currentUser = MutableStateFlow<User?>(null)
    private val _avatarOverride = MutableStateFlow<String?>(null)
    private val _isServerOnline = MutableStateFlow(true)
    override val isServerOnline: Flow<Boolean> = _isServerOnline.asStateFlow()

    private val lastProgressUpdate = mutableMapOf<Int, Long>()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                _currentUser.value = User(
                    username = firebaseUser.displayName ?: "Usuario",
                    email = firebaseUser.email ?: "",
                    profilePictureUri = firebaseUser.photoUrl?.toString(),
                    isLoggedIn = true
                )
            } else {
                _currentUser.value = null
            }
        }
    }

    override fun getAllContent(): Flow<List<Movie>> = movieDao.getAllMovies().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getPagedContent(): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 2),
            pagingSourceFactory = { movieDao.getPagedMovies() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getMovies(): Flow<List<Movie>> = movieDao.getMoviesByType(ContentType.MOVIE.name).map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getSeries(): Flow<List<Movie>> = movieDao.getMoviesByType(ContentType.TV.name).map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getTrends(): Flow<List<Movie>> = flow {
        try {
            val snapshot = firestore.collection("trends")
                .orderBy("count", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
            
            val trendingIds = snapshot.documents.mapNotNull { it.id.toIntOrNull() }
            val movies = trendingIds.mapNotNull { id ->
                movieDao.getMovieById(id)?.toDomain()
            }
            emit(movies)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun getMovieById(id: Int): Movie? {
        return movieDao.getMovieById(id)?.toDomain()
    }

    override suspend fun getDownloadUrl(serverName: String, fileId: String): String? {
        Log.d("MovieRepository", "Resolving stream URL for server=$serverName, fileId=$fileId")

        if (fileId.startsWith("http://", ignoreCase = true) || fileId.startsWith("https://", ignoreCase = true)) {
            return fileId
        }

        if (serverName.contains("Streamtape", ignoreCase = true) || streamtapeLogin.isNotBlank()) {
            try {
                Log.d("StreamtapeApi", "Requesting download ticket for fileId=$fileId")
                val ticketResp = streamtapeApi.getDownloadTicket(fileId, streamtapeLogin, streamtapeKey)
                Log.d("StreamtapeApi", "Ticket status=${ticketResp.status}, msg=${ticketResp.msg}, result=${ticketResp.result}")

                val result = ticketResp.result
                val ticket = result?.ticket
                if (ticket != null) {
                    val waitSeconds = result.waitTime.coerceAtLeast(1)
                    val waitMs = (waitSeconds * 1000L) + 500L
                    Log.d("StreamtapeApi", "Streamtape ticket acquired. Waiting ${waitMs}ms before requesting link...")
                    delay(waitMs)

                    val dlResp = streamtapeApi.getDownloadLink(fileId, ticket)
                    Log.d("StreamtapeApi", "Download link status=${dlResp.status}, msg=${dlResp.msg}, url=${dlResp.result?.url}")
                    val url = dlResp.result?.url
                    if (!url.isNullOrBlank()) {
                        return url
                    }
                }
            } catch (e: Exception) {
                Log.e("MovieRepository", "Error resolving Streamtape link: ${e.message}", e)
            }

            return "https://streamtape.com/get_video?id=$fileId"
        }

        return null
    }

    override suspend fun refreshContent() {
        withContext(Dispatchers.IO) {
            val syncStartTime = System.currentTimeMillis()
            try {
                val remoteData = fetchRemoteUserData()
                
                val allFiles = try {
                    val list = getStreamtapeFilesSafe()
                    _isServerOnline.value = list.isNotEmpty()
                    list
                } catch (e: Exception) {
                    Log.e("PrimazoSync", "Streamtape server offline: ${e.message}")
                    _isServerOnline.value = false
                    emptyList()
                }

                if (allFiles.isEmpty()) {
                    return@withContext
                }

                val resolvedItems = allFiles.map { triple ->
                    var info = FileNameParser.parseFileName(triple.first)
                    val link = triple.second.copy(season = info.season, episode = info.episode, language = info.language)
                    
                    if (triple.third == "TV") {
                        info = info.copy(isTv = true)
                    } else if (triple.third == "MOVIE") {
                        info = info.copy(isTv = false, isAnime = false)
                    }

                    val id = (info.cleanTitle + (info.year ?: "")).hashCode()
                    Triple(info, link, id)
                }

                val grouped = resolvedItems.groupBy { triple ->
                    val info = triple.first
                    val titleLower = info.cleanTitle.lowercase()
                        .replace(Regex("[-–—:]"), " ")
                        .replace(Regex("\\b(season|s\\d+|hen|part|cour|arc|ova|special|movie|uncut|season\\s*\\d+|temporada\\s*\\d+|capitulo\\s*\\d+|ep\\s*\\d+|vol\\s*\\d+|santen|sensou)\\b.*"), "")
                        .replace(Regex("[^a-z0-9]"), " ")
                        .trim()
                    
                    val words = titleLower.split(Regex("\\s+")).filter { it.isNotBlank() }
                    val baseKey = words.take(2).joinToString(" ").ifBlank { titleLower }
                    
                    if (info.isTv || info.isAnime) "tv|$baseKey" else "movie|$baseKey|${info.year ?: "unknown"}"
                }

                val activeMovieIds = mutableSetOf<Int>()

                // Carga Atómica en 1 solo paso: Procesamiento paralelo en memoria con TMDB y caché antes de guardar en DB
                val deferredMovies = grouped.values.map { items ->
                    async(Dispatchers.IO) {
                        val primaryInfo = items.first().first
                        val allLinks = items.map { it.second }.distinctBy { "${it.serverName}_${it.fileId}" }
                        val id = (primaryInfo.cleanTitle + (primaryInfo.year ?: "")).hashCode()
                        activeMovieIds.add(id)

                        val existing = movieDao.getMovieById(id)
                        val existingNonStreamtape = existing?.serverLinks?.filter { !it.serverName.equals("Streamtape", true) } ?: emptyList()
                        val mergedLinks = (allLinks + existingNonStreamtape).distinctBy { "${it.serverName}_${it.fileId}" }

                        val isLiked = if (remoteData != null) remoteData.liked.contains(id) else existing?.isLiked ?: false
                        val isSaved = if (remoteData != null) remoteData.saved.contains(id) else existing?.isSaved ?: false
                        val isWatchLater = if (remoteData != null) remoteData.watchLater.contains(id) else existing?.isWatchLater ?: false
                        val isFinished = if (remoteData != null) remoteData.finished.contains(id) else existing?.isFinished ?: false
                        
                        val remoteProgress = remoteData?.watching?.get(id)
                        val isWatching = remoteProgress != null || (existing?.isWatching ?: false)
                        val lastPos = remoteProgress?.position ?: existing?.lastPosition ?: 0L
                        val totalDur = remoteProgress?.duration ?: existing?.totalDuration ?: 0L
                        val timestamp = remoteProgress?.timestamp ?: existing?.timestamp ?: System.currentTimeMillis()

                        var title = existing?.title ?: primaryInfo.cleanTitle.capitalizeWords()
                        var overview = existing?.overview ?: "Contenido disponible."
                        var posterUrl = existing?.posterUrl
                        var backdropUrl = existing?.backdropUrl
                        var releaseDate = existing?.releaseDate ?: primaryInfo.year ?: "Desconocido"
                        var rating = existing?.rating ?: 0.0
                        var genres = existing?.genres ?: emptyList()
                        var trailerUrl = existing?.trailerUrl
                        var cast = existing?.cast ?: emptyList()
                        var duration = existing?.duration
                        var finalLinks = mergedLinks

                        val isCompleteInCache = !posterUrl.isNullOrBlank() && overview != "Cargando metadatos..." && !finalLinks.any { it.thumbnailUri.isNullOrBlank() }

                        if (!isCompleteInCache) {
                            parallelSemaphore.withPermit {
                                try {
                                    val seasons = finalLinks.mapNotNull { it.season }.distinct()
                                    val episodeThumbnails = mutableMapOf<Pair<Int, Int>, Triple<String?, String?, String?>>()

                                    if ((primaryInfo.isTv || primaryInfo.isAnime) && tmdbApiKey.isNotBlank() && seasons.isNotEmpty()) {
                                        try {
                                            val tvId = tmdbApi.searchTv(primaryInfo.cleanTitle, primaryInfo.year, "es-MX").results.firstOrNull()?.id
                                                ?: tmdbApi.searchTv(primaryInfo.cleanTitle, primaryInfo.year, "en-US").results.firstOrNull()?.id
                                                ?: tmdbApi.searchTv(primaryInfo.cleanTitle, primaryInfo.year, "ja-JP").results.firstOrNull()?.id

                                            if (tvId != null) {
                                                seasons.forEach { seasonNum ->
                                                    try {
                                                        val seasonDetail = tmdbApi.getTvSeasonDetails(tvId, seasonNum)
                                                        seasonDetail.episodes?.forEach { ep ->
                                                            if (ep.seasonNumber != null && ep.episodeNumber != null) {
                                                                val thumb = ep.stillPath?.let { "https://image.tmdb.org/t/p/w300$it" }
                                                                episodeThumbnails[ep.seasonNumber to ep.episodeNumber] = Triple(thumb, ep.name, ep.overview)
                                                            }
                                                        }
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    }

                                    finalLinks = finalLinks.map { link ->
                                        val s = link.season
                                        val e = link.episode
                                        if (s != null && e != null) {
                                            val thumbInfo = episodeThumbnails[s to e]
                                            link.copy(
                                                thumbnailUri = thumbInfo?.first ?: link.thumbnailUri,
                                                episodeTitle = thumbInfo?.second ?: link.episodeTitle,
                                                overview = thumbInfo?.third ?: link.overview
                                            )
                                        } else {
                                            link
                                        }
                                    }

                                    val maxSeason = finalLinks.mapNotNull { it.season }.maxOrNull()
                                    val tmdbData = tmdbEnricher.fetchTmdbData(primaryInfo, maxSeason)

                                    if (tmdbData != null) {
                                        title = tmdbData.title
                                        overview = tmdbData.overview
                                        posterUrl = tmdbData.posterUrl ?: posterUrl
                                        backdropUrl = tmdbData.backdropUrl ?: backdropUrl
                                        rating = tmdbData.rating
                                        genres = tmdbData.genres
                                        trailerUrl = tmdbData.trailerUrl
                                        cast = tmdbData.cast
                                        duration = tmdbData.duration
                                    }
                                } catch (e: Exception) {
                                    Log.e("PrimazoSync", "Error enriqueciendo ${primaryInfo.cleanTitle}: ${e.message}")
                                }
                            }
                        }

                        MovieEntity(
                            id = id,
                            title = title,
                            overview = overview,
                            posterUrl = posterUrl,
                            backdropUrl = backdropUrl,
                            releaseDate = releaseDate,
                            rating = rating,
                            serverLinks = finalLinks,
                            contentType = when {
                                primaryInfo.isAnime -> ContentType.ANIME.name
                                primaryInfo.isTv -> ContentType.TV.name
                                else -> ContentType.MOVIE.name
                            },
                            genres = genres,
                            year = primaryInfo.year,
                            trailerUrl = trailerUrl,
                            cast = cast,
                            season = primaryInfo.season,
                            episode = primaryInfo.episode,
                            duration = duration,
                            isLiked = isLiked,
                            isSaved = isSaved,
                            isWatchLater = isWatchLater,
                            isWatching = isWatching,
                            isFinished = isFinished,
                            lastPosition = lastPos,
                            totalDuration = totalDur,
                            timestamp = timestamp,
                            lastUpdated = syncStartTime
                        )
                    }
                }

                val results = deferredMovies.awaitAll()
                
                // Conservar todo el contenido guardado en DB aunque su servidor ya no esté disponible
                val existingEntities = movieDao.getAllMoviesSync()
                val inactiveEntities = existingEntities.filter { !activeMovieIds.contains(it.id) }.map {
                    it.copy(serverLinks = emptyList())
                }

                // Inserción única y atómica: La UI recibe portadas y metadatos juntos al instante
                if (results.isNotEmpty() || inactiveEntities.isNotEmpty()) {
                    movieDao.insertMovies(results + inactiveEntities)
                }
            } catch (e: Exception) {
                Log.e("PrimazoSync", "Error en actualización: ${e.message}")
            }
        }
    }

    private suspend fun fetchStreamtapeFilesSafe(): List<Triple<String, ServerLink, String>> {
        val results = mutableListOf<Triple<String, ServerLink, String>>()
        val folderQueue = mutableListOf<Pair<String?, String?>>()
        folderQueue.add(null to null)
        var iters = 0
        val videoExtensions = listOf(".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m3u8")

        while (folderQueue.isNotEmpty() && iters < 500) {
            val pair = folderQueue.removeAt(0)
            val currentId = pair.first
            val currentParent = pair.second
            try {
                Log.d("StreamtapeApi", "Listing folder: id=$currentId, parent=$currentParent")
                val response = streamtapeApi.listFolder(streamtapeLogin, streamtapeKey, currentId)
                Log.d("StreamtapeApi", "listFolder response status: ${response.status}, msg: ${response.msg}, files count: ${response.result?.files?.size ?: 0}")
                response.result?.let { res ->
                    res.files?.forEach { file ->
                        val fileName = file.name.lowercase()
                        if (videoExtensions.any { fileName.endsWith(it) }) {
                            val name = if (currentParent != null && file.name.length < 10) "$currentParent ${file.name}" else file.name
                            val parsed = FileNameParser.parseFileName(name)
                            results.add(Triple(name, ServerLink("Streamtape", file.linkid, "HD", season = parsed.season, episode = parsed.episode, language = parsed.language), if (parsed.isTv || parsed.isAnime) "TV" else "MOVIE"))
                        }
                    }
                    res.folders?.forEach { folder ->
                        val folderName = folder.name.lowercase()
                        if (!folderName.contains("thumbnail") && !folderName.startsWith(".")) {
                            folderQueue.add(folder.id to folder.name)
                        }
                    }
                }
                delay(100)
            } catch (e: Exception) {
                Log.e("StreamtapeApi", "Error listing folder id=$currentId: ${e.message}", e)
            }
            iters++
        }
        Log.d("StreamtapeApi", "Total Streamtape files found: ${results.size}")
        return results
    }

    private suspend fun fetchRemoteUserData(): RemoteUserData? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return try {
            val doc = firestore.collection("users").document(firebaseUser.uid).get().await()
            if (doc.exists()) {
                val liked = (doc.get("liked") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()
                val saved = (doc.get("saved") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()
                val watchLater = (doc.get("watchLater") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()
                val finished = (doc.get("finished") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()
                
                val watchingMap = doc.get("watching") as? Map<String, Map<String, Any>> ?: emptyMap()
                val watching = watchingMap.mapNotNull { (idStr, data) ->
                    val id = idStr.toIntOrNull() ?: return@mapNotNull null
                    id to RemoteWatchingProgress(
                        position = (data["position"] as? Long) ?: 0L,
                        duration = (data["duration"] as? Long) ?: 0L,
                        timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis()
                    )
                }.toMap()

                RemoteUserData(liked, saved, watchLater, finished, watching)
            } else null
        } catch (e: Exception) {
            Log.e("FirebaseUserData", "Error fetching remote user data: ${e.message}")
            null
        }
    }

    private fun getStreamtapeFilesSafe(): List<Triple<String, ServerLink, String>> {
        return runBlocking { fetchStreamtapeFilesSafe() }
    }

    override fun searchMovies(query: String): Flow<List<Movie>> = movieDao.searchMovies(query).map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getLikedContent(): Flow<List<Movie>> = movieDao.getAllMovies().map { list ->
        list.filter { it.isLiked }.map { it.toDomain() }
    }

    override fun getWatchLaterContent(): Flow<List<Movie>> = movieDao.getAllMovies().map { list ->
        list.filter { it.isWatchLater }.map { it.toDomain() }
    }

    override fun getWatchingContent(): Flow<List<Movie>> = movieDao.getAllMovies().map { list ->
        list.filter { it.isWatching }.sortedByDescending { it.timestamp }.map { it.toDomain() }
    }

    override fun getSavedContent(): Flow<List<Movie>> = movieDao.getAllMovies().map { list ->
        list.filter { it.isSaved }.map { it.toDomain() }
    }

    override fun getFinishedContent(): Flow<List<Movie>> = movieDao.getAllMovies().map { list ->
        list.filter { it.isFinished }.map { it.toDomain() }
    }

    override suspend fun toggleLike(movieId: Int) {
        val m = movieDao.getMovieById(movieId) ?: return
        val newValue = !m.isLiked
        movieDao.updateLiked(movieId, newValue)
        
        firebaseAuth.currentUser?.let { user ->
            val userRef = firestore.collection("users").document(user.uid)
            if (newValue) {
                userRef.update("liked", FieldValue.arrayUnion(movieId)).await()
                firestore.collection("trends").document(movieId.toString())
                    .set(mapOf("count" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())
            } else {
                userRef.update("liked", FieldValue.arrayRemove(movieId)).await()
                firestore.collection("trends").document(movieId.toString())
                    .set(mapOf("count" to FieldValue.increment(-1)), com.google.firebase.firestore.SetOptions.merge())
            }
        }
    }

    override suspend fun toggleWatchLater(movieId: Int) {
        val m = movieDao.getMovieById(movieId) ?: return
        val newValue = !m.isWatchLater
        movieDao.updateWatchLater(movieId, newValue)

        firebaseAuth.currentUser?.let { user ->
            val userRef = firestore.collection("users").document(user.uid)
            if (newValue) {
                userRef.update("watchLater", FieldValue.arrayUnion(movieId)).await()
            } else {
                userRef.update("watchLater", FieldValue.arrayRemove(movieId)).await()
            }
        }
    }

    override suspend fun toggleSave(movieId: Int) {
        val m = movieDao.getMovieById(movieId) ?: return
        val newValue = !m.isSaved
        movieDao.updateSaved(movieId, newValue)

        firebaseAuth.currentUser?.let { user ->
            val userRef = firestore.collection("users").document(user.uid)
            if (newValue) {
                userRef.update("saved", FieldValue.arrayUnion(movieId)).await()
            } else {
                userRef.update("saved", FieldValue.arrayRemove(movieId)).await()
            }
        }
    }

    override suspend fun toggleFinished(movieId: Int) {
        val m = movieDao.getMovieById(movieId) ?: return
        val newValue = !m.isFinished
        movieDao.updateFinished(movieId, newValue)

        firebaseAuth.currentUser?.let { user ->
            val userRef = firestore.collection("users").document(user.uid)
            if (newValue) {
                userRef.update("finished", FieldValue.arrayUnion(movieId)).await()
            } else {
                userRef.update("finished", FieldValue.arrayRemove(movieId)).await()
            }
        }
    }

    override suspend fun addToWatching(movieId: Int) {
        val timestamp = System.currentTimeMillis()
        movieDao.updateWatching(movieId, true, timestamp)
        
        firebaseAuth.currentUser?.let { user ->
            val userRef = firestore.collection("users").document(user.uid)
            userRef.update("watching.$movieId.timestamp", timestamp).await()
            
            firestore.collection("trends").document(movieId.toString())
                .set(mapOf("views" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())
        }
    }

    override suspend fun updatePlaybackProgress(movieId: Int, position: Long, duration: Long) {
        movieDao.updatePlaybackProgress(movieId, position, duration)
        
        val now = System.currentTimeMillis()
        val lastUpdate = lastProgressUpdate[movieId] ?: 0L
        
        if (now - lastUpdate > 10000) {
            firebaseAuth.currentUser?.let { user ->
                val userRef = firestore.collection("users").document(user.uid)
                val data = mapOf(
                    "watching.$movieId.position" to position,
                    "watching.$movieId.duration" to duration,
                    "watching.$movieId.timestamp" to now
                )
                userRef.update(data)
                lastProgressUpdate[movieId] = now
            }
        }

        if (duration > 0 && position.toDouble() / duration.toDouble() > 0.95) {
            val m = movieDao.getMovieById(movieId)
            if (m != null && !m.isFinished) {
                toggleFinished(movieId)
            }
        }
    }

    override suspend fun clearWatchingHistory() {}

    override suspend fun clearCache() {
        movieDao.clearAllMovies()
    }

    override fun isMovieLiked(movieId: Int): Flow<Boolean> = movieDao.getAllMovies().map { list ->
        list.any { it.id == movieId && it.isLiked }
    }

    override fun isMovieSaved(movieId: Int): Flow<Boolean> = movieDao.getAllMovies().map { list ->
        list.any { it.id == movieId && it.isSaved }
    }

    override fun isMovieInWatchLater(movieId: Int): Flow<Boolean> = movieDao.getAllMovies().map { list ->
        list.any { it.id == movieId && it.isWatchLater }
    }

    override fun isMovieFinished(movieId: Int): Flow<Boolean> = movieDao.getAllMovies().map { list ->
        list.any { it.id == movieId && it.isFinished }
    }

    override fun getSearchHistory(): Flow<List<String>> = searchHistoryDao.getSearchHistory()

    override suspend fun addSearchQuery(query: String) {
        searchHistoryDao.insertSearch(SearchHistoryEntity(query))
    }

    override suspend fun clearSearchHistory() {
        searchHistoryDao.clearHistory()
    }

    override suspend fun reportBrokenLink(movieId: Int, serverName: String, fileId: String) {
        val report = mapOf(
            "movieId" to movieId,
            "server" to serverName,
            "fileId" to fileId,
            "timestamp" to System.currentTimeMillis(),
            "userId" to (firebaseAuth.currentUser?.uid ?: "anonymous")
        )
        firestore.collection("reports").add(report).await()
    }

    override fun getUser(): Flow<User?> = combine(_currentUser.asStateFlow(), _avatarOverride.asStateFlow()) { user, override ->
        val baseUser = user ?: User(username = "Usuario", isLoggedIn = false)
        if (override != null) {
            baseUser.copy(profilePictureUri = override)
        } else {
            baseUser
        }
    }

    override suspend fun register(username: String, email: String, password: String) {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Error al crear usuario")
        
        _currentUser.value = User(username, email, null, true)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
                
                val userData = mapOf(
                    "username" to username,
                    "email" to email,
                    "createdAt" to System.currentTimeMillis(),
                    "liked" to emptyList<Int>(),
                    "saved" to emptyList<Int>(),
                    "watchLater" to emptyList<Int>(),
                    "finished" to emptyList<Int>(),
                    "watching" to emptyMap<String, Any>()
                )
                firestore.collection("users").document(firebaseUser.uid).set(userData).await()
            } catch (e: Exception) {
                Log.e("AuthBackground", "Error en tareas secundarias de registro: ${e.message}")
            }
        }
    }

    override suspend fun login(email: String, password: String): Boolean {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
        _avatarOverride.value = null
    }

    override suspend fun updateAvatar(uri: String) {
        _avatarOverride.value = uri

        try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setPhotoUri(uri.toUri())
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
                
                firestore.collection("users").document(firebaseUser.uid)
                    .update("profilePictureUri", uri).await()
            }
        } catch (e: Exception) {
            Log.e("MovieRepository", "Error actualizando avatar: ${e.message}")
        }
    }

    private fun MovieEntity.toDomain() = Movie(
        id = id,
        title = title,
        overview = overview,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        releaseDate = releaseDate,
        rating = rating,
        serverLinks = serverLinks,
        contentType = ContentType.valueOf(contentType),
        genres = genres,
        trailerUrl = trailerUrl,
        cast = cast,
        year = year,
        season = season,
        episode = episode,
        duration = duration,
        lastPosition = lastPosition,
        totalDuration = totalDuration,
        isLiked = isLiked,
        isWatchLater = isWatchLater,
        isWatching = isWatching,
        isSaved = isSaved,
        isFinished = isFinished
    )

    private fun String.capitalizeWords() = split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
