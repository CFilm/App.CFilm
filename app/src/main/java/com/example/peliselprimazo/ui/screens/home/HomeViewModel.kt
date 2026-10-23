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

    private fun createSections(list: List<Movie>, watching: List<Movie>, trends: List<Movie>): List<HomeSection> {
        if (list.isEmpty()) return emptyList()
        val watchLater = list.filter { it.isWatchLater }

        // Algoritmo de Recomendación Personalizada para "✨ Para Ti"
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
                .take(15)
                .ifEmpty { list.shuffled().take(15) }
        } else {
            list.sortedByDescending { it.rating }.take(15)
        }

        return buildList {
            if (watching.isNotEmpty()) add(HomeSection("🍿 Continuar Viendo", watching.take(12), isWatchingSection = true))
            add(HomeSection("✨ Para Ti", personalizedForYou))
            if (trends.isNotEmpty()) add(HomeSection("🔥 Tendencias Globales", trends)) else add(HomeSection("🔥 Tendencias Globales", list.sortedByDescending { it.rating }.take(15)))
            add(HomeSection("🚀 Estrenos Exclusivos", list.sortedByDescending { it.releaseDate }.take(15)))
            add(HomeSection("⭐ Los Más Valorados", list.sortedByDescending { it.rating }.take(15)))
            if (watchLater.isNotEmpty()) add(HomeSection("🕒 Guardados Para Ver Después", watchLater))
            add(HomeSection("❤️ Recomendados Especiales", list.shuffled().take(15)))
            
            val genreSections = listOf(
                "💥 Acción Extrema & Adrenalina" to listOf("Acción", "Aventura"),
                "🛸 Ciencia Ficción & Futuro" to listOf("Ciencia Ficción", "Sci-Fi"),
                "😱 Terror & Suspenso Nocturno" to listOf("Terror", "Suspenso", "Horror"),
                "😂 Comedias Para Reír A Carcajadas" to listOf("Comedia"),
                "🎭 Dramas Intensos & Conmovedores" to listOf("Drama"),
                "🦄 Fantasía & Mundos Mágicos" to listOf("Fantasía", "Fantasy"),
                "🔍 Misterio & Investigaciones" to listOf("Misterio", "Crimen"),
                "💘 Romances & Amores Inolvidables" to listOf("Romance"),
                "⚡ Aventura & Viajes Épicos" to listOf("Aventura"),
                "🏆 Joyas Del Cine Aclamadas" to listOf("Drama", "Historia"),
                "👑 Maratón De Súper Sagas" to listOf("Acción", "Fantasía"),
                "🌟 Lo Más Visto De La Semana" to listOf("Acción", "Comedia"),
                "🎬 Favoritos De La Crítica" to listOf("Drama", "Suspenso")
            )

            genreSections.forEach { (title, genres) ->
                val filtered = list.filter { m -> genres.any { g -> m.genres.any { mg -> mg.contains(g, ignoreCase = true) } } }
                val content = if (filtered.isNotEmpty()) filtered.shuffled().take(15) else list.shuffled().take(12)
                add(HomeSection(title, content))
            }
        }
    }

    private fun createMovieSections(list: List<Movie>): List<HomeSection> {
        if (list.isEmpty()) return emptyList()
        return buildList {
            add(HomeSection("🎬 Recomendados De Películas", list.shuffled().take(15)))
            add(HomeSection("✨ Películas Para Ti", list.shuffled().take(15)))
            add(HomeSection("🔥 Películas Más Populares", list.sortedByDescending { it.rating }.take(15)))
            add(HomeSection("⭐ Las Mejor Valoradas", list.sortedByDescending { it.rating }.take(15)))
            add(HomeSection("🚀 Recientes & Estrenos", list.sortedByDescending { it.releaseDate }.take(15)))

            val categoryMap = listOf(
                "🍿 Cine De Taquilla" to listOf("Acción", "Aventura"),
                "💥 Pura Acción & Explosiones" to listOf("Acción"),
                "🛸 Ciencia Ficción & Universos" to listOf("Ciencia Ficción"),
                "👻 Espantosas & Terror" to listOf("Terror"),
                "😂 Para Morirse De Risa" to listOf("Comedia"),
                "🎭 Historias Dramáticas" to listOf("Drama"),
                "🔪 Suspenso & Thrillers" to listOf("Suspenso"),
                "🏰 Fantasía & Magia" to listOf("Fantasía"),
                "🕵️‍♂️ Crimen & Investigaciones" to listOf("Crimen"),
                "💘 Historias De Amor" to listOf("Romance"),
                "🌴 Aventuras Inolvidables" to listOf("Aventura"),
                "🦸‍♂️ Superhéroes & Cómics" to listOf("Acción", "Fantasía"),
                "👾 Animación & Familia" to listOf("Animación", "Familia"),
                "📜 Hechos Reales & Historia" to listOf("Historia", "Biografía"),
                "💎 Clásicos Que Tienes Que Ver" to listOf("Drama", "Acción")
            )

            categoryMap.forEach { (title, genres) ->
                val filtered = list.filter { m -> genres.any { g -> m.genres.any { mg -> mg.contains(g, ignoreCase = true) } } }
                val content = if (filtered.isNotEmpty()) filtered.shuffled().take(15) else list.shuffled().take(12)
                add(HomeSection(title, content))
            }
        }
    }

    private fun createSeriesSections(list: List<Movie>): List<HomeSection> {
        if (list.isEmpty()) return emptyList()
        return buildList {
            add(HomeSection("📺 Recomendados De Series", list.shuffled().take(15)))
            add(HomeSection("✨ Series Para Ti", list.shuffled().take(15)))
            add(HomeSection("🔥 Series Más Populares", list.sortedByDescending { it.rating }.take(15)))
            add(HomeSection("🏆 Series Aclamadas", list.sortedByDescending { it.rating }.take(15)))
            add(HomeSection("🍿 Maratones De Fin De Semana", list.shuffled().take(15)))

            val categoryMap = listOf(
                "💥 Series De Acción" to listOf("Acción"),
                "🛸 Universos & Ciencia Ficción" to listOf("Ciencia Ficción"),
                "😱 Suspenso & Tensión" to listOf("Suspenso"),
                "😂 Comedias Divertidas" to listOf("Comedia"),
                "🎭 Dramas & Pasiones" to listOf("Drama"),
                "🔍 Casos & Misterios" to listOf("Misterio", "Crimen"),
                "🐉 Fantasía Épica" to listOf("Fantasía"),
                "🚓 Crimen & Mafia" to listOf("Crimen"),
                "💘 Romances De Serie" to listOf("Romance"),
                "🤠 Aventuras & Fronteras" to listOf("Aventura"),
                "👑 Intrigas De Poder" to listOf("Drama"),
                "🏫 Sobrenatural & Paranormal" to listOf("Terror", "Fantasía"),
                "🦾 Dystopias & Futuro" to listOf("Ciencia Ficción"),
                "⭐ Éxitos De Temporada" to listOf("Acción", "Drama"),
                "🌟 Imperdibles En Pantalla" to listOf("Drama", "Comedia")
            )

            categoryMap.forEach { (title, genres) ->
                val filtered = list.filter { m -> genres.any { g -> m.genres.any { mg -> mg.contains(g, ignoreCase = true) } } }
                val content = if (filtered.isNotEmpty()) filtered.shuffled().take(15) else list.shuffled().take(12)
                add(HomeSection(title, content))
            }
        }
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
            Log.d("CFilmHome", "refreshData called: isFirstLoad=$isFirstLoad")
            if (isFirstLoad && allContent.value.isEmpty()) {
                _isLoading.value = true
                _uiState.value = HomeUiState.Loading
            } else if (!isFirstLoad) {
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
