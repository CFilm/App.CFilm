package com.example.peliselprimazo.data.service

import com.example.peliselprimazo.data.UpdateManager
import com.example.peliselprimazo.domain.service.UpdateService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidUpdateService(
    private val updateManager: UpdateManager
) : UpdateService {
    override val isDownloading: StateFlow<Boolean> = updateManager.isDownloading
    override val downloadProgress: StateFlow<Float> = updateManager.downloadProgress
    
    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    override fun downloadAndInstall(url: String) {
        updateManager.startDownload(url, "cfilm-update.apk")
    }

    override fun checkAndInstallPending() {}

    override fun clearError() {
        _error.value = null
    }
}
