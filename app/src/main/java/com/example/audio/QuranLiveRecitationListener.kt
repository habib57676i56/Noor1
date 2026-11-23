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
    val accumulatedTranscript: String = "",
    val trackedSurah: Int? = null,
    val trackedAyah: Int? = null,
    val trackedPage: Int? = null,
    val confidence: Float = 0f,
    val isRepeatedVerse: Boolean = false,
    val crossSurahJumpTarget: Pair<Int, Int>? = null,
    val pauseCount: Int = 0,
    val longestPauseMs: Long = 0L,
    val isOfflinePreferred: Boolean = true,
    val isOfflineFallbackUsed: Boolean = false,
    val statusNoteAr: String = "",
    val statusNoteEn: String = "",
    val errorMessageAr: String = "",
    val errorMessageEn: String = ""
) {
    val isActive: Boolean
        get() = status == LiveRecitationStatus.LISTENING ||
            status == LiveRecitationStatus.MATCHED ||
            status == LiveRecitationStatus.UNCERTAIN

    val effectiveTranscript: String
        get() = accumulatedTranscript.ifBlank { heardText }
}

/**
 * Repaired, resilient Arabic Speech-to-Text & Live Quran Recitation Listener.
 *
 * Key repairs:
 * - Android 11+ <queries> manifest visibility enabled so SpeechRecognizer works on modern Android.
 * - Offline-first (ar-SA) with automatic one-shot fallback to hybrid/online recognition when the
 *   device lacks an installed Arabic offline speech pack (ERROR_LANGUAGE_UNAVAILABLE / NOT_SUPPORTED),
 *   always informing the user transparently instead of looping silently with empty text.
 * - Preserves full accumulated Arabic transcript across recognizer restarts AND after stopListening()
 *   so Hifz recitation assessment always receives the complete spoken verses.
 * - Tracks real pause durations between spoken segments (pauses >= 2.5s) for Hifz hesitation analysis.
 * - Supports injecting text from Android's system RecognizerIntent Activity fallback seamlessly.
 */
