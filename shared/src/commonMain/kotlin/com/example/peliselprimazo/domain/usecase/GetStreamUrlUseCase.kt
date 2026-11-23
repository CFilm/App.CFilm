package com.example.peliselprimazo.domain.usecase

import com.example.peliselprimazo.data.extractor.StreamExtractorFactory
import com.example.peliselprimazo.domain.repository.MovieRepository

class GetStreamUrlUseCase(
    private val repository: MovieRepository,
    private val extractorFactory: StreamExtractorFactory
) {
    suspend operator fun invoke(serverName: String, fileId: String): String? {
        val extractor = extractorFactory.getExtractor(serverName)
        return extractor.extract(fileId, repository)
    }
}
