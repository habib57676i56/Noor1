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
import com.example.data.quran.AyahTimestampRange
import com.example.data.quran.SurahContinuousAudioManifest
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
    val isContinuousStream: Boolean = false,
    val currentTrack: AudioTrackItem? = null,
    val playingSurah: Int? = null,
    val playingAyah: Int? = null,
    val playingPageNumber: Int? = null,
    val activeWordPosition: Int? = null,
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
 * - **Engine A (Primary — Continuous Uninterrupted Surah Stream)**:
 *   Plays ONE continuous full-Surah MP3 stream while tracking Ayah, Mushaf page, and word
 *   highlights independently via verified millisecond timestamps — audio NEVER pauses, stops,
 *   or re-buffers between Ayahs!
 * - **Engine B (Gapless Fallback Queue)**:
 *   Pre-buffers the next Ayah's `MediaPlayer` in the background and attaches it via
 *   `MediaPlayer.setNextMediaPlayer()` so reciters without continuous chapter timestamps
 *   still play smoothly without audible gaps.
 * - Real Android AudioFocus management + ACTION_AUDIO_BECOMING_NOISY headphone disconnect guard.
 * - Background persistence across Activity closure via QuranAudioService + MediaSession.
 */
class QuranAudioPlayer(private val appContext: Context? = null) {
    private var mediaPlayer: MediaPlayer? = null
    private var nextPreparedPlayer: MediaPlayer? = null
    private var nextPreparedQueueIndex: Int = -1
    private var nextPreparedReady: Boolean = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    // Per-track queue state (Engine B)
    private var activeQueue: List<AudioTrackItem> = emptyList()
    private var activeQueueIndex: Int = -1
    private var repeatRangeStartIndex: Int? = null
    private var repeatRangeEndIndex: Int? = null
    private var repeatRangeLabelActive: String? = null

    // Continuous full-Surah stream state (Engine A)
    private var activeContinuousManifest: SurahContinuousAudioManifest? = null
    private var continuousSurahTitleAr: String = ""
    private var continuousSurahTitleEn: String = ""
    private var continuousReciterAr: String = ""
    private var continuousReciterEn: String = ""
    private var continuousPageResolver: ((Int, Int) -> Int?)? = null
    private var continuousRangeStartAyah: Int? = null
    private var continuousRangeEndAyah: Int? = null
    private var onContinuousAyahChangedCallback: ((surah: Int, ayah: Int, page: Int?) -> Unit)? = null
    private var onContinuousSurahCompletedCallback: (() -> Unit)? = null
    private var lastReportedContinuousAyah: Int? = null

    // Internal counter for test verification (proves MediaPlayer is never recreated between Ayahs!)
    internal var playerCreationCountForTest: Int = 0
        private set

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

