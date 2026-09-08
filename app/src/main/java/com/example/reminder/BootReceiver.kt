package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = UserPreferencesRepository(context)
            CoroutineScope(Dispatchers.IO).launch {
                val isEnabled = prefs.hifzReminderEnabled.first()
                if (isEnabled) {
                    val hour = prefs.hifzReminderHour.first()
                    val minute = prefs.hifzReminderMinute.first()
                    HifzReminderScheduler.scheduleDailyReminder(context, hour, minute)
                }
            }
        }
    }
}
