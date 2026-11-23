package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.audio.AdhanPlaybackService
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.domain.model.PrayerName
import com.example.widgets.PrayerTimesWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoorAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PrayerNotificationController.ACTION_REFRESH_PERSISTENT_PRAYER) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = UserPreferencesRepository(context).getPreferencesOnce()
                    PrayerNotificationController.updateOrCancel(context, prefs)
                    PrayerTimesWidgetProvider.requestUpdate(context)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val title = intent.getStringExtra(NoorAlarmScheduler.EXTRA_TITLE) ?: "نور · NOOR"
        val body = intent.getStringExtra(NoorAlarmScheduler.EXTRA_BODY) ?: ""
        val channel = intent.getStringExtra(NoorAlarmScheduler.EXTRA_CHANNEL)
            ?: NoorNotificationHelper.CHANNEL_PRAYER
        val id = intent.getIntExtra(NoorAlarmScheduler.EXTRA_NOTIFICATION_ID, 3001)
        val isExactPrayer = intent.getBooleanExtra(NoorAlarmScheduler.EXTRA_IS_EXACT_PRAYER, false)
        val isTestAlarm = intent.getBooleanExtra(NoorAlarmScheduler.EXTRA_IS_TEST_ALARM, false)
        val prayerKey = intent.getStringExtra(NoorAlarmScheduler.EXTRA_PRAYER_KEY)

        val prayer = prayerKey?.let { runCatching { PrayerName.valueOf(it) }.getOrNull() }
        val allowPlayback = if (isExactPrayer && prayer != null) {
            com.example.audio.AdhanDuplicateGuard.shouldAllowPlayback(context, prayer, isTestAlarm = isTestAlarm)
        } else {
            true
        }

        if (allowPlayback) {
            NoorNotificationHelper.showNotification(context, id, channel, title, body)
        }

        if (isExactPrayer && prayer != null && allowPlayback) {
            AdhanPlaybackService.startAdhan(context, prayer, title, body, isTestAlarm = isTestAlarm)
        }

        if (!isTestAlarm) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = UserPreferencesRepository(context).getPreferencesOnce()
                    NoorAlarmScheduler.rescheduleAll(context, prefs)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
