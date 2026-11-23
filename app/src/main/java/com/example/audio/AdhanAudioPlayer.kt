package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Dedicated Adhan Audio Player supporting:
 * - Instant preview in Adhan Settings
 * - Full Adhan playback from background prayer alarm service
 * - Proper Android AudioFocus management
 * - Automatic fallback to built-in default Adhan or device ringtone if selected file/stream is unavailable
 */
class AdhanAudioPlayer(private val appContext: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _playingVoiceId = MutableStateFlow<String?>(null)
    val playingVoiceId: StateFlow<String?> = _playingVoiceId.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS ||
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

        val clampedVol = volume.coerceIn(0.05f, 1.0f)

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
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(usage)
                        .build()
                )
                setDataSource(source)
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
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (uri == null) {
                stop()
                onCompleted?.invoke()
                return
            }
            val mp = MediaPlayer().apply {
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

    private fun requestFocus(isAlarmPlayback: Boolean) {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(if (isAlarmPlayback) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
                    .build()
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
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
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
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
        _playingVoiceId.value = null
        _isBuffering.value = false
    }
}
