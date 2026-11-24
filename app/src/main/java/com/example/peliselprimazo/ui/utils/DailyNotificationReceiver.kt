package com.example.peliselprimazo.ui.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.peliselprimazo.MainActivity
import com.example.peliselprimazo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class DailyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("NOTIFICATION_TYPE") ?: "SHARE"
        Log.d("DailyNotification", "Ejecutando notificación tipo: $type")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(context, notificationManager)

        if (type == "SHARE") {
            showShareNotification(context, notificationManager)
        } else {
            showRecommendationNotification(context, notificationManager, intent)
        }

        // Re-programar la siguiente notificación con hora aleatoria para mantener el ciclo
        DailyNotificationScheduler.scheduleDailyNotifications(context)
    }

    private fun createNotificationChannel(context: Context, manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "daily_recommendations",
                "Notificaciones CFilm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de recomendación personalizada y compartir la app"
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun showShareNotification(context: Context, manager: NotificationManager) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "¡Disfruta de tus películas y series favoritas gratis en CFilm! Descárgala aquí: https://cfilm.github.io/App.CFilm/")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 101, Intent.createChooser(shareIntent, "Comparte CFilm"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "daily_recommendations")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🍿 ¡Comparte CFilm con tus amigos!")
            .setContentText("Disfruta del mejor contenido con tus conocidos. ¡Toca aquí para compartir la app!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(2001, notification)
    }

    private fun showRecommendationNotification(context: Context, manager: NotificationManager, intent: Intent) {
        val movieId = intent.getIntExtra("MOVIE_ID", 1)
        val movieTitle = intent.getStringExtra("MOVIE_TITLE") ?: "Contenido Especial CFilm"
        val posterUrl = intent.getStringExtra("POSTER_URL") ?: ""

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("MOVIE_ID", movieId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 102, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Variaciones personalizadas para que cada notificación se sienta única y adaptada al usuario
        val personalizedTitles = listOf(
            "🍿 Hola de nuevo, ¿listo para una función? Te recomendamos:",
            "✨ Pensando en tus gustos, hoy te sugerimos ver:",
            "🔥 Esta joya está imperdible hoy en CFilm:",
            "🎬 ¿Continuamos la maratón? Échale un vistazo a:",
            "⭐ Nuestra recomendación especial para ti:"
        )
        val randomTitle = personalizedTitles.random()

        CoroutineScope(Dispatchers.IO).launch {
            var bitmap: Bitmap? = null
            if (posterUrl.isNotBlank()) {
                try {
                    val loader = ImageLoader(context)
                    val req = ImageRequest.Builder(context)
                        .data(posterUrl)
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(req)
                    bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                } catch (e: Exception) {
                    Log.e("DailyNotification", "Error cargando portada: ${e.message}")
                }
            }

            val builder = NotificationCompat.Builder(context, "daily_recommendations")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(randomTitle)
                .setContentText("$movieTitle - ¡Toca para ver ahora en CFilm!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            if (bitmap != null) {
                builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
            }

            manager.notify(2002, builder.build())
        }
    }
}

object DailyNotificationScheduler {
    fun scheduleDailyNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Notificación Obligatoria: Compartir App (Diseño fijo a la 1:00 PM)
        val shareIntent = Intent(context, DailyNotificationReceiver::class.java).apply {
            putExtra("NOTIFICATION_TYPE", "SHARE")
        }
        val sharePending = PendingIntent.getBroadcast(
            context, 1001, shareIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calShare = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calShare.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                sharePending
            )
        } catch (e: Exception) {
            Log.e("DailyNotification", "Error programando notificación de compartir", e)
        }

        // 2. Notificación Personalizada: Recomendación con HORA ALEATORIA entre las 2:00 PM y las 9:00 PM
        val randomHour = (14..21).random()
        val randomMinute = (0..59).random()

        val recIntent = Intent(context, DailyNotificationReceiver::class.java).apply {
            putExtra("NOTIFICATION_TYPE", "RECOMMENDATION")
            putExtra("MOVIE_ID", 1)
            putExtra("MOVIE_TITLE", "Película o Serie Destacada")
        }
        val recPending = PendingIntent.getBroadcast(
            context, 1002, recIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calRec = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, randomHour)
            set(Calendar.MINUTE, randomMinute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calRec.timeInMillis,
                recPending
            )
        } catch (e: Exception) {
            Log.e("DailyNotification", "Error programando recomendación aleatoria", e)
        }
    }

    fun scheduleDynamicRecommendation(context: Context, movieId: Int, title: String, posterUrl: String?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val randomHour = (14..21).random()
        val randomMinute = (0..59).random()

        val recIntent = Intent(context, DailyNotificationReceiver::class.java).apply {
            putExtra("NOTIFICATION_TYPE", "RECOMMENDATION")
            putExtra("MOVIE_ID", movieId)
            putExtra("MOVIE_TITLE", title)
            putExtra("POSTER_URL", posterUrl ?: "")
        }
        val recPending = PendingIntent.getBroadcast(
            context, 1002, recIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calRec = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, randomHour)
            set(Calendar.MINUTE, randomMinute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calRec.timeInMillis,
                recPending
            )
        } catch (e: Exception) {
            Log.e("DailyNotification", "Error actualizando recomendación dinámica", e)
        }
    }
}
