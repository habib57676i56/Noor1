package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
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
import java.io.File

enum class RecitationRecordingStatus {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED,
    ERROR
}

data class RecitationRecordingState(
    val status: RecitationRecordingStatus = RecitationRecordingStatus.IDLE,
    val elapsedMs: Long = 0L,
    val amplitudeNormalized: Float = 0f,
    val lastSavedFilePath: String? = null,
    val lastSavedDurationMs: Long = 0L,
    val lastSavedSizeBytes: Long = 0L,
    val activeAudioSourceLabel: String = "",
    val errorAr: String? = null,
    val errorEn: String? = null
) {
    val isRecordingOrPaused: Boolean
        get() = status == RecitationRecordingStatus.RECORDING || status == RecitationRecordingStatus.PAUSED
}

data class RecordingPlaybackState(
    val isPlaying: Boolean = false,
    val playingFilePath: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorAr: String? = null,
    val errorEn: String? = null
)

/**
 * Real Android MediaRecorder + MediaPlayer engine for Quran Hifz recitation recording and review.
 *
 * Key guarantees:
 * - Verifies BOTH microphone permission (`RECORD_AUDIO`) and microphone hardware availability.
 * - Acquires Android AudioFocus (`AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE`) during recording and handles
 *   interruptions (calls/focus loss) cleanly without leaving dangling locks or corrupt files.
 * - Uses `MediaRecorder.AudioSource.VOICE_RECOGNITION` with automatic OEM fallback to
 *   `MediaRecorder.AudioSource.MIC` so recording works reliably across all Android devices.
 * - Records high-clarity speech in M4A (MPEG-4 / AAC-LC, 44.1kHz, 96kbps) into app-private storage.
 * - Verifies file existence & integrity (>1KB) before reporting success — never fakes recording!
 */
