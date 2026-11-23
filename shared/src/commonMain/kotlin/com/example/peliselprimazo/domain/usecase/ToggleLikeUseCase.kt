package com.example.peliselprimazo.domain.usecase

import com.example.peliselprimazo.domain.repository.MovieRepository

class ToggleLikeUseCase(
    private val repository: MovieRepository
) {
    suspend operator fun invoke(movieId: Int) {
        repository.toggleLike(movieId)
    }
}
