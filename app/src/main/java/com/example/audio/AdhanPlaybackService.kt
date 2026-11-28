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
import android.os.PowerManager
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
import java.io.File

/**
 * Foreground Media Playback Service for reliable prayer-time Adhan playback
 * even when the app is closed, the phone screen is off/locked, or Do Not Disturb is active.
 * Holds a timed PARTIAL_WAKE_LOCK across preference load and MediaPlayer playback,
 * routes audio on AudioAttributes.USAGE_ALARM (DND bypass), and exposes a 1-tap Stop action.
 */
class AdhanPlaybackService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var adhanPlayer: AdhanAudioPlayer
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        adhanPlayer = AdhanAudioPlayer(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquirePlaybackWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            if (wakeLock == null) {
                wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "NOOR:AdhanPlaybackWakeLock"
                ).apply { setReferenceCounted(false) }
            }
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(MAX_WAKE_LOCK_MS)
            }
        } catch (_: Exception) {
        }
    }

    private fun releasePlaybackWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
    }

    private fun finishAndStop() {
        adhanPlayer.stop()
        releasePlaybackWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ADHAN -> {
                finishAndStop()
                return START_NOT_STICKY
            }
            ACTION_PLAY_ADHAN -> {
                acquirePlaybackWakeLock()
                val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY) ?: PrayerName.FAJR.name
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "حان وقت الأذان · Adhan"
                val body = intent.getStringExtra(EXTRA_BODY) ?: ""
                val isTestAlarm = intent.getBooleanExtra(EXTRA_IS_TEST_ALARM, false)
                val intentVoiceId = intent.getStringExtra(EXTRA_VOICE_ID).orEmpty()
                val intentLocalPath = intent.getStringExtra(EXTRA_CUSTOM_LOCAL_PATH).orEmpty()

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
                    try {
                        val prefs = UserPreferencesRepository(applicationContext).getPreferencesOnce()
                        val prayer = runCatching { PrayerName.valueOf(prayerKey) }.getOrDefault(PrayerName.FAJR)
                        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                        val isSilentOrVibrate = audioManager != null &&
                            audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL

                        // By default adhanRespectSilentMode is FALSE so Adhan plays even in Silent / Vibrate / DND.
                        // Respect mute ONLY when the user explicitly opted into adhanRespectSilentMode or notification-only mode.
                        val canPlayAudio = (isTestAlarm || (prefs.adhanSoundEnabled && !prefs.adhanNotificationOnly)) &&
                            (isTestAlarm || !(prefs.adhanRespectSilentMode && isSilentOrVibrate)) &&
                            AdhanDuplicateGuard.shouldAllowPlayback(applicationContext, prayer, isTestAlarm = isTestAlarm)

                        if (!canPlayAudio) {
                            finishAndStop()
                            return@launch
                        }

                        if (!isTestAlarm) {
                            AdhanDuplicateGuard.markPlayed(applicationContext, prayer)
                        }

                        // Resolve the user's chosen voice (live preference first, falling back to Intent extra)
                        val resolvedVoiceId = prefs.adhanConfig.voiceIdForPrayer(prayer)
                            .ifBlank { intentVoiceId }
                            .ifBlank { AdhanCatalog.DEFAULT_VOICE_ID }
                        var voice = AdhanCatalog.resolveVoice(resolvedVoiceId, prefs.customAdhanRecords, applicationContext)

                        // Guarantee custom imported/cached local file path attached at schedule time is preserved
                        if (voice.localFilePath.isNullOrBlank() &&
                            intentLocalPath.isNotBlank() &&
                            File(intentLocalPath).let { it.exists() && it.length() > 0L }
                        ) {
                            voice = voice.copy(
                                localFilePath = intentLocalPath,
                                isDownloadedOffline = true
                            )
                        }

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
                            onCompleted = { finishAndStop() }
                        )
                    } catch (_: Exception) {
                        finishAndStop()
                    }
                }
            }
            ACTION_PLAY_REMINDER_SOUND -> {
                acquirePlaybackWakeLock()
                val voiceId = intent.getStringExtra(EXTRA_VOICE_ID).orEmpty()
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "تذكير نور · NOOR Reminder"
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
                    try {
                        val prefs = UserPreferencesRepository(applicationContext).getPreferencesOnce()
                        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                        val isSilentOrVibrate = audioManager != null &&
                            audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL

                        val canPlayAudio = isTestAlarm || !(prefs.adhanRespectSilentMode && isSilentOrVibrate)
                        if (!canPlayAudio || voiceId == AdhanCatalog.SYSTEM_DEFAULT_ID) {
                            finishAndStop()
                            return@launch
                        }

                        val voice = AdhanCatalog.resolveVoice(voiceId, prefs.customAdhanRecords, applicationContext)
                        val soundLabel = if (prefs.language == AppLanguage.ARABIC) voice.nameAr else voice.nameEn
                        val subText = if (body.isNotBlank()) "$soundLabel · $body" else soundLabel
                        val updatedNotif = buildNotification(title, subText)
                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(NOTIFICATION_ID, updatedNotif)

                        adhanPlayer.playVoice(
                            voice = voice,
                            isAlarmPlayback = true,
                            volume = prefs.adhanVolume,
                            onCompleted = { finishAndStop() }
                        )
                    } catch (_: Exception) {
                        finishAndStop()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        adhanPlayer.stop()
        releasePlaybackWakeLock()
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
                description = "Live Adhan audio playback and Stop Adhan controls (bypasses Do Not Disturb)"
                setSound(null, null)
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSilent(true)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "إيقاف الأذان / Stop", stopPendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "noor_adhan_playback"
        const val NOTIFICATION_ID = 4205
        private const val MAX_WAKE_LOCK_MS = 5L * 60L * 1000L
        const val ACTION_PLAY_ADHAN = "com.example.audio.ACTION_PLAY_ADHAN"
        const val ACTION_PLAY_REMINDER_SOUND = "com.example.audio.ACTION_PLAY_REMINDER_SOUND"
        const val ACTION_STOP_ADHAN = "com.example.audio.ACTION_STOP_ADHAN"
        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
        const val EXTRA_VOICE_ID = "extra_voice_id"
        const val EXTRA_CUSTOM_LOCAL_PATH = "extra_custom_local_path"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_IS_TEST_ALARM = "extra_is_test_alarm"
        const val EXTRA_SHORTCUT_ROUTE = "extra_shortcut_route"

        fun startAdhan(
            context: Context,
            prayer: PrayerName,
            title: String,
            body: String,
            isTestAlarm: Boolean = false,
            voiceId: String? = null,
            customLocalPath: String? = null
        ): Boolean {
            return try {
                val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                    action = ACTION_PLAY_ADHAN
                    putExtra(EXTRA_PRAYER_KEY, prayer.name)
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_BODY, body)
                    putExtra(EXTRA_IS_TEST_ALARM, isTestAlarm)
                    if (!voiceId.isNullOrBlank()) {
                        putExtra(EXTRA_VOICE_ID, voiceId)
                    }
                    if (!customLocalPath.isNullOrBlank()) {
                        putExtra(EXTRA_CUSTOM_LOCAL_PATH, customLocalPath)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            } catch (_: Exception) {
                false
            }
        }

        fun startReminderSound(
            context: Context,
            voiceId: String,
            title: String,
            body: String,
            isTestAlarm: Boolean = false
        ): Boolean {
            if (voiceId.isBlank() || voiceId == AdhanCatalog.SYSTEM_DEFAULT_ID) return false
            return try {
                val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                    action = ACTION_PLAY_REMINDER_SOUND
                    putExtra(EXTRA_VOICE_ID, voiceId)
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_BODY, body)
                    putExtra(EXTRA_IS_TEST_ALARM, isTestAlarm)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            } catch (_: Exception) {
                false
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