class QuranRecitationRecorder(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioManager: AudioManager? =
        appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var recordFocusRequest: AudioFocusRequest? = null
    private var playbackFocusRequest: AudioFocusRequest? = null

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var activeFile: File? = null

    private var recordTimerJob: Job? = null
    private var playbackProgressJob: Job? = null
    private var accumulatedElapsedMs: Long = 0L
    private var segmentStartTimeMs: Long = 0L

    private val _recordingState = MutableStateFlow(RecitationRecordingState())
    val recordingState: StateFlow<RecitationRecordingState> = _recordingState.asStateFlow()

    private val _playbackState = MutableStateFlow(RecordingPlaybackState())
    val playbackState: StateFlow<RecordingPlaybackState> = _playbackState.asStateFlow()

    private val recordFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        onRecordAudioFocusChanged(focusChange)
    }

    internal fun onRecordAudioFocusChanged(focusChange: Int) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        ) {
            if (_recordingState.value.status == RecitationRecordingStatus.RECORDING) {
                if (!pauseRecording()) {
                    stopRecording()
                }
            }
        }
    }

    fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun hasMicrophoneHardware(): Boolean =
        runCatching {
            appContext.packageManager?.hasSystemFeature(PackageManager.FEATURE_MICROPHONE) ?: true
        }.getOrDefault(true)

    fun recordingsDirectory(): File {
        val dir = File(appContext.filesDir, "hifz_recordings")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun startRecording(filePrefix: String = "recitation"): Boolean {
        if (!hasMicrophonePermission()) {
            _recordingState.value = _recordingState.value.copy(
                status = RecitationRecordingStatus.ERROR,
                amplitudeNormalized = 0f,
                errorAr = "يرجى منح إذن الميكروفون أولاً لتسجيل تلاوتك.",
                errorEn = "Microphone permission is required before recording."
            )
            return false
        }
        if (!hasMicrophoneHardware()) {
            _recordingState.value = _recordingState.value.copy(
                status = RecitationRecordingStatus.ERROR,
                amplitudeNormalized = 0f,
                errorAr = "لا يتوفر ميكروفون نشط على هذا الجهاز.",
                errorEn = "No microphone hardware detected on this device."
            )
            return false
        }

        // Stop any active playback or stale recorder before starting fresh
        stopPlayback()
        releaseRecorderOnly(deleteIncompleteFile = false)
        requestRecordingAudioFocus()

        val safePrefix = filePrefix.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32).ifBlank { "recitation" }
        val outputFile = File(recordingsDirectory(), "${safePrefix}_${System.currentTimeMillis()}.m4a")

        // Try VOICE_RECOGNITION first (best speech clarity), then fall back to MIC on OEMs where
        // VOICE_RECOGNITION is unsupported or locked by another service.
        val audioSources = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION to "VOICE_RECOGNITION",
            MediaRecorder.AudioSource.MIC to "MIC"
        )

        var lastError: Exception? = null
        for ((audioSource, label) in audioSources) {
            var candidate: MediaRecorder? = null
            try {
                if (outputFile.exists()) outputFile.delete()
                candidate = createMediaRecorder().apply {
                    setAudioSource(audioSource)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(96_000)
                    setAudioSamplingRate(44_100)
                    setAudioChannels(1)
                    setOutputFile(outputFile.absolutePath)
                    prepare()
                    start()
                }
                mediaRecorder = candidate
                activeFile = outputFile
                accumulatedElapsedMs = 0L
                segmentStartTimeMs = System.currentTimeMillis()

                _recordingState.value = RecitationRecordingState(
                    status = RecitationRecordingStatus.RECORDING,
                    elapsedMs = 0L,
                    amplitudeNormalized = 0f,
                    lastSavedFilePath = _recordingState.value.lastSavedFilePath,
                    lastSavedDurationMs = _recordingState.value.lastSavedDurationMs,
                    lastSavedSizeBytes = _recordingState.value.lastSavedSizeBytes,
                    activeAudioSourceLabel = label
                )
                startRecordingPolling()
                return true
            } catch (e: Exception) {
                lastError = e
                runCatching { candidate?.reset() }
                runCatching { candidate?.release() }
                runCatching { outputFile.delete() }
            }
        }

        abandonRecordingAudioFocus()
        releaseRecorderOnly(deleteIncompleteFile = true)
        _recordingState.value = _recordingState.value.copy(
            status = RecitationRecordingStatus.ERROR,
            amplitudeNormalized = 0f,
            errorAr = "تعذّر بدء تسجيل الصوت (${lastError?.localizedMessage ?: "الميكروفون مشغول أو غير متاح"}).",
            errorEn = "Unable to start audio recording (${lastError?.localizedMessage ?: "microphone busy or unavailable"})."
        )
        return false
    }

    fun pauseRecording(): Boolean {
        val recorder = mediaRecorder ?: return false
        if (_recordingState.value.status != RecitationRecordingStatus.RECORDING) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            _recordingState.value = _recordingState.value.copy(
                errorAr = "الإيقاف المؤقت للتسجيل يتطلب أندرويد 7 أو أحدث.",
                errorEn = "Pausing recording requires Android 7.0 or newer."
            )
            return false
        }
        return try {
            recorder.pause()
            accumulatedElapsedMs += (System.currentTimeMillis() - segmentStartTimeMs).coerceAtLeast(0L)
            recordTimerJob?.cancel()
            _recordingState.value = _recordingState.value.copy(
                status = RecitationRecordingStatus.PAUSED,
                elapsedMs = accumulatedElapsedMs,
                amplitudeNormalized = 0f,
                errorAr = null,
                errorEn = null
            )
            true
        } catch (_: Exception) {
            _recordingState.value = _recordingState.value.copy(
                errorAr = "تعذّر إيقاف التسجيل مؤقتاً.",
                errorEn = "Could not pause recording."
            )
            false
        }
    }

    fun resumeRecording(): Boolean {
        val recorder = mediaRecorder ?: return false
        if (_recordingState.value.status != RecitationRecordingStatus.PAUSED) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        return try {
            requestRecordingAudioFocus()
            recorder.resume()
            segmentStartTimeMs = System.currentTimeMillis()
            _recordingState.value = _recordingState.value.copy(
                status = RecitationRecordingStatus.RECORDING,
                errorAr = null,
                errorEn = null
            )
            startRecordingPolling()
            true
        } catch (_: Exception) {
            _recordingState.value = _recordingState.value.copy(
                status = RecitationRecordingStatus.ERROR,
                amplitudeNormalized = 0f,
                errorAr = "تعذّر استئناف التسجيل.",
                errorEn = "Could not resume recording."
            )
            false
        }
    }

    /**
     * Stops the current recording, verifies the output file is non-empty (> 1KB), and returns
     * its path on success (or null if recording was too short or failed).
     */
    fun stopRecording(): String? {
        recordTimerJob?.cancel()
        val recorder = mediaRecorder
        val file = activeFile
        val totalDuration = if (_recordingState.value.status == RecitationRecordingStatus.RECORDING) {
            accumulatedElapsedMs + (System.currentTimeMillis() - segmentStartTimeMs).coerceAtLeast(0L)
        } else {
            accumulatedElapsedMs
        }

        abandonRecordingAudioFocus()

        if (recorder == null || file == null) {
            _recordingState.value = _recordingState.value.copy(
                status = RecitationRecordingStatus.IDLE,
                amplitudeNormalized = 0f
            )
            return null
        }

        var stoppedCleanly = false
        try {
            recorder.stop()
            stoppedCleanly = true
        } catch (_: Exception) {
            stoppedCleanly = false
        } finally {
            runCatching { recorder.reset() }
            runCatching { recorder.release() }
            mediaRecorder = null
            activeFile = null
        }

        if (!stoppedCleanly || !file.exists() || file.length() < 1024L) {
            runCatching { file.delete() }
            _recordingState.value = _recordingState.value.copy(
                status = RecitationRecordingStatus.ERROR,
                elapsedMs = 0L,
                amplitudeNormalized = 0f,
                errorAr = "التسجيل قصير جداً أو لم يلتقط صوتاً صالحاً. يرجى التلاوة لثانية واحدة على الأقل.",
                errorEn = "Recording was too short or empty. Please recite for at least 1 second."
            )
            return null
        }

        _recordingState.value = RecitationRecordingState(
            status = RecitationRecordingStatus.STOPPED,
            elapsedMs = totalDuration.coerceAtLeast(500L),
            amplitudeNormalized = 0f,
            lastSavedFilePath = file.absolutePath,
            lastSavedDurationMs = totalDuration.coerceAtLeast(500L),
            lastSavedSizeBytes = file.length(),
            activeAudioSourceLabel = _recordingState.value.activeAudioSourceLabel,
            errorAr = null,
            errorEn = null
        )
        return file.absolutePath
    }

    fun playRecording(filePath: String): Boolean {
        val file = File(filePath)
        if (!file.exists() || file.length() < 512L) {
            _playbackState.value = RecordingPlaybackState(
                errorAr = "ملف التسجيل غير موجود أو تالف.",
                errorEn = "Recording file not found or corrupted."
            )
            return false
        }

        if (_recordingState.value.isRecordingOrPaused) {
            stopRecording()
        }
        stopPlayback()
        requestPlaybackAudioFocus()

        return try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnPreparedListener { player ->
                    player.start()
                    _playbackState.value = RecordingPlaybackState(
                        isPlaying = true,
                        playingFilePath = file.absolutePath,
                        currentPositionMs = 0L,
                        durationMs = player.duration.coerceAtLeast(0).toLong()
                    )
                    startPlaybackPolling()
                }
                setOnCompletionListener {
                    playbackProgressJob?.cancel()
                    abandonPlaybackAudioFocus()
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = _playbackState.value.durationMs
                    )
                }
                setOnErrorListener { _, _, _ ->
                    playbackProgressJob?.cancel()
                    abandonPlaybackAudioFocus()
                    _playbackState.value = RecordingPlaybackState(
                        errorAr = "تعذّر تشغيل التسجيل الصوتي.",
                        errorEn = "Unable to play recording file."
                    )
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
            true
        } catch (_: Exception) {
            abandonPlaybackAudioFocus()
            _playbackState.value = RecordingPlaybackState(
                errorAr = "تعذّر فتح ملف التسجيل للتشغيل.",
                errorEn = "Unable to open recording for playback."
            )
            false
        }
    }

    fun pausePlayback() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
            }
        } catch (_: Exception) {
        }
    }

    fun resumePlayback() {
        val mp = mediaPlayer ?: return
        try {
            if (!mp.isPlaying) {
                requestPlaybackAudioFocus()
                mp.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                startPlaybackPolling()
            }
        } catch (_: Exception) {
            stopPlayback()
        }
    }

    fun stopPlayback() {
        playbackProgressJob?.cancel()
        abandonPlaybackAudioFocus()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
        _playbackState.value = RecordingPlaybackState()
    }

    fun deleteRecordingFile(filePath: String): Boolean {
        if (_playbackState.value.playingFilePath == filePath) {
            stopPlayback()
        }
        val deleted = runCatching { File(filePath).delete() }.getOrDefault(false)
        if (_recordingState.value.lastSavedFilePath == filePath) {
            _recordingState.value = _recordingState.value.copy(
                lastSavedFilePath = null,
                lastSavedDurationMs = 0L,
                lastSavedSizeBytes = 0L
            )
        }
        return deleted
    }

    fun clearError() {
        _recordingState.value = _recordingState.value.copy(errorAr = null, errorEn = null)
        _playbackState.value = _playbackState.value.copy(errorAr = null, errorEn = null)
    }

    fun release() {
        recordTimerJob?.cancel()
        playbackProgressJob?.cancel()
        abandonRecordingAudioFocus()
        abandonPlaybackAudioFocus()
        releaseRecorderOnly(deleteIncompleteFile = true)
        stopPlayback()
    }

    private fun startRecordingPolling() {
        recordTimerJob?.cancel()
        recordTimerJob = scope.launch {
            while (isActive && _recordingState.value.status == RecitationRecordingStatus.RECORDING && mediaRecorder != null) {
                val elapsed = accumulatedElapsedMs + (System.currentTimeMillis() - segmentStartTimeMs).coerceAtLeast(0L)
                val amp = try {
                    val raw = mediaRecorder?.maxAmplitude ?: 0
                    if (raw <= 0) 0f else (raw.toFloat() / 22_000f).coerceIn(0.02f, 1.0f)
                } catch (_: Exception) {
                    0f
                }
                _recordingState.value = _recordingState.value.copy(
                    elapsedMs = elapsed,
                    amplitudeNormalized = amp
                )
                delay(120L)
            }
        }
    }

    private fun startPlaybackPolling() {
        playbackProgressJob?.cancel()
        playbackProgressJob = scope.launch {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null) {
                    try {
                        if (mp.isPlaying) {
                            _playbackState.value = _playbackState.value.copy(
                                currentPositionMs = mp.currentPosition.coerceAtLeast(0).toLong(),
                                durationMs = mp.duration.coerceAtLeast(0).toLong()
                            )
                        }
                    } catch (_: Exception) {
                    }
                }
                delay(200L)
            }
        }
    }

    private fun requestRecordingAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .build()
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener(recordFocusListener)
                    .build()
                recordFocusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    recordFocusListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun abandonRecordingAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                recordFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                recordFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(recordFocusListener)
            }
        } catch (_: Exception) {
        }
    }

    private fun requestPlaybackAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attrs)
                    .build()
                playbackFocusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }
        } catch (_: Exception) {
        }
    }

    private fun abandonPlaybackAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                playbackFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                playbackFocusRequest = null
            }
        } catch (_: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            MediaRecorder()
        }
    }

    private fun releaseRecorderOnly(deleteIncompleteFile: Boolean) {
        recordTimerJob?.cancel()
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
        if (deleteIncompleteFile) {
            runCatching { activeFile?.delete() }
        }
        activeFile = null
    }
}