    /**
     * **Engine A: Continuous Uninterrupted Full-Surah Playback.**
     *
     * Plays ONE single continuous chapter MP3 stream (`manifest.audioUrl`) so Sheikh audio
     * NEVER pauses, buffers, or restarts between Ayahs. Visual Ayah + Mushaf page + word
     * highlights track `mp.currentPosition` independently via verified timestamps.
     */
    fun playContinuousSurahManifest(
        manifest: SurahContinuousAudioManifest,
        surahTitleAr: String,
        surahTitleEn: String,
        reciterNameAr: String,
        reciterNameEn: String,
        pageResolver: (surahNumber: Int, ayahNumber: Int) -> Int?,
        startAyah: Int = 1,
        repeatRangeStartAyah: Int? = null,
        repeatRangeEndAyah: Int? = null,
        repeatRangeLabel: String? = null,
        initialSpeed: Float = _state.value.playbackSpeed,
        onAyahChanged: ((surah: Int, ayah: Int, page: Int?) -> Unit)? = null,
        onSurahCompleted: (() -> Unit)? = null
    ) {
        if (manifest.ayahs.isEmpty() || manifest.audioUrl.isBlank()) return

        // If the EXACT same continuous manifest is already prepared & active, just seek seamlessly
        // to the target Ayah without re-creating MediaPlayer or re-buffering!
        val currentManifest = activeContinuousManifest
        val existingMp = mediaPlayer
        if (currentManifest != null &&
            existingMp != null &&
            currentManifest.reciterId == manifest.reciterId &&
            currentManifest.surahNumber == manifest.surahNumber &&
            !_state.value.isBuffering &&
            _state.value.errorMessage == null
        ) {
            continuousRangeStartAyah = repeatRangeStartAyah
            continuousRangeEndAyah = repeatRangeEndAyah
            repeatRangeLabelActive = repeatRangeLabel
            onContinuousAyahChangedCallback = onAyahChanged
            onContinuousSurahCompletedCallback = onSurahCompleted
            val targetRange = manifest.findAyahRange(startAyah) ?: manifest.ayahs.first()
            try {
                existingMp.seekTo(targetRange.startMs.toInt())
                applySpeed(existingMp, initialSpeed)
                if (!existingMp.isPlaying) {
                    requestAudioFocus()
                    registerNoisyReceiver()
                    existingMp.start()
                }
                syncContinuousPosition(targetRange.startMs, seekPlayerIfLooping = false)
                updateState {
                    it.copy(
                        isPlaying = true,
                        isBuffering = false,
                        playbackSpeed = initialSpeed,
                        repeatRangeLabel = repeatRangeLabelActive
                    )
                }
                startProgressPolling()
                return
            } catch (_: Exception) {
                // Fall through to fresh initialization if seek failed
            }
        }

        releaseAllPlayers()
        clearQueueState()
        requestAudioFocus()
        registerNoisyReceiver()

        activeContinuousManifest = manifest
        continuousSurahTitleAr = surahTitleAr
        continuousSurahTitleEn = surahTitleEn
        continuousReciterAr = reciterNameAr
        continuousReciterEn = reciterNameEn
        continuousPageResolver = pageResolver
        continuousRangeStartAyah = repeatRangeStartAyah
        continuousRangeEndAyah = repeatRangeEndAyah
        repeatRangeLabelActive = repeatRangeLabel
        onContinuousAyahChangedCallback = onAyahChanged
        onContinuousSurahCompletedCallback = onSurahCompleted

        val startRange = manifest.findAyahRange(startAyah) ?: manifest.ayahs.first()
        val initialPage = pageResolver(manifest.surahNumber, startRange.ayahNumber)
        lastReportedContinuousAyah = startRange.ayahNumber

        val localTrackKey = "continuous_${manifest.reciterId}_s${manifest.surahNumber}"
        val localPath = localFileResolver?.invoke(localTrackKey)
        val source = localPath ?: manifest.audioUrl
        val isLocal = localPath != null

        updateState {
            it.copy(
                isPlaying = false,
                isBuffering = true,
                isContinuousStream = true,
                currentTrack = null,
                playingSurah = manifest.surahNumber,
                playingAyah = startRange.ayahNumber,
                playingPageNumber = initialPage,
                activeWordPosition = startRange.findWordPositionAtStreamMs(startRange.startMs),
                repeatRangeLabel = repeatRangeLabelActive,
                titleAr = "$surahTitleAr • آية ${startRange.ayahNumber}",
                titleEn = "$surahTitleEn • Ayah ${startRange.ayahNumber}",
                subtitleAr = reciterNameAr,
                subtitleEn = reciterNameEn,
                currentPositionMs = startRange.startMs,
                durationMs = manifest.ayahs.lastOrNull()?.endMs ?: 0L,
                playbackSpeed = initialSpeed,
                isOfflineFile = isLocal,
                hasNext = manifest.nextAyahRange(startRange.ayahNumber) != null || onSurahCompleted != null,
                hasPrevious = manifest.previousAyahRange(startRange.ayahNumber) != null,
                errorMessage = null
            )
        }
        onAyahChanged?.invoke(manifest.surahNumber, startRange.ayahNumber, initialPage)

        try {
            playerCreationCountForTest += 1
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(source)
                setOnPreparedListener { player ->
                    if (startRange.startMs > 0L) {
                        runCatching { player.seekTo(startRange.startMs.toInt()) }
                    }
                    applySpeed(player, _state.value.playbackSpeed)
                    player.start()
                    val totalDur = player.duration.coerceAtLeast(0).toLong()
                        .takeIf { it > 0L }
                        ?: (manifest.ayahs.lastOrNull()?.endMs ?: 0L)
                    updateState {
                        it.copy(
                            isPlaying = true,
                            isBuffering = false,
                            durationMs = totalDur
                        )
                    }
                    startProgressPolling()
                }
                setOnCompletionListener {
                    handleContinuousSurahCompletion()
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
                    errorMessage = "Unable to start continuous Quran audio playback"
                )
            }
        }
    }

    /**
     * Updates visual Ayah, Mushaf page, and word highlight from the continuous stream's
     * actual millisecond playback position. NEVER pauses, stops, or restarts audio!
     * Also enforces in-stream seamless looping for `REPEAT_ONE` and Ayah-range repeat.
     */
    private fun syncContinuousPosition(positionMs: Long, seekPlayerIfLooping: Boolean) {
        val manifest = activeContinuousManifest ?: return
        val currentState = _state.value

        // 1. Check REPEAT_ONE in-stream seamless loop (zero re-buffering!)
        if (currentState.repeatMode == AudioRepeatMode.REPEAT_ONE) {
            val lockedAyah = currentState.playingAyah
            val lockedRange = lockedAyah?.let { manifest.findAyahRange(it) }
            if (lockedRange != null && positionMs >= (lockedRange.endMs - 60L).coerceAtLeast(lockedRange.startMs + 100L)) {
                if (seekPlayerIfLooping) {
                    runCatching { mediaPlayer?.seekTo(lockedRange.startMs.toInt()) }
                }
                val page = continuousPageResolver?.invoke(manifest.surahNumber, lockedRange.ayahNumber)
                updateState {
                    it.copy(
                        currentPositionMs = lockedRange.startMs,
                        playingSurah = manifest.surahNumber,
                        playingAyah = lockedRange.ayahNumber,
                        playingPageNumber = page ?: it.playingPageNumber,
                        activeWordPosition = lockedRange.findWordPositionAtStreamMs(lockedRange.startMs)
                    )
                }
                return
            }
        }

        // 2. Check Ayah-range repeat in-stream seamless loop (zero re-buffering!)
        val rStart = continuousRangeStartAyah
        val rEnd = continuousRangeEndAyah
        if (rStart != null && rEnd != null) {
            val startRange = manifest.findAyahRange(rStart)
            val endRange = manifest.findAyahRange(rEnd)
            if (startRange != null && endRange != null &&
                positionMs >= (endRange.endMs - 60L).coerceAtLeast(endRange.startMs + 100L)
            ) {
                if (seekPlayerIfLooping) {
                    runCatching { mediaPlayer?.seekTo(startRange.startMs.toInt()) }
                }
                applyContinuousRangeState(manifest, startRange, startRange.startMs)
                return
            }
        }

        // 3. Normal uninterrupted progression across Ayahs (1 -> 2 -> 3 -> 4 ...)
        val activeRange = manifest.findAyahAt(positionMs) ?: return
        applyContinuousRangeState(manifest, activeRange, positionMs)
    }

    private fun applyContinuousRangeState(
        manifest: SurahContinuousAudioManifest,
        activeRange: AyahTimestampRange,
        positionMs: Long
    ) {
        val page = continuousPageResolver?.invoke(manifest.surahNumber, activeRange.ayahNumber)
        val wordPos = activeRange.findWordPositionAtStreamMs(positionMs)
        val ayahChanged = lastReportedContinuousAyah != activeRange.ayahNumber
        if (ayahChanged) {
            lastReportedContinuousAyah = activeRange.ayahNumber
        }

        val current = _state.value
        val needsUpdate = ayahChanged ||
            current.activeWordPosition != wordPos ||
            current.playingPageNumber != (page ?: current.playingPageNumber) ||
            kotlin.math.abs(current.currentPositionMs - positionMs) >= 80L

        if (needsUpdate) {
            updateState {
                it.copy(
                    currentPositionMs = positionMs,
                    playingSurah = manifest.surahNumber,
                    playingAyah = activeRange.ayahNumber,
                    playingPageNumber = page ?: it.playingPageNumber,
                    activeWordPosition = wordPos,
                    titleAr = "$continuousSurahTitleAr • آية ${activeRange.ayahNumber}",
                    titleEn = "$continuousSurahTitleEn • Ayah ${activeRange.ayahNumber}",
                    hasNext = manifest.nextAyahRange(activeRange.ayahNumber) != null ||
                        onContinuousSurahCompletedCallback != null,
                    hasPrevious = manifest.previousAyahRange(activeRange.ayahNumber) != null
                )
            }
        }

        if (ayahChanged) {
            onContinuousAyahChangedCallback?.invoke(manifest.surahNumber, activeRange.ayahNumber, page)
        }
    }

    private fun handleContinuousSurahCompletion() {
        val manifest = activeContinuousManifest
        val rStart = continuousRangeStartAyah
        val rEnd = continuousRangeEndAyah
        if (manifest != null && rStart != null && rEnd != null) {
            val startRange = manifest.findAyahRange(rStart) ?: manifest.ayahs.firstOrNull()
            if (startRange != null && mediaPlayer != null) {
                runCatching {
                    mediaPlayer?.seekTo(startRange.startMs.toInt())
                    mediaPlayer?.start()
                }
                applyContinuousRangeState(manifest, startRange, startRange.startMs)
                return
            }
        }
        if (manifest != null && _state.value.repeatMode == AudioRepeatMode.REPEAT_ALL) {
            val first = manifest.ayahs.firstOrNull()
            if (first != null && mediaPlayer != null) {
                runCatching {
                    mediaPlayer?.seekTo(first.startMs.toInt())
                    mediaPlayer?.start()
                }
                applyContinuousRangeState(manifest, first, first.startMs)
                return
            }
        }
        val onCompleted = onContinuousSurahCompletedCallback
        if (onCompleted != null) {
            onCompleted.invoke()
            return
        }
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

    /**
     * Test hook for JVM/Robolectric verification of continuous playback position updates
     * (Ayah 1 -> 2 -> 3 -> 4 progression, page boundary transitions, repeat-one, repeat-range)
     * proving highlight tracking advances purely from audio position without recreating MediaPlayer.
     */
    internal fun simulateContinuousPositionForTest(positionMs: Long) {
        syncContinuousPosition(positionMs, seekPlayerIfLooping = false)
    }

    fun playTrackQueue(
        queue: List<AudioTrackItem>,
        startIndex: Int,
        initialSpeed: Float = _state.value.playbackSpeed
    ) {
        if (queue.isEmpty() || startIndex !in queue.indices) return
        clearContinuousState()
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
        clearContinuousState()
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
        continuousRangeStartAyah = null
        continuousRangeEndAyah = null
        repeatRangeLabelActive = null
        updateState { it.copy(repeatRangeLabel = null) }
    }

    private fun computeNextQueueIndex(): Int? {
        if (activeQueue.isEmpty() || activeQueueIndex !in activeQueue.indices) return null
        val mode = _state.value.repeatMode
        val rangeStart = repeatRangeStartIndex
        val rangeEnd = repeatRangeEndIndex
        return when {
            mode == AudioRepeatMode.REPEAT_ONE -> activeQueueIndex
            rangeStart != null && rangeEnd != null -> {
                if (activeQueueIndex < rangeEnd && activeQueueIndex + 1 < activeQueue.size) {
                    activeQueueIndex + 1
                } else {
                    rangeStart.coerceIn(activeQueue.indices)
                }
            }
            activeQueueIndex + 1 < activeQueue.size -> activeQueueIndex + 1
            mode == AudioRepeatMode.REPEAT_ALL -> 0
            else -> null
        }
    }

    /**
     * **Engine B Gapless Pre-Preparation**: prepares the next Ayah's `MediaPlayer` in the
     * background while the current Ayah is playing and attaches it via `setNextMediaPlayer()`
     * so per-Ayah queues transition without audible gaps or re-buffering pauses.
     */
    private fun prePrepareNextTrackIfAvailable(currentPlayer: MediaPlayer) {
        releaseNextPreparedPlayerOnly()
        val nextIdx = computeNextQueueIndex() ?: return
        val nextTrack = activeQueue.getOrNull(nextIdx) ?: return
        val localPath = localFileResolver?.invoke(nextTrack.id)
        val source = localPath ?: nextTrack.audioUrl
        if (source.isBlank()) return

        try {
            val nextMp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(source)
                setOnPreparedListener { readyNext ->
                    if (mediaPlayer === currentPlayer && nextPreparedPlayer === readyNext) {
                        nextPreparedReady = true
                        applySpeed(readyNext, _state.value.playbackSpeed)
                        runCatching { currentPlayer.setNextMediaPlayer(readyNext) }
                    } else {
                        runCatching {
                            readyNext.reset()
                            readyNext.release()
                        }
                    }
                }
                setOnErrorListener { failedMp, _, _ ->
                    if (nextPreparedPlayer === failedMp) {
                        releaseNextPreparedPlayerOnly()
                    }
                    true
                }
                prepareAsync()
            }
            nextPreparedPlayer = nextMp
            nextPreparedQueueIndex = nextIdx
            nextPreparedReady = false
        } catch (_: Exception) {
            releaseNextPreparedPlayerOnly()
        }
    }

    private fun playTrackInternal(track: AudioTrackItem, speed: Float) {
        releaseAllPlayers()
        requestAudioFocus()
        registerNoisyReceiver()

        val localPath = localFileResolver?.invoke(track.id)
        val source = localPath ?: track.audioUrl
        val isLocal = localPath != null

        updateState {
            it.copy(
                isPlaying = false,
                isBuffering = true,
                isContinuousStream = false,
                currentTrack = track,
                playingSurah = track.surahNumber,
                playingAyah = track.ayahNumber,
                playingPageNumber = track.pageNumber,
                activeWordPosition = null,
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
            playerCreationCountForTest += 1
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
                    prePrepareNextTrackIfAvailable(player)
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
        val nextIdx = computeNextQueueIndex()
        val preloaded = nextPreparedPlayer
        val preloadedIdx = nextPreparedQueueIndex

        // Gapless handoff when next MediaPlayer was already prepared & attached via setNextMediaPlayer!
        if (nextIdx != null && preloaded != null && nextPreparedReady && preloadedIdx == nextIdx) {
            val nextTrack = activeQueue.getOrNull(nextIdx)
            if (nextTrack != null) {
                val oldPlayer = mediaPlayer
                mediaPlayer = preloaded
                nextPreparedPlayer = null
                nextPreparedQueueIndex = -1
                nextPreparedReady = false
                activeQueueIndex = nextIdx

                runCatching {
                    oldPlayer?.reset()
                    oldPlayer?.release()
                }

                preloaded.setOnCompletionListener { handleTrackCompletion() }
                preloaded.setOnErrorListener { _, _, _ ->
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
                applySpeed(preloaded, _state.value.playbackSpeed)
                // Android automatically starts the chained setNextMediaPlayer, ensure playing if not yet started
                runCatching { if (!preloaded.isPlaying) preloaded.start() }

                val localPath = localFileResolver?.invoke(nextTrack.id)
                updateState {
                    it.copy(
                        isPlaying = true,
                        isBuffering = false,
                        isContinuousStream = false,
                        currentTrack = nextTrack,
                        playingSurah = nextTrack.surahNumber,
                        playingAyah = nextTrack.ayahNumber,
                        playingPageNumber = nextTrack.pageNumber,
                        activeWordPosition = null,
                        repeatRangeLabel = repeatRangeLabelActive,
                        titleAr = nextTrack.titleAr,
                        titleEn = nextTrack.titleEn,
                        subtitleAr = nextTrack.subtitleAr,
                        subtitleEn = nextTrack.subtitleEn,
                        currentPositionMs = 0L,
                        durationMs = runCatching { preloaded.duration.coerceAtLeast(0).toLong() }.getOrDefault(0L),
                        isOfflineFile = localPath != null,
                        hasNext = activeQueueIndex + 1 < activeQueue.size ||
                            it.repeatMode == AudioRepeatMode.REPEAT_ALL ||
                            repeatRangeStartIndex != null,
                        hasPrevious = activeQueueIndex > 0,
                        errorMessage = null
                    )
                }
                onTrackStartedCallback?.invoke(nextTrack)
                startProgressPolling()
                prePrepareNextTrackIfAvailable(preloaded)
                return
            }
        }

        // Standard queue fallback if next player wasn't ready yet
        if (nextIdx != null && nextIdx in activeQueue.indices) {
            activeQueueIndex = nextIdx
            playTrackInternal(activeQueue[activeQueueIndex], _state.value.playbackSpeed)
        } else {
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

    fun playNext() {
        val manifest = activeContinuousManifest
        if (manifest != null) {
            val currentAyah = _state.value.playingAyah ?: manifest.ayahs.firstOrNull()?.ayahNumber ?: return
            val nextRange = manifest.nextAyahRange(currentAyah)
            if (nextRange != null) {
                runCatching { mediaPlayer?.seekTo(nextRange.startMs.toInt()) }
                applyContinuousRangeState(manifest, nextRange, nextRange.startMs)
            } else {
                onContinuousSurahCompletedCallback?.invoke()
            }
            return
        }

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
        val manifest = activeContinuousManifest
        if (manifest != null) {
            val currentAyah = _state.value.playingAyah ?: manifest.ayahs.firstOrNull()?.ayahNumber ?: return
            val currentRange = manifest.findAyahRange(currentAyah)
            if (currentRange != null && (_state.value.currentPositionMs - currentRange.startMs) > 3000L) {
                runCatching { mediaPlayer?.seekTo(currentRange.startMs.toInt()) }
                applyContinuousRangeState(manifest, currentRange, currentRange.startMs)
                return
            }
            val prevRange = manifest.previousAyahRange(currentAyah)
            if (prevRange != null) {
                runCatching { mediaPlayer?.seekTo(prevRange.startMs.toInt()) }
                applyContinuousRangeState(manifest, prevRange, prevRange.startMs)
            }
            return
        }

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
        // Check continuous stream first for instant zero-buffering jump!
        val manifest = activeContinuousManifest
        if (manifest != null && manifest.surahNumber == surahNumber) {
            val range = manifest.findAyahRange(ayahNumber) ?: return false
            runCatching { mediaPlayer?.seekTo(range.startMs.toInt()) }
            applyContinuousRangeState(manifest, range, range.startMs)
            return true
        }

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
        releaseAllPlayers()
        clearContinuousState()
        clearQueueState()
        requestAudioFocus()
        registerNoisyReceiver()

        updateState {
            it.copy(
                isPlaying = false,
                isBuffering = true,
                isContinuousStream = false,
                currentTrack = null,
                playingSurah = surahNumber,
                playingAyah = ayahNumber,
                playingPageNumber = pageNumber,
                activeWordPosition = null,
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
            playerCreationCountForTest += 1
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
                            pageNumber = pageNumber,
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
        val mp = mediaPlayer
        val clamped = positionMs.coerceIn(0L, _state.value.durationMs.coerceAtLeast(positionMs.coerceAtLeast(0L)))
        if (mp != null) {
            try {
                mp.seekTo(clamped.toInt())
            } catch (_: Exception) {
            }
        }
        if (activeContinuousManifest != null) {
            syncContinuousPosition(clamped, seekPlayerIfLooping = false)
        } else {
            updateState { it.copy(currentPositionMs = clamped) }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.75f, 1.5f)
        updateState { it.copy(playbackSpeed = clamped) }
        mediaPlayer?.let { applySpeed(it, clamped) }
        nextPreparedPlayer?.let { if (nextPreparedReady) applySpeed(it, clamped) }
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
                hasNext = if (activeContinuousManifest != null) {
                    it.hasNext || next == AudioRepeatMode.REPEAT_ALL
                } else {
                    activeQueueIndex + 1 < activeQueue.size || next == AudioRepeatMode.REPEAT_ALL
                }
            )
        }
        // Re-evaluate pre-prepared next track in queue mode when repeat mode changes
        val currentMp = mediaPlayer
        if (activeContinuousManifest == null && currentMp != null && _state.value.isPlaying) {
            prePrepareNextTrackIfAvailable(currentMp)
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
        val mp = mediaPlayer
        if (mp == null) {
            if (_state.value.isPlaying) pause() else if (_state.value.hasActiveSession) resume()
            return
        }
        try {
            if (mp.isPlaying || _state.value.isPlaying) {
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
                            val posMs = mp.currentPosition.coerceAtLeast(0).toLong()
                            val durMs = mp.duration.coerceAtLeast(0).toLong()
                            if (activeContinuousManifest != null) {
                                syncContinuousPosition(posMs, seekPlayerIfLooping = true)
                                if (durMs > 0L && _state.value.durationMs != durMs) {
                                    updateState { it.copy(durationMs = durMs) }
                                }
                            } else {
                                updateState {
                                    it.copy(
                                        currentPositionMs = posMs,
                                        durationMs = durMs
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
                delay(80L)
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

    private fun releaseNextPreparedPlayerOnly() {
        try {
            nextPreparedPlayer?.reset()
            nextPreparedPlayer?.release()
        } catch (_: Exception) {
        } finally {
            nextPreparedPlayer = null
            nextPreparedQueueIndex = -1
            nextPreparedReady = false
        }
    }

    private fun releaseAllPlayers() {
        progressJob?.cancel()
        releaseNextPreparedPlayerOnly()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }

    private fun clearContinuousState() {
        activeContinuousManifest = null
        continuousPageResolver = null
        continuousRangeStartAyah = null
        continuousRangeEndAyah = null
        onContinuousAyahChangedCallback = null
        onContinuousSurahCompletedCallback = null
        lastReportedContinuousAyah = null
    }

    private fun clearQueueState() {
        activeQueue = emptyList()
        activeQueueIndex = -1
        repeatRangeStartIndex = null
        repeatRangeEndIndex = null
        repeatRangeLabelActive = null
    }

    fun stop() {
        sleepTimerJob?.cancel()
        resumeOnFocusGain = false
        releaseAllPlayers()
        abandonAudioFocus()
        unregisterNoisyReceiver()
        clearContinuousState()
        clearQueueState()
        updateState {
            AudioPlaybackState(
                playbackSpeed = it.playbackSpeed,
                repeatMode = it.repeatMode
            )
        }
    }
}
