package com.example.peliselprimazo.domain.usecase

import com.example.peliselprimazo.domain.repository.MovieRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SyncContentUseCaseTest {

    private lateinit var movieRepository: MovieRepository
    private lateinit var syncContentUseCase: SyncContentUseCase

    @Before
    fun setUp() {
        movieRepository = mock()
        syncContentUseCase = SyncContentUseCase(movieRepository)
    }

    @Test
    fun `invoke calls refreshContent on repository`() = runTest {
        syncContentUseCase()
        verify(movieRepository).refreshContent()
    }
}
