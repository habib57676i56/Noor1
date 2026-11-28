package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import kotlin.math.roundToInt

/**
 * Dedicated Adhan Audio Player supporting:
 * - Instant preview in Adhan Settings
 * - Full Adhan playback from background prayer alarm service (with CPU WakeLock + DND bypass via USAGE_ALARM)
 * - Reliable local custom/offline file descriptor playback
 * - Proper Android AudioFocus management without letting notification chimes abort live Adhan
 * - Automatic fallback to built-in default Adhan or device alarm tone if selected file/stream is unavailable
 */
class AdhanAudioPlayer(private val appContext: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var isCurrentPlaybackAlarm: Boolean = false
    private var savedAlarmStreamVolume: Int? = null
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _playingVoiceId = MutableStateFlow<String?>(null)
    val playingVoiceId: StateFlow<String?> = _playingVoiceId.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (isCurrentPlaybackAlarm) {
            // During live prayer Adhan, ignore transient notification dips so system chimes never kill the Adhan;
            // only stop on permanent loss (e.g. an incoming phone call).
            if (change == AudioManager.AUDIOFOCUS_LOSS) {
                stop()
            }
        } else if (change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        ) {
            stop()
        }
    }

    fun playVoice(
        voice: AdhanVoiceOption,
        isAlarmPlayback: Boolean = false,
        volume: Float = 1.0f,
        onCompleted: (() -> Unit)? = null,
        onFallbackNotice: ((String) -> Unit)? = null
    ) {
        if (_playingVoiceId.value == voice.id && !isAlarmPlayback) {
            stop()
            return
        }
        stop()

        isCurrentPlaybackAlarm = isAlarmPlayback
        val clampedVol = volume.coerceIn(0.05f, 1.0f)

        if (isAlarmPlayback) {
            ensureAudibleAlarmStream(clampedVol)
        }

        if (voice.isSystemDefault) {
            playSystemFallback(voice.id, isAlarmPlayback, clampedVol, onCompleted)
            return
        }

        val localPath = voice.localFilePath
        val verifiedLocal = localPath?.takeIf { File(it).exists() && File(it).length() > 0L }
        val source: String? = when {
            verifiedLocal != null -> verifiedLocal
            voice.audioUrl.isNotBlank() -> voice.audioUrl
            voice.fallbackUrl.isNotBlank() -> voice.fallbackUrl
            else -> null
        }

        if (source == null) {
            onFallbackNotice?.invoke("Selected Adhan file unavailable — playing default tone")
            playSystemFallback(voice.id, isAlarmPlayback, clampedVol, onCompleted)
            return
        }

        startMediaSource(
            voice = voice,
            source = source,
            secondaryUrl = if (verifiedLocal == null && source == voice.audioUrl) voice.fallbackUrl else "",
            isLocal = verifiedLocal != null,
            isAlarmPlayback = isAlarmPlayback,
            volume = clampedVol,
            onCompleted = onCompleted,
            onFallbackNotice = onFallbackNotice
        )
    }

    private fun startMediaSource(
        voice: AdhanVoiceOption,
        source: String,
        secondaryUrl: String,
        isLocal: Boolean,
        isAlarmPlayback: Boolean,
        volume: Float,
        onCompleted: (() -> Unit)?,
        onFallbackNotice: ((String) -> Unit)?
    ) {
        releaseMediaPlayerOnly()
        requestFocus(isAlarmPlayback)
        _playingVoiceId.value = voice.id
        _isBuffering.value = !isLocal

        try {
            val usage = if (isAlarmPlayback) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
            val mp = MediaPlayer().apply {
                if (isAlarmPlayback) {
                    runCatching { setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK) }
                }
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(usage)
                        .build()
                )
                if (isLocal) {
                    val file = File(source)
                    FileInputStream(file).use { fis ->
                        setDataSource(fis.fd)
                    }
                } else {
                    setDataSource(source)
                }
                setOnPreparedListener { player ->
                    _isBuffering.value = false
                    player.setVolume(volume, volume)
                    player.start()
                }
                setOnCompletionListener {
                    stop()
                    onCompleted?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    if (secondaryUrl.isNotBlank()) {
                        startMediaSource(
                            voice = voice,
                            source = secondaryUrl,
                            secondaryUrl = "",
                            isLocal = false,
                            isAlarmPlayback = isAlarmPlayback,
                            volume = volume,
                            onCompleted = onCompleted,
                            onFallbackNotice = onFallbackNotice
                        )
                    } else if (isLocal && voice.audioUrl.isNotBlank()) {
                        startMediaSource(
                            voice = voice,
                            source = voice.audioUrl,
                            secondaryUrl = voice.fallbackUrl,
                            isLocal = false,
                            isAlarmPlayback = isAlarmPlayback,
                            volume = volume,
                            onCompleted = onCompleted,
                            onFallbackNotice = onFallbackNotice
                        )
                    } else {
                        onFallbackNotice?.invoke("Unable to play selected Adhan — using default tone")
                        playSystemFallback(voice.id, isAlarmPlayback, volume, onCompleted)
                    }
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            if (secondaryUrl.isNotBlank()) {
                startMediaSource(
                    voice = voice,
                    source = secondaryUrl,
                    secondaryUrl = "",
                    isLocal = false,
                    isAlarmPlayback = isAlarmPlayback,
                    volume = volume,
                    onCompleted = onCompleted,
                    onFallbackNotice = onFallbackNotice
                )
            } else {
                onFallbackNotice?.invoke("Unable to open Adhan source — using default tone")
                playSystemFallback(voice.id, isAlarmPlayback, volume, onCompleted)
            }
        }
    }

    private fun playSystemFallback(
        voiceId: String,
        isAlarmPlayback: Boolean,
        volume: Float = 1.0f,
        onCompleted: (() -> Unit)?
    ) {
        releaseMediaPlayerOnly()
        requestFocus(isAlarmPlayback)
        _playingVoiceId.value = voiceId
        _isBuffering.value = false
        try {
            val uri = if (isAlarmPlayback) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
            if (uri == null) {
                stop()
                onCompleted?.invoke()
                return
            }
            val mp = MediaPlayer().apply {
                if (isAlarmPlayback) {
                    runCatching { setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK) }
                }
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(if (isAlarmPlayback) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                setDataSource(appContext, uri)
                setOnPreparedListener {
                    it.setVolume(volume, volume)
                    it.start()
                }
                setOnCompletionListener {
                    stop()
                    onCompleted?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    stop()
                    onCompleted?.invoke()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            stop()
            onCompleted?.invoke()
        }
    }

    /**
     * USAGE_ALARM bypasses Do Not Disturb & Silent/Vibrate mode on Android.
     * If the user's ALARM stream was accidentally left at 0, temporarily lift it to an audible level
     * and restore the previous level cleanly once Adhan finishes or is stopped.
     */
    private fun ensureAudibleAlarmStream(targetFraction: Float) {
        val am = audioManager ?: return
        try {
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            if (maxVol <= 0) return
            val currentVol = am.getStreamVolume(AudioManager.STREAM_ALARM)
            if (currentVol == 0) {
                savedAlarmStreamVolume = currentVol
                val desired = (maxVol * targetFraction.coerceIn(0.35f, 1.0f)).roundToInt().coerceIn(1, maxVol)
                am.setStreamVolume(AudioManager.STREAM_ALARM, desired, 0)
            }
        } catch (_: SecurityException) {
            // Some devices restrict volume mutation in Total Silence DND; USAGE_ALARM still plays at system level
        } catch (_: Exception) {
        }
    }

    private fun restoreAlarmStreamIfAdjusted() {
        val prev = savedAlarmStreamVolume ?: return
        savedAlarmStreamVolume = null
        val am = audioManager ?: return
        try {
            am.setStreamVolume(AudioManager.STREAM_ALARM, prev, 0)
        } catch (_: Exception) {
        }
    }

    private fun requestFocus(isAlarmPlayback: Boolean) {
        val am = audioManager ?: return
        val focusGain = if (isAlarmPlayback) {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
        } else {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(if (isAlarmPlayback) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
                    .build()
                val req = AudioFocusRequest.Builder(focusGain)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener(focusListener)
                    .build()
                focusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    focusListener,
                    if (isAlarmPlayback) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC,
                    focusGain
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { am.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(focusListener)
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseMediaPlayerOnly() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }

    fun stop() {
        releaseMediaPlayerOnly()
        abandonFocus()
        restoreAlarmStreamIfAdjusted()
        isCurrentPlaybackAlarm = false
        _playingVoiceId.value = null
        _isBuffering.value = false
    }
}
