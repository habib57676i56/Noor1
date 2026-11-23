package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.PrayerName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Media Playback Service for reliable prayer-time Adhan playback
 * with one-tap Stop Adhan action and respect for device ringer settings.
 */
class AdhanPlaybackService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var adhanPlayer: AdhanAudioPlayer

    override fun onCreate() {
        super.onCreate()
        adhanPlayer = AdhanAudioPlayer(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ADHAN -> {
                adhanPlayer.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PLAY_ADHAN -> {
                val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY) ?: PrayerName.FAJR.name
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "حان وقت الأذان · Adhan"
                val body = intent.getStringExtra(EXTRA_BODY) ?: ""
                val isTestAlarm = intent.getBooleanExtra(EXTRA_IS_TEST_ALARM, false)

                ensureChannel()
                val notification = buildNotification(title, body)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                scope.launch {
                    val prefs = UserPreferencesRepository(applicationContext).getPreferencesOnce()
                    val prayer = runCatching { PrayerName.valueOf(prayerKey) }.getOrDefault(PrayerName.FAJR)
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    val isSilentOrVibrate = audioManager != null &&
                        audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL

                    // Respect notification-only mode, disabled sound, silent mode, and duplicate-playback guard
                    val canPlayAudio = (isTestAlarm || (prefs.adhanSoundEnabled && !prefs.adhanNotificationOnly)) &&
                        (isTestAlarm || !(prefs.adhanRespectSilentMode && isSilentOrVibrate)) &&
                        AdhanDuplicateGuard.shouldAllowPlayback(applicationContext, prayer, isTestAlarm = isTestAlarm)

                    if (!canPlayAudio) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@launch
                    }

                    if (!isTestAlarm) {
                        AdhanDuplicateGuard.markPlayed(applicationContext, prayer)
                    }

                    val voiceId = prefs.adhanConfig.voiceIdForPrayer(prayer)
                    val voice = AdhanCatalog.resolveVoice(voiceId, prefs.customAdhanRecords, applicationContext)

                    val offlineTag = if (voice.isDownloadedOffline) {
                        if (prefs.language == AppLanguage.ARABIC) " (محلي)" else " (Offline)"
                    } else ""
                    val subText = if (prefs.language == AppLanguage.ARABIC) {
                        "${voice.nameAr}$offlineTag${if (body.isNotBlank()) " · $body" else ""}"
                    } else {
                        "${voice.nameEn}$offlineTag${if (body.isNotBlank()) " · $body" else ""}"
                    }
                    val updatedNotif = buildNotification(title, subText)
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, updatedNotif)

                    adhanPlayer.playVoice(
                        voice = voice,
                        isAlarmPlayback = true,
                        volume = prefs.adhanVolume,
                        onCompleted = {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                    )
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        adhanPlayer.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NOOR Adhan Playback",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live Adhan audio playback and Stop Adhan controls"
                setSound(null, null)
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, subtitle: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SHORTCUT_ROUTE, "prayers")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            5101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AdhanPlaybackService::class.java).apply {
            action = ACTION_STOP_ADHAN
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            5102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "إيقاف الأذان / Stop", stopPendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "noor_adhan_playback"
        const val NOTIFICATION_ID = 4205
        const val ACTION_PLAY_ADHAN = "com.example.audio.ACTION_PLAY_ADHAN"
        const val ACTION_STOP_ADHAN = "com.example.audio.ACTION_STOP_ADHAN"
        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_IS_TEST_ALARM = "extra_is_test_alarm"
        const val EXTRA_SHORTCUT_ROUTE = "extra_shortcut_route"

        fun startAdhan(
            context: Context,
            prayer: PrayerName,
            title: String,
            body: String,
            isTestAlarm: Boolean = false
        ) {
            try {
                val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                    action = ACTION_PLAY_ADHAN
                    putExtra(EXTRA_PRAYER_KEY, prayer.name)
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_BODY, body)
                    putExtra(EXTRA_IS_TEST_ALARM, isTestAlarm)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        }

        fun stopAdhan(context: Context) {
            try {
                val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                    action = ACTION_STOP_ADHAN
                }
                context.startService(intent)
            } catch (_: Exception) {
            }
        }
    }
}
