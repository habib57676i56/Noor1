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
    val isOfflinePreferred: Boolean = false,
    val isOfflineFallbackUsed: Boolean = false,
    val lastErrorCode: Int? = null,
    val isRateLimited: Boolean = false,
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

enum class SpeechErrorAction {
    RESTART_AFTER_DELAY,
    SWITCH_TO_HYBRID_AND_RESTART,
    PAUSE_CLEANLY,
    STOP_WITH_ERROR
}

data class SpeechErrorDecision(
    val action: SpeechErrorAction,
    val delayMs: Long = 0L,
    val destroyRecognizerFirst: Boolean = false,
    val isRateLimited: Boolean = false,
    val statusNoteAr: String = "",
    val statusNoteEn: String = "",
    val errorMessageAr: String = "",
    val errorMessageEn: String = ""
)

/**
 * Pure, deterministic recovery policy for Android SpeechRecognizer errors —
 * specifically fixing Error 10 (ERROR_TOO_MANY_REQUESTS) caused by rapid restart loops
 * on Android 12+ devices, plus silence auto-pause, busy backoff, and offline-pack fallback.
 */
object SpeechErrorRecoveryPolicy {
    const val ERROR_TOO_MANY_REQUESTS = 10
    const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
    const val ERROR_LANGUAGE_UNAVAILABLE = 13
    const val MIN_SESSION_SPACING_MS = 1200L
    const val MAX_CONSECUTIVE_SILENCE_TIMEOUTS = 3
    const val MAX_RATE_LIMIT_RETRIES = 2

