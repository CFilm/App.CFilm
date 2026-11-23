package com.example.peliselprimazo.core.model.player

import kotlinx.coroutines.flow.StateFlow

enum class PlayboardStatus {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
    ERROR
}

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
    ERROR
}

interface VideoPlayerController {
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val isPlaying: StateFlow<Boolean>
    val playbackStatus: StateFlow<PlaybackStatus>
    val playbackSpeed: StateFlow<Float>

    fun prepare(videoUrl: String, headers: Map<String, String> = emptyMap(), resumePosition: Long = 0L)
    fun play()
    fun pause()
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun seekForward(offsetMs: Long = 10000L)
    fun seekBackward(offsetMs: Long = 10000L)
    fun setPlaybackSpeed(speed: Float)
    fun stop()
    fun release()
}
