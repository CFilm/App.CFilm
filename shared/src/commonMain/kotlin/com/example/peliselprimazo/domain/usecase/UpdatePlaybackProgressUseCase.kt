package com.example.peliselprimazo.domain.usecase

import com.example.peliselprimazo.domain.repository.MovieRepository

class UpdatePlaybackProgressUseCase(
    private val repository: MovieRepository
) {
    suspend operator fun invoke(movieId: Int, position: Long, duration: Long) {
        repository.updatePlaybackProgress(movieId, position, duration)
    }
}
