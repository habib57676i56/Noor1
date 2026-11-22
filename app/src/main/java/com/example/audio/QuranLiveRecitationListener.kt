package com.example.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.domain.model.QuranVerse
import com.example.domain.quran.QuranVerseTrackerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LiveRecitationStatus {
    IDLE,
    LISTENING,
    PAUSED,
    MATCHED,
    UNCERTAIN,
    ERROR
}

data class LiveRecitationState(
    val status: LiveRecitationStatus = LiveRecitationStatus.IDLE,
    val rmsLevel: Float = 0f,
    val heardText: String = "",
    val trackedSurah: Int? = null,
    val trackedAyah: Int? = null,
    val trackedPage: Int? = null,
    val confidence: Float = 0f,
    val isRepeatedVerse: Boolean = false,
    val crossSurahJumpTarget: Pair<Int, Int>? = null,
    val statusNoteAr: String = "",
    val statusNoteEn: String = ""
) {
    val isActive: Boolean
        get() = status == LiveRecitationStatus.LISTENING ||
            status == LiveRecitationStatus.MATCHED ||
            status == LiveRecitationStatus.UNCERTAIN
}

/**
 * Privacy-first, on-device real-time Quran recitation microphone listener.
 *
 * - Uses Android's SpeechRecognizer with ar-SA and EXTRA_PREFER_OFFLINE=true for local processing.
 * - Never writes audio to disk and never uploads recordings to external servers.
 * - Streams live partial and final Arabic hypotheses directly into QuranVerseTrackerEngine
 *   with automatic continuous listening across pauses, repeats, and Surah/Page boundaries.
 */