    fun decide(
        errorCode: Int,
        consecutiveSilence: Int,
        consecutiveRateLimits: Int,
        consecutiveOtherErrors: Int,
        sessionOfflineFlag: Boolean
    ): SpeechErrorDecision {
        return when (errorCode) {
            // Normal silence/pause during recitation — DO NOT tight-loop forever!
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                if (consecutiveSilence >= MAX_CONSECUTIVE_SILENCE_TIMEOUTS) {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.PAUSE_CLEANLY,
                        destroyRecognizerFirst = true,
                        statusNoteAr = "توقف الاستماع مؤقتاً لعدم سماع صوت — اضغط «استئناف» عند متابعة التلاوة.",
                        statusNoteEn = "Listening paused during silence — tap Resume when continuing recitation."
                    )
                } else {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.RESTART_AFTER_DELAY,
                        delayMs = MIN_SESSION_SPACING_MS + (consecutiveSilence * 500L),
                        destroyRecognizerFirst = false,
                        statusNoteAr = "في انتظار متابعة التلاوة...",
                        statusNoteEn = "Waiting for recitation..."
                    )
                }
            }

            // CRITICAL FIX FOR ERROR 10 (SpeechRecognizer.ERROR_TOO_MANY_REQUESTS):
            // Never rapid-retry! Destroy recognizer immediately and apply exponential backoff,
            // or pause cleanly preserving the full transcript and offering System Voice / Audio Recording.
            ERROR_TOO_MANY_REQUESTS -> {
                if (consecutiveRateLimits >= MAX_RATE_LIMIT_RETRIES) {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.PAUSE_CLEANLY,
                        destroyRecognizerFirst = true,
                        isRateLimited = true,
                        statusNoteAr = "تم إيقاف التعرف التلقائي مؤقتاً لتفادي حد النظام (خطأ 10). تم حفظ النص — انتظر ثواني ثم اضغط «استئناف» أو استخدم «إملاء صوتي بديل» أو «تسجيل صوتي».",
                        statusNoteEn = "Auto speech recognition paused to respect Android rate limit (Error 10). Transcript preserved — wait a moment and tap Resume, or use System Voice Fallback / Audio Recorder."
                    )
                } else {
                    val backoffMs = when (consecutiveRateLimits) {
                        0 -> 3500L
                        1 -> 7000L
                        else -> 12000L
                    }
                    SpeechErrorDecision(
                        action = SpeechErrorAction.RESTART_AFTER_DELAY,
                        delayMs = backoffMs,
                        destroyRecognizerFirst = true,
                        isRateLimited = true,
                        statusNoteAr = "تهدئة مؤقتة لمحرك الصوت (${backoffMs / 1000} ثوانٍ) لتجاوز حد الطلبات (خطأ 10)...",
                        statusNoteEn = "Cooling down speech recognizer (${backoffMs / 1000}s) after Android rate limit (Error 10)..."
                    )
                }
            }

            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                if (consecutiveOtherErrors >= 3) {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.PAUSE_CLEANLY,
                        destroyRecognizerFirst = true,
                        statusNoteAr = "محرك الصوت مشغول حالياً — تم الإيقاف المؤقت مع حفظ النص. اضغط استئناف بعد لحظة.",
                        statusNoteEn = "Speech recognizer busy — paused cleanly with transcript saved. Tap Resume shortly."
                    )
                } else {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.RESTART_AFTER_DELAY,
                        delayMs = 1800L + (consecutiveOtherErrors * 800L),
                        destroyRecognizerFirst = true,
                        statusNoteAr = "إعادة تهيئة محرك الصوت بهدوء...",
                        statusNoteEn = "Resetting speech recognizer..."
                    )
                }
            }

            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                SpeechErrorDecision(
                    action = SpeechErrorAction.STOP_WITH_ERROR,
                    destroyRecognizerFirst = true,
                    statusNoteAr = "يرجى تفعيل إذن الميكروفون من إعدادات التطبيق.",
                    statusNoteEn = "Please grant Microphone permission to continue.",
                    errorMessageAr = "إذن الميكروفون مرفوض.",
                    errorMessageEn = "Microphone permission rejected."
                )
            }

            SpeechRecognizer.ERROR_AUDIO -> {
                SpeechErrorDecision(
                    action = SpeechErrorAction.PAUSE_CLEANLY,
                    destroyRecognizerFirst = true,
                    statusNoteAr = "الميكروفون مشغول أو تعذّر التقاط الصوت. أغلق أي تسجيل آخر ثم اضغط استئناف.",
                    statusNoteEn = "Microphone busy or audio capture error. Stop any other recording then tap Resume.",
                    errorMessageAr = "تعذّر فتح الميكروفون (خطأ صوتي).",
                    errorMessageEn = "Audio capture error."
                )
            }

            ERROR_LANGUAGE_NOT_SUPPORTED,
            ERROR_LANGUAGE_UNAVAILABLE,
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER -> {
                if (sessionOfflineFlag && (errorCode == ERROR_LANGUAGE_NOT_SUPPORTED || errorCode == ERROR_LANGUAGE_UNAVAILABLE)) {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.SWITCH_TO_HYBRID_AND_RESTART,
                        delayMs = MIN_SESSION_SPACING_MS,
                        destroyRecognizerFirst = true,
                        statusNoteAr = "حزمة العربية غير المتصلة غير مثبّتة على الجهاز — تم التحويل للتعرف الصوتي الهجين تلقائياً.",
                        statusNoteEn = "Offline Arabic speech pack not installed — switched to hybrid recognition."
                    )
                } else if (consecutiveOtherErrors >= 2) {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.STOP_WITH_ERROR,
                        destroyRecognizerFirst = true,
                        statusNoteAr = "تعذّر التعرف الصوتي للغة العربية (قد يلزم تثبيت حزمة العربية دون إنترنت أو اتصال شبكي أو استخدام التسجيل الصوتي / الإملاء البديل).",
                        statusNoteEn = "Arabic STT unavailable (install offline Arabic pack, check internet, or use Audio Recorder / System Voice Fallback).",
                        errorMessageAr = "رمز خطأ التعرف الصوتي: $errorCode",
                        errorMessageEn = "SpeechRecognizer error code: $errorCode"
                    )
                } else {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.RESTART_AFTER_DELAY,
                        delayMs = 2000L,
                        destroyRecognizerFirst = true,
                        statusNoteAr = "إعادة محاولة التعرف الصوتي للغة العربية...",
                        statusNoteEn = "Retrying Arabic speech recognition..."
                    )
                }
            }

            else -> {
                if (consecutiveOtherErrors >= 2) {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.PAUSE_CLEANLY,
                        destroyRecognizerFirst = true,
                        statusNoteAr = "توقف التعرف الصوتي مؤقتاً (رمز $errorCode) مع حفظ النص. يمكنك الضغط على استئناف أو استخدام الإملاء الصوتي البديل.",
                        statusNoteEn = "Speech recognition paused (code $errorCode) with transcript preserved. Tap Resume or use System Voice Fallback.",
                        errorMessageAr = "رمز خطأ التعرف الصوتي: $errorCode",
                        errorMessageEn = "SpeechRecognizer error code: $errorCode"
                    )
                } else {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.RESTART_AFTER_DELAY,
                        delayMs = 2000L,
                        destroyRecognizerFirst = true,
                        statusNoteAr = "إعادة تهيئة التعرف الصوتي...",
                        statusNoteEn = "Reinitializing speech recognition..."
                    )
                }
            }
        }
    }
}

