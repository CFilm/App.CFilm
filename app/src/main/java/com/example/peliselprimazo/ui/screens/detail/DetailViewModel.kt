package com.example.peliselprimazo.ui.screens.detail

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.peliselprimazo.data.AdsManager
import com.example.peliselprimazo.data.util.AnalyticsHelper
import com.example.peliselprimazo.domain.model.Movie
import com.example.peliselprimazo.domain.model.ServerLink
import com.example.peliselprimazo.domain.repository.MovieRepository
import com.example.peliselprimazo.domain.usecase.GetMovieDetailUseCase
import com.example.peliselprimazo.domain.usecase.GetStreamUrlUseCase
import com.example.peliselprimazo.domain.usecase.ToggleLikeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val getMovieDetailUseCase: GetMovieDetailUseCase,
    private val getStreamUrlUseCase: GetStreamUrlUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val adsManager: AdsManager,
    private val analyticsHelper: AnalyticsHelper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tag = "DetailViewModel"
    private val movieId: Int = checkNotNull(savedStateHandle["movieId"])

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _isLoadingAuth = MutableStateFlow(false)
    val isLoadingAuth = _isLoadingAuth.asStateFlow()

    private val _preloadingVideo = MutableStateFlow(false)
    val preloadingVideo = _preloadingVideo.asStateFlow()

    private val _downloadAdState = MutableStateFlow<DownloadAdState?>(null)
    val downloadAdState: StateFlow<DownloadAdState?> = _downloadAdState.asStateFlow()

    fun prepareDownload(movieTitle: String, serverLink: ServerLink, isEpisode: Boolean = false) {
        _downloadAdState.value = DownloadAdState(
            serverLink = serverLink,
            movieTitle = movieTitle,
            isEpisode = isEpisode,
            season = serverLink.season,
            episode = serverLink.episode,
            episodeTitle = serverLink.episodeTitle,
            adsWatched = 0,
            isProcessing = false,
            statusMessage = null,
            isCompleted = false
        )
    }

    fun dismissDownloadDialog() {
        _downloadAdState.value = null
    }

    fun watchNextAd(activity: Activity) {
        val currentState = _downloadAdState.value ?: return
        val currentStep = currentState.adsWatched
        if (currentStep >= 2 || currentState.isProcessing) return

        val nextAdNumber = currentStep + 1
        _downloadAdState.value = currentState.copy(
            isProcessing = true,
            statusMessage = "Cargando anuncio $nextAdNumber de 2..."
        )

        showRewardedVideo(activity, "download_ad_$nextAdNumber") {
            val updatedState = _downloadAdState.value ?: return@showRewardedVideo
            val newAdsWatched = updatedState.adsWatched + 1
            val completed = newAdsWatched >= 2
            _downloadAdState.value = updatedState.copy(
                adsWatched = newAdsWatched,
                isProcessing = false,
                isCompleted = completed,
                statusMessage = if (completed) "¡2 de 2 anuncios completados! Presiona el botón para iniciar la descarga." else "¡Anuncio 1 completado! Presiona para ver el segundo anuncio."
            )
        }
    }

    fun startFileDownload(activity: Activity) {
        val currentState = _downloadAdState.value ?: return
        if (currentState.adsWatched < 2) return

        _downloadAdState.value = currentState.copy(
            isProcessing = true,
            statusMessage = "Obteniendo enlace de descarga..."
        )

        viewModelScope.launch {
            try {
                val serverLink = currentState.serverLink
                val downloadUrl = repository.getDownloadUrl(serverLink.serverName, serverLink.fileId)
                if (!downloadUrl.isNullOrBlank()) {
                    val rawName = if (currentState.isEpisode) {
                        "${currentState.movieTitle}_S${currentState.season ?: 1}E${currentState.episode ?: 1}"
                    } else {
                        currentState.movieTitle
                    }
                    val cleanFileName = rawName.replace("[^a-zA-Z0-9_-]".toRegex(), "_") + ".mp4"

                    val request = DownloadManager.Request(Uri.parse(downloadUrl))
                        .setTitle("Descargando $rawName")
                        .setDescription("CFilm Descargas")
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, cleanFileName)

                    val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    dm.enqueue(request)

                    _downloadAdState.value = currentState.copy(
                        isProcessing = false,
                        isCompleted = true,
                        statusMessage = "¡Descarga iniciada exitosamente! Revisa las notificaciones de tu dispositivo."
                    )
                } else {
                    _downloadAdState.value = currentState.copy(
                        isProcessing = false,
                        statusMessage = "Error: Servidor no devolvió un enlace de descarga válido."
                    )
                }
            } catch (e: Exception) {
                _downloadAdState.value = currentState.copy(
                    isProcessing = false,
                    statusMessage = "Error al descargar: ${e.localizedMessage}"
                )
            }
        }
    }

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    val user = repository.getUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isServerOnline = repository.isServerOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isFavorito = repository.isMovieLiked(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isViendo = repository.getAllContent().map { list ->
        list.any { it.id == movieId && it.isWatching }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isTerminado = repository.isMovieFinished(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isVerDespues = repository.isMovieInWatchLater(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val similarContent: StateFlow<List<Movie>> = repository.getAllContent().map { list ->
        val currentMovie = (_uiState.value as? DetailUiState.Success)?.movie
        list.filter { movie ->
            movie.id != movieId && (
                (currentMovie != null && movie.contentType == currentMovie.contentType) ||
                movie.genres.any { genre -> currentMovie?.genres?.contains(genre) == true }
            )
        }.take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadMovieDetail()
    }

    fun loadMovieDetail() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val movie = getMovieDetailUseCase(movieId)
                if (movie != null) {
                    _uiState.value = DetailUiState.Success(movie)
                    movie.serverLinks.firstOrNull()?.let { link ->
                        prefetchStream(link.serverName, link.fileId)
                    }
                } else {
                    _uiState.value = DetailUiState.Error("No se encontró el contenido")
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    // Actualizado: Ahora acepta placementName para Unity Ads
    fun showRewardedVideo(activity: Activity, placementName: String? = null, onReward: () -> Unit) {
        adsManager.showRewardedVideo(activity, placementName, onReward)
    }

    fun prefetchStream(server: String, fileId: String) {
        viewModelScope.launch {
            try {
                getStreamUrlUseCase(server, fileId)
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error prefetching stream: ${e.message}")
            }
        }
    }

    fun prepareAndPlay(
        server: String, 
        fileId: String, 
        onReady: (String, String?) -> Unit
    ) {
        viewModelScope.launch {
            _preloadingVideo.value = true
            try {
                val videoUrl = getStreamUrlUseCase(server, fileId).url
                if (!videoUrl.isNullOrBlank()) {
                    Log.d(tag, "Video extraído correctamente. Pasando al reproductor.")
                    onReady(videoUrl, null)
                } else {
                    _error.value = "No se pudo obtener el enlace de video"
                }
            } catch (e: Exception) {
                Log.e(tag, "Error al preparar video", e)
                _error.value = "Error al preparar video: ${e.message}"
            } finally {
                _preloadingVideo.value = false
            }
        }
    }

    fun toggleFavorito() { viewModelScope.launch { toggleLikeUseCase(movieId) } }
    fun toggleViendo() { viewModelScope.launch { repository.addToWatching(movieId) } }
    fun toggleTerminado() { viewModelScope.launch { repository.toggleFinished(movieId) } }
    fun toggleVerDespues() { viewModelScope.launch { repository.toggleWatchLater(movieId) } }

    fun isGuestMode(): Boolean = repository.isGuestMode()

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoadingAuth.value = true
            _error.value = null
            try {
                if (repository.loginAnonymously()) {
                    analyticsHelper.logGuestModeEntered()
                    _isLoadingAuth.value = false
                    onSuccess()
                } else {
                    _error.value = "Error al ingresar como invitado"
                    _isLoadingAuth.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al ingresar como invitado"
                _isLoadingAuth.value = false
            }
        }
    }

    fun login(e: String, p: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoadingAuth.value = true
            _error.value = null
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

    fun register(u: String, e: String, p: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoadingAuth.value = true
            _error.value = null
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

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String) -> Unit) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            onResult(false, "Ingresa tu correo para restablecer la contraseña.")
            return
        }
        viewModelScope.launch {
            _error.value = null
            _isLoadingAuth.value = true
            try {
                val (success, message) = repository.sendPasswordResetEmail(cleanEmail)
                _isLoadingAuth.value = false
                if (!success) {
                    _error.value = message
                }
                onResult(success, message)
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Error al enviar correo de recuperación"
                _error.value = msg
                _isLoadingAuth.value = false
                onResult(false, msg)
            }
        }
    }
}

data class DownloadAdState(
    val serverLink: ServerLink,
    val movieTitle: String,
    val isEpisode: Boolean = false,
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
    val adsWatched: Int = 0,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val isCompleted: Boolean = false
)

sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Success(val movie: Movie) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}
