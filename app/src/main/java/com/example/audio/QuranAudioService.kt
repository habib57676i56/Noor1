package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import com.example.MainActivity
import com.example.NoorApplication
import com.example.R
import com.example.data.local.catalog.AudioLibraryCatalog
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.domain.model.AppLanguage
import com.example.widgets.QuranPlayerWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Media Playback Service with full Android MediaSession + MediaStyle notification
 * providing live Notification Panel, Lock Screen, Bluetooth/Headset, and Home Screen Widget controls
 * even after MainActivity has been closed.
 */
class QuranAudioService : Service() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val player: QuranAudioPlayer
        get() = (application as NoorApplication).quranAudioPlayer

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        initMediaSession()
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "NoorQuranMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    handlePlayOrStartDefault()
                }

                override fun onPause() {
                    player.pause()
                    syncSessionAndNotification()
                }

                override fun onSkipToNext() {
                    player.playNext()
                    syncSessionAndNotification()
                }

                override fun onSkipToPrevious() {
                    player.playPrevious()
                    syncSessionAndNotification()
                }

                override fun onSeekTo(pos: Long) {
                    player.seekTo(pos)
                    syncSessionAndNotification()
                }

                override fun onStop() {
                    player.stop()
                    stopForegroundAndShutdown()
                }
            })
            isActive = true
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                player.stop()
                stopForegroundAndShutdown()
                return START_NOT_STICKY
            }
            ACTION_PLAY_PAUSE -> {
                val st = player.state.value
                if (!st.hasActiveSession) {
                    handlePlayOrStartDefault()
                } else {
                    player.togglePauseResume()
                    syncSessionAndNotification()
                }
            }
            ACTION_NEXT -> {
                val st = player.state.value
                if (!st.hasActiveSession) {
                    handlePlayOrStartDefault(offsetSurah = 1)
                } else {
                    player.playNext()
                    syncSessionAndNotification()
                }
            }
            ACTION_PREVIOUS -> {
                val st = player.state.value
                if (!st.hasActiveSession) {
                    handlePlayOrStartDefault(offsetSurah = -1)
                } else {
                    player.playPrevious()
                    syncSessionAndNotification()
                }
            }
            ACTION_UPDATE -> {
                syncSessionAndNotification(
                    fallbackTitle = intent.getStringExtra(EXTRA_TITLE),
                    fallbackSubtitle = intent.getStringExtra(EXTRA_SUBTITLE)
                )
            }
        }
        return START_NOT_STICKY
    }

    private fun handlePlayOrStartDefault(offsetSurah: Int = 0) {
        val st = player.state.value
        if (st.hasActiveSession) {
            player.resume()
            syncSessionAndNotification()
            return
        }
        serviceScope.launch {
            val prefs = UserPreferencesRepository(applicationContext).getPreferencesOnce()
            val queue = AudioLibraryCatalog.surahTracksForReciter(prefs.quranReciter)
            val targetSurah = (prefs.lastReadSurah + offsetSurah).coerceIn(1, 114)
            val startIdx = queue.indexOfFirst { it.surahNumber == targetSurah }.coerceAtLeast(0)
            player.playTrackQueue(queue, startIdx, prefs.audioPlaybackSpeed)
            syncSessionAndNotification()
        }
    }

    private fun syncSessionAndNotification(
        fallbackTitle: String? = null,
        fallbackSubtitle: String? = null
    ) {
        val st = player.state.value
        if (!st.hasActiveSession) {
            stopForegroundAndShutdown()
            return
        }

        val isArabic = try {
            resources.configuration.locales[0].language == "ar"
        } catch (_: Exception) {
            true
        }

        val title = fallbackTitle?.takeIf { it.isNotBlank() }
            ?: (if (isArabic) st.titleAr else st.titleEn).ifBlank { "نور — القرآن الكريم" }
        val subtitle = fallbackSubtitle?.takeIf { it.isNotBlank() }
            ?: (if (isArabic) st.subtitleAr else st.subtitleEn).ifBlank { "تلاوة قرآنية" }

        updateMediaSessionState(title, subtitle, st)

        val notification = buildMediaNotification(title, subtitle, st)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, notification)
        }

        QuranPlayerWidgetProvider.requestUpdate(applicationContext)
    }

    private fun updateMediaSessionState(
        title: String,
        subtitle: String,
        st: AudioPlaybackState
    ) {
        val session = mediaSession ?: return
        val pbState = when {
            st.isBuffering -> PlaybackState.STATE_BUFFERING
            st.isPlaying -> PlaybackState.STATE_PLAYING
            else -> PlaybackState.STATE_PAUSED
        }

        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_STOP or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_SEEK_TO

        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(pbState, st.currentPositionMs, if (st.isPlaying) st.playbackSpeed else 0f)
                .build()
        )

        val art = runCatching {
            BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        }.getOrNull()

        val metaBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, subtitle)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "NOOR · نور")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, st.durationMs.coerceAtLeast(0L))
        if (art != null) {
            metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art)
        }
        session.setMetadata(metaBuilder.build())
        session.isActive = true
    }

    private fun stopForegroundAndShutdown() {
        try {
            mediaSession?.isActive = false
            mediaSession?.setPlaybackState(
                PlaybackState.Builder()
                    .setState(PlaybackState.STATE_STOPPED, 0L, 0f)
                    .build()
            )
        } catch (_: Exception) {
        }
        QuranPlayerWidgetProvider.requestUpdate(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        try {
            mediaSession?.release()
        } catch (_: Exception) {
        }
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NOOR Quran Recitation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background Quran audio playback and lock-screen media controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildMediaNotification(
        title: String,
        subtitle: String,
        st: AudioPlaybackState
    ): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SHORTCUT_ROUTE, "audio_library")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            4100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPending = serviceActionPendingIntent(ACTION_PREVIOUS, 4101)
        val playPausePending = serviceActionPendingIntent(ACTION_PLAY_PAUSE, 4102)
        val nextPending = serviceActionPendingIntent(ACTION_NEXT, 4103)
        val stopPending = serviceActionPendingIntent(ACTION_STOP, 4104)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val largeIcon = runCatching {
            BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        }.getOrNull()

        val playPauseIcon = if (st.isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseLabel = if (st.isPlaying) "Pause" else "Play"

        val mediaStyle = Notification.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
        mediaSession?.sessionToken?.let { mediaStyle.setMediaSession(it) }

        val ayahSub = if (st.playingAyah != null) {
            "$subtitle · Ayah ${st.playingAyah}"
        } else {
            subtitle
        }

        builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(ayahSub)
            .setOngoing(st.isPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setStyle(mediaStyle)
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    prevPending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    playPauseIcon,
                    playPauseLabel,
                    playPausePending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_next,
                    "Next",
                    nextPending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Stop",
                    stopPending
                ).build()
            )

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        return builder.build()
    }

    private fun serviceActionPendingIntent(actionName: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, QuranAudioService::class.java).apply { action = actionName }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "noor_audio_playback"
        const val NOTIFICATION_ID = 4102
        const val ACTION_UPDATE = "com.example.audio.ACTION_UPDATE"
        const val ACTION_PLAY_PAUSE = "com.example.audio.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.audio.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.audio.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.example.audio.ACTION_STOP"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_SHORTCUT_ROUTE = "extra_shortcut_route"

        fun sendAction(context: Context, actionName: String) {
            try {
                val intent = Intent(context, QuranAudioService::class.java).apply {
                    action = actionName
                }
                if (actionName == ACTION_STOP) {
                    context.startService(intent)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        }

        fun updateNotification(context: Context, title: String, subtitle: String, isPlaying: Boolean) {
            try {
                val intent = Intent(context, QuranAudioService::class.java).apply {
                    action = ACTION_UPDATE
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_SUBTITLE, subtitle)
                    putExtra(EXTRA_IS_PLAYING, isPlaying)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                // Safe fallback in restricted background or Robolectric test environments
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, QuranAudioService::class.java).apply {
                    action = ACTION_STOP
                }
                context.stopService(intent)
            } catch (_: Exception) {
            }
        }
    }
}
