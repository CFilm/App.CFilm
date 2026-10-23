package com.example.peliselprimazo.domain.usecase

import com.example.peliselprimazo.domain.repository.MovieRepository
import javax.inject.Inject

class UpdatePlaybackProgressUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    suspend operator fun invoke(movieId: Int, position: Long, duration: Long) {
        repository.updatePlaybackProgress(movieId, position, duration)
    }
}
