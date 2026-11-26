package com.example.peliselprimazo.data.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    fun logVideoPlayed(movieId: Int, movieTitle: String, contentType: String, serverName: String) {
        try {
            firebaseAnalytics.logEvent("play_content") {
                param("movie_id", movieId.toLong())
                param("movie_title", movieTitle)
                param("content_type", contentType)
                param("server_name", serverName)
            }
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun logGenreSelected(genre: String) {
        try {
            firebaseAnalytics.logEvent("select_genre") {
                param("genre_name", genre)
            }
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun logGuestModeEntered() {
        try {
            firebaseAnalytics.logEvent("guest_mode_entered", null)
            setUserType(true)
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun setUserType(isGuest: Boolean) {
        try {
            val userType = if (isGuest) "guest" else "registered"
            firebaseAnalytics.setUserProperty("user_type", userType)
            firebaseAnalytics.logEvent("user_session_type", android.os.Bundle().apply {
                putString("user_type", userType)
            })
        } catch (e: Exception) {
            // Non-blocking
        }
    }
}
