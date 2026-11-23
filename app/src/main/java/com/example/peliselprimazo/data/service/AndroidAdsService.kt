package com.example.peliselprimazo.data.service

import com.example.peliselprimazo.data.AdsManager
import com.example.peliselprimazo.domain.service.AdsService
import kotlinx.coroutines.flow.StateFlow

class AndroidAdsService(
    private val adsManager: AdsManager
) : AdsService {
    override val isRewardedVideoReady: StateFlow<Boolean> = adsManager.isRewardedVideoReady

    override fun showRewardedVideo(placementName: String?, onReward: () -> Unit) {
        adsManager.showRewardedVideo(placementName, onReward)
    }

    override fun getPlayerAdUrl(): String? = adsManager.getPlayerAdUrl()
}
