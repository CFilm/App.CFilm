package com.example.peliselprimazo.domain.service

interface NotificationService {
    fun showNewEpisodeNotification(movieTitle: String, season: Int?, episode: Int?, movieId: Int)
}

class NoOpNotificationService : NotificationService {
    override fun showNewEpisodeNotification(movieTitle: String, season: Int?, episode: Int?, movieId: Int) {
        // No-op on platforms without local push notifications
    }
}
