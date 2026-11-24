package com.example.peliselprimazo.domain.usecase

import com.example.peliselprimazo.data.extractor.StreamExtractorFactory
import com.example.peliselprimazo.domain.repository.MovieRepository
import javax.inject.Inject

data class StreamResult(
    val url: String?,
    val headers: Map<String, String> = emptyMap()
)

class GetStreamUrlUseCase @Inject constructor(
    private val repository: MovieRepository,
    private val extractorFactory: StreamExtractorFactory
) {
    suspend operator fun invoke(serverName: String, fileId: String): StreamResult {
        val extractor = extractorFactory.getExtractor(serverName)
        val url = extractor.extract(fileId, repository)
        val headers = extractor.getHeaders(fileId)
        return StreamResult(url, headers)
    }
}
