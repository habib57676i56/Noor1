package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.PowerManager
import com.example.NoorApplication
import com.example.audio.AdhanCatalog
import com.example.audio.AdhanDuplicateGuard
import com.example.audio.AdhanPlaybackService
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.domain.model.PrayerName
import com.example.widgets.PrayerTimesWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

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

        // Acquire a short CPU WakeLock immediately so locked/Doze phones never suspend before AdhanPlaybackService starts
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val receiverWakeLock = runCatching {
            pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NOOR:AdhanAlarmReceiver")?.apply {
                setReferenceCounted(false)
                acquire(30_000L)
            }
        }.getOrNull()

        val title = intent.getStringExtra(NoorAlarmScheduler.EXTRA_TITLE) ?: "نور · NOOR"
        val body = intent.getStringExtra(NoorAlarmScheduler.EXTRA_BODY) ?: ""
        val channel = intent.getStringExtra(NoorAlarmScheduler.EXTRA_CHANNEL)
            ?: NoorNotificationHelper.CHANNEL_PRAYER
        val id = intent.getIntExtra(NoorAlarmScheduler.EXTRA_NOTIFICATION_ID, 3001)
        val isExactPrayer = intent.getBooleanExtra(NoorAlarmScheduler.EXTRA_IS_EXACT_PRAYER, false)
        val isTestAlarm = intent.getBooleanExtra(NoorAlarmScheduler.EXTRA_IS_TEST_ALARM, false)
        val prayerKey = intent.getStringExtra(NoorAlarmScheduler.EXTRA_PRAYER_KEY)
        val reminderType = intent.getStringExtra(NoorAlarmScheduler.EXTRA_REMINDER_TYPE)
        val customVoiceId = intent.getStringExtra(NoorAlarmScheduler.EXTRA_CUSTOM_VOICE_ID)
        val customLocalPath = intent.getStringExtra(NoorAlarmScheduler.EXTRA_CUSTOM_LOCAL_PATH)

        val prayer = prayerKey?.let { runCatching { PrayerName.valueOf(it) }.getOrNull() }
        val allowPlayback = if (isExactPrayer && prayer != null) {
            AdhanDuplicateGuard.shouldAllowPlayback(context, prayer, isTestAlarm = isTestAlarm)
        } else {
            true
        }

        val willPlayAdhanAudio = isExactPrayer && prayer != null && allowPlayback
        val willPlayReminderAudio = !willPlayAdhanAudio &&
            !reminderType.isNullOrBlank() &&
            !customVoiceId.isNullOrBlank() &&
            customVoiceId != AdhanCatalog.SYSTEM_DEFAULT_ID

        var fgsStarted = false
        if (willPlayAdhanAudio && prayer != null) {
            fgsStarted = AdhanPlaybackService.startAdhan(
                context = context,
                prayer = prayer,
                title = title,
                body = body,
                isTestAlarm = isTestAlarm,
                voiceId = customVoiceId,
                customLocalPath = customLocalPath
            )
        } else if (willPlayReminderAudio && !customVoiceId.isNullOrBlank()) {
            fgsStarted = AdhanPlaybackService.startReminderSound(
                context = context,
                voiceId = customVoiceId,
                title = title,
                body = body,
                isTestAlarm = isTestAlarm
            )
        }

        // Show prayer/reminder banner; mark silent when AdhanPlaybackService is playing custom audio
        // so the system notification ding never collides with or steals audio focus from the Adhan!
        if (allowPlayback) {
            NoorNotificationHelper.showNotification(
                context = context,
                notificationId = id,
                channelId = channel,
                title = title,
                body = body,
                silentBecauseAdhanPlays = (willPlayAdhanAudio || willPlayReminderAudio) && fgsStarted
            )
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = UserPreferencesRepository(context).getPreferencesOnce()

                // Fallback audio playback if an OEM ROM blocked background startForegroundService
                if (willPlayAdhanAudio && prayer != null && !fgsStarted) {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    val isSilentOrVibrate = audioManager != null &&
                        audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
                    val canPlayAudio = (isTestAlarm || (prefs.adhanSoundEnabled && !prefs.adhanNotificationOnly)) &&
                        (isTestAlarm || !(prefs.adhanRespectSilentMode && isSilentOrVibrate)) &&
                        AdhanDuplicateGuard.shouldAllowPlayback(context, prayer, isTestAlarm = isTestAlarm)

                    if (canPlayAudio) {
                        if (!isTestAlarm) {
                            AdhanDuplicateGuard.markPlayed(context, prayer)
                        }
                        val voiceId = prefs.adhanConfig.voiceIdForPrayer(prayer)
                            .ifBlank { customVoiceId.orEmpty() }
                            .ifBlank { AdhanCatalog.DEFAULT_VOICE_ID }
                        var voice = AdhanCatalog.resolveVoice(voiceId, prefs.customAdhanRecords, context)
                        if (voice.localFilePath.isNullOrBlank() &&
                            !customLocalPath.isNullOrBlank() &&
                            File(customLocalPath).let { it.exists() && it.length() > 0L }
                        ) {
                            voice = voice.copy(localFilePath = customLocalPath, isDownloadedOffline = true)
                        }
                        val appPlayer = (context.applicationContext as? NoorApplication)?.adhanAudioPlayer
                        appPlayer?.playVoice(
                            voice = voice,
                            isAlarmPlayback = true,
                            volume = prefs.adhanVolume
                        )
                    }
                }

                if (!isTestAlarm) {
                    NoorAlarmScheduler.rescheduleAll(context, prefs)
                }
            } finally {
                runCatching {
                    if (receiverWakeLock?.isHeld == true) receiverWakeLock.release()
                }
                pendingResult.finish()
            }
        }
    }
}
