package com.example.peliselprimazo.domain.usecase

import com.example.peliselprimazo.domain.repository.MovieRepository
import javax.inject.Inject

class ToggleLikeUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    suspend operator fun invoke(movieId: Int) {
        repository.toggleLike(movieId)
    }
}
