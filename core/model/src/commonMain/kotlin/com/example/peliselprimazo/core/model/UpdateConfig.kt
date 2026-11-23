package com.example.peliselprimazo.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateConfig(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val updateUrl: String,
    val isUpdateAvailable: Boolean = false,
    val isForceUpdate: Boolean = false,
    val visualFlags: Map<String, String> = emptyMap()
)