class QuranLiveRecitationListener(private val appContext: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val trackerEngine = QuranVerseTrackerEngine()
    private var speechRecognizer: SpeechRecognizer? = null
    private var shouldKeepListening = false
    private var isPaused = false
    private var preferOfflineMode = true
    private var sessionOfflineFlag = true
    private var consecutiveErrors = 0
    private var lastSpeechEndMs = 0L

    private val accumulatedSegments = mutableListOf<String>()

    private val _state = MutableStateFlow(LiveRecitationState())
    val state: StateFlow<LiveRecitationState> = _state.asStateFlow()

    fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun isSpeechServiceAvailable(): Boolean =
        runCatching { SpeechRecognizer.isRecognitionAvailable(appContext) }.getOrDefault(false)

    fun setPreferOffline(preferOffline: Boolean) {
        preferOfflineMode = preferOffline
        sessionOfflineFlag = preferOffline
        _state.value = _state.value.copy(isOfflinePreferred = preferOffline)
    }

    /**
     * Builds a system RecognizerIntent for fallback voice dictation via ActivityResultLauncher
     * when on-device background SpeechRecognizer is blocked by OEM restrictions.
     */
    fun buildSystemSpeechIntent(promptAr: String = "اتلُ الآيات الآن بوضوح"): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_PROMPT, promptAr)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (preferOfflineMode) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
    }

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

    fun clearTranscript() {
        accumulatedSegments.clear()
        lastSpeechEndMs = 0L
        _state.value = _state.value.copy(
            heardText = "",
            accumulatedTranscript = "",
            pauseCount = 0,
            longestPauseMs = 0L,
            errorMessageAr = "",
            errorMessageEn = ""
        )
    }

    /**
     * Injects recognized Arabic text (e.g. from RecognizerIntent Activity fallback or unit tests)
     * directly into the live tracking & accumulated transcript pipeline.
     */
    fun injectRecognizedText(text: String, isFinal: Boolean = true) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        handleHypotheses(listOf(cleaned), isFinal = isFinal)
    }

    fun startListening(resetTranscript: Boolean = false) {
        if (!hasMicrophonePermission()) {
            _state.value = _state.value.copy(
                status = LiveRecitationStatus.ERROR,
                statusNoteAr = "يرجى منح إذن الميكروفون لتشغيل التعرف الصوتي.",
                statusNoteEn = "Microphone permission is required for speech recognition.",
                errorMessageAr = "إذن الميكروفون غير ممنوح.",
                errorMessageEn = "Microphone permission denied."
            )
            return
        }
        if (!isSpeechServiceAvailable()) {
            _state.value = _state.value.copy(
                status = LiveRecitationStatus.ERROR,
                statusNoteAr = "خدمة التعرف الصوتي للنظام غير متوفرة؛ يمكنك استخدام زر الإملاء الصوتي البديل أو تسجيل التلاوة صوتياً.",
                statusNoteEn = "System SpeechRecognizer unavailable; use the System Voice Fallback or Audio Recorder.",
                errorMessageAr = "لم يتم العثور على خدمة RecognitionService في النظام.",
                errorMessageEn = "No Android SpeechRecognizer service found on this device."
            )
            return
        }

        if (resetTranscript) {
            accumulatedSegments.clear()
            lastSpeechEndMs = 0L
        }

        shouldKeepListening = true
        isPaused = false
        consecutiveErrors = 0
        sessionOfflineFlag = preferOfflineMode
        trackerEngine.resetTrackingState()
        _state.value = _state.value.copy(
            status = LiveRecitationStatus.LISTENING,
            isOfflinePreferred = preferOfflineMode,
            isOfflineFallbackUsed = false,
            heardText = if (resetTranscript) "" else _state.value.heardText,
            accumulatedTranscript = if (resetTranscript) "" else _state.value.accumulatedTranscript,
            pauseCount = if (resetTranscript) 0 else _state.value.pauseCount,
            longestPauseMs = if (resetTranscript) 0L else _state.value.longestPauseMs,
            errorMessageAr = "",
            errorMessageEn = "",
            statusNoteAr = if (preferOfflineMode) {
                "يستمع الآن للتلاوة العربية (أولوية العمل دون إنترنت)..."
            } else {
                "يستمع الآن للتلاوة العربية..."
            },
            statusNoteEn = if (preferOfflineMode) {
                "Listening to Arabic recitation (offline-first)..."
            } else {
                "Listening to Arabic recitation..."
            }
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
            statusNoteAr = "تم إيقاف الاستماع مؤقتاً مع حفظ النص وموضع الآية.",
            statusNoteEn = "Listening paused — transcript and Ayah preserved."
        )
    }

    fun resumeListening() {
        if (!shouldKeepListening) {
            startListening(resetTranscript = false)
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

    /**
     * Stops listening cleanly while PRESERVING heardText and accumulatedTranscript so the
     * memorization assessment engine can evaluate the recited passage immediately after stopping.
     */
    fun stopListening(clearTranscript: Boolean = false) {
        shouldKeepListening = false
        isPaused = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post { destroyRecognizer() }
        if (clearTranscript) {
            accumulatedSegments.clear()
            lastSpeechEndMs = 0L
        }
        _state.value = _state.value.copy(
            status = LiveRecitationStatus.IDLE,
            rmsLevel = 0f,
            heardText = if (clearTranscript) "" else _state.value.heardText,
            accumulatedTranscript = if (clearTranscript) "" else _state.value.accumulatedTranscript,
            crossSurahJumpTarget = null,
            statusNoteAr = if (clearTranscript) "" else _state.value.statusNoteAr,
            statusNoteEn = if (clearTranscript) "" else _state.value.statusNoteEn
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
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, sessionOfflineFlag)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2800L)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            shouldKeepListening = false
            _state.value = _state.value.copy(
                status = LiveRecitationStatus.ERROR,
                statusNoteAr = "تعذّر تشغيل خدمة التعرف الصوتي على الجهاز.",
                statusNoteEn = "Unable to start speech recognition service.",
                errorMessageAr = "خطأ في تشغيل SpeechRecognizer (${e.localizedMessage ?: "غير متاح"}).",
                errorMessageEn = "SpeechRecognizer failed to start (${e.localizedMessage ?: "unavailable"})."
            )
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
        consecutiveErrors = 0

        if (isFinal) {
            val trimmed = bestRaw.trim()
            if (trimmed.isNotEmpty() && accumulatedSegments.lastOrNull() != trimmed) {
                accumulatedSegments.add(trimmed)
            }
        }

        val combinedTranscript = if (isFinal) {
            accumulatedSegments.joinToString(" ")
        } else {
            (accumulatedSegments + bestRaw.trim()).filter { it.isNotBlank() }.joinToString(" ")
        }

        var bestDecision = trackerEngine.processTranscript(bestRaw, isFinal)
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
                heardText = bestRaw.take(180),
                accumulatedTranscript = combinedTranscript,
                confidence = bestDecision.confidence,
                crossSurahJumpTarget = bestDecision.matchedSurah to bestDecision.matchedAyah,
                errorMessageAr = "",
                errorMessageEn = "",
                statusNoteAr = "تم التعرّف على تلاوة سورة ${bestDecision.matchedSurah} · آية ${bestDecision.matchedAyah}",
                statusNoteEn = "Detected Surah ${bestDecision.matchedSurah} · Ayah ${bestDecision.matchedAyah}"
            )
            return
        }

        if (bestDecision.isCertain) {
            _state.value = _state.value.copy(
                status = LiveRecitationStatus.MATCHED,
                heardText = bestRaw.take(180),
                accumulatedTranscript = combinedTranscript,
                trackedSurah = bestDecision.matchedSurah,
                trackedAyah = bestDecision.matchedAyah,
                trackedPage = bestDecision.matchedPage,
                confidence = bestDecision.confidence,
                isRepeatedVerse = bestDecision.isRepeatedVerse,
                errorMessageAr = "",
                errorMessageEn = "",
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
                status = if (shouldKeepListening) LiveRecitationStatus.UNCERTAIN else _state.value.status,
                heardText = bestRaw.take(180),
                accumulatedTranscript = combinedTranscript,
                confidence = bestDecision.confidence,
                errorMessageAr = "",
                errorMessageEn = "",
                statusNoteAr = "جارٍ الاستماع والتحقق من التلاوة...",
                statusNoteEn = "Listening and verifying recitation..."
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

        override fun onBeginningOfSpeech() {
            val now = System.currentTimeMillis()
            if (lastSpeechEndMs > 0L) {
                val pauseMs = now - lastSpeechEndMs
                if (pauseMs >= 2500L) {
                    _state.value = _state.value.copy(
                        pauseCount = _state.value.pauseCount + 1,
                        longestPauseMs = maxOf(_state.value.longestPauseMs, pauseMs)
                    )
                }
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _state.value = _state.value.copy(rmsLevel = normalized)
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            lastSpeechEndMs = System.currentTimeMillis()
            _state.value = _state.value.copy(rmsLevel = 0f)
        }

        override fun onError(error: Int) {
            _state.value = _state.value.copy(rmsLevel = 0f)
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // Normal silence/pause during recitation -> continue cleanly without losing transcript
                    scheduleRestart(300L)
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    scheduleRestart(650L)
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    shouldKeepListening = false
                    _state.value = _state.value.copy(
                        status = LiveRecitationStatus.ERROR,
                        statusNoteAr = "يرجى تفعيل إذن الميكروفون من إعدادات التطبيق.",
                        statusNoteEn = "Please grant Microphone permission to continue.",
                        errorMessageAr = "إذن الميكروفون مرفوض.",
                        errorMessageEn = "Microphone permission rejected."
                    )
                }
                12, // SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
                13, // SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                SpeechRecognizer.ERROR_SERVER -> {
                    // If offline-only pack isn't installed on this device, retry once in hybrid mode
                    if (sessionOfflineFlag && (error == 12 || error == 13)) {
                        sessionOfflineFlag = false
                        _state.value = _state.value.copy(
                            isOfflineFallbackUsed = true,
                            statusNoteAr = "حزمة العربية غير المتصلة غير مثبّتة على الجهاز — تم التحويل للتعرف الصوتي الهجين تلقائياً.",
                            statusNoteEn = "Offline Arabic speech pack not installed — switched to hybrid recognition."
                        )
                        destroyRecognizer()
                        scheduleRestart(350L)
                    } else {
                        consecutiveErrors += 1
                        if (consecutiveErrors >= 3) {
                            shouldKeepListening = false
                            _state.value = _state.value.copy(
                                status = LiveRecitationStatus.ERROR,
                                statusNoteAr = "تعذّر التعرف الصوتي للغة العربية (قد يلزم تثبيت حزمة العربية دون إنترنت أو اتصال شبكي أو استخدام التسجيل الصوتي).",
                                statusNoteEn = "Arabic STT unavailable (install offline Arabic pack, check internet, or use Audio Recorder).",
                                errorMessageAr = "رمز خطأ التعرف الصوتي: $error",
                                errorMessageEn = "SpeechRecognizer error code: $error"
                            )
                        } else {
                            scheduleRestart(900L)
                        }
                    }
                }
                else -> {
                    consecutiveErrors += 1
                    if (consecutiveErrors >= 4) {
                        shouldKeepListening = false
                        _state.value = _state.value.copy(
                            status = LiveRecitationStatus.ERROR,
                            statusNoteAr = "توقف التعرف الصوتي مؤقتاً (خطأ $error). يمكنك إعادة المحاولة أو استخدام زر الإملاء البديل.",
                            statusNoteEn = "Speech recognition paused (error $error). Retry or use System Voice Fallback.",
                            errorMessageAr = "رمز خطأ التعرف الصوتي: $error",
                            errorMessageEn = "SpeechRecognizer error code: $error"
                        )
                    } else {
                        scheduleRestart(850L)
                    }
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
