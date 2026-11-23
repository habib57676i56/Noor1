package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
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
 * - Records high-clarity speech in M4A (MPEG-4 / AAC-LC, 44.1kHz, 96kbps) into app-private storage.
 * - Supports Start, Pause (API 24+), Resume, Stop, live elapsed timer, live microphone amplitude,
 *   file integrity verification (>1KB), and immediate playback review.
 * - Never simulates audio or produces empty placeholder files; always reports real errors clearly.
 */
class QuranRecitationRecorder(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
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

    fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun recordingsDirectory(): File {
        val dir = File(appContext.filesDir, "hifz_recordings")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun startRecording(filePrefix: String = "recitation"): Boolean {
        if (!hasMicrophonePermission()) {
            _recordingState.value = _recordingState.value.copy(
                status = RecitationRecordingStatus.ERROR,
                errorAr = "يرجى منح إذن الميكروفون أولاً لتسجيل تلاوتك.",
                errorEn = "Microphone permission is required before recording."
            )
            return false
        }

        // Stop any active playback or stale recorder before starting fresh
        stopPlayback()
        releaseRecorderOnly(deleteIncompleteFile = false)

        val safePrefix = filePrefix.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32).ifBlank { "recitation" }
        val outputFile = File(recordingsDirectory(), "${safePrefix}_${System.currentTimeMillis()}.m4a")

        return try {
            val recorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setAudioChannels(1)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            activeFile = outputFile
            accumulatedElapsedMs = 0L
            segmentStartTimeMs = System.currentTimeMillis()

            _recordingState.value = RecitationRecordingState(
                status = RecitationRecordingStatus.RECORDING,
                elapsedMs = 0L,
                amplitudeNormalized = 0.05f,
                lastSavedFilePath = _recordingState.value.lastSavedFilePath,
                lastSavedDurationMs = _recordingState.value.lastSavedDurationMs,
                lastSavedSizeBytes = _recordingState.value.lastSavedSizeBytes
            )
            startRecordingPolling()
            true
        } catch (e: Exception) {
            runCatching { outputFile.delete() }
            releaseRecorderOnly(deleteIncompleteFile = true)
            _recordingState.value = _recordingState.value.copy(
                status = RecitationRecordingStatus.ERROR,
                amplitudeNormalized = 0f,
                errorAr = "تعذّر بدء تسجيل الصوت (${e.localizedMessage ?: "خطأ بالميكروفون"}).",
                errorEn = "Unable to start audio recording (${e.localizedMessage ?: "microphone error"})."
            )
            false
        }
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
        } catch (e: Exception) {
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
            recorder.resume()
            segmentStartTimeMs = System.currentTimeMillis()
            _recordingState.value = _recordingState.value.copy(
                status = RecitationRecordingStatus.RECORDING,
                errorAr = null,
                errorEn = null
            )
            startRecordingPolling()
            true
        } catch (e: Exception) {
            _recordingState.value = _recordingState.value.copy(
                status = RecitationRecordingStatus.ERROR,
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
            // Very short recordings (<300ms) can throw RuntimeException on MediaRecorder.stop()
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
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = _playbackState.value.durationMs
                    )
                }
                setOnErrorListener { _, _, _ ->
                    playbackProgressJob?.cancel()
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
        } catch (e: Exception) {
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
        releaseRecorderOnly(deleteIncompleteFile = true)
        stopPlayback()
    }

    private fun startRecordingPolling() {
        recordTimerJob?.cancel()
        recordTimerJob = scope.launch {
            while (isActive && _recordingState.value.status == RecitationRecordingStatus.RECORDING) {
                val elapsed = accumulatedElapsedMs + (System.currentTimeMillis() - segmentStartTimeMs).coerceAtLeast(0L)
                val amp = try {
                    val raw = mediaRecorder?.maxAmplitude ?: 0
                    (raw.toFloat() / 22_000f).coerceIn(0.05f, 1.0f)
                } catch (_: Exception) {
                    0.05f
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
                delay(250L)
            }
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
