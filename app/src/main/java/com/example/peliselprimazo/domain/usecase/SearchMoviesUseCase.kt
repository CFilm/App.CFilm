package com.example.peliselprimazo.domain.usecase

import com.example.peliselprimazo.domain.model.Movie
import com.example.peliselprimazo.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchMoviesUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    operator fun invoke(query: String): Flow<List<Movie>> {
        return repository.searchMovies(query)
    }
}
