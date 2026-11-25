package com.example.audio

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
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

/**
 * Explicitly distinguishes every speech-recognition failure cause so Error 12 is NEVER
 * conflated with offline network status or missing permissions.
 */
enum class SpeechErrorReason {
    NONE,
    SERVICE_UNAVAILABLE,
    ARABIC_UNSUPPORTED,
    OFFLINE_PACK_MISSING,
    NO_INTERNET,
    PERMISSION_DENIED,
    RECOGNITION_TIMEOUT,
    RATE_LIMITED,
    NETWORK_OR_PROVIDER_FAILURE
}

data class RecognitionProviderInfo(
    val packageName: String,
    val serviceName: String,
    val label: String,
    val isPreferredOnlineProvider: Boolean
) {
    fun componentName(): ComponentName = ComponentName(packageName, serviceName)
}

data class ArabicSttCapabilities(
    val serviceAvailable: Boolean = false,
    val isInternetAvailable: Boolean = false,
    val selectedProvider: RecognitionProviderInfo? = null,
    val installedProviders: List<RecognitionProviderInfo> = emptyList(),
    val supportedOnlineArabicLocales: List<String> = emptyList(),
    val installedOfflineArabicLocales: List<String> = emptyList(),
    val pendingOfflineArabicLocales: List<String> = emptyList(),
    val isArabicSupportedOnline: Boolean? = null,
    val isArabicOfflineInstalled: Boolean? = null
)

data class LiveRecitationState(
    val status: LiveRecitationStatus = LiveRecitationStatus.IDLE,
    val errorReason: SpeechErrorReason = SpeechErrorReason.NONE,
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
    val usingOnlineMode: Boolean = true,
    val isInternetAvailable: Boolean = true,
    val activeProviderName: String = "",
    val activeLocale: String = "ar-SA",
    val isArabicOfflineModelInstalled: Boolean? = null,
    val supportedArabicLocales: List<String> = emptyList(),
    val hasReceivedEmptyResult: Boolean = false,
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
    TRY_NEXT_LOCALE_OR_PROVIDER,
    SWITCH_TO_HYBRID_AND_RESTART,
    PAUSE_CLEANLY,
    STOP_WITH_ERROR
}

data class SpeechErrorDecision(
    val action: SpeechErrorAction,
    val reason: SpeechErrorReason = SpeechErrorReason.NONE,
    val delayMs: Long = 0L,
    val destroyRecognizerFirst: Boolean = false,
    val isRateLimited: Boolean = false,
    val statusNoteAr: String = "",
    val statusNoteEn: String = "",
    val errorMessageAr: String = "",
    val errorMessageEn: String = ""
)

/**
 * Pure, deterministic recovery & diagnostic policy for Android SpeechRecognizer errors.
 *
 * Key repairs:
 * - Properly handles Error 12 (`ERROR_LANGUAGE_NOT_SUPPORTED`) without falsely claiming internet is down:
 *   1. If offline-only mode failed and internet is available, seamlessly switches to online Arabic recognition.
 *   2. If offline-only mode failed and internet is unavailable, clearly reports `OFFLINE_PACK_MISSING`.
 *   3. If online mode hit Error 12, steps through fallback Arabic locales (`ar-SA` -> `ar` -> `ar-EG` -> ...)
 *      and alternate installed `RecognitionService` providers before reporting `ARABIC_UNSUPPORTED`.
 * - Clearly distinguishes all 7+ causes: `SERVICE_UNAVAILABLE`, `ARABIC_UNSUPPORTED`, `OFFLINE_PACK_MISSING`,
 *   `NO_INTERNET`, `PERMISSION_DENIED`, `RECOGNITION_TIMEOUT`, `RATE_LIMITED`, and `NETWORK_OR_PROVIDER_FAILURE`.
 * - Preserves Android 12+ Error 10 (`ERROR_TOO_MANY_REQUESTS`) exponential cooldown & silence auto-pause.
 */
object SpeechErrorRecoveryPolicy {
    const val ERROR_TOO_MANY_REQUESTS = 10
    const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
    const val ERROR_LANGUAGE_UNAVAILABLE = 13
    const val MIN_SESSION_SPACING_MS = 1200L
    const val MAX_CONSECUTIVE_SILENCE_TIMEOUTS = 3
    const val MAX_RATE_LIMIT_RETRIES = 2

    val ARABIC_LOCALE_CANDIDATES: List<String> = listOf(
        "ar-SA",
        "ar",
        "ar-EG",
        "ar-AE",
        "ar-JO",
        "ar-001"
    )

