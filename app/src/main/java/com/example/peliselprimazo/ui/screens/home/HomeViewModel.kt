package com.example.peliselprimazo.ui.screens.home

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.peliselprimazo.data.AdsManager
import com.example.peliselprimazo.data.UpdateManager
import com.example.peliselprimazo.data.util.NetworkMonitor
import com.example.peliselprimazo.domain.model.ContentType
import com.example.peliselprimazo.domain.model.Movie
import com.example.peliselprimazo.domain.repository.MovieRepository
import com.example.peliselprimazo.domain.usecase.GetStreamUrlUseCase
import com.example.peliselprimazo.domain.usecase.GetUpdateConfigUseCase
import com.example.peliselprimazo.domain.usecase.SyncContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val repository: MovieRepository,
    private val syncContentUseCase: SyncContentUseCase,
    private val getStreamUrlUseCase: GetStreamUrlUseCase,
    private val networkMonitor: NetworkMonitor,
    private val adsManager: AdsManager,
    private val getUpdateConfigUseCase: GetUpdateConfigUseCase,
    private val updateManager: UpdateManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isLoadingAuth = MutableStateFlow(false)
    val isLoadingAuth = _isLoadingAuth.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isPreloading = MutableStateFlow(false)
    val isPreloading = _isPreloading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isServerOnline = MutableStateFlow(true)
    val isServerOnline = _isServerOnline.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre = _selectedGenre.asStateFlow()

    val user = repository.getUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val allContent = repository.getAllContent()
        .catch { Log.e("HomeViewModel", "Error cargando todo el contenido", it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchingContent = repository.getWatchingContent()
        .catch { Log.e("HomeViewModel", "Error cargando contenido viendo", it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val finishedContent = repository.getFinishedContent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedContent = repository.getSavedContent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedContent = repository.getLikedContent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val watchLaterContent = repository.getWatchLaterContent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory = repository.getSearchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendingContent = repository.getTrends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isRewardedVideoReady = adsManager.isRewardedVideoReady

    // Update Configuration
    val updateConfig = getUpdateConfigUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isDownloading = updateManager.isDownloading
    val downloadProgress = updateManager.downloadProgress
    val updateError = updateManager.error

    val moviesListFiltered = combine(allContent, _selectedGenre) { list, genre ->
        list.filter { it.contentType == ContentType.MOVIE && (genre == null || it.genres.contains(genre)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seriesListFiltered = combine(allContent, _selectedGenre) { list, genre ->
        list.filter { it.contentType == ContentType.TV && (genre == null || it.genres.contains(genre)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchTypeFilter = MutableStateFlow<String>("ALL")
    val searchGenreFilter = MutableStateFlow<String?>(null)
    val searchRatingFilter = MutableStateFlow<Double?>(null)
    val searchYearFilter = MutableStateFlow<String?>(null)

    val searchResults = combine(
        allContent,
        _searchQuery,
        searchTypeFilter,
        searchGenreFilter,
        searchRatingFilter,
        searchYearFilter
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val list = flows[0] as List<Movie>
        val query = flows[1] as String
        val type = flows[2] as String
        val genre = flows[3] as String?
        val rating = flows[4] as Double?
        val year = flows[5] as String?

        var filtered = list

        if (query.isNotBlank()) {
            filtered = filtered.filter { movie ->
                movie.title.contains(query, ignoreCase = true) ||
                movie.overview.contains(query, ignoreCase = true)
            }
        }

        if (type != "ALL") {
            filtered = filtered.filter { it.contentType.name.equals(type, ignoreCase = true) }
        }

        if (genre != null && genre != "ALL") {
            filtered = filtered.filter { m -> m.genres.any { g -> g.contains(genre, ignoreCase = true) } }
        }

        if (rating != null && rating > 0.0) {
            filtered = filtered.filter { it.rating >= rating }
        }

        if (year != null && year != "ALL") {
            filtered = filtered.filter { it.year == year || (it.releaseDate != null && it.releaseDate.startsWith(year)) }
        }

        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalHours = allContent.map { list ->
        val totalMs = list.sumOf { it.totalDuration }
        totalMs / (1000 * 60 * 60)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val homeSections = combine(allContent, watchingContent, trendingContent) { list, watching, trends ->
        createSections(list, watching, trends)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movieSections = allContent.map { list ->
        createMovieSections(list.filter { it.contentType == ContentType.MOVIE })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seriesSections = allContent.map { list ->
        createSeriesSections(list.filter { it.contentType == ContentType.TV })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun createDiverseSelection(list: List<Movie>): List<Movie> {
        if (list.isEmpty()) return emptyList()
        val groupedByGenre = mutableMapOf<String, MutableList<Movie>>()
        list.forEach { movie ->
            val primaryGenre = movie.genres.firstOrNull() ?: "General"
            groupedByGenre.getOrPut(primaryGenre) { mutableListOf<Movie>() }.add(movie)
        }
        
        groupedByGenre.values.forEach { group -> group.sortByDescending { it.rating } }
        
        val result = mutableListOf<Movie>()
        var addedAny = true
        var index = 0
        while (result.size < 15 && addedAny) {
            addedAny = false
            for (group in groupedByGenre.values) {
                if (index < group.size && result.none { it.id == group[index].id }) {
                    result.add(group[index])
                    addedAny = true
                    if (result.size >= 15) break
                }
            }
            index++
        }
        return if (result.isNotEmpty()) result else list.sortedByDescending { it.rating }.take(15)
    }

    private fun createSections(list: List<Movie>, watching: List<Movie>, trends: List<Movie>): List<HomeSection> {
        if (list.isEmpty()) return emptyList()
        val watchLater = list.filter { it.isWatchLater }

        val userLikedOrWatched = list.filter { it.isLiked || it.isSaved || it.isWatching }
        val favoriteGenres = userLikedOrWatched.flatMap { it.genres }.groupingBy { it }.eachCount()

        val personalizedForYou = if (favoriteGenres.isNotEmpty()) {
            list.filter { !it.isFinished && !it.isWatching }
                .sortedByDescending { movie ->
                    var score = movie.rating
                    movie.genres.forEach { genre ->
                        val count = favoriteGenres[genre] ?: 0
                        score += count * 2.0
                    }
                    if (movie.isWatchLater) score += 3.0
                    score
                }
                .take(12)
        } else {
            createDiverseSelection(list).take(12)
        }

        val globalTrends = if (trends.isNotEmpty()) trends.take(12) else list.sortedByDescending { it.rating }.take(12)
        val sortedByRating = list.sortedByDescending { it.rating }
        val exclusives = list.sortedByDescending { it.releaseDate }.take(12)
        val topRated = sortedByRating.take(12)
        val specials = sortedByRating.drop(4).take(12).let { if (it.isNotEmpty()) it else sortedByRating.take(12) }
        val masterWorks = sortedByRating.filter { it.rating >= 7.5 }.take(12).let { if (it.isNotEmpty()) it else sortedByRating.take(12) }

        return buildList {
            if (watching.isNotEmpty()) add(HomeSection("🍿 Continuar Viendo", watching.take(12), isWatchingSection = true))
            add(HomeSection("✨ Para Ti", personalizedForYou))
            add(HomeSection("🔥 Tendencias Globales", globalTrends))
            add(HomeSection("🚀 Estrenos Exclusivos", exclusives))
            add(HomeSection("⭐ Los Más Valorados", topRated))
            if (watchLater.isNotEmpty()) add(HomeSection("🕒 Guardados Para Ver Después", watchLater.take(12)))
            add(HomeSection("❤️ Recomendados Especiales", specials))
            add(HomeSection("🏆 Obras Maestras De La Pantalla", masterWorks))

            val genreSections = listOf(
                "💥 Acción & Adrenalina" to listOf("Acción"),
                "🛸 Ciencia Ficción & Futuro" to listOf("Ciencia Ficción", "Sci-Fi"),
                "😱 Terror & Horror Nocturno" to listOf("Terror", "Horror"),
                "😂 Comedia & Risas" to listOf("Comedia"),
                "🎭 Dramas Intensos" to listOf("Drama"),
                "🦄 Fantasía & Mundos Mágicos" to listOf("Fantasía", "Fantasy"),
                "🔍 Misterio & Policías" to listOf("Misterio", "Crimen"),
                "💘 Romances & Amores" to listOf("Romance"),
                "🌴 Aventuras & Exploración" to listOf("Aventura"),
                "👾 Animación & Dibujos" to listOf("Animación"),
                "🦸‍♂️ Superhéroes & Cómics" to listOf("Acción", "Fantasía"),
                "🔪 Suspenso & Thrillers" to listOf("Suspenso"),
                "📜 Hechos Reales & Historia" to listOf("Historia", "Biografía"),
                "👪 Cine & Entretenimiento Familiar" to listOf("Familia"),
                "🌌 Viajes En El Tiempo & Espacio" to listOf("Ciencia Ficción"),
                "🥋 Artes Marciales & Combate" to listOf("Acción"),
                "💎 Joyas Ocultas De La Pantalla" to listOf("Drama"),
                "🎶 Música & Espectáculo" to listOf("Música"),
                "🔥 Éxitos De Cartelera" to listOf("Acción", "Aventura"),
                "🧠 Enigmas & Mente" to listOf("Misterio", "Suspenso"),
                "🏹 Leyendas & Reinos" to listOf("Fantasía", "Aventura"),
                "🌊 Odiseas & Naturaleza" to listOf("Aventura"),
                "🏙️ Historias Urbanas" to listOf("Drama", "Crimen"),
                "💣 Tensión Al Límite" to listOf("Suspenso", "Acción"),
                "🌟 Favoritas De La Crítica" to listOf("Drama"),
                "🐉 Universos Fantásticos" to listOf("Fantasía"),
                "🎬 Grandes Producciones" to listOf("Acción", "Ciencia Ficción")
            )

            genreSections.forEachIndexed { idx, (title, genres) ->
                val filtered = list.filter { m -> genres.any { g -> m.genres.any { mg -> mg.contains(g, ignoreCase = true) } } }
                val content = if (filtered.isNotEmpty()) {
                    filtered.sortedByDescending { it.rating }.take(12)
                } else {
                    sortedByRating.drop((idx * 2) % sortedByRating.size).take(12)
                }
                add(HomeSection(title, content))
            }
        }.take(35)
    }

    private fun createMovieSections(list: List<Movie>): List<HomeSection> {
        if (list.isEmpty()) return emptyList()

        val topMovies = list.sortedByDescending { it.rating }
        val recentMovies = list.sortedByDescending { it.releaseDate }
        val diverseRecommended = createDiverseSelection(list)
        val highRatedMovies = topMovies.drop(3).take(15).let { if (it.isNotEmpty()) it else topMovies.take(15) }
        val blockbusterMovies = topMovies.take(15)
        val classicsMovies = recentMovies.takeLast(15).let { if (it.isNotEmpty()) it else topMovies.take(15) }

        return buildList {
            add(HomeSection("🎬 Recomendados De Películas", diverseRecommended))
            add(HomeSection("🔥 Películas Más Populares", topMovies.take(15)))
            add(HomeSection("🚀 Recientes & Estrenos", recentMovies.take(15)))
            add(HomeSection("⭐ Las Mejor Valoradas", highRatedMovies))
            add(HomeSection("🍿 Cine De Taquilla & Blockbusters", blockbusterMovies))
            add(HomeSection("🌟 Grandes Clásicos Del Cine", classicsMovies))

            val categoryMap = listOf(
                "💥 Pura Acción & Explosiones" to listOf("Acción"),
                "🛸 Ciencia Ficción & Universos" to listOf("Ciencia Ficción", "Sci-Fi"),
                "👻 Espantosas & Terror" to listOf("Terror", "Horror"),
                "😂 Para Morirse De Risa" to listOf("Comedia"),
                "🎭 Historias Dramáticas" to listOf("Drama"),
                "🔪 Suspenso & Thrillers" to listOf("Suspenso"),
                "🏰 Fantasía & Magia" to listOf("Fantasía", "Fantasy"),
                "🕵️‍♂️ Crimen & Investigaciones" to listOf("Crimen", "Misterio"),
                "💘 Historias De Amor" to listOf("Romance"),
                "🌴 Aventuras Inolvidables" to listOf("Aventura"),
                "👾 Animación & Dibujos" to listOf("Animación"),
                "👪 Para Toda La Familia" to listOf("Familia"),
                "📜 Hechos Reales & Basado En Historia" to listOf("Historia", "Biografía"),
                "🌌 Odiseas Espaciales & Futuro" to listOf("Ciencia Ficción"),
                "🗡️ Espada & Brujería" to listOf("Fantasía"),
                "🧠 Thrillers Psicológicos" to listOf("Suspenso"),
                "🥋 Artes Marciales & Combate" to listOf("Acción"),
                "💎 Selección Imprescindible" to listOf("Drama"),
                "🔥 Películas Más Vistas De La Semana" to listOf("Acción", "Aventura"),
                "🎶 Musicales & Ritmo" to listOf("Música"),
                "🏎️ Velocidad & Persecuciones" to listOf("Acción"),
                "🧟‍♂️ Invasión & Apocalipsis" to listOf("Terror", "Ciencia Ficción"),
                "🕵️ Chispas & Comedia Romántica" to listOf("Comedia", "Romance"),
                "🏴‍☠️ Piratas & Misiones" to listOf("Aventura"),
                "🏛️ Épicas De Época" to listOf("Historia", "Drama"),
                "🔫 Agentes & Espionaje" to listOf("Acción", "Suspenso"),
                "🌌 Cyberpunk & Robots" to listOf("Ciencia Ficción"),
                "🧩 Rompecabezas & Giros" to listOf("Misterio"),
                "🎬 Selección Estelar" to listOf("Drama", "Acción")
            )

            categoryMap.forEachIndexed { idx, (title, genres) ->
                val filtered = list.filter { m -> genres.any { g -> m.genres.any { mg -> mg.contains(g, ignoreCase = true) } } }
                val content = if (filtered.isNotEmpty()) {
                    filtered.sortedByDescending { it.rating }.take(15)
                } else {
                    topMovies.drop((idx * 2) % topMovies.size).take(15)
                }
                add(HomeSection(title, content))
            }
        }.take(35)
    }

    private fun createSeriesSections(list: List<Movie>): List<HomeSection> {
        if (list.isEmpty()) return emptyList()

        val topSeries = list.sortedByDescending { it.rating }
        val recentSeries = list.sortedByDescending { it.releaseDate }
        val diverseSeries = createDiverseSelection(list)
        val highRatedSeries = topSeries.drop(3).take(15).let { if (it.isNotEmpty()) it else topSeries.take(15) }
        val weekendBingeSeries = recentSeries.take(15)
        val hallOfFameSeries = topSeries.take(15)

        return buildList {
            add(HomeSection("📺 Recomendados De Series", diverseSeries))
            add(HomeSection("🔥 Series Más Populares", topSeries.take(15)))
            add(HomeSection("🚀 Recientes & Estrenos", recentSeries.take(15)))
            add(HomeSection("🏆 Series Aclamadas", highRatedSeries))
            add(HomeSection("🍿 Maratones De Fin De Semana", weekendBingeSeries))
            add(HomeSection("🌟 Salón De La Fama De Series", hallOfFameSeries))

            val categoryMap = listOf(
                "💥 Series De Acción" to listOf("Acción"),
                "🛸 Universos & Ciencia Ficción" to listOf("Ciencia Ficción", "Sci-Fi"),
                "😱 Suspenso & Tensión" to listOf("Suspenso"),
                "😂 Comedias Divertidas" to listOf("Comedia"),
                "🎭 Dramas & Pasiones" to listOf("Drama"),
                "🔍 Casos & Misterios" to listOf("Misterio"),
                "🐉 Fantasía Épica" to listOf("Fantasía", "Fantasy"),
                "🚓 Crimen & Mafia" to listOf("Crimen"),
                "💘 Romances De Serie" to listOf("Romance"),
                "🤠 Aventuras & Fronteras" to listOf("Aventura"),
                "🏫 Sobrenatural & Paranormal" to listOf("Terror"),
                "👾 Animación & Anime" to listOf("Animación"),
                "👑 Intrigas De Poder" to listOf("Drama"),
                "🕵️‍♂️ Detectives & Policiales" to listOf("Misterio", "Crimen"),
                "🩸 Terror & Criaturas" to listOf("Terror"),
                "🚀 Ciencia Ficción Espacial" to listOf("Ciencia Ficción"),
                "🏆 Grandes Sagas De Series" to listOf("Fantasía"),
                "⭐ Éxitos De Temporada" to listOf("Acción"),
                "🌟 Imperdibles En Pantalla" to listOf("Drama"),
                "⌛ Miniseries & Ediciones Especiales" to listOf("Drama", "Misterio"),
                "🤖 Inteligencia & Futuro" to listOf("Ciencia Ficción"),
                "🏙️ Dramas De Ciudad" to listOf("Drama"),
                "🎮 Adaptaciones & Universos" to listOf("Acción", "Fantasía"),
                "🔥 Series Más Comentadas" to listOf("Acción", "Drama"),
                "🌌 Dimensiones & Magia" to listOf("Fantasía"),
                "🗡️ Venganza & Honor" to listOf("Acción", "Drama"),
                "🧩 Misterios Desconcertantes" to listOf("Misterio"),
                "🎉 Comedias Ligeras" to listOf("Comedia"),
                "📺 Grandes Estrenos De La Televisión" to listOf("Drama", "Acción")
            )

            categoryMap.forEachIndexed { idx, (title, genres) ->
                val filtered = list.filter { m -> genres.any { g -> m.genres.any { mg -> mg.contains(g, ignoreCase = true) } } }
                val content = if (filtered.isNotEmpty()) {
                    filtered.sortedByDescending { it.rating }.take(15)
                } else {
                    topSeries.drop((idx * 2) % topSeries.size).take(15)
                }
                add(HomeSection(title, content))
            }
        }.take(35)
    }

    private val _selectedAccentColor = MutableStateFlow<String?>(null)
    val selectedAccentColor = _selectedAccentColor.asStateFlow()

    fun setAccentColor(hex: String) {
        _selectedAccentColor.value = hex
        val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_accent_color", hex).apply()
    }

    init {
        Log.d("CFilmHome", "HomeViewModel initialized")
        val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedAccent = prefs.getString("selected_accent_color", null)
        if (savedAccent != null) {
            _selectedAccentColor.value = savedAccent
        }

        viewModelScope.launch {
            allContent.collect { list ->
                Log.d("CFilmHome", "AllContent collected: ${list.size} items")
                if (list.isNotEmpty()) {
                    _isLoading.value = false
                }
            }
        }

        // Auto-sync when internet / server connectivity returns
        var isInitial = true
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                if (isInitial) {
                    isInitial = false
                    return@collect
                }
                if (connected) {
                    Log.d("CFilmHome", "Network connectivity restored, auto-syncing...")
                    refreshData()
                }
            }
        }

        refreshData(isFirstLoad = true)
        syncConfig()
    }

    fun syncConfig() {
        viewModelScope.launch {
            try {
                getUpdateConfigUseCase.sync()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error syncing config: ${e.message}")
            }
        }
    }

    fun startUpdate(url: String) {
        updateManager.downloadAndInstall(url)
    }

    fun checkPendingInstall() {
        updateManager.checkAndInstallPending()
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
            searchGenreFilter.value = null
            searchTypeFilter.value = "ALL"
            searchRatingFilter.value = null
            searchYearFilter.value = null
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            viewModelScope.launch {
                repository.addSearchQuery(query)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        setSearchQuery(query)
    }

    fun onSearchActiveChange(active: Boolean) {
        setSearchActive(active)
    }

    fun setSearchTypeFilter(type: String) { searchTypeFilter.value = type }
    fun setSearchGenreFilter(genre: String?) { searchGenreFilter.value = genre }
    fun setSearchRatingFilter(rating: Double?) { searchRatingFilter.value = rating }
    fun setSearchYearFilter(year: String?) { searchYearFilter.value = year }

    fun resetSearchFilters() {
        searchTypeFilter.value = "ALL"
        searchGenreFilter.value = null
        searchRatingFilter.value = null
        searchYearFilter.value = null
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun setSelectedGenre(genre: String?) {
        _selectedGenre.value = if (_selectedGenre.value == genre) null else genre
    }

    fun showRewardedVideo(activity: Activity, placementName: String? = null, onReward: () -> Unit) {
        adsManager.showRewardedVideo(activity, placementName, onReward)
    }

    fun launchTestSuite(activity: Activity) {
        adsManager.launchTestSuite(activity)
    }

    fun refreshData(isFirstLoad: Boolean = false) {
        viewModelScope.launch {
            Log.d("CFilmHome", "refreshData called: isFirstLoad=$isFirstLoad, cachedItems=${allContent.value.size}")
            if (isFirstLoad) {
                if (allContent.value.isNotEmpty()) {
                    _isLoading.value = false
                } else {
                    _isLoading.value = true
                    _uiState.value = HomeUiState.Loading
                }
            } else {
                _isRefreshing.value = true
            }
            try {
                syncContentUseCase()
                Log.d("CFilmHome", "syncContentUseCase completed successfully")
                _error.value = null
                _isServerOnline.value = true
                _uiState.value = HomeUiState.Success(allContent.value.size)
            } catch (e: Exception) {
                Log.e("CFilmHome", "refreshData exception (offline/error): ${e.message}", e)
                _error.value = "Sin servidor disponible - Mostrando contenido guardado"
                _isServerOnline.value = false
                _uiState.value = HomeUiState.Error("Sin servidor disponible - Mostrando contenido guardado")
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun prepareAndPlay(movie: Movie, onReady: (String) -> Unit) {
        val firstLink = movie.serverLinks.firstOrNull() ?: return
        viewModelScope.launch {
            _isPreloading.value = true
            try {
                val videoUrl = getStreamUrlUseCase(firstLink.serverName, firstLink.fileId)
                if (!videoUrl.isNullOrBlank()) {
                    onReady(videoUrl)
                } else {
                    _error.value = "No se pudo obtener el enlace de reproducción"
                }
            } catch (e: Exception) {
                _error.value = "Error al preparar video: ${e.message}"
            } finally {
                _isPreloading.value = false
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            refreshData()
        }
    }

    fun clearUpdateError() {
        updateManager.clearError()
    }

    fun downloadUpdate(url: String) {
        updateManager.downloadAndInstall(url)
    }

    fun updateAvatar(uri: String) { viewModelScope.launch { repository.updateAvatar(uri) } }
    
    fun clearAuthStatus() {
        _isLoadingAuth.value = false
        _error.value = null
    }

    fun register(u: String, e: String, p: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _error.value = null
            _isLoadingAuth.value = true
            try {
                repository.register(u, e, p)
                _isLoadingAuth.value = false
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al registrarse"
                _isLoadingAuth.value = false
            }
        }
    }

    fun login(e: String, p: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _error.value = null
            _isLoadingAuth.value = true
            try {
                if (repository.login(e, p)) {
                    _isLoadingAuth.value = false
                    onSuccess()
                } else {
                    _error.value = "Credenciales incorrectas"
                    _isLoadingAuth.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al iniciar sesión"
                _isLoadingAuth.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}

data class HomeSection(
    val title: String, 
    val items: List<Movie>,
    val isWatchingSection: Boolean = false
)

sealed interface HomeUiState {
    data object Idle : HomeUiState
    data object Loading : HomeUiState
    data class Success(val contentCount: Int) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
