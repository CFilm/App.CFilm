package com.example.peliselprimazo.data

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.peliselprimazo.BuildConfig
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.model.Placement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AdsManager {
    private val tag = "AdsManager"

    private val _isRewardedVideoReady = MutableStateFlow(false)
    val isRewardedVideoReady = _isRewardedVideoReady.asStateFlow()

    private var onRewardCallback: (() -> Unit)? = null
    
    @Volatile
    private var isInitialized = false

    @Volatile
    private var currentActivity: Activity? = null

    fun init(context: Context) {
        val activity = context as? Activity
        if (activity != null) {
            currentActivity = activity
        }
        if (isInitialized) {
            Log.d(tag, "AdsManager ya está inicializado.")
            return
        }

        val appKey = BuildConfig.IRONSOURCE_APP_KEY.ifEmpty { "2852f0ccd" }
        val appContext = context.applicationContext
        
        val prefs = appContext.getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        var userId = prefs.getString("user_id_ads", null)
        if (userId == null) {
            userId = "user_" + UUID.randomUUID().toString().replace("-", "").take(8)
            prefs.edit().putString("user_id_ads", userId).apply()
        }

        IronSource.setUserId(userId)
        IronSource.setConsent(true)
        if (activity != null) {
            IronSource.shouldTrackNetworkState(activity, true)
        }

        IronSource.setLevelPlayRewardedVideoListener(object : LevelPlayRewardedVideoListener {
            override fun onAdAvailable(adInfo: AdInfo) {
                Log.d(tag, "Anuncio recompensado disponible: ${adInfo.adNetwork}")
                _isRewardedVideoReady.value = true
            }
            override fun onAdUnavailable() { 
                Log.d(tag, "Anuncio recompensado no disponible")
                _isRewardedVideoReady.value = false 
            }
            override fun onAdOpened(adInfo: AdInfo) {
                Log.d(tag, "Anuncio recompensado abierto")
            }
            override fun onAdClosed(adInfo: AdInfo) {
                Log.d(tag, "Anuncio recompensado cerrado")
                // Intentar cargar el siguiente anuncio
                IronSource.loadRewardedVideo()
            }
            override fun onAdRewarded(placement: Placement, adInfo: AdInfo) {
                Log.d(tag, "¡Recompensa obtenida en placement=${placement.placementName}!")
                onRewardCallback?.invoke()
                onRewardCallback = null
            }
            override fun onAdShowFailed(error: IronSourceError, adInfo: AdInfo) {
                Log.e(tag, "Error al mostrar anuncio recompensado: ${error.errorMessage}")
                // Si falla el despliegue del anuncio, permitimos continuar para no bloquear al usuario
                onRewardCallback?.invoke()
                onRewardCallback = null
            }
            override fun onAdClicked(placement: Placement, adInfo: AdInfo) {}
        })

        if (activity != null) {
            IronSource.init(activity, appKey, IronSource.AD_UNIT.REWARDED_VIDEO)
        } else {
            IronSource.init(appContext as? android.app.Application ?: appContext, appKey, IronSource.AD_UNIT.REWARDED_VIDEO)
        }

        // Cargar el primer anuncio
        IronSource.loadRewardedVideo()

        isInitialized = true
        Log.d(tag, "AdsManager inicializado correctamente.")
    }

    fun showRewardedVideo(context: Context, placementName: String? = null, onReward: () -> Unit) {
        val activity = (context as? Activity) ?: currentActivity
        if (activity != null && !isInitialized) {
            init(activity)
        } else if (!isInitialized) {
            init(context)
        }

        this.onRewardCallback = onReward

        if (IronSource.isRewardedVideoAvailable()) {
            Log.d(tag, "Mostrando anuncio recompensado LevelPlay en placement=$placementName")
            if (activity != null) {
                if (!placementName.isNullOrBlank()) {
                    IronSource.showRewardedVideo(activity, placementName)
                } else {
                    IronSource.showRewardedVideo(activity)
                }
            } else {
                Log.w(tag, "No hay Activity disponible para mostrar el anuncio")
                onReward()
            }
        } else {
            Log.d(tag, "Anuncio no disponible aún. Solicitando carga...")
            IronSource.loadRewardedVideo()
            
            // Mostrar aviso al usuario de que el anuncio está cargando
            val ctx = activity ?: context
            Toast.makeText(ctx, "Cargando anuncio, por favor intenta de nuevo en unos segundos...", Toast.LENGTH_SHORT).show()
            
            // Limpiamos el callback pendiente
            onRewardCallback = null
        }
    }

    fun getPlayerAdUrl(): String? = null
    fun launchTestSuite(activity: Activity) = IronSource.launchTestSuite(activity)

    fun onResume(activity: Activity) {
        currentActivity = activity
        IronSource.onResume(activity)
    }

    fun onPause(activity: Activity) {
        IronSource.onPause(activity)
    }
}