    fun decide(
        errorCode: Int,
        consecutiveSilence: Int,
        consecutiveRateLimits: Int,
        consecutiveOtherErrors: Int,
        sessionOfflineFlag: Boolean,
        isInternetAvailable: Boolean = true,
        hasMoreLocaleOrProviderFallbacks: Boolean = false,
        isOfflineArabicModelInstalled: Boolean? = null,
        activeProviderName: String = "",
        activeLocale: String = "ar-SA"
    ): SpeechErrorDecision {
        val providerTag = activeProviderName.takeIf { it.isNotBlank() } ?: "Android RecognitionService"
        return when (errorCode) {
            // Normal silence/pause or recognition timeout during recitation — DO NOT tight-loop forever!
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                if (consecutiveSilence >= MAX_CONSECUTIVE_SILENCE_TIMEOUTS) {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.PAUSE_CLEANLY,
                        reason = SpeechErrorReason.RECOGNITION_TIMEOUT,
                        destroyRecognizerFirst = true,
                        statusNoteAr = "انتهت مهلة الاستماع لعدم سماع صوت واضح — اضغط «استئناف» أو «إعادة المحاولة» عند متابعة التلاوة.",
                        statusNoteEn = "Recognition timed out due to silence — tap Resume or Retry when continuing recitation."
                    )
                } else {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.RESTART_AFTER_DELAY,
                        reason = SpeechErrorReason.RECOGNITION_TIMEOUT,
                        delayMs = MIN_SESSION_SPACING_MS + (consecutiveSilence * 500L),
                        destroyRecognizerFirst = false,
                        statusNoteAr = "في انتظار متابعة التلاوة بوضوح...",
                        statusNoteEn = "Waiting for clear Arabic recitation..."
                    )
                }
            }

            // CRITICAL FIX FOR ERROR 10 (SpeechRecognizer.ERROR_TOO_MANY_REQUESTS)
            ERROR_TOO_MANY_REQUESTS -> {
                if (consecutiveRateLimits >= MAX_RATE_LIMIT_RETRIES) {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.PAUSE_CLEANLY,
                        reason = SpeechErrorReason.RATE_LIMITED,
                        destroyRecognizerFirst = true,
                        isRateLimited = true,
                        statusNoteAr = "تم إيقاف التعرف التلقائي مؤقتاً لتفادي حد النظام (خطأ 10). تم حفظ النص — انتظر لحظة ثم اضغط «استئناف» أو استخدم التسجيل الصوتي.",
                        statusNoteEn = "Auto speech recognition paused to respect Android rate limit (Error 10). Transcript preserved — wait a moment and tap Resume, or use Audio Recorder."
                    )
                } else {
                    val backoffMs = when (consecutiveRateLimits) {
                        0 -> 3500L
                        1 -> 7000L
                        else -> 12000L
                    }
                    SpeechErrorDecision(
                        action = SpeechErrorAction.RESTART_AFTER_DELAY,
                        reason = SpeechErrorReason.RATE_LIMITED,
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
                        reason = SpeechErrorReason.NETWORK_OR_PROVIDER_FAILURE,
                        destroyRecognizerFirst = true,
                        statusNoteAr = "محرك الصوت مشغول حالياً — تم الإيقاف المؤقت مع حفظ النص. اضغط استئناف بعد لحظة.",
                        statusNoteEn = "Speech recognizer busy — paused cleanly with transcript saved. Tap Resume shortly."
                    )
                } else {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.RESTART_AFTER_DELAY,
                        reason = SpeechErrorReason.NETWORK_OR_PROVIDER_FAILURE,
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
                    reason = SpeechErrorReason.PERMISSION_DENIED,
                    destroyRecognizerFirst = true,
                    statusNoteAr = "تم رفض إذن الميكروفون. يرجى تفعيله من إعدادات التطبيق.",
                    statusNoteEn = "Microphone permission was denied. Please grant it in app settings.",
                    errorMessageAr = "إذن الميكروفون مرفوض.",
                    errorMessageEn = "Microphone permission denied."
                )
            }

            SpeechRecognizer.ERROR_AUDIO -> {
                SpeechErrorDecision(
                    action = SpeechErrorAction.PAUSE_CLEANLY,
                    reason = SpeechErrorReason.NETWORK_OR_PROVIDER_FAILURE,
                    destroyRecognizerFirst = true,
                    statusNoteAr = "الميكروفون مشغول أو تعذّر التقاط الصوت. أغلق أي تسجيل آخر ثم اضغط استئناف.",
                    statusNoteEn = "Microphone busy or audio capture error. Stop any other recording then tap Resume.",
                    errorMessageAr = "تعذّر فتح الميكروفون (خطأ صوتي).",
                    errorMessageEn = "Audio capture error."
                )
            }

            // CRITICAL FIX FOR ERROR 12 (ERROR_LANGUAGE_NOT_SUPPORTED) & ERROR 13 (ERROR_LANGUAGE_UNAVAILABLE):
            // NEVER treat Error 12 as proof that the internet is disconnected!
            ERROR_LANGUAGE_NOT_SUPPORTED,
            ERROR_LANGUAGE_UNAVAILABLE -> {
                when {
                    // Offline-only mode requested, but offline Arabic model isn't installed -> switch to online if internet exists!
                    sessionOfflineFlag && isInternetAvailable -> {
                        SpeechErrorDecision(
                            action = SpeechErrorAction.SWITCH_TO_HYBRID_AND_RESTART,
                            reason = SpeechErrorReason.OFFLINE_PACK_MISSING,
                            delayMs = MIN_SESSION_SPACING_MS,
                            destroyRecognizerFirst = true,
                            statusNoteAr = "حزمة اللغة العربية دون إنترنت غير مثبّتة — تم التحويل تلقائياً إلى التعرف الصوتي العربي عبر الإنترنت.",
                            statusNoteEn = "Offline Arabic language pack is not installed — switched automatically to online Arabic recognition."
                        )
                    }

                    // Offline mode requested (or forced) AND internet is unavailable -> report missing offline pack honestly
                    sessionOfflineFlag && !isInternetAvailable -> {
                        SpeechErrorDecision(
                            action = SpeechErrorAction.STOP_WITH_ERROR,
                            reason = SpeechErrorReason.OFFLINE_PACK_MISSING,
                            destroyRecognizerFirst = true,
                            statusNoteAr = "حزمة اللغة العربية دون إنترنت غير مثبّتة ولا يوجد اتصال بالإنترنت حالياً. يمكنك استخدام التسجيل الصوتي لتلاوتك دون إنترنت.",
                            statusNoteEn = "Offline Arabic language pack is missing and there is no internet connection. You can use the Audio Recorder fallback to record and review your recitation.",
                            errorMessageAr = "حزمة العربية دون إنترنت غير مثبّتة (رمز $errorCode).",
                            errorMessageEn = "Offline Arabic pack missing (code $errorCode)."
                        )
                    }

                    // Online mode hit Error 12/13 -> try fallback Arabic locales (ar-SA -> ar -> ar-EG...) or alternate provider!
                    hasMoreLocaleOrProviderFallbacks -> {
                        SpeechErrorDecision(
                            action = SpeechErrorAction.TRY_NEXT_LOCALE_OR_PROVIDER,
                            reason = SpeechErrorReason.ARABIC_UNSUPPORTED,
                            delayMs = MIN_SESSION_SPACING_MS,
                            destroyRecognizerFirst = true,
                            statusNoteAr = "جارٍ تجربة صيغة لغة عربية أخرى أو مزوّد تعرّف صوتي متاح عبر الإنترنت...",
                            statusNoteEn = "Trying alternate Arabic locale or online speech provider..."
                        )
                    }

                    // Online mode exhausted fallbacks AND internet is actually disconnected
                    !isInternetAvailable -> {
                        val offlineNoteAr = if (isOfflineArabicModelInstalled == false) {
                            "لا يوجد اتصال بالإنترنت ولا تتوفر حزمة عربية دون إنترنت."
                        } else {
                            "الاتصال بالإنترنت غير متوفر حالياً للتعرّف الصوتي عبر الإنترنت."
                        }
                        val offlineNoteEn = if (isOfflineArabicModelInstalled == false) {
                            "No internet connection and no offline Arabic speech model is installed."
                        } else {
                            "Internet connection is unavailable for online Arabic speech recognition."
                        }
                        SpeechErrorDecision(
                            action = SpeechErrorAction.STOP_WITH_ERROR,
                            reason = SpeechErrorReason.NO_INTERNET,
                            destroyRecognizerFirst = true,
                            statusNoteAr = "$offlineNoteAr يمكنك استخدام التسجيل الصوتي كبديل.",
                            statusNoteEn = "$offlineNoteEn Use the Audio Recorder fallback instead.",
                            errorMessageAr = "لا يوجد اتصال بالإنترنت (رمز $errorCode).",
                            errorMessageEn = "Internet unavailable (code $errorCode)."
                        )
                    }

                    // Internet IS available, yet the installed recognition provider rejected all Arabic locales (Error 12/13)
                    else -> {
                        SpeechErrorDecision(
                            action = SpeechErrorAction.STOP_WITH_ERROR,
                            reason = SpeechErrorReason.ARABIC_UNSUPPORTED,
                            destroyRecognizerFirst = true,
                            statusNoteAr = "الاتصال بالإنترنت متوفر، لكن مزوّد التعرف الصوتي المثبّت ($providerTag) لا يدعم اللغة العربية ($activeLocale). جرّب «إملاء النظام» أو «التسجيل الصوتي» أو ثبّت خدمات Google للتعرف الصوتي.",
                            statusNoteEn = "Internet is connected, but the installed speech provider ($providerTag) does not support Arabic ($activeLocale). Try System Voice Input, install Google Speech Services, or use the Audio Recorder fallback.",
                            errorMessageAr = "اللغة العربية غير مدعومة لدى مزوّد التعرف الصوتي (رمز $errorCode).",
                            errorMessageEn = "Arabic language unsupported by $providerTag (SpeechRecognizer code $errorCode)."
                        )
                    }
                }
            }

            // True Network / Server failures — distinct from Error 12 language support!
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER -> {
                if (!isInternetAvailable) {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.STOP_WITH_ERROR,
                        reason = SpeechErrorReason.NO_INTERNET,
                        destroyRecognizerFirst = true,
                        statusNoteAr = "انقطع الاتصال بالإنترنت أثناء التعرّف الصوتي العربي. تحقق من الشبكة ثم أعد المحاولة أو استخدم التسجيل الصوتي.",
                        statusNoteEn = "Internet connection is unavailable for online Arabic recognition. Check your network and retry, or use Audio Recorder.",
                        errorMessageAr = "لا يوجد اتصال بالإنترنت (رمز $errorCode).",
                        errorMessageEn = "Internet connection unavailable (code $errorCode)."
                    )
                } else if (consecutiveOtherErrors >= 1) {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.STOP_WITH_ERROR,
                        reason = SpeechErrorReason.NETWORK_OR_PROVIDER_FAILURE,
                        destroyRecognizerFirst = true,
                        statusNoteAr = "فشل مؤقت من مزوّد التعرف الصوتي عبر الشبكة ($providerTag). يمكنك إعادة المحاولة أو استخدام التسجيل الصوتي.",
                        statusNoteEn = "Temporary network or provider error from $providerTag. Tap Retry or use Audio Recorder.",
                        errorMessageAr = "خطأ شبكة أو خادم في مزوّد الصوت (رمز $errorCode).",
                        errorMessageEn = "Network or provider failure (SpeechRecognizer code $errorCode)."
                    )
                } else {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.RESTART_AFTER_DELAY,
                        reason = SpeechErrorReason.NETWORK_OR_PROVIDER_FAILURE,
                        delayMs = 2000L,
                        destroyRecognizerFirst = true,
                        statusNoteAr = "إعادة الاتصال بمزوّد التعرّف الصوتي العربي عبر الإنترنت...",
                        statusNoteEn = "Reconnecting to online Arabic speech provider..."
                    )
                }
            }

            else -> {
                if (consecutiveOtherErrors >= 2) {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.PAUSE_CLEANLY,
                        reason = SpeechErrorReason.NETWORK_OR_PROVIDER_FAILURE,
                        destroyRecognizerFirst = true,
                        statusNoteAr = "توقف التعرف الصوتي مؤقتاً بسبب خطأ في المزوّد (رمز $errorCode) مع حفظ النص. يمكنك إعادة المحاولة أو استخدام التسجيل الصوتي.",
                        statusNoteEn = "Speech recognition paused due to provider error (code $errorCode) with transcript preserved. Tap Retry or use Audio Recorder.",
                        errorMessageAr = "خطأ في مزوّد التعرف الصوتي (رمز $errorCode).",
                        errorMessageEn = "Speech provider error (code $errorCode)."
                    )
                } else {
                    SpeechErrorDecision(
                        action = SpeechErrorAction.RESTART_AFTER_DELAY,
                        reason = SpeechErrorReason.NETWORK_OR_PROVIDER_FAILURE,
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
 * Online-first, resilient Arabic Speech-to-Text & Live Quran Recitation Listener.
 *
 * Key repairs:
 * - Queries installed Android `RecognitionService` providers via `PackageManager` and prefers
 *   online-capable providers (e.g., Google Speech Services) over stub/on-device-only recognizers.
 * - Probes real Arabic online & offline support via API 33+ `SpeechRecognizer.checkRecognitionSupport`
 *   and real internet connectivity via `ConnectivityManager`.
 * - Removes `EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE = true` and omits `EXTRA_PREFER_OFFLINE` in online
 *   mode (the primary configuration bugs that triggered `ERROR_LANGUAGE_NOT_SUPPORTED` code 12).
 * - Automatically steps through Arabic locales (`ar-SA`, `ar`, `ar-EG`, `ar-AE`, `ar-JO`, `ar-001`)
 *   and alternate installed recognition providers if Error 12 occurs before reporting unsupported.
 * - Never fabricates text or treats empty recognition results as success.
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

    private var candidateProviders: List<RecognitionProviderInfo> = emptyList()
    private var activeProviderIndex: Int = 0
    private var candidateLocales: List<String> = SpeechErrorRecoveryPolicy.ARABIC_LOCALE_CANDIDATES
    private var activeLocaleIndex: Int = 0

    // Optional deterministic hooks used strictly by JVM/Robolectric unit tests
    private var testNetworkOverride: (() -> Boolean)? = null
    private var testProvidersOverride: (() -> List<RecognitionProviderInfo>)? = null
    private var testServiceAvailableOverride: (() -> Boolean)? = null
    private var testPermissionOverride: (() -> Boolean)? = null
    private var testOfflineArabicInstalledOverride: Boolean? = null

    private var cachedCapabilities: ArabicSttCapabilities = ArabicSttCapabilities()

    private val accumulatedSegments = mutableListOf<String>()

    private val _state = MutableStateFlow(LiveRecitationState())
    val state: StateFlow<LiveRecitationState> = _state.asStateFlow()

    fun hasMicrophonePermission(): Boolean {
        testPermissionOverride?.let { return it() }
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun isSpeechServiceAvailable(): Boolean {
        testServiceAvailableOverride?.let { return it() }
        val apiAvail = runCatching { SpeechRecognizer.isRecognitionAvailable(appContext) }.getOrDefault(false)
        if (apiAvail) return true
        return queryInstalledRecognitionProviders().isNotEmpty()
    }

    fun isInternetConnected(): Boolean {
        testNetworkOverride?.let { return it() }
        return runCatching {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return@runCatching false
            val network = cm.activeNetwork ?: return@runCatching false
            val caps = cm.getNetworkCapabilities(network) ?: return@runCatching false
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (!hasInternet) return@runCatching false
            val isRobolectric = Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
            if (isRobolectric) true else caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }.getOrDefault(false)
    }

    /**
     * Queries Android `PackageManager` for all installed `RecognitionService` providers and ranks
     * known online-capable Arabic providers (Google Speech Recognition & Synthesis, Google App) first.
     */
    fun queryInstalledRecognitionProviders(): List<RecognitionProviderInfo> {
        testProvidersOverride?.let { return it() }
        return runCatching {
            val pm = appContext.packageManager ?: return@runCatching emptyList()
            val queryIntent = Intent(RecognitionService.SERVICE_INTERFACE)
            @Suppress("DEPRECATION")
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentServices(queryIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                pm.queryIntentServices(queryIntent, 0)
            }
            resolved.mapNotNull { info ->
                val svc = info.serviceInfo ?: return@mapNotNull null
                val pkg = svc.packageName ?: return@mapNotNull null
                val cls = svc.name ?: return@mapNotNull null
                val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg).ifBlank { pkg }
                val isPreferredOnline = PREFERRED_ONLINE_PACKAGES.any { pkg.equals(it, ignoreCase = true) }
                RecognitionProviderInfo(
                    packageName = pkg,
                    serviceName = cls,
                    label = label,
                    isPreferredOnlineProvider = isPreferredOnline
                )
            }.sortedWith(
                compareByDescending<RecognitionProviderInfo> { it.isPreferredOnlineProvider }
                    .thenBy { providerPriorityRank(it.packageName) }
            )
        }.getOrDefault(emptyList())
    }

    /**
     * Inspects current recognition service availability, installed providers, and internet connectivity.
     */
    fun inspectCapabilities(): ArabicSttCapabilities {
        val providers = queryInstalledRecognitionProviders()
        val serviceAvail = isSpeechServiceAvailable()
        val internet = isInternetConnected()
        val selected = providers.firstOrNull()
        val offlineInstalled = testOfflineArabicInstalledOverride ?: cachedCapabilities.isArabicOfflineInstalled
        val caps = cachedCapabilities.copy(
            serviceAvailable = serviceAvail,
            isInternetAvailable = internet,
            selectedProvider = selected,
            installedProviders = providers,
            isArabicOfflineInstalled = offlineInstalled
        )
        cachedCapabilities = caps
        return caps
    }

    fun setPreferOffline(preferOffline: Boolean) {
        preferOfflineMode = preferOffline
        sessionOfflineFlag = preferOffline
        _state.value = _state.value.copy(
            isOfflinePreferred = preferOffline,
            usingOnlineMode = !preferOffline
        )
    }

    /**
     * Builds an Android `RecognizerIntent` properly configured for Arabic speech recognition.
     *
     * CRITICAL FIX:
     * - Omits `EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE` (which caused Error 12 on Google/AOSP services).
     * - Omits `EXTRA_PREFER_OFFLINE` in online mode so online providers route to cloud Arabic STT.
     * - Sets `EXTRA_CALLING_PACKAGE`, `EXTRA_LANGUAGE`, `EXTRA_LANGUAGE_PREFERENCE`, and `EXTRA_PARTIAL_RESULTS`.
     */
    fun buildRecognizerIntent(
        locale: String = activeLocale(),
        preferOffline: Boolean = sessionOfflineFlag
    ): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
            if (preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
    }

    /**
     * Builds a system RecognizerIntent for voice dictation via ActivityResultLauncher.
     * Uses online Arabic recognition by default and never passes Error-12-triggering strict flags.
     */
    fun buildSystemSpeechIntent(promptAr: String = "اتلُ الآيات الآن بوضوح"): Intent {
        val locale = activeLocale()
        val knownOfflineInstalled = testOfflineArabicInstalledOverride ?: cachedCapabilities.isArabicOfflineInstalled
        val offlineOnly = preferOfflineMode && (knownOfflineInstalled != false)
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            putExtra(RecognizerIntent.EXTRA_PROMPT, promptAr)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            if (offlineOnly) {
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
            errorReason = SpeechErrorReason.NONE,
            hasReceivedEmptyResult = false,
            isRateLimited = false,
            errorMessageAr = "",
            errorMessageEn = ""
        )
    }

    /**
     * Injects recognized Arabic text (e.g. from RecognizerIntent Activity fallback or unit tests)
     * directly into the live tracking & accumulated transcript pipeline.
     * Rejects empty/whitespace strings so empty transcripts are never reported as successful.
     */
    fun injectRecognizedText(text: String, isFinal: Boolean = true) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) {
            handleEmptyRecognitionResult()
            return
        }
        handleHypotheses(listOf(cleaned), isFinal = isFinal)
    }

    fun retryListening(resetTranscript: Boolean = false) {
        startListening(resetTranscript = resetTranscript, initialDelayMs = 0L)
    }

    fun startListening(resetTranscript: Boolean = false, initialDelayMs: Long = 0L) {
        val internetAvailable = isInternetConnected()
        val providers = queryInstalledRecognitionProviders()
        val activeProviderLabel = providers.firstOrNull()?.label ?: "Android SpeechRecognizer"

        if (!hasMicrophonePermission()) {
            _state.value = _state.value.copy(
                status = LiveRecitationStatus.ERROR,
                errorReason = SpeechErrorReason.PERMISSION_DENIED,
                isInternetAvailable = internetAvailable,
                activeProviderName = activeProviderLabel,
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
                errorReason = SpeechErrorReason.SERVICE_UNAVAILABLE,
                isInternetAvailable = internetAvailable,
                activeProviderName = "",
                lastErrorCode = null,
                statusNoteAr = "لا توجد خدمة تعرّف صوتي (RecognitionService) مثبّتة على هذا الجهاز. يمكنك استخدام التسجيل الصوتي لتلاوتك كبديل.",
                statusNoteEn = "No Android speech recognition service is installed on this device. Use the Audio Recorder fallback to record and review your recitation.",
                errorMessageAr = "خدمة التعرف الصوتي غير متوفرة على الجهاز.",
                errorMessageEn = "Speech recognition service unavailable on this device."
            )
            return
        }

        // Offline-only requested or internet disconnected: verify offline Arabic model before claiming offline STT works!
        val knownOfflineInstalled = testOfflineArabicInstalledOverride ?: cachedCapabilities.isArabicOfflineInstalled
        if (!internetAvailable && knownOfflineInstalled == false) {
            _state.value = _state.value.copy(
                status = LiveRecitationStatus.ERROR,
                errorReason = SpeechErrorReason.NO_INTERNET,
                isInternetAvailable = false,
                usingOnlineMode = false,
                isArabicOfflineModelInstalled = false,
                activeProviderName = activeProviderLabel,
                statusNoteAr = "لا يوجد اتصال بالإنترنت وحزمة اللغة العربية دون إنترنت غير مثبّتة. يمكنك استخدام التسجيل الصوتي لتلاوتك دون إنترنت.",
                statusNoteEn = "Internet is unavailable and no offline Arabic speech model is installed. Use the Audio Recorder fallback to record your recitation.",
                errorMessageAr = "الاتصال بالإنترنت غير متوفر وحزمة العربية دون إنترنت غير مثبّتة.",
                errorMessageEn = "No internet connection and offline Arabic pack missing."
            )
            return
        }

        if (resetTranscript) {
            accumulatedSegments.clear()
            lastSpeechEndMs = 0L
        }

        mainHandler.removeCallbacksAndMessages(null)
        candidateProviders = providers
        activeProviderIndex = 0
        candidateLocales = buildOrderedLocaleCandidates()
        activeLocaleIndex = 0
        shouldKeepListening = true
        isPaused = false
        consecutiveErrors = 0
        consecutiveSilenceTimeouts = 0
        consecutiveRateLimits = 0
        // Prefer online when internet is available unless user explicitly requested offline AND offline pack isn't known missing
        sessionOfflineFlag = when {
            !internetAvailable -> true
            preferOfflineMode && knownOfflineInstalled != false -> true
            else -> false
        }
        trackerEngine.resetTrackingState()

        val currentLocale = activeLocale()
        val providerName = activeProvider()?.label ?: "Android SpeechRecognizer"
        _state.value = _state.value.copy(
            status = LiveRecitationStatus.LISTENING,
            errorReason = SpeechErrorReason.NONE,
            isOfflinePreferred = preferOfflineMode,
            isOfflineFallbackUsed = false,
            usingOnlineMode = !sessionOfflineFlag,
            isInternetAvailable = internetAvailable,
            activeProviderName = providerName,
            activeLocale = currentLocale,
            isArabicOfflineModelInstalled = knownOfflineInstalled,
            supportedArabicLocales = cachedCapabilities.supportedOnlineArabicLocales,
            hasReceivedEmptyResult = false,
            lastErrorCode = null,
            isRateLimited = false,
            heardText = if (resetTranscript) "" else _state.value.heardText,
            accumulatedTranscript = if (resetTranscript) "" else _state.value.accumulatedTranscript,
            pauseCount = if (resetTranscript) 0 else _state.value.pauseCount,
            longestPauseMs = if (resetTranscript) 0L else _state.value.longestPauseMs,
            errorMessageAr = "",
            errorMessageEn = "",
            statusNoteAr = if (sessionOfflineFlag) {
                "يستمع الآن للتلاوة العربية (وضع دون إنترنت · $currentLocale)..."
            } else {
                "يستمع الآن للتلاوة العربية عبر الإنترنت ($providerName · $currentLocale)..."
            },
            statusNoteEn = if (sessionOfflineFlag) {
                "Listening to Arabic recitation (offline model · $currentLocale)..."
            } else {
                "Listening to Arabic recitation online ($providerName · $currentLocale)..."
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
            errorReason = SpeechErrorReason.NONE,
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
            errorReason = SpeechErrorReason.NONE,
            rmsLevel = 0f,
            heardText = if (clearTranscript) "" else _state.value.heardText,
            accumulatedTranscript = if (clearTranscript) "" else _state.value.accumulatedTranscript,
            crossSurahJumpTarget = null,
            isRateLimited = false,
            statusNoteAr = if (clearTranscript) "" else _state.value.statusNoteAr,
            statusNoteEn = if (clearTranscript) "" else _state.value.statusNoteEn
        )
    }

    private fun activeProvider(): RecognitionProviderInfo? =
        candidateProviders.getOrNull(activeProviderIndex)

    private fun activeLocale(): String =
        candidateLocales.getOrElse(activeLocaleIndex) { "ar-SA" }

    private fun hasMoreLocaleOrProviderFallbacks(): Boolean {
        val moreLocales = activeLocaleIndex + 1 < candidateLocales.size
        val moreProviders = activeProviderIndex + 1 < candidateProviders.size
        return moreLocales || moreProviders
    }

    private fun advanceLocaleOrProviderFallback(): Boolean {
        if (activeLocaleIndex + 1 < candidateLocales.size) {
            activeLocaleIndex += 1
            return true
        }
        if (activeProviderIndex + 1 < candidateProviders.size) {
            activeProviderIndex += 1
            activeLocaleIndex = 0
            return true
        }
        return false
    }

    private fun buildOrderedLocaleCandidates(): List<String> {
        val discovered = (cachedCapabilities.supportedOnlineArabicLocales + cachedCapabilities.installedOfflineArabicLocales)
            .filter { isArabicLocaleTag(it) }
        return (discovered + SpeechErrorRecoveryPolicy.ARABIC_LOCALE_CANDIDATES)
            .distinctBy { it.lowercase() }
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
                val chosenProvider = activeProvider()
                val created = if (chosenProvider != null) {
                    runCatching {
                        SpeechRecognizer.createSpeechRecognizer(appContext, chosenProvider.componentName())
                    }.getOrNull()
                } else {
                    null
                } ?: SpeechRecognizer.createSpeechRecognizer(appContext)
                speechRecognizer = created.apply {
                    setRecognitionListener(listener)
                }
                probeRecognitionSupportOnApi33(created)
            }

            val locale = activeLocale()
            val providerName = activeProvider()?.label ?: "Android SpeechRecognizer"
            val intent = buildRecognizerIntent(locale = locale, preferOffline = sessionOfflineFlag)
            _state.value = _state.value.copy(
                activeLocale = locale,
                activeProviderName = providerName,
                usingOnlineMode = !sessionOfflineFlag
            )
            isSessionRunning = true
            lastSessionStartMs = System.currentTimeMillis()
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isSessionRunning = false
            shouldKeepListening = false
            _state.value = _state.value.copy(
                status = LiveRecitationStatus.ERROR,
                errorReason = SpeechErrorReason.NETWORK_OR_PROVIDER_FAILURE,
                statusNoteAr = "تعذّر تشغيل خدمة التعرف الصوتي على الجهاز.",
                statusNoteEn = "Unable to start speech recognition service.",
                errorMessageAr = "خطأ في تشغيل SpeechRecognizer (${e.localizedMessage ?: "غير متاح"}).",
                errorMessageEn = "SpeechRecognizer failed to start (${e.localizedMessage ?: "unavailable"})."
            )
        }
    }

    /**
     * Uses Android 13+ (API 33) `SpeechRecognizer.checkRecognitionSupport` asynchronously to discover
     * genuine online & offline Arabic language support without blocking or fabricating capability flags.
     */
    private fun probeRecognitionSupportOnApi33(recognizer: SpeechRecognizer) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        runCatching {
            val probeIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            }
            recognizer.checkRecognitionSupport(
                probeIntent,
                { runnable -> mainHandler.post(runnable) },
                object : RecognitionSupportCallback {
                    override fun onSupportResult(recognitionSupport: RecognitionSupport) {
                        val onlineAr = recognitionSupport.onlineLanguages.filter { isArabicLocaleTag(it) }
                        val installedOfflineAr = recognitionSupport.installedOnDeviceLanguages.filter { isArabicLocaleTag(it) }
                        val pendingOfflineAr = recognitionSupport.pendingOnDeviceLanguages.filter { isArabicLocaleTag(it) }
                        val allSupportedAr = (onlineAr + installedOfflineAr).distinctBy { it.lowercase() }

                        cachedCapabilities = cachedCapabilities.copy(
                            supportedOnlineArabicLocales = onlineAr,
                            installedOfflineArabicLocales = installedOfflineAr,
                            pendingOfflineArabicLocales = pendingOfflineAr,
                            isArabicSupportedOnline = onlineAr.isNotEmpty(),
                            isArabicOfflineInstalled = installedOfflineAr.isNotEmpty()
                        )
                        if (allSupportedAr.isNotEmpty()) {
                            candidateLocales = (allSupportedAr + SpeechErrorRecoveryPolicy.ARABIC_LOCALE_CANDIDATES)
                                .distinctBy { it.lowercase() }
                        }
                        _state.value = _state.value.copy(
                            supportedArabicLocales = allSupportedAr,
                            isArabicOfflineModelInstalled = installedOfflineAr.isNotEmpty()
                        )
                    }

                    override fun onError(error: Int) {
                        // Some providers do not implement checkRecognitionSupport; rely on live recognition callbacks.
                    }
                }
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

    private fun handleEmptyRecognitionResult() {
        _state.value = _state.value.copy(
            status = if (shouldKeepListening) LiveRecitationStatus.UNCERTAIN else _state.value.status,
            hasReceivedEmptyResult = true,
            statusNoteAr = "لم يتم التقاط نص عربي واضح — يرجى التلاوة بصوت واضح أو إعادة المحاولة.",
            statusNoteEn = "No clear Arabic words were recognized — please recite clearly and retry."
        )
    }

    private fun handleHypotheses(hypotheses: List<String>, isFinal: Boolean) {
        val bestRaw = hypotheses.firstOrNull { it.isNotBlank() }
        if (bestRaw == null) {
            if (isFinal) handleEmptyRecognitionResult()
            return
        }
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
                errorReason = SpeechErrorReason.NONE,
                hasReceivedEmptyResult = false,
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
                errorReason = SpeechErrorReason.NONE,
                hasReceivedEmptyResult = false,
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
                errorReason = SpeechErrorReason.NONE,
                hasReceivedEmptyResult = false,
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

    private fun handleRecognitionError(error: Int) {
        isSessionRunning = false
        val internetAvailable = isInternetConnected()
        val providerName = activeProvider()?.label ?: _state.value.activeProviderName.ifBlank { "Android RecognitionService" }
        val offlineInstalled = testOfflineArabicInstalledOverride ?: cachedCapabilities.isArabicOfflineInstalled
        _state.value = _state.value.copy(
            rmsLevel = 0f,
            lastErrorCode = error,
            isInternetAvailable = internetAvailable,
            activeProviderName = providerName
        )

        val decision = SpeechErrorRecoveryPolicy.decide(
            errorCode = error,
            consecutiveSilence = consecutiveSilenceTimeouts,
            consecutiveRateLimits = consecutiveRateLimits,
            consecutiveOtherErrors = consecutiveErrors,
            sessionOfflineFlag = sessionOfflineFlag,
            isInternetAvailable = internetAvailable,
            hasMoreLocaleOrProviderFallbacks = hasMoreLocaleOrProviderFallbacks(),
            isOfflineArabicModelInstalled = offlineInstalled,
            activeProviderName = providerName,
            activeLocale = activeLocale()
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
            SpeechErrorAction.TRY_NEXT_LOCALE_OR_PROVIDER -> {
                if (advanceLocaleOrProviderFallback()) {
                    val nextLocale = activeLocale()
                    val nextProvider = activeProvider()?.label ?: providerName
                    _state.value = _state.value.copy(
                        errorReason = SpeechErrorReason.NONE,
                        activeLocale = nextLocale,
                        activeProviderName = nextProvider,
                        statusNoteAr = "تجربة اللغة العربية ($nextLocale) عبر $nextProvider...",
                        statusNoteEn = "Trying Arabic ($nextLocale) via $nextProvider..."
                    )
                    scheduleRestart(decision.delayMs)
                } else {
                    shouldKeepListening = false
                    isPaused = false
                    mainHandler.removeCallbacksAndMessages(null)
                    _state.value = _state.value.copy(
                        status = LiveRecitationStatus.ERROR,
                        errorReason = SpeechErrorReason.ARABIC_UNSUPPORTED,
                        statusNoteAr = "مزوّد التعرف الصوتي المثبّت ($providerName) لا يدعم اللغة العربية. يمكنك استخدام التسجيل الصوتي كبديل.",
                        statusNoteEn = "Installed recognition provider ($providerName) does not support Arabic. Use the Audio Recorder fallback.",
                        errorMessageAr = "اللغة العربية غير مدعومة لدى المزوّد (رمز $error).",
                        errorMessageEn = "Arabic language unsupported by $providerName (code $error)."
                    )
                }
            }

            SpeechErrorAction.RESTART_AFTER_DELAY -> {
                _state.value = _state.value.copy(
                    errorReason = decision.reason,
                    isRateLimited = decision.isRateLimited,
                    statusNoteAr = decision.statusNoteAr.ifBlank { _state.value.statusNoteAr },
                    statusNoteEn = decision.statusNoteEn.ifBlank { _state.value.statusNoteEn }
                )
                scheduleRestart(decision.delayMs)
            }

            SpeechErrorAction.SWITCH_TO_HYBRID_AND_RESTART -> {
                sessionOfflineFlag = false
                consecutiveErrors = 0
                activeLocaleIndex = 0
                _state.value = _state.value.copy(
                    errorReason = decision.reason,
                    isOfflineFallbackUsed = true,
                    usingOnlineMode = true,
                    isArabicOfflineModelInstalled = false,
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
                    errorReason = decision.reason,
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
                    errorReason = decision.reason,
                    isRateLimited = decision.isRateLimited,
                    statusNoteAr = decision.statusNoteAr,
                    statusNoteEn = decision.statusNoteEn,
                    errorMessageAr = decision.errorMessageAr,
                    errorMessageEn = decision.errorMessageEn
                )
            }
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
            handleRecognitionError(error)
        }

        override fun onResults(results: Bundle?) {
            isSessionRunning = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            val nonBlank = matches.filter { it.isNotBlank() }
            if (nonBlank.isNotEmpty()) {
                handleHypotheses(nonBlank, isFinal = true)
            } else {
                handleEmptyRecognitionResult()
            }
            // Enforce polite spacing between recognition sessions to prevent Error 10 throttling
            scheduleRestart(SpeechErrorRecoveryPolicy.MIN_SESSION_SPACING_MS)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            val nonBlank = matches.filter { it.isNotBlank() }
            if (nonBlank.isNotEmpty()) {
                handleHypotheses(nonBlank, isFinal = false)
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

    // ---- Internal Test Hooks (used strictly by Robolectric JVM tests) ----

    internal fun setTestOverrides(
        networkAvailable: (() -> Boolean)? = null,
        providers: (() -> List<RecognitionProviderInfo>)? = null,
        serviceAvailable: (() -> Boolean)? = null,
        micPermissionGranted: (() -> Boolean)? = null,
        offlineArabicInstalled: Boolean? = null
    ) {
        testNetworkOverride = networkAvailable
        testProvidersOverride = providers
        testServiceAvailableOverride = serviceAvailable
        testPermissionOverride = micPermissionGranted
        testOfflineArabicInstalledOverride = offlineArabicInstalled
    }

    internal fun simulateRecognizerErrorForTest(errorCode: Int) {
        handleRecognitionError(errorCode)
    }

    internal fun simulateRecognizerResultsForTest(matches: List<String>, isFinal: Boolean = true) {
        val bundle = Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, ArrayList(matches))
        }
        if (isFinal) listener.onResults(bundle) else listener.onPartialResults(bundle)
    }

    companion object {
        private val PREFERRED_ONLINE_PACKAGES = listOf(
            "com.google.android.tts",
            "com.google.android.googlequicksearchbox",
            "com.google.android.as"
        )

        private fun providerPriorityRank(packageName: String): Int {
            val idx = PREFERRED_ONLINE_PACKAGES.indexOfFirst { it.equals(packageName, ignoreCase = true) }
            return if (idx >= 0) idx else 100
        }

        fun isArabicLocaleTag(tag: String): Boolean {
            val lower = tag.trim().lowercase()
            return lower == "ar" || lower.startsWith("ar-") || lower.startsWith("ar_")
        }
    }
}
