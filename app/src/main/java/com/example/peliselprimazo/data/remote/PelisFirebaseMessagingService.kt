package com.example.peliselprimazo.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.peliselprimazo.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PelisFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "🔔 ¡Nuevo episodio / estreno disponible!"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "¡Se ha añadido un nuevo episodio de tu serie o película favorita en CFilm!"

        val movieIdStr = remoteMessage.data["MOVIE_ID"] ?: remoteMessage.data["movieId"]
        val movieId = movieIdStr?.toIntOrNull()

        sendNotification(title, body, movieId)
    }

    override fun onNewToken(token: String) {
        // Token listo para registro en servidor o Firebase Topic
    }

    private fun sendNotification(title: String, messageBody: String, movieId: Int?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (movieId != null) {
                putExtra("MOVIE_ID", movieId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            movieId ?: 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "content_updates"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Actualizaciones de Contenido y Episodios",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de nuevos episodios y estrenos favoritos"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(movieId ?: System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
