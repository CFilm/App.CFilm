package com.example.peliselprimazo.domain.usecase

import com.example.peliselprimazo.domain.repository.MovieRepository
import javax.inject.Inject

class SyncContentUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    suspend operator fun invoke() {
        repository.refreshContent()
    }
}
