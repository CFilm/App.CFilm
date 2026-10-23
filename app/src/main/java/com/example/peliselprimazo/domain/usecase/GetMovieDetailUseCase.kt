package com.example.peliselprimazo.domain.usecase

import com.example.peliselprimazo.domain.model.Movie
import com.example.peliselprimazo.domain.repository.MovieRepository
import javax.inject.Inject

class GetMovieDetailUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    suspend operator fun invoke(movieId: Int): Movie? {
        return repository.getMovieById(movieId)
    }
}
