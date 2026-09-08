package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.db.AppDatabase
import com.example.data.local.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class HifzReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val prefsRepo = UserPreferencesRepository(context)
                val isEnabled = prefsRepo.hifzReminderEnabled.first()
                if (!isEnabled) {
                    return@launch
                }

                val db = AppDatabase.getInstance(context)
                val now = System.currentTimeMillis()
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDayMillis = calendar.timeInMillis

                val dueReviews = db.memorizationDao().getDueForReview(now).first()
                val reviewedToday = db.memorizationDao().getReviewedSinceCount(startOfDayMillis)
                val dailyTarget = prefsRepo.dailyMemorizationTarget.first()

                val title: String
                val message: String

                when {
                    dueReviews.isNotEmpty() -> {
                        title = "Révision Hifz due"
                        message = "${dueReviews.size} verset(s) sont prêts pour votre répétition espacée."
                    }
                    reviewedToday < dailyTarget -> {
                        title = "Objectif Hifz du jour"
                        message = "Progrès : $reviewedToday / $dailyTarget versets travaillés aujourd'hui."
                    }
                    else -> {
                        title = "Mémorisation du Coran"
                        message = "Objectif atteint ! Prenez un instant pour tester vos versets mémorisés."
                    }
                }

                showNotification(context, title, message)

                // Reschedule for next day
                val hour = prefsRepo.hifzReminderHour.first()
                val minute = prefsRepo.hifzReminderMinute.first()
                HifzReminderScheduler.scheduleDailyReminder(context, hour, minute)
            } catch (_: Exception) {
                // Ignore unexpected exceptions in background receiver
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "hifz_daily_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Rappels Hifz & Mémorisation",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Rappels pour la mémorisation et les révisions espacées du Coran"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("nav_route", "memorization")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (_: SecurityException) {
            // Handled when notification permission is revoked
        }
    }
}