class QuranLiveRecitationListener(private val appContext: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val trackerEngine = QuranVerseTrackerEngine()
    private var speechRecognizer: SpeechRecognizer? = null
    private var shouldKeepListening = false
    private var isPaused = false

    private val _state = MutableStateFlow(LiveRecitationState())
    val state: StateFlow<LiveRecitationState> = _state.asStateFlow()

    fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun loadSurahContext(surahNumber: Int, verses: List<QuranVerse>, initialAyah: Int? = null) {
        trackerEngine.loadSurahVerses(surahNumber, verses, initialAyah)
        val anchorAyah = initialAyah ?: verses.firstOrNull()?.numberInSurah
        val anchorPage = verses.firstOrNull { it.numberInSurah == anchorAyah }?.pageNumber
            ?: verses.firstOrNull()?.pageNumber
        _state.value = _state.value.copy(
            trackedSurah = surahNumber,
            trackedAyah = anchorAyah,
            trackedPage = anchorPage,
            crossSurahJumpTarget = null
        )
    }

    fun clearCrossSurahJumpTarget() {
        _state.value = _state.value.copy(crossSurahJumpTarget = null)
    }

    fun startListening() {
        if (!hasMicrophonePermission()) {
            _state.value = _state.value.copy(
                status = LiveRecitationStatus.ERROR,
                statusNoteAr = "يرجى منح إذن الميكروفون لتشغيل ميزة الاستماع والمتابعة.",
                statusNoteEn = "Microphone permission is required for Listen & Follow."
            )
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _state.value = _state.value.copy(
                status = LiveRecitationStatus.ERROR,
                statusNoteAr = "خدمة التعرف الصوتي على الجهاز غير متوفرة في هذا الجهاز.",
                statusNoteEn = "On-device speech recognition service is unavailable on this device."
            )
            return
        }

        shouldKeepListening = true
        isPaused = false
        trackerEngine.resetTrackingState()
        _state.value = _state.value.copy(
            status = LiveRecitationStatus.LISTENING,
            statusNoteAr = "يستمع الآن للتلاوة محلياً على جهازك...",
            statusNoteEn = "Listening locally on your device..."
        )
        mainHandler.post { startRecognizerSession() }
    }

    fun pauseListening() {
        isPaused = true
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {
            }
        }
        _state.value = _state.value.copy(
            status = LiveRecitationStatus.PAUSED,
            rmsLevel = 0f,
            statusNoteAr = "تم إيقاف المتابعة مؤقتاً مع حفظ موضع الآية.",
            statusNoteEn = "Tracking paused — current Ayah preserved."
        )
    }

    fun resumeListening() {
        if (!shouldKeepListening) {
            startListening()
            return
        }
        isPaused = false
        _state.value = _state.value.copy(
            status = LiveRecitationStatus.LISTENING,
            statusNoteAr = "تم استئناف الاستماع للتلاوة...",
            statusNoteEn = "Resumed listening..."
        )
        mainHandler.post { startRecognizerSession() }
    }

    fun stopListening() {
        shouldKeepListening = false
        isPaused = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post { destroyRecognizer() }
        _state.value = _state.value.copy(
            status = LiveRecitationStatus.IDLE,
            rmsLevel = 0f,
            heardText = "",
            crossSurahJumpTarget = null,
            statusNoteAr = "",
            statusNoteEn = ""
        )
    }

    private fun startRecognizerSession() {
        if (!shouldKeepListening || isPaused) return
        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                    setRecognitionListener(listener)
                }
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            }
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            _state.value = _state.value.copy(
                status = LiveRecitationStatus.ERROR,
                statusNoteAr = "تعذّر تشغيل الميكروفون للتعرف الصوتي.",
                statusNoteEn = "Unable to start microphone recognition."
            )
            shouldKeepListening = false
        }
    }

    private fun scheduleRestart(delayMs: Long = 350L) {
        if (!shouldKeepListening || isPaused) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (shouldKeepListening && !isPaused) {
                startRecognizerSession()
            }
        }, delayMs)
    }

    private fun handleHypotheses(hypotheses: List<String>, isFinal: Boolean) {
        val bestRaw = hypotheses.firstOrNull { it.isNotBlank() } ?: return
        var bestDecision = trackerEngine.processTranscript(bestRaw, isFinal)

        // Check alternative hypotheses if top hypothesis was uncertain
        if (!bestDecision.isCertain && hypotheses.size > 1) {
            for (alt in hypotheses.drop(1)) {
                val altDec = trackerEngine.processTranscript(alt, isFinal)
                if (altDec.isCertain && altDec.confidence > bestDecision.confidence) {
                    bestDecision = altDec
                    break
                }
            }
        }

        if (bestDecision.isCrossSurahJump && bestDecision.matchedSurah != null && bestDecision.matchedAyah != null) {
            _state.value = _state.value.copy(
                status = LiveRecitationStatus.MATCHED,
                heardText = bestRaw.take(120),
                confidence = bestDecision.confidence,
                crossSurahJumpTarget = bestDecision.matchedSurah to bestDecision.matchedAyah,
                statusNoteAr = "تم التعرّف على تلاوة سورة ${bestDecision.matchedSurah} · آية ${bestDecision.matchedAyah}",
                statusNoteEn = "Detected Surah ${bestDecision.matchedSurah} · Ayah ${bestDecision.matchedAyah}"
            )
            return
        }

        if (bestDecision.isCertain) {
            _state.value = _state.value.copy(
                status = LiveRecitationStatus.MATCHED,
                heardText = bestRaw.take(120),
                trackedSurah = bestDecision.matchedSurah,
                trackedAyah = bestDecision.matchedAyah,
                trackedPage = bestDecision.matchedPage,
                confidence = bestDecision.confidence,
                isRepeatedVerse = bestDecision.isRepeatedVerse,
                statusNoteAr = if (bestDecision.isRepeatedVerse) {
                    "متابعة الآية ${bestDecision.matchedAyah} (إعادة)"
                } else {
                    "تتبع مباشر · الآية ${bestDecision.matchedAyah}"
                },
                statusNoteEn = if (bestDecision.isRepeatedVerse) {
                    "Following Ayah ${bestDecision.matchedAyah} (Repeated)"
                } else {
                    "Live Sync · Ayah ${bestDecision.matchedAyah}"
                }
            )
        } else {
            _state.value = _state.value.copy(
                status = LiveRecitationStatus.UNCERTAIN,
                heardText = bestRaw.take(120),
                confidence = bestDecision.confidence,
                statusNoteAr = "جارٍ التحقق من التلاوة قبل الانتقال...",
                statusNoteEn = "Verifying recitation before advancing..."
            )
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            if (!isPaused && shouldKeepListening) {
                _state.value = _state.value.copy(
                    status = if (_state.value.status == LiveRecitationStatus.MATCHED) {
                        LiveRecitationStatus.MATCHED
                    } else {
                        LiveRecitationStatus.LISTENING
                    }
                )
            }
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _state.value = _state.value.copy(rmsLevel = normalized)
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(rmsLevel = 0f)
        }

        override fun onError(error: Int) {
            _state.value = _state.value.copy(rmsLevel = 0f)
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // Normal pause during recitation -> keep listening cleanly without resetting Ayah
                    scheduleRestart(300L)
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    scheduleRestart(700L)
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    shouldKeepListening = false
                    _state.value = _state.value.copy(
                        status = LiveRecitationStatus.ERROR,
                        statusNoteAr = "يرجى تفعيل إذن الميكروفون من إعدادات التطبيق.",
                        statusNoteEn = "Please grant Microphone permission to continue."
                    )
                }
                else -> {
                    // Transient audio/network hiccup -> retry gently
                    scheduleRestart(900L)
                }
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            if (matches.isNotEmpty()) {
                handleHypotheses(matches, isFinal = true)
            }
            scheduleRestart(250L)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            if (matches.isNotEmpty()) {
                handleHypotheses(matches, isFinal = false)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        } finally {
            speechRecognizer = null
        }
    }
}
