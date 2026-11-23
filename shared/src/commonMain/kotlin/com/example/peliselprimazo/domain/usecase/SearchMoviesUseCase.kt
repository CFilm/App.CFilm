package com.example.peliselprimazo.domain.usecase

import com.example.peliselprimazo.core.model.Movie
import com.example.peliselprimazo.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow

class SearchMoviesUseCase(
    private val repository: MovieRepository
) {
    operator fun invoke(query: String): Flow<List<Movie>> {
        return repository.searchMovies(query)
    }
}
