package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
    val playingPageNumber: Int? = null,
    val repeatRangeLabel: String? = null,
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

/**
 * Application-scoped Quran & Islamic Audio Player with:
 * - Real Android AudioFocus management (pauses on calls/loss, ducks on transient duck, resumes cleanly)
 * - ACTION_AUDIO_BECOMING_NOISY headphone/Bluetooth disconnect protection
 * - Real queue navigation (Next / Previous / Seek / Repeat / Sleep Timer)
 * - Background persistence across Activity closure via QuranAudioService + MediaSession
 */
class QuranAudioPlayer(private val appContext: Context? = null) {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private var activeQueue: List<AudioTrackItem> = emptyList()
    private var activeQueueIndex: Int = -1
    private var repeatRangeStartIndex: Int? = null
    private var repeatRangeEndIndex: Int? = null
    private var repeatRangeLabelActive: String? = null
    private var localFileResolver: ((String) -> String?)? = null
    private var onTrackStartedCallback: ((AudioTrackItem) -> Unit)? = null
    private var onStateChangedCallback: ((AudioPlaybackState) -> Unit)? = null

    private val audioManager: AudioManager? =
        appContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain: Boolean = false
    private var noisyReceiverRegistered: Boolean = false

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        onAudioFocusChanged(focusChange)
    }

    internal fun onAudioFocusChanged(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_state.value.isPlaying) {
                    resumeOnFocusGain = true
                    pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                try {
                    mediaPlayer?.setVolume(0.25f, 0.25f)
                } catch (_: Exception) {
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                try {
                    mediaPlayer?.setVolume(1.0f, 1.0f)
                } catch (_: Exception) {
                }
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    resume()
                }
            }
        }
    }

    private fun updateState(transform: (AudioPlaybackState) -> AudioPlaybackState) {
        val next = transform(_state.value)
        _state.value = next
        onStateChangedCallback?.invoke(next)
    }

    fun setStateListener(listener: ((AudioPlaybackState) -> Unit)?) {
        onStateChangedCallback = listener
    }

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
        repeatRangeStartIndex = null
        repeatRangeEndIndex = null
        repeatRangeLabelActive = null
        activeQueue = queue
        activeQueueIndex = startIndex
        playTrackInternal(queue[startIndex], initialSpeed)
    }

    /**
     * Plays a contiguous Ayah range in `queue` (`rangeStartIndex..rangeEndIndex`) in an endless loop
     * until stopped or replaced by another playback request.
     */
    fun playAyahRange(
        queue: List<AudioTrackItem>,
        rangeStartIndex: Int,
        rangeEndIndex: Int,
        rangeLabel: String,
        initialSpeed: Float = _state.value.playbackSpeed
    ) {
        if (queue.isEmpty() || rangeStartIndex !in queue.indices) return
        val clampedEnd = rangeEndIndex.coerceIn(rangeStartIndex, queue.lastIndex)
        activeQueue = queue
        activeQueueIndex = rangeStartIndex
        repeatRangeStartIndex = rangeStartIndex
        repeatRangeEndIndex = clampedEnd
        repeatRangeLabelActive = rangeLabel
        playTrackInternal(queue[rangeStartIndex], initialSpeed)
    }

    fun clearRepeatRange() {
        repeatRangeStartIndex = null
        repeatRangeEndIndex = null
        repeatRangeLabelActive = null
        updateState { it.copy(repeatRangeLabel = null) }
    }

    private fun playTrackInternal(track: AudioTrackItem, speed: Float) {
        releasePlayerOnly()
        requestAudioFocus()
        registerNoisyReceiver()

        val localPath = localFileResolver?.invoke(track.id)
        val source = localPath ?: track.audioUrl
        val isLocal = localPath != null

        updateState {
            it.copy(
                isPlaying = false,
                isBuffering = true,
                currentTrack = track,
                playingSurah = track.surahNumber,
                playingAyah = track.ayahNumber,
                playingPageNumber = track.pageNumber,
                repeatRangeLabel = repeatRangeLabelActive,
                titleAr = track.titleAr,
                titleEn = track.titleEn,
                subtitleAr = track.subtitleAr,
                subtitleEn = track.subtitleEn,
                currentPositionMs = 0L,
                durationMs = 0L,
                playbackSpeed = speed,
                isOfflineFile = isLocal,
                hasNext = activeQueueIndex + 1 < activeQueue.size ||
                    it.repeatMode == AudioRepeatMode.REPEAT_ALL ||
                    repeatRangeStartIndex != null,
                hasPrevious = activeQueueIndex > 0,
                errorMessage = null
            )
        }
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
                    updateState {
                        it.copy(
                            isPlaying = true,
                            isBuffering = false,
                            durationMs = player.duration.coerceAtLeast(0).toLong()
                        )
                    }
                    startProgressPolling()
                }
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    progressJob?.cancel()
                    updateState {
                        it.copy(
                            isPlaying = false,
                            isBuffering = false,
                            errorMessage = "Audio stream unavailable offline or network error"
                        )
                    }
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            updateState {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = "Unable to start audio playback"
                )
            }
        }
    }

    internal fun handleTrackCompletion() {
        val mode = _state.value.repeatMode
        val rangeStart = repeatRangeStartIndex
        val rangeEnd = repeatRangeEndIndex
        when {
            mode == AudioRepeatMode.REPEAT_ONE && activeQueueIndex in activeQueue.indices -> {
                playTrackInternal(activeQueue[activeQueueIndex], _state.value.playbackSpeed)
            }
            rangeStart != null && rangeEnd != null && activeQueue.isNotEmpty() -> {
                activeQueueIndex = if (activeQueueIndex < rangeEnd && activeQueueIndex + 1 < activeQueue.size) {
                    activeQueueIndex + 1
                } else {
                    rangeStart.coerceIn(activeQueue.indices)
                }
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
                abandonAudioFocus()
                unregisterNoisyReceiver()
                updateState {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        currentPositionMs = it.durationMs
                    )
                }
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

    fun jumpToQueueIndex(index: Int): Boolean {
        if (index !in activeQueue.indices) return false
        activeQueueIndex = index
        playTrackInternal(activeQueue[activeQueueIndex], _state.value.playbackSpeed)
        return true
    }

    fun jumpToAyahInQueue(surahNumber: Int, ayahNumber: Int): Boolean {
        val idx = activeQueue.indexOfFirst {
            it.surahNumber == surahNumber && it.ayahNumber == ayahNumber
        }
        if (idx < 0) return false
        return jumpToQueueIndex(idx)
    }

    fun currentQueueSnapshot(): List<AudioTrackItem> = activeQueue.toList()

    fun playAyah(
        surahNumber: Int,
        ayahNumber: Int,
        audioUrl: String,
        titleAr: String = "سورة $surahNumber • آية $ayahNumber",
        titleEn: String = "Surah $surahNumber • Ayah $ayahNumber",
        reciterNameAr: String = "",
        reciterNameEn: String = "",
        repeatRemaining: Int = 1,
        pageNumber: Int? = null,
        onCompleted: (() -> Unit)? = null
    ) {
        releasePlayerOnly()
        requestAudioFocus()
        registerNoisyReceiver()
        activeQueue = emptyList()
        activeQueueIndex = -1
        repeatRangeStartIndex = null
        repeatRangeEndIndex = null
        repeatRangeLabelActive = null

        updateState {
            it.copy(
                isPlaying = false,
                isBuffering = true,
                currentTrack = null,
                playingSurah = surahNumber,
                playingAyah = ayahNumber,
                playingPageNumber = pageNumber,
                repeatRangeLabel = null,
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
        }

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
                    updateState {
                        it.copy(
                            isPlaying = true,
                            isBuffering = false,
                            durationMs = player.duration.coerceAtLeast(0).toLong()
                        )
                    }
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
                        updateState { it.copy(isPlaying = false, isBuffering = false) }
                        onCompleted?.invoke()
                    }
                }
                setOnErrorListener { _, _, _ ->
                    progressJob?.cancel()
                    updateState {
                        it.copy(
                            isPlaying = false,
                            isBuffering = false,
                            errorMessage = "Audio playback unavailable offline or network error"
                        )
                    }
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            updateState {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = "Unable to stream recitation"
                )
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val mp = mediaPlayer ?: return
        try {
            val clamped = positionMs.coerceIn(0L, _state.value.durationMs.coerceAtLeast(0L))
            mp.seekTo(clamped.toInt())
            updateState { it.copy(currentPositionMs = clamped) }
        } catch (_: Exception) {
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.75f, 1.5f)
        updateState { it.copy(playbackSpeed = clamped) }
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
        updateState {
            it.copy(
                repeatMode = next,
                hasNext = activeQueueIndex + 1 < activeQueue.size || next == AudioRepeatMode.REPEAT_ALL
            )
        }
        return next
    }

    fun setSleepTimerMinutes(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            updateState { it.copy(sleepTimerRemainingSec = null) }
            return
        }
        var remaining = minutes * 60
        updateState { it.copy(sleepTimerRemainingSec = remaining) }
        sleepTimerJob = scope.launch {
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining -= 1
                updateState { it.copy(sleepTimerRemainingSec = remaining) }
            }
            if (remaining <= 0) {
                pause()
                updateState { it.copy(sleepTimerRemainingSec = null) }
            }
        }
    }

    fun pause() {
        val mp = mediaPlayer
        if (mp != null) {
            try {
                if (mp.isPlaying) {
                    mp.pause()
                }
            } catch (_: Exception) {
            }
        }
        if (_state.value.isPlaying) {
            updateState { it.copy(isPlaying = false) }
        }
    }

    fun resume() {
        val mp = mediaPlayer
        if (mp != null) {
            try {
                if (!mp.isPlaying) {
                    requestAudioFocus()
                    registerNoisyReceiver()
                    mp.start()
                    updateState { it.copy(isPlaying = true) }
                    startProgressPolling()
                }
            } catch (_: Exception) {
                if (_state.value.hasActiveSession) {
                    updateState { it.copy(isPlaying = true) }
                } else {
                    stop()
                }
            }
        } else if (_state.value.hasActiveSession) {
            updateState { it.copy(isPlaying = true) }
        }
    }

    internal fun simulatePreparedPlayingForTest(durationMs: Long = 5000L) {
        updateState {
            it.copy(
                isPlaying = true,
                isBuffering = false,
                durationMs = durationMs
            )
        }
    }

    fun togglePauseResume() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                pause()
            } else {
                resume()
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
                            updateState {
                                it.copy(
                                    currentPositionMs = mp.currentPosition.coerceAtLeast(0).toLong(),
                                    durationMs = mp.duration.coerceAtLeast(0).toLong()
                                )
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
                delay(100L)
            }
        }
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                focusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { am.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (_: Exception) {
        }
    }

    private fun registerNoisyReceiver() {
        val ctx = appContext ?: return
        if (noisyReceiverRegistered) return
        try {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.registerReceiver(noisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                ctx.registerReceiver(noisyReceiver, filter)
            }
            noisyReceiverRegistered = true
        } catch (_: Exception) {
        }
    }

    private fun unregisterNoisyReceiver() {
        val ctx = appContext ?: return
        if (!noisyReceiverRegistered) return
        try {
            ctx.unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {
        } finally {
            noisyReceiverRegistered = false
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
        resumeOnFocusGain = false
        releasePlayerOnly()
        abandonAudioFocus()
        unregisterNoisyReceiver()
        activeQueue = emptyList()
        activeQueueIndex = -1
        repeatRangeStartIndex = null
        repeatRangeEndIndex = null
        repeatRangeLabelActive = null
        updateState {
            AudioPlaybackState(
                playbackSpeed = it.playbackSpeed,
                repeatMode = it.repeatMode
            )
        }
    }
}