/**
 * Repaired, resilient Arabic Speech-to-Text & Live Quran Recitation Listener.
 *
 * Key repairs (v6 Error-10 Fix):
 * - Eliminates Android 12+ `SpeechRecognizer.ERROR_TOO_MANY_REQUESTS` (Error 10) by enforcing
 *   a strict 1200ms minimum spacing between `startListening()` calls, tracking active session
 *   lifecycle (`isSessionRunning`), auto-pausing cleanly after 3 silent timeouts instead of
 *   infinite 300ms loops, and applying exponential cooldown backoff (3.5s -> 7s -> clean pause).
 * - Preserves full accumulated Arabic transcript across recognizer pauses/restarts and after
 *   `stopListening()` so memorization assessment always receives the recited verses.
 * - Offline-first (`ar-SA`) with automatic one-shot fallback to hybrid recognition when an
 *   offline Arabic speech pack is not installed on the device.
 * - Cleanly supports Android's system `RecognizerIntent` Activity fallback (never hits
 *   background Error 10 throttling).
 */
class QuranLiveRecitationListener(private val appContext: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val trackerEngine = QuranVerseTrackerEngine()
    private var speechRecognizer: SpeechRecognizer? = null
    private var shouldKeepListening = false
    private var isPaused = false
    private var isSessionRunning = false
    private var preferOfflineMode = false
    private var sessionOfflineFlag = false
    private var consecutiveErrors = 0
    private var consecutiveSilenceTimeouts = 0
    private var consecutiveRateLimits = 0
    private var lastSessionStartMs = 0L
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
     * Builds a system RecognizerIntent for reliable voice dictation via ActivityResultLauncher.
     * Works smoothly even when OEMs throttle background SpeechRecognizer (Error 10).
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
            lastErrorCode = null,
            isRateLimited = false,
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

    fun startListening(resetTranscript: Boolean = false, initialDelayMs: Long = 0L) {
        if (!hasMicrophonePermission()) {
            _state.value = _state.value.copy(
                status = LiveRecitationStatus.ERROR,
                lastErrorCode = SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
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

        mainHandler.removeCallbacksAndMessages(null)
        shouldKeepListening = true
        isPaused = false
        consecutiveErrors = 0
        consecutiveSilenceTimeouts = 0
        consecutiveRateLimits = 0
        sessionOfflineFlag = preferOfflineMode
        trackerEngine.resetTrackingState()
        _state.value = _state.value.copy(
            status = LiveRecitationStatus.LISTENING,
            isOfflinePreferred = preferOfflineMode,
            isOfflineFallbackUsed = false,
            lastErrorCode = null,
            isRateLimited = false,
            heardText = if (resetTranscript) "" else _state.value.heardText,
            accumulatedTranscript = if (resetTranscript) "" else _state.value.accumulatedTranscript,
            pauseCount = if (resetTranscript) 0 else _state.value.pauseCount,
            longestPauseMs = if (resetTranscript) 0L else _state.value.longestPauseMs,
            errorMessageAr = "",
            errorMessageEn = "",
            statusNoteAr = if (preferOfflineMode) {
                "يستمع الآن للتلاوة العربية (وضع دون إنترنت — يتطلب حزمة اللغة العربية)..."
            } else {
                "يستمع الآن ويحوّل تلاوتك إلى نص عربي مباشر (عبر محرك أندرويد للصوت العربي)..."
            },
            statusNoteEn = if (preferOfflineMode) {
                "Listening to Arabic recitation (offline Arabic pack mode)..."
            } else {
                "Listening & transcribing Arabic recitation live (Android Arabic speech engine)..."
            }
        )

        val elapsedSinceLastStart = System.currentTimeMillis() - lastSessionStartMs
        val cooldownNeeded = (SpeechErrorRecoveryPolicy.MIN_SESSION_SPACING_MS - elapsedSinceLastStart)
            .coerceAtLeast(initialDelayMs)
        if (cooldownNeeded > 0L) {
            mainHandler.postDelayed({ startRecognizerSession() }, cooldownNeeded)
        } else {
            mainHandler.post { startRecognizerSession() }
        }
    }

    fun pauseListening() {
        isPaused = true
        isSessionRunning = false
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
        consecutiveSilenceTimeouts = 0
        consecutiveRateLimits = 0
        consecutiveErrors = 0
        _state.value = _state.value.copy(
            status = LiveRecitationStatus.LISTENING,
            isRateLimited = false,
            errorMessageAr = "",
            errorMessageEn = "",
            statusNoteAr = "تم استئناف الاستماع للتلاوة...",
            statusNoteEn = "Resumed listening..."
        )
        val elapsed = System.currentTimeMillis() - lastSessionStartMs
        val waitMs = (SpeechErrorRecoveryPolicy.MIN_SESSION_SPACING_MS - elapsed).coerceAtLeast(250L)
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({ startRecognizerSession() }, waitMs)
    }

    /**
     * Stops listening cleanly while PRESERVING heardText and accumulatedTranscript so the
     * memorization assessment engine can evaluate the recited passage immediately after stopping.
     */
    fun stopListening(clearTranscript: Boolean = false) {
        shouldKeepListening = false
        isPaused = false
        isSessionRunning = false
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
            isRateLimited = false,
            statusNoteAr = if (clearTranscript) "" else _state.value.statusNoteAr,
            statusNoteEn = if (clearTranscript) "" else _state.value.statusNoteEn
        )
    }

    private fun startRecognizerSession() {
        if (!shouldKeepListening || isPaused || isSessionRunning) return
        try {
            val now = System.currentTimeMillis()
            val sinceLast = now - lastSessionStartMs
            if (lastSessionStartMs > 0L && sinceLast < SpeechErrorRecoveryPolicy.MIN_SESSION_SPACING_MS) {
                scheduleRestart(SpeechErrorRecoveryPolicy.MIN_SESSION_SPACING_MS - sinceLast)
                return
            }

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
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
            }
            isSessionRunning = true
            lastSessionStartMs = System.currentTimeMillis()
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isSessionRunning = false
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

    private fun scheduleRestart(delayMs: Long = SpeechErrorRecoveryPolicy.MIN_SESSION_SPACING_MS) {
        if (!shouldKeepListening || isPaused) return
        val safeDelay = delayMs.coerceAtLeast(SpeechErrorRecoveryPolicy.MIN_SESSION_SPACING_MS)
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (shouldKeepListening && !isPaused && !isSessionRunning) {
                startRecognizerSession()
            }
        }, safeDelay)
    }

    private fun handleHypotheses(hypotheses: List<String>, isFinal: Boolean) {
        val bestRaw = hypotheses.firstOrNull { it.isNotBlank() } ?: return
        consecutiveErrors = 0
        consecutiveSilenceTimeouts = 0
        consecutiveRateLimits = 0

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
                isRateLimited = false,
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
                isRateLimited = false,
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
                isRateLimited = false,
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
            consecutiveSilenceTimeouts = 0
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
            isSessionRunning = false
            _state.value = _state.value.copy(rmsLevel = 0f, lastErrorCode = error)

            val decision = SpeechErrorRecoveryPolicy.decide(
                errorCode = error,
                consecutiveSilence = consecutiveSilenceTimeouts,
                consecutiveRateLimits = consecutiveRateLimits,
                consecutiveOtherErrors = consecutiveErrors,
                sessionOfflineFlag = sessionOfflineFlag
            )

            // Update counters strictly after computing decision
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> consecutiveSilenceTimeouts += 1
                SpeechErrorRecoveryPolicy.ERROR_TOO_MANY_REQUESTS -> consecutiveRateLimits += 1
                else -> consecutiveErrors += 1
            }

            if (decision.destroyRecognizerFirst) {
                destroyRecognizer()
            }

            when (decision.action) {
                SpeechErrorAction.RESTART_AFTER_DELAY -> {
                    _state.value = _state.value.copy(
                        isRateLimited = decision.isRateLimited,
                        statusNoteAr = decision.statusNoteAr.ifBlank { _state.value.statusNoteAr },
                        statusNoteEn = decision.statusNoteEn.ifBlank { _state.value.statusNoteEn }
                    )
                    scheduleRestart(decision.delayMs)
                }

                SpeechErrorAction.SWITCH_TO_HYBRID_AND_RESTART -> {
                    sessionOfflineFlag = false
                    consecutiveErrors = 0
                    _state.value = _state.value.copy(
                        isOfflineFallbackUsed = true,
                        isRateLimited = false,
                        statusNoteAr = decision.statusNoteAr,
                        statusNoteEn = decision.statusNoteEn
                    )
                    scheduleRestart(decision.delayMs)
                }

                SpeechErrorAction.PAUSE_CLEANLY -> {
                    isPaused = true
                    mainHandler.removeCallbacksAndMessages(null)
                    _state.value = _state.value.copy(
                        status = LiveRecitationStatus.PAUSED,
                        isRateLimited = decision.isRateLimited,
                        statusNoteAr = decision.statusNoteAr,
                        statusNoteEn = decision.statusNoteEn,
                        errorMessageAr = decision.errorMessageAr,
                        errorMessageEn = decision.errorMessageEn
                    )
                }

                SpeechErrorAction.STOP_WITH_ERROR -> {
                    shouldKeepListening = false
                    isPaused = false
                    mainHandler.removeCallbacksAndMessages(null)
                    _state.value = _state.value.copy(
                        status = LiveRecitationStatus.ERROR,
                        isRateLimited = decision.isRateLimited,
                        statusNoteAr = decision.statusNoteAr,
                        statusNoteEn = decision.statusNoteEn,
                        errorMessageAr = decision.errorMessageAr,
                        errorMessageEn = decision.errorMessageEn
                    )
                }
            }
        }

        override fun onResults(results: Bundle?) {
            isSessionRunning = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            if (matches.isNotEmpty()) {
                handleHypotheses(matches, isFinal = true)
            }
            // Enforce polite spacing between recognition sessions to prevent Error 10 throttling
            scheduleRestart(SpeechErrorRecoveryPolicy.MIN_SESSION_SPACING_MS)
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
        isSessionRunning = false
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        } finally {
            speechRecognizer = null
        }
    }
}
