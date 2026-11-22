package com.example.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import com.example.domain.model.AudioRepeatMode
import com.example.domain.model.AudioTrackItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentTrack: AudioTrackItem? = null,
    val playingSurah: Int? = null,
    val playingAyah: Int? = null,
    val titleAr: String = "",
    val titleEn: String = "",
    val subtitleAr: String = "",
    val subtitleEn: String = "",
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: AudioRepeatMode = AudioRepeatMode.OFF,
    val sleepTimerRemainingSec: Int? = null,
    val isOfflineFile: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val errorMessage: String? = null
) {
    val hasActiveSession: Boolean
        get() = currentTrack != null || playingSurah != null || isBuffering || isPlaying
}

class QuranAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private var activeQueue: List<AudioTrackItem> = emptyList()
    private var activeQueueIndex: Int = -1
    private var localFileResolver: ((String) -> String?)? = null
    private var onTrackStartedCallback: ((AudioTrackItem) -> Unit)? = null

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    fun configureCallbacks(
        resolveLocalFile: (trackId: String) -> String?,
        onTrackStarted: (AudioTrackItem) -> Unit
    ) {
        localFileResolver = resolveLocalFile
        onTrackStartedCallback = onTrackStarted
    }

    fun playTrackQueue(
        queue: List<AudioTrackItem>,
        startIndex: Int,
        initialSpeed: Float = _state.value.playbackSpeed
    ) {
        if (queue.isEmpty() || startIndex !in queue.indices) return
        activeQueue = queue
        activeQueueIndex = startIndex
        playTrackInternal(queue[startIndex], initialSpeed)
    }

    private fun playTrackInternal(track: AudioTrackItem, speed: Float) {
        releasePlayerOnly()
        val localPath = localFileResolver?.invoke(track.id)
        val source = localPath ?: track.audioUrl
        val isLocal = localPath != null

        _state.value = _state.value.copy(
            isPlaying = false,
            isBuffering = true,
            currentTrack = track,
            playingSurah = track.surahNumber,
            playingAyah = null,
            titleAr = track.titleAr,
            titleEn = track.titleEn,
            subtitleAr = track.subtitleAr,
            subtitleEn = track.subtitleEn,
            currentPositionMs = 0L,
            durationMs = 0L,
            playbackSpeed = speed,
            isOfflineFile = isLocal,
            hasNext = activeQueueIndex + 1 < activeQueue.size || _state.value.repeatMode == AudioRepeatMode.REPEAT_ALL,
            hasPrevious = activeQueueIndex > 0,
            errorMessage = null
        )
        onTrackStartedCallback?.invoke(track)

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(source)
                setOnPreparedListener { player ->
                    applySpeed(player, _state.value.playbackSpeed)
                    player.start()
                    _state.value = _state.value.copy(
                        isPlaying = true,
                        isBuffering = false,
                        durationMs = player.duration.coerceAtLeast(0).toLong()
                    )
                    startProgressPolling()
                }
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    progressJob?.cancel()
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = "Audio stream unavailable offline or network error"
                    )
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                errorMessage = "Unable to start audio playback"
            )
        }
    }

    private fun handleTrackCompletion() {
        val mode = _state.value.repeatMode
        when {
            mode == AudioRepeatMode.REPEAT_ONE && activeQueueIndex in activeQueue.indices -> {
                playTrackInternal(activeQueue[activeQueueIndex], _state.value.playbackSpeed)
            }
            activeQueueIndex + 1 < activeQueue.size -> {
                activeQueueIndex += 1
                playTrackInternal(activeQueue[activeQueueIndex], _state.value.playbackSpeed)
            }
            mode == AudioRepeatMode.REPEAT_ALL && activeQueue.isNotEmpty() -> {
                activeQueueIndex = 0
                playTrackInternal(activeQueue[0], _state.value.playbackSpeed)
            }
            else -> {
                progressJob?.cancel()
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    currentPositionMs = _state.value.durationMs
                )
            }
        }
    }

    fun playNext() {
        if (activeQueue.isEmpty()) return
        if (activeQueueIndex + 1 < activeQueue.size) {
            activeQueueIndex += 1
            playTrackInternal(activeQueue[activeQueueIndex], _state.value.playbackSpeed)
        } else if (_state.value.repeatMode == AudioRepeatMode.REPEAT_ALL) {
            activeQueueIndex = 0
            playTrackInternal(activeQueue[0], _state.value.playbackSpeed)
        }
    }

    fun playPrevious() {
        val mp = mediaPlayer
        if (mp != null && _state.value.currentPositionMs > 4000L) {
            seekTo(0L)
            return
        }
        if (activeQueue.isNotEmpty() && activeQueueIndex > 0) {
            activeQueueIndex -= 1
            playTrackInternal(activeQueue[activeQueueIndex], _state.value.playbackSpeed)
        }
    }

    fun playAyah(
        surahNumber: Int,
        ayahNumber: Int,
        audioUrl: String,
        titleAr: String = "سورة $surahNumber • آية $ayahNumber",
        titleEn: String = "Surah $surahNumber • Ayah $ayahNumber",
        reciterNameAr: String = "",
        reciterNameEn: String = "",
        repeatRemaining: Int = 1,
        onCompleted: (() -> Unit)? = null
    ) {
        releasePlayerOnly()
        activeQueue = emptyList()
        activeQueueIndex = -1

        _state.value = _state.value.copy(
            isPlaying = false,
            isBuffering = true,
            currentTrack = null,
            playingSurah = surahNumber,
            playingAyah = ayahNumber,
            titleAr = titleAr,
            titleEn = titleEn,
            subtitleAr = reciterNameAr,
            subtitleEn = reciterNameEn,
            currentPositionMs = 0L,
            durationMs = 0L,
            isOfflineFile = false,
            hasNext = false,
            hasPrevious = false,
            errorMessage = null
        )

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(audioUrl)
                setOnPreparedListener { player ->
                    applySpeed(player, _state.value.playbackSpeed)
                    player.start()
                    _state.value = _state.value.copy(
                        isPlaying = true,
                        isBuffering = false,
                        durationMs = player.duration.coerceAtLeast(0).toLong()
                    )
                    startProgressPolling()
                }
                setOnCompletionListener {
                    if (_state.value.repeatMode == AudioRepeatMode.REPEAT_ONE || repeatRemaining > 1) {
                        val nextRem = if (_state.value.repeatMode == AudioRepeatMode.REPEAT_ONE) repeatRemaining else repeatRemaining - 1
                        playAyah(
                            surahNumber = surahNumber,
                            ayahNumber = ayahNumber,
                            audioUrl = audioUrl,
                            titleAr = titleAr,
                            titleEn = titleEn,
                            reciterNameAr = reciterNameAr,
                            reciterNameEn = reciterNameEn,
                            repeatRemaining = nextRem,
                            onCompleted = onCompleted
                        )
                    } else {
                        progressJob?.cancel()
                        _state.value = _state.value.copy(isPlaying = false, isBuffering = false)
                        onCompleted?.invoke()
                    }
                }
                setOnErrorListener { _, _, _ ->
                    progressJob?.cancel()
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = "Audio playback unavailable offline or network error"
                    )
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                errorMessage = "Unable to stream recitation"
            )
        }
    }

    fun seekTo(positionMs: Long) {
        val mp = mediaPlayer ?: return
        try {
            val clamped = positionMs.coerceIn(0L, _state.value.durationMs.coerceAtLeast(0L))
            mp.seekTo(clamped.toInt())
            _state.value = _state.value.copy(currentPositionMs = clamped)
        } catch (_: Exception) {
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.75f, 1.5f)
        _state.value = _state.value.copy(playbackSpeed = clamped)
        mediaPlayer?.let { applySpeed(it, clamped) }
    }

    private fun applySpeed(player: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = player.playbackParams ?: PlaybackParams()
                params.speed = speed
                player.playbackParams = params
            } catch (_: Exception) {
            }
        }
    }

    fun cycleRepeatMode(): AudioRepeatMode {
        val next = when (_state.value.repeatMode) {
            AudioRepeatMode.OFF -> AudioRepeatMode.REPEAT_ONE
            AudioRepeatMode.REPEAT_ONE -> AudioRepeatMode.REPEAT_ALL
            AudioRepeatMode.REPEAT_ALL -> AudioRepeatMode.OFF
        }
        _state.value = _state.value.copy(
            repeatMode = next,
            hasNext = activeQueueIndex + 1 < activeQueue.size || next == AudioRepeatMode.REPEAT_ALL
        )
        return next
    }

    fun setSleepTimerMinutes(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _state.value = _state.value.copy(sleepTimerRemainingSec = null)
            return
        }
        var remaining = minutes * 60
        _state.value = _state.value.copy(sleepTimerRemainingSec = remaining)
        sleepTimerJob = scope.launch {
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining -= 1
                _state.value = _state.value.copy(sleepTimerRemainingSec = remaining)
            }
            if (remaining <= 0) {
                pauseIfPlaying()
                _state.value = _state.value.copy(sleepTimerRemainingSec = null)
            }
        }
    }

    private fun pauseIfPlaying() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
                _state.value = _state.value.copy(isPlaying = false)
            }
        } catch (_: Exception) {
        }
    }

    fun togglePauseResume() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
                _state.value = _state.value.copy(isPlaying = false)
            } else {
                mp.start()
                _state.value = _state.value.copy(isPlaying = true)
                startProgressPolling()
            }
        } catch (_: Exception) {
            stop()
        }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null) {
                    try {
                        if (mp.isPlaying) {
                            _state.value = _state.value.copy(
                                currentPositionMs = mp.currentPosition.coerceAtLeast(0).toLong(),
                                durationMs = mp.duration.coerceAtLeast(0).toLong()
                            )
                        }
                    } catch (_: Exception) {
                    }
                }
                delay(500L)
            }
        }
    }

    private fun releasePlayerOnly() {
        progressJob?.cancel()
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
        sleepTimerJob?.cancel()
        releasePlayerOnly()
        activeQueue = emptyList()
        activeQueueIndex = -1
        _state.value = AudioPlaybackState(
            playbackSpeed = _state.value.playbackSpeed,
            repeatMode = _state.value.repeatMode
        )
    }
}
