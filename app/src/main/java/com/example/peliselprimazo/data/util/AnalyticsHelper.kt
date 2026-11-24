package com.example.peliselprimazo.data.util

import android.content.Context
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
        } catch (e: Exception) {
            // Non-blocking
        }
    }
}
