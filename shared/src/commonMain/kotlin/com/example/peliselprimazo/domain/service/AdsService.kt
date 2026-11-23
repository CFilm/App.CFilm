package com.example.peliselprimazo.domain.service

import kotlinx.coroutines.flow.StateFlow

interface AdsService {
    val isRewardedVideoReady: StateFlow<Boolean>
    fun showRewardedVideo(placementName: String? = null, onReward: () -> Unit)
    fun getPlayerAdUrl(): String? = null
}
