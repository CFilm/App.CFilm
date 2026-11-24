package com.example.peliselprimazo.data

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.example.peliselprimazo.BuildConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.model.Placement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdsManager @Inject constructor() {
    private val tag = "AdsManager"

    private val _isRewardedVideoReady = MutableStateFlow(false)
    val isRewardedVideoReady = _isRewardedVideoReady.asStateFlow()

    private var onRewardCallback: (() -> Unit)? = null

    fun init(activity: Activity) {
        val appKey = BuildConfig.IRONSOURCE_APP_KEY.ifEmpty { "2852f0ccd" }
        
        val prefs = activity.getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        var userId = prefs.getString("user_id_ads", null)
        if (userId == null) {
            userId = "user_" + UUID.randomUUID().toString().replace("-", "").take(8)
            prefs.edit().putString("user_id_ads", userId).apply()
        }
        
        IronSource.setUserId(userId)
        IronSource.setConsent(true)
        IronSource.shouldTrackNetworkState(activity, true)

        IronSource.setLevelPlayRewardedVideoListener(object : LevelPlayRewardedVideoListener {
            override fun onAdAvailable(adInfo: AdInfo) {
                Log.d(tag, "Subasta terminada: Ganó ${adInfo.adNetwork}")
                _isRewardedVideoReady.value = true
            }
            override fun onAdUnavailable() { 
                _isRewardedVideoReady.value = false 
            }
            override fun onAdOpened(adInfo: AdInfo) {}
            override fun onAdClosed(adInfo: AdInfo) {}
            override fun onAdRewarded(placement: Placement, adInfo: AdInfo) {
                onRewardCallback?.invoke()
                onRewardCallback = null
            }
            override fun onAdShowFailed(error: IronSourceError, adInfo: AdInfo) {
                onRewardCallback?.invoke()
                onRewardCallback = null
            }
            override fun onAdClicked(placement: Placement, adInfo: AdInfo) {}
        })

        IronSource.init(activity, appKey, IronSource.AD_UNIT.REWARDED_VIDEO)

        // Configurar propiedad de usuario (Mobile vs Android TV) para Firebase Analytics
        try {
            val firebaseAnalytics = FirebaseAnalytics.getInstance(activity)
            val uiModeManager = activity.getSystemService(Context.UI_MODE_SERVICE) as? android.app.UiModeManager
            val isTv = uiModeManager?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
            val deviceType = if (isTv) "android_tv" else "mobile"
            firebaseAnalytics.setUserProperty("device_type", deviceType)

            // Registrar Listener de Ingresos por Impresión (LevelPlay / IronSource)
            IronSource.addImpressionDataListener(object : com.ironsource.mediationsdk.impressionData.ImpressionDataListener {
                override fun onImpressionSuccess(impressionData: com.ironsource.mediationsdk.impressionData.ImpressionData?) {
                    val revenue = impressionData?.revenue ?: return
                    if (revenue <= 0.0) return

                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_PLATFORM, "LevelPlay")
                        putString(FirebaseAnalytics.Param.AD_SOURCE, impressionData.adNetwork ?: "unknown")
                        putString(FirebaseAnalytics.Param.AD_FORMAT, impressionData.adFormat ?: "unknown")
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, impressionData.instanceName ?: "default")
                        putString(FirebaseAnalytics.Param.CURRENCY, "USD")
                        putDouble(FirebaseAnalytics.Param.VALUE, revenue)
                    }

                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                    Log.d(tag, "Ad Impression recorded: source=${impressionData.adNetwork}, revenue=$revenue")
                }
            })
        } catch (e: Exception) {
            Log.e(tag, "Error setting up LevelPlay impression listener: ${e.message}", e)
        }
    }

    fun showRewardedVideo(activity: Activity, placementName: String? = null, onReward: () -> Unit) {
        this.onRewardCallback = onReward
        if (IronSource.isRewardedVideoAvailable()) {
            if (!placementName.isNullOrBlank()) {
                IronSource.showRewardedVideo(placementName)
            } else {
                IronSource.showRewardedVideo()
            }
        } else {
            Log.d(tag, "Anuncio no disponible en este momento.")
            onReward()
        }
    }

    fun getPlayerAdUrl(): String? = null
    fun launchTestSuite(activity: Activity) = IronSource.launchTestSuite(activity)
    fun onResume(activity: Activity) = IronSource.onResume(activity)
    fun onPause(activity: Activity) = IronSource.onPause(activity)
}
