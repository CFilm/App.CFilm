package com.example.peliselprimazo.domain.usecase

import com.example.peliselprimazo.core.model.UpdateConfig
import com.example.peliselprimazo.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow

class GetUpdateConfigUseCase(
    private val repository: ConfigRepository
) {
    operator fun invoke(): Flow<UpdateConfig> = repository.getUpdateConfig()
    
    suspend fun sync() = repository.fetchAndActivate()
}
