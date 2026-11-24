package com.example.peliselprimazo.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class LocalWatchingProgress(
    val contentId: Int,
    val episodeId: String? = null,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

@Singleton
class LocalUserStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("cfilm_local_user_storage", Context.MODE_PRIVATE)
    private val gson = Gson()

    var isGuestMode: Boolean
        get() = prefs.getBoolean("is_guest_mode", true)
        set(value) = prefs.edit().putBoolean("is_guest_mode", value).apply()

    fun getFavorites(): Set<Int> {
        val json = prefs.getString("local_favorites", null) ?: return emptySet()
        val type = object : TypeToken<Set<Int>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun toggleFavorite(movieId: Int): Boolean {
        val current = getFavorites().toMutableSet()
        val isFav = if (current.contains(movieId)) {
            current.remove(movieId)
            false
        } else {
            current.add(movieId)
            true
        }
        val json = gson.toJson(current)
        prefs.edit().putString("local_favorites", json).apply()
        return isFav
    }

    fun isFavorite(movieId: Int): Boolean {
        return getFavorites().contains(movieId)
    }

    fun getWatchHistory(): Map<Int, LocalWatchingProgress> {
        val json = prefs.getString("local_watch_history", null) ?: return emptyMap()
        val type = object : TypeToken<Map<Int, LocalWatchingProgress>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun updateWatchProgress(contentId: Int, episodeId: String?, positionMs: Long, durationMs: Long) {
        val current = getWatchHistory().toMutableMap()
        current[contentId] = LocalWatchingProgress(
            contentId = contentId,
            episodeId = episodeId,
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAt = System.currentTimeMillis()
        )
        val json = gson.toJson(current)
        prefs.edit().putString("local_watch_history", json).apply()
    }
}
