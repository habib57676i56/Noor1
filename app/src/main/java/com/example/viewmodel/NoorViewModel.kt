package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoorApplication
import com.example.audio.AdhanAudioPlayer
import com.example.audio.AdhanCatalog
import com.example.audio.AdhanDownloadState
import com.example.audio.AdhanImportResult
import com.example.audio.AdhanPlaybackService
import com.example.audio.AdhanVoiceOption
import com.example.audio.AudioPlaybackState
import com.example.audio.CustomAdhanRecord
import com.example.audio.DhikrTtsHelper
import com.example.audio.LiveRecitationState
import com.example.audio.QuranAudioPlayer
import com.example.audio.QuranAudioService
import com.example.audio.QuranLiveRecitationListener
import com.example.data.cloud.AdCardDoc
import com.example.data.cloud.AdminMessageDoc
import com.example.data.cloud.AdminRole
import com.example.data.cloud.AppAnnouncementDoc
import com.example.data.cloud.CmsContentCategory
import com.example.data.cloud.CmsContentDoc
import com.example.data.cloud.FirebaseAuthHelper
import com.example.data.cloud.GlobalAppConfigDoc
import com.example.data.cloud.NoorCloudRepository
import com.example.data.cloud.SupportTicketDoc
import com.example.data.cloud.UserAccountDoc
import com.example.data.repository.EcosystemRepository
import com.example.data.local.catalog.AdhkarCatalog
import com.example.data.local.catalog.AudioLibraryCatalog
import com.example.data.local.catalog.DailyAyahItem
import com.example.data.local.catalog.MorningEveningAdhkarCatalog
import com.example.data.local.catalog.SurahCatalog
import com.example.data.local.db.AdhkarCompletionHistoryEntity
import com.example.data.local.db.AdhkarProgressEntity
import com.example.data.local.db.AudioPlaylistEntity
import com.example.data.local.db.AudioPlaylistItemEntity
import com.example.data.local.db.AudioRecentEntity
import com.example.data.local.db.CustomDhikrEntity
import com.example.data.local.db.CustomRoutineEntity
import com.example.data.local.db.DailyActivityEntity
import com.example.data.local.db.QuranBookmarkEntity
import com.example.data.local.db.QuranVerseCacheEntity
import com.example.data.local.db.TasbihSessionEntity
import com.example.data.local.catalog.MadinahMushafPageCatalog
import com.example.data.local.db.HifzPlanEntity
import com.example.data.local.prefs.UserPreferences
import com.example.data.quran.AyahWordTimings
import com.example.data.quran.CustomReciterItem
import com.example.data.quran.CustomReciterRepository
import com.example.data.quran.MushafCoordinateRepository
import com.example.data.quran.MushafHighlightColor
import com.example.data.quran.MushafHighlightStyle
import com.example.data.quran.MushafPageImageManager
import com.example.data.quran.QuranLinguisticRepository
import com.example.data.quran.QuranWordTimingRepository
import com.example.domain.prayer.PrayerTimeGate
import com.example.data.remote.MosqueSearchResult
import com.example.data.repository.NoorRepository
import com.example.data.repository.SurahLoadResult
import com.example.domain.calendar.HijriCalendarEngine
import com.example.domain.model.AchievementItem
import com.example.domain.model.AppLanguage
import com.example.domain.model.AsrMadhhab
import com.example.domain.model.AudioTrackItem
import com.example.domain.model.AuthenticHadith
import com.example.domain.model.CalculationMethod
import com.example.domain.model.CityLocation
import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.DhikrItem
import com.example.domain.model.HijriDateModel
import com.example.domain.model.HomeDashboardConfig
import com.example.domain.model.IqamahDelays
import com.example.domain.model.NearbyMosque
import com.example.domain.model.NextPrayerInfo
import com.example.domain.model.PrayerName
import com.example.domain.model.QuranDisplayMode
import com.example.domain.model.QuranReadingTheme
import com.example.domain.model.QuranReciter
import com.example.domain.model.QuranVerse
import com.example.domain.model.ThemeMode
import com.example.domain.prayer.PrayerTimeCalculator
import com.example.location.LocationHelper
import com.example.notifications.NoorAlarmScheduler
import com.example.notifications.NoorNotificationHelper
import com.example.notifications.PrayerNotificationController
import com.example.shortcuts.NoorShortcutManager
import com.example.widgets.PrayerTimesWidgetProvider
import com.example.widgets.QuranPlayerWidgetProvider
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import java.util.Locale
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicLong

data class PrayerDashboardState(
    val todaySchedule: DailyPrayerSchedule,
    val weeklySchedules: List<DailyPrayerSchedule>,
    val monthlySchedules: List<DailyPrayerSchedule>,
    val nextPrayerInfo: NextPrayerInfo,
    val hijriToday: HijriDateModel,
    val gregorianCal: Calendar,
    val dailyDhikr: DhikrItem,
    val dailyAyah: DailyAyahItem,
    val dailyHadith: AuthenticHadith
)

sealed class SurahReaderState {
    data object Loading : SurahReaderState()
    data class Ready(val surahNumber: Int, val verses: List<QuranVerse>, val isOfflineCached: Boolean) : SurahReaderState()
    data class Error(val surahNumber: Int, val messageAr: String, val messageEn: String) : SurahReaderState()
}

sealed class NearbyMosquesUiState {
    data object Idle : NearbyMosquesUiState()
    data object Loading : NearbyMosquesUiState()
    data class Ready(val mosques: List<NearbyMosque>, val radiusMeters: Int) : NearbyMosquesUiState()
    data class Error(val messageAr: String, val messageEn: String) : NearbyMosquesUiState()
}

data class StatsSummary(
    val currentStreakDays: Int = 0,
    val allTimeTasbih: Int = 0,
    val weekTasbih: Int = 0,
    val totalTasbihSessions: Int = 0,
    val totalCompletedAdhkar: Int = 0,
    val totalQuranVersesRead: Int = 0,
    val totalPrayersLogged: Int = 0,
    val recentActivities: List<DailyActivityEntity> = emptyList(),
    val achievements: List<AchievementItem> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class NoorViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: NoorRepository = (application as NoorApplication).repository
    private val ecosystemRepo: EcosystemRepository = (application as NoorApplication).ecosystemRepository
    private val cloudRepo: NoorCloudRepository = (application as NoorApplication).cloudRepository
    private val quranAudioPlayer: QuranAudioPlayer = (application as NoorApplication).quranAudioPlayer
    private val customReciterRepo: CustomReciterRepository = (application as NoorApplication).customReciterRepository

    // Single-session playback request serialization: prevents rapid taps or slow async loads
    // from launching duplicate or stale Sheikh audio sessions.
    private var activePlaybackRequestJob: Job? = null
    private val playbackRequestToken = AtomicLong(0L)
    private var lastPlaySignature: String? = null
    private var lastPlayRequestAtMs: Long = 0L

    private fun cancelPendingPlaybackRequests(): Long {
        activePlaybackRequestJob?.cancel()
        activePlaybackRequestJob = null
        return playbackRequestToken.incrementAndGet()
    }
    private val adhanAudioPlayer: AdhanAudioPlayer = (application as NoorApplication).adhanAudioPlayer
    val mushafPageImageManager: MushafPageImageManager = (application as NoorApplication).mushafPageImageManager
    val mushafCoordinateRepository: MushafCoordinateRepository = (application as NoorApplication).mushafCoordinateRepository
    val quranWordTimingRepository: QuranWordTimingRepository = (application as NoorApplication).quranWordTimingRepository
    val quranLinguisticRepository: QuranLinguisticRepository = (application as NoorApplication).quranLinguisticRepository
    private val liveRecitationListener = QuranLiveRecitationListener(application)
    private val dhikrTtsHelper = DhikrTtsHelper(application)
    private val notifPrefs = application.getSharedPreferences("noor_notif_prefs", android.content.Context.MODE_PRIVATE)
    private val mushafPrefs = application.getSharedPreferences("noor_mushaf_prefs", android.content.Context.MODE_PRIVATE)

    private val _mushafHighlightStyle = MutableStateFlow(
        MushafHighlightStyle(
            colorPreset = MushafHighlightColor.fromKey(mushafPrefs.getString("highlight_color", "gold")),
            alpha = mushafPrefs.getFloat("highlight_alpha", 0.28f).coerceIn(0.15f, 0.50f),
            pulseAnimated = mushafPrefs.getBoolean("highlight_pulse", true)
        )
    )
    val mushafHighlightStyle: StateFlow<MushafHighlightStyle> = _mushafHighlightStyle.asStateFlow()

    // Verified Quran.com v4 millisecond word segments for active (reciter, surah) — strictly NO fake timers!
    private val _activeSurahWordTimings = MutableStateFlow<Pair<String, Map<Int, AyahWordTimings>>?>(null)

    private val _nonEssentialNotificationsEnabled = MutableStateFlow(
        notifPrefs.getBoolean("non_essential_notifications", true)
    )
    val nonEssentialNotificationsEnabled: StateFlow<Boolean> = _nonEssentialNotificationsEnabled.asStateFlow()

    val liveRecitationState: StateFlow<LiveRecitationState> = liveRecitationListener.state
    val previewingAdhanVoiceId: StateFlow<String?> = adhanAudioPlayer.playingVoiceId
    val adhanPreviewBuffering: StateFlow<Boolean> = adhanAudioPlayer.isBuffering

    private val _adhanDownloadStates = MutableStateFlow<Map<String, AdhanDownloadState>>(
        AdhanCatalog.scanOfflineDownloadStates(application)
    )
    val adhanDownloadStates: StateFlow<Map<String, AdhanDownloadState>> = _adhanDownloadStates.asStateFlow()

    private val _pendingShortcutRoute = MutableStateFlow<String?>(null)
    val pendingShortcutRoute: StateFlow<String?> = _pendingShortcutRoute.asStateFlow()

    // --- NOOR 3.0 Auth, RBAC, Live Cloud Config, Announcements & CMS Streams ---
    val currentUser: StateFlow<FirebaseUser?> = FirebaseAuthHelper.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val globalAppConfig: StateFlow<GlobalAppConfigDoc> = currentUser
        .flatMapLatest { user ->
            if (user != null) cloudRepo.observeGlobalAppConfig() else flowOf(GlobalAppConfigDoc())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GlobalAppConfigDoc())

    val activeAnnouncements: StateFlow<List<AppAnnouncementDoc>> = currentUser
        .flatMapLatest { user ->
            if (user != null) cloudRepo.observeActiveAnnouncements() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val publishedCmsContent: StateFlow<List<CmsContentDoc>> = currentUser
        .flatMapLatest { user ->
            if (user != null) cloudRepo.observePublishedCmsContent() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mySupportTickets: StateFlow<List<SupportTicketDoc>> = currentUser
        .flatMapLatest { user ->
            if (user != null) cloudRepo.observeMySupportTickets(user.uid) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserProfile: StateFlow<UserAccountDoc?> = currentUser
        .flatMapLatest { user ->
            if (user != null) cloudRepo.observeCurrentUserProfile(user.uid) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val cloudCustomReciters = currentUser
        .flatMapLatest { user ->
            if (user != null) cloudRepo.observeCustomReciters() else flowOf(emptyList())
        }

    private val _selectedMushafCustomReciterId = MutableStateFlow<String?>(null)
    val selectedMushafCustomReciterId: StateFlow<String?> = _selectedMushafCustomReciterId.asStateFlow()

    val customReciters: StateFlow<List<CustomReciterItem>> = combine(
        customReciterRepo.reciters,
        cloudCustomReciters
    ) { localList, cloudDocs ->
        val cloudItems = cloudDocs.map { doc ->
            CustomReciterItem(
                id = doc.id,
                nameAr = doc.nameAr,
                nameEn = doc.nameEn,
                styleAr = doc.styleAr,
                styleEn = doc.styleEn,
                surahTitleAr = doc.surahTitleAr,
                surahTitleEn = doc.surahTitleEn,
                surahNumber = doc.surahNumber,
                localAudioPath = "",
                audioUrl = doc.audioUrl,
                fileName = doc.fileName,
                addedByEmail = doc.addedByEmail,
                createdAtMillis = (doc.createdAt?.seconds ?: 0L) * 1000L
            )
        }
        customReciterRepo.mergeWithCloudReciters(cloudItems)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val myAdminRecord = currentUser
        .flatMapLatest { user ->
            if (user != null) cloudRepo.observeMyAdminRecord(user.uid) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val resolvedAdminRole: StateFlow<AdminRole> = combine(currentUser, myAdminRecord, currentUserProfile) { user, adminDoc, profile ->
        when {
            FirebaseAuthHelper.isBootstrappedOwner(user) -> AdminRole.SUPER_ADMIN
            adminDoc != null && adminDoc.active -> AdminRole.fromKey(adminDoc.role)
            profile != null && profile.status == "ACTIVE" -> AdminRole.fromKey(profile.role)
            else -> AdminRole.USER
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdminRole.USER)

    private val cloudEnabledAds = currentUser
        .flatMapLatest { user -> if (user != null) cloudRepo.observeEnabledAdCards() else flowOf(emptyList()) }

    val activeAdCards: StateFlow<List<AdCardDoc>> = combine(
        cloudEnabledAds,
        ecosystemRepo.enabledAdCards,
        globalAppConfig
    ) { cloudAds, localAds, config ->
        if (!config.featureAdsEnabled) return@combine emptyList()
        val now = System.currentTimeMillis()
        val merged = LinkedHashMap<String, AdCardDoc>()
        localAds.forEach { e ->
            merged[e.id] = AdCardDoc(
                id = e.id,
                titleAr = e.titleAr,
                titleEn = e.titleEn,
                descriptionAr = e.descriptionAr,
                descriptionEn = e.descriptionEn,
                imageUrl = e.imageUriOrUrl,
                destinationUrl = e.destinationUrl,
                placement = e.placement,
                enabled = e.enabled,
                startAtMillis = e.startAtMillis,
                endAtMillis = e.endAtMillis,
                displayOrder = e.displayOrder,
                impressionsCount = e.impressionsCount,
                clicksCount = e.clicksCount,
                createdBy = e.createdBy
            )
        }
        cloudAds.forEach { c ->
            val existing = merged[c.id]
            merged[c.id] = if (existing != null && c.imageUrl.isBlank() && existing.imageUrl.isNotBlank()) {
                c.copy(imageUrl = existing.imageUrl)
            } else {
                c
            }
        }
        merged.values
            .filter { it.isCurrentlyActive(now) }
            .sortedWith(compareBy<AdCardDoc> { it.displayOrder }.thenBy { it.titleAr })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val cloudBroadcastMessages = currentUser
        .flatMapLatest { user -> if (user != null) cloudRepo.observeBroadcastAndGroupMessages() else flowOf(emptyList()) }

    private val cloudDirectMessages = currentUser
        .flatMapLatest { user -> if (user != null) cloudRepo.observeDirectMessagesForUser(user.uid) else flowOf(emptyList()) }

    val userAdminMessages: StateFlow<List<AdminMessageDoc>> = combine(
        cloudBroadcastMessages,
        cloudDirectMessages,
        ecosystemRepo.adminMessages,
        currentUser,
        _nonEssentialNotificationsEnabled
    ) { broadcasts, directs, localMsgs, user, allowNonEssential ->
        val now = System.currentTimeMillis()
        val merged = LinkedHashMap<String, AdminMessageDoc>()
        localMsgs.forEach { e ->
            val targetMatches = e.targetType == "ALL" ||
                e.targetType == "GROUP" ||
                (user != null && (e.targetUserUid == user.uid || e.targetUserEmail.equals(user.email, ignoreCase = true))) ||
                user == null
            if (targetMatches && e.delivered) {
                merged[e.id] = AdminMessageDoc(
                    id = e.id,
                    targetType = e.targetType,
                    targetGroup = e.targetGroup,
                    targetUserUid = e.targetUserUid,
                    targetUserEmail = e.targetUserEmail,
                    titleAr = e.titleAr,
                    titleEn = e.titleEn,
                    bodyAr = e.bodyAr,
                    bodyEn = e.bodyEn,
                    messageType = e.messageType,
                    priority = e.priority,
                    imageUrl = e.imageUriOrUrl,
                    scheduledAtMillis = e.scheduledAtMillis,
                    active = e.delivered,
                    readByUids = if (e.readByCurrentUser) listOf(user?.uid ?: "local_user") else emptyList(),
                    createdBy = e.sentByUid,
                    createdByEmail = e.sentByEmail
                )
            }
        }
        (broadcasts + directs).forEach { c ->
            val existing = merged[c.id]
            val mergedReads = (c.readByUids + (existing?.readByUids ?: emptyList())).distinct()
            merged[c.id] = c.copy(readByUids = mergedReads)
        }
        merged.values.filter { msg ->
            val due = msg.scheduledAtMillis <= 0L || msg.scheduledAtMillis <= now
            val essential = msg.priority in listOf("HIGH", "URGENT") ||
                msg.messageType in listOf("SYSTEM", "MAINTENANCE", "DIRECT")
            due && (allowNonEssential || essential)
        }.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadAdminMessagesCount: StateFlow<Int> = combine(userAdminMessages, currentUser) { msgs, user ->
        val myId = user?.uid ?: "local_user"
        msgs.count { !it.readByUids.contains(myId) && !it.readByUids.contains("local_user") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val morningAdhkarItems: StateFlow<List<DhikrItem>> = publishedCmsContent
        .map { cms ->
            val extras = cms.filter { it.category == CmsContentCategory.MORNING_ADHKAR.key }.map { c ->
                DhikrItem(
                    id = "cms_${c.id}",
                    category = com.example.domain.model.AdhkarCategory.MORNING,
                    textAr = c.bodyAr,
                    targetCount = c.repetitionCount.coerceAtLeast(1),
                    sourceAr = c.sourceAr,
                    sourceEn = c.sourceEn.ifBlank { c.sourceAr },
                    gradingAr = c.gradingAr.ifBlank { "موثّق" },
                    gradingEn = c.gradingEn.ifBlank { "Verified" },
                    meaningAr = c.titleAr,
                    meaningEn = c.bodyEn.ifBlank { c.titleEn },
                    transliteration = c.transliteration
                )
            }
            MorningEveningAdhkarCatalog.morningAdhkar + extras
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MorningEveningAdhkarCatalog.morningAdhkar)

    val eveningAdhkarItems: StateFlow<List<DhikrItem>> = publishedCmsContent
        .map { cms ->
            val extras = cms.filter { it.category == CmsContentCategory.EVENING_ADHKAR.key }.map { c ->
                DhikrItem(
                    id = "cms_${c.id}",
                    category = com.example.domain.model.AdhkarCategory.EVENING,
                    textAr = c.bodyAr,
                    targetCount = c.repetitionCount.coerceAtLeast(1),
                    sourceAr = c.sourceAr,
                    sourceEn = c.sourceEn.ifBlank { c.sourceAr },
                    gradingAr = c.gradingAr.ifBlank { "موثّق" },
                    gradingEn = c.gradingEn.ifBlank { "Verified" },
                    meaningAr = c.titleAr,
                    meaningEn = c.bodyEn.ifBlank { c.titleEn },
                    transliteration = c.transliteration
                )
            }
            MorningEveningAdhkarCatalog.eveningAdhkar + extras
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MorningEveningAdhkarCatalog.eveningAdhkar)

    val morningAdhkarHistory: StateFlow<List<AdhkarCompletionHistoryEntity>> =
        repo.observeAdhkarHistory("morning")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eveningAdhkarHistory: StateFlow<List<AdhkarCompletionHistoryEntity>> =
        repo.observeAdhkarHistory("evening")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences: StateFlow<UserPreferences> = repo.userPreferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    val prayerDashboard: StateFlow<PrayerDashboardState> = combine(preferences, _nowMillis) { prefs, now ->
        val tz = TimeZone.getTimeZone(prefs.timezoneId)
        val todayCal = Calendar.getInstance(tz).apply { timeInMillis = now }
        val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }

        val todaySchedule = PrayerTimeCalculator.calculateForCalendar(
            todayCal, prefs.latitude, prefs.longitude, prefs.calculationMethod, prefs.asrMadhhab, prefs.offsets
        )
        val tomorrowSchedule = PrayerTimeCalculator.calculateForCalendar(
            tomorrowCal, prefs.latitude, prefs.longitude, prefs.calculationMethod, prefs.asrMadhhab, prefs.offsets
        )
        val weekly = PrayerTimeCalculator.calculateRange(
            todayCal, 7, prefs.latitude, prefs.longitude, prefs.calculationMethod, prefs.asrMadhhab, prefs.offsets
        )
        val monthly = PrayerTimeCalculator.calculateRange(
            todayCal, 30, prefs.latitude, prefs.longitude, prefs.calculationMethod, prefs.asrMadhhab, prefs.offsets
        )
        val nextInfo = PrayerTimeCalculator.computeNextPrayer(now, todaySchedule, tomorrowSchedule)
        val hijri = HijriCalendarEngine.gregorianToHijri(
            todayCal.get(Calendar.YEAR),
            todayCal.get(Calendar.MONTH) + 1,
            todayCal.get(Calendar.DAY_OF_MONTH),
            prefs.hijriAdjustmentDays
        )
        val dayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)

        PrayerDashboardState(
            todaySchedule = todaySchedule,
            weeklySchedules = weekly,
            monthlySchedules = monthly,
            nextPrayerInfo = nextInfo,
            hijriToday = hijri,
            gregorianCal = todayCal,
            dailyDhikr = AdhkarCatalog.dailyDhikr(dayOfYear),
            dailyAyah = AdhkarCatalog.dailyAyah(dayOfYear),
            dailyHadith = AdhkarCatalog.dailyHadith(dayOfYear)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = buildInitialDashboard(UserPreferences())
    )

    val todayActivity: StateFlow<DailyActivityEntity?> = repo.observeDailyActivity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tasbihSessions: StateFlow<List<TasbihSessionEntity>> = repo.tasbihSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customDhikrPresets: StateFlow<List<CustomDhikrEntity>> = repo.customDhikrPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTasbihTotal: StateFlow<Int> = repo.observeTodayTasbihTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val favoriteDhikrIds: StateFlow<Set<String>> = repo.favoriteDhikrIds
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val adhkarProgressMap: StateFlow<Map<String, AdhkarProgressEntity>> = repo.observeAdhkarProgress()
        .map { list -> list.associateBy { it.dhikrId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val quranBookmarks: StateFlow<List<QuranBookmarkEntity>> = repo.quranBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- NOOR 2.0 Audio Library & Routines Flows ---
    val favoriteAudioIds: StateFlow<Set<String>> = repo.favoriteAudioIds
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val recentAudioTracks: StateFlow<List<AudioRecentEntity>> = repo.recentAudioTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioPlaylists: StateFlow<List<AudioPlaylistEntity>> = repo.audioPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioPlaylistItems: StateFlow<List<AudioPlaylistItemEntity>> = repo.audioPlaylistItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customRoutines: StateFlow<List<CustomRoutineEntity>> = repo.customRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _downloadedAudioIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedAudioIds: StateFlow<Set<String>> = _downloadedAudioIds.asStateFlow()

    private val _downloadingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTrackIds: StateFlow<Set<String>> = _downloadingTrackIds.asStateFlow()

    private val _nearbyMosquesState = MutableStateFlow<NearbyMosquesUiState>(NearbyMosquesUiState.Idle)
    val nearbyMosquesState: StateFlow<NearbyMosquesUiState> = _nearbyMosquesState.asStateFlow()

    val audioState: StateFlow<AudioPlaybackState> = quranAudioPlayer.state
    val speakingDhikrId: StateFlow<String?> = dhikrTtsHelper.speakingDhikrId

    private val _surahReaderState = MutableStateFlow<SurahReaderState>(SurahReaderState.Loading)
    val surahReaderState: StateFlow<SurahReaderState> = _surahReaderState.asStateFlow()

    private val _verseSearchResults = MutableStateFlow<List<QuranVerseCacheEntity>>(emptyList())
    val verseSearchResults: StateFlow<List<QuranVerseCacheEntity>> = _verseSearchResults.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isDetectingLocation = MutableStateFlow(false)
    val isDetectingLocation: StateFlow<Boolean> = _isDetectingLocation.asStateFlow()

    private val _liveTasbihCount = MutableStateFlow(0)
    val liveTasbihCount: StateFlow<Int> = _liveTasbihCount.asStateFlow()
    private var tasbihHydrated = false

    val statsSummary: StateFlow<StatsSummary> = combine(
        repo.allTimeTasbihTotal,
        repo.tasbihSessionCount,
        repo.totalCompletedAdhkar,
        repo.recentDailyActivities
    ) { allTasbih, sessionCount, completedAdhkar, activities ->
        val totalVerses = activities.sumOf { it.quranVersesRead }
        val totalPrayers = activities.sumOf { it.prayersCompletedCount }
        val activeDays = activities.count {
            it.prayersCompletedCount > 0 || it.tasbihCount > 0 || it.adhkarCompleted > 0 || it.quranVersesRead > 0
        }
        val weekTasbih = activities.take(7).sumOf { it.tasbihCount }
        val combinedTasbih = maxOf(allTasbih, activities.sumOf { it.tasbihCount })

        val achievements = listOf(
            AchievementItem(
                id = "streak_3",
                titleAr = "بداية الثبات",
                titleEn = "Steady Beginning",
                subtitleAr = "المحافظة على الورد اليومي لمدة ٣ أيام",
                subtitleEn = "Maintain your daily worship routine for 3 days",
                unlocked = activeDays >= 3,
                progress = (activeDays / 3f).coerceIn(0f, 1f)
            ),
            AchievementItem(
                id = "tasbih_300",
                titleAr = "ذاكر الله",
                titleEn = "Mindful Remembrance",
                subtitleAr = "إتمام ٣٠٠ تسبيحة في المسبحة الإلكترونية",
                subtitleEn = "Complete 300 Tasbih counts",
                unlocked = combinedTasbih >= 300,
                progress = (combinedTasbih / 300f).coerceIn(0f, 1f)
            ),
            AchievementItem(
                id = "adhkar_10",
                titleAr = "حصن المسلم",
                titleEn = "Fortress of Remembrance",
                subtitleAr = "إتمام ١٠ أذكار يومية كاملة",
                subtitleEn = "Complete 10 daily Adhkar items",
                unlocked = completedAdhkar >= 10,
                progress = (completedAdhkar / 10f).coerceIn(0f, 1f)
            ),
            AchievementItem(
                id = "prayers_5",
                titleAr = "محافظ على الصلوات",
                titleEn = "Guardian of Prayers",
                subtitleAr = "تسجيل إتمام ٥ صلوات مفروضة",
                subtitleEn = "Log completion of 5 obligatory prayers",
                unlocked = totalPrayers >= 5,
                progress = (totalPrayers / 5f).coerceIn(0f, 1f)
            ),
            AchievementItem(
                id = "quran_20",
                titleAr = "رفيق القرآن",
                titleEn = "Companion of the Quran",
                subtitleAr = "قراءة ٢٠ آية من المصحف الشريف",
                subtitleEn = "Read 20 verses from the Holy Quran",
                unlocked = totalVerses >= 20,
                progress = (totalVerses / 20f).coerceIn(0f, 1f)
            )
        )

        StatsSummary(
            currentStreakDays = activeDays,
            allTimeTasbih = combinedTasbih,
            weekTasbih = weekTasbih,
            totalTasbihSessions = sessionCount,
            totalCompletedAdhkar = completedAdhkar,
            totalQuranVersesRead = totalVerses,
            totalPrayersLogged = totalPrayers,
            recentActivities = activities,
            achievements = achievements
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsSummary())

    init {
        quranAudioPlayer.configureCallbacks(
            resolveLocalFile = { trackId -> repo.getCachedAudioPath(trackId) },
            onTrackStarted = { track ->
                viewModelScope.launch {
                    repo.recordRecentAudio(track)
                    cloudRepo.incrementDailyAnalytics(audioPlaysDelta = 1L, section = "audio")
                }
            }
        )
        _downloadedAudioIds.value = repo.listDownloadedAudioIds()

        // Keep background media foreground service & Home Screen widget synced with live audio playback state
        viewModelScope.launch {
            audioState.collect { st ->
                val app = getApplication<Application>()
                if (st.hasActiveSession) {
                    val title = if (preferences.value.language == AppLanguage.ARABIC) st.titleAr else st.titleEn
                    val sub = if (preferences.value.language == AppLanguage.ARABIC) st.subtitleAr else st.subtitleEn
                    QuranAudioService.updateNotification(app, title.ifBlank { "NOOR Audio" }, sub, st.isPlaying)
                } else {
                    QuranAudioService.stopService(app)
                }
                QuranPlayerWidgetProvider.requestUpdate(app)
            }
        }

        // Load verified Quran.com v4 word-level timing segments whenever active recited Surah or Reciter changes
        viewModelScope.launch {
            combine(
                audioState.map { it.playingSurah },
                preferences.map { it.quranReciter }
            ) { surah, reciter -> surah to reciter }
                .collect { (surah, reciter) ->
                    if (surah == null || !quranWordTimingRepository.supportsVerifiedWordTimings(reciter)) {
                        _activeSurahWordTimings.value = null
                    } else {
                        val cacheKey = "${reciter.id}_$surah"
                        if (_activeSurahWordTimings.value?.first != cacheKey) {
                            val timings = quranWordTimingRepository.getSurahWordTimings(reciter, surah)
                            _activeSurahWordTimings.value = if (timings.isNotEmpty()) cacheKey to timings else null
                        }
                    }
                }
        }

        // Sync user profile & live city/GPS location automatically whenever signed in or location changes
        viewModelScope.launch {
            combine(
                currentUser,
                preferences.map { p ->
                    listOf(
                        p.language.code,
                        p.cityNameEn,
                        p.cityNameAr,
                        String.format(Locale.US, "%.4f", p.latitude),
                        String.format(Locale.US, "%.4f", p.longitude)
                    )
                }.distinctUntilChanged()
            ) { user, _ -> user }
                .collect { user ->
                    if (user != null) {
                        val p = preferences.value
                        cloudRepo.syncCurrentUserProfile(
                            preferredLanguage = p.language.code,
                            cityName = p.cityNameEn,
                            cityNameAr = p.cityNameAr,
                            latitude = p.latitude,
                            longitude = p.longitude
                        )
                    }
                }
        }

        viewModelScope.launch {
            while (isActive) {
                _nowMillis.value = System.currentTimeMillis()
                delay(1000L)
            }
        }
        viewModelScope.launch {
            preferences.collect { prefs ->
                if (!tasbihHydrated) {
                    _liveTasbihCount.value = prefs.activeTasbihCount
                    tasbihHydrated = true
                }
                quranAudioPlayer.setPlaybackSpeed(prefs.audioPlaybackSpeed)
                val app = getApplication<Application>()
                NoorAlarmScheduler.rescheduleAll(app, prefs)
                NoorShortcutManager.publishShortcuts(app, prefs)
                PrayerTimesWidgetProvider.requestUpdate(app)
                QuranPlayerWidgetProvider.requestUpdate(app)
            }
        }
    }

    private fun buildInitialDashboard(prefs: UserPreferences): PrayerDashboardState {
        val now = System.currentTimeMillis()
        val tz = TimeZone.getTimeZone(prefs.timezoneId)
        val todayCal = Calendar.getInstance(tz).apply { timeInMillis = now }
        val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        val today = PrayerTimeCalculator.calculateForCalendar(
            todayCal, prefs.latitude, prefs.longitude, prefs.calculationMethod, prefs.asrMadhhab, prefs.offsets
        )
        val tomorrow = PrayerTimeCalculator.calculateForCalendar(
            tomorrowCal, prefs.latitude, prefs.longitude, prefs.calculationMethod, prefs.asrMadhhab, prefs.offsets
        )
        val dayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)
        return PrayerDashboardState(
            todaySchedule = today,
            weeklySchedules = listOf(today),
            monthlySchedules = listOf(today),
            nextPrayerInfo = PrayerTimeCalculator.computeNextPrayer(now, today, tomorrow),
            hijriToday = HijriCalendarEngine.gregorianToHijri(
                todayCal.get(Calendar.YEAR),
                todayCal.get(Calendar.MONTH) + 1,
                todayCal.get(Calendar.DAY_OF_MONTH),
                prefs.hijriAdjustmentDays
            ),
            gregorianCal = todayCal,
            dailyDhikr = AdhkarCatalog.dailyDhikr(dayOfYear),
            dailyAyah = AdhkarCatalog.dailyAyah(dayOfYear),
            dailyHadith = AdhkarCatalog.dailyHadith(dayOfYear)
        )
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    // --- Profile, Theme, Language & Home Dashboard ---
    fun setUserName(name: String) = viewModelScope.launch {
        val clean = name.trim().take(48)
        repo.prefs.setUserName(clean)
        if (clean.isNotBlank() && currentUser.value != null) {
            cloudRepo.updateCurrentUserDisplayName(clean)
        }
        val lang = preferences.value.language
        _statusBanner.value = when (lang) {
            AppLanguage.ARABIC -> if (clean.isBlank()) "تم مسح الاسم" else "تم تحديث الاسم بنجاح: $clean"
            AppLanguage.UZBEK -> if (clean.isBlank()) "Ism tozalandi" else "Ism muvaffaqiyatli yangilandi: $clean"
            AppLanguage.RUSSIAN -> if (clean.isBlank()) "Имя очищено" else "Имя обновлено: $clean"
            AppLanguage.ENGLISH -> if (clean.isBlank()) "Name cleared" else "Name updated: $clean"
        }
    }

    fun setCustomAvatarUri(uri: String) = viewModelScope.launch {
        repo.prefs.setCustomAvatarUri(uri)
        val lang = preferences.value.language
        _statusBanner.value = when (lang) {
            AppLanguage.ARABIC -> "تم تحديث صورة الحساب بنجاح"
            AppLanguage.UZBEK -> "Profil rasmi yangilandi"
            AppLanguage.RUSSIAN -> "Фото профиля обновлено"
            AppLanguage.ENGLISH -> "Profile photo updated"
        }
    }

    fun setLanguage(lang: AppLanguage) = viewModelScope.launch {
        repo.prefs.setLanguage(lang)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        repo.prefs.setThemeMode(mode)
    }

    fun setBackgroundStarsEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.prefs.setBackgroundStarsEnabled(enabled)
    }

    fun setBackgroundAuroraEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.prefs.setBackgroundAuroraEnabled(enabled)
    }

    fun setHomeDashboardConfig(config: HomeDashboardConfig) = viewModelScope.launch {
        repo.prefs.setHomeDashboardConfig(config)
    }

    fun setDailyGoals(quranVerses: Int, tasbihCount: Int) = viewModelScope.launch {
        repo.prefs.setDailyGoals(quranVerses, tasbihCount)
    }

    // --- Location, Prayer Config & Iqamah ---
    fun selectCity(city: CityLocation) = viewModelScope.launch {
        repo.prefs.setCity(city, isAuto = false)
    }

    fun detectLocationAutomatically() = viewModelScope.launch {
        _isDetectingLocation.value = true
        val detected = LocationHelper.detectCurrentCity(getApplication())
        _isDetectingLocation.value = false
        val isAr = preferences.value.language == AppLanguage.ARABIC
        if (detected != null) {
            repo.prefs.setCity(detected, isAuto = true)
            _statusBanner.value = if (isAr) "تم تحديث الموقع: ${detected.nameAr}" else "Location updated: ${detected.nameEn}"
        } else {
            _statusBanner.value = if (isAr) {
                "تعذّر تحديد الموقع تلقائياً. يمكنك اختيار مدينتك يدوياً."
            } else {
                "Could not detect GPS location. You can choose your city manually."
            }
        }
    }

    fun setCalculationMethod(method: CalculationMethod) = viewModelScope.launch {
        repo.prefs.setCalculationMethod(method)
    }

    fun setAsrMadhhab(madhhab: AsrMadhhab) = viewModelScope.launch {
        repo.prefs.setAsrMadhhab(madhhab)
    }

    fun setPrayerOffset(prayer: PrayerName, offset: Int) = viewModelScope.launch {
        repo.prefs.setPrayerOffset(prayer, offset)
    }

    fun setIqamahDelays(delays: IqamahDelays) = viewModelScope.launch {
        repo.prefs.setIqamahDelays(delays)
    }

    fun setHijriAdjustment(days: Int) = viewModelScope.launch {
        repo.prefs.setHijriAdjustment(days)
    }

    fun isPrayerTimeEntered(
        prayer: PrayerName,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean = PrayerTimeGate.hasPrayerEntered(
        schedule = prayerDashboard.value.todaySchedule,
        prayer = prayer,
        nowMillis = nowMillis
    )

    fun togglePrayerLogged(
        prayer: PrayerName,
        nowMillis: Long = System.currentTimeMillis()
    ) = viewModelScope.launch {
        if (!isPrayerTimeEntered(prayer, nowMillis)) return@launch
        repo.togglePrayerLogged(prayer)
    }

    // --- Nearby Mosques Finder ---
    fun searchNearbyMosques(radiusMeters: Int = 5000) = viewModelScope.launch {
        _nearbyMosquesState.value = NearbyMosquesUiState.Loading
        val prefs = preferences.value
        when (val res = repo.findNearbyMosques(prefs.latitude, prefs.longitude, radiusMeters)) {
            is MosqueSearchResult.Success -> {
                _nearbyMosquesState.value = NearbyMosquesUiState.Ready(res.mosques, radiusMeters)
            }
            is MosqueSearchResult.Error -> {
                _nearbyMosquesState.value = NearbyMosquesUiState.Error(res.messageAr, res.messageEn)
            }
        }
    }

    // --- Notifications ---
    fun setPrayerNotification(prayer: PrayerName, enabled: Boolean) = viewModelScope.launch {
        repo.prefs.setPrayerNotification(prayer, enabled)
    }

    fun setPrePrayerAlertMinutes(minutes: Int) = viewModelScope.launch {
        repo.prefs.setPrePrayerAlertMinutes(minutes)
    }

    fun setReminder(type: String, enabled: Boolean, hour: Int? = null, minute: Int? = null) = viewModelScope.launch {
        repo.prefs.setReminder(type, enabled, hour, minute)
    }

    fun setReminderSound(type: String, voiceId: String) = viewModelScope.launch {
        repo.prefs.setReminderSound(type, voiceId)
        val lang = preferences.value.language
        val voice = AdhanCatalog.resolveVoice(voiceId, preferences.value.customAdhanRecords, getApplication())
        val soundName = com.example.ui.i18n.L10n.pick(lang, voice.nameAr, voice.nameEn)
        _statusBanner.value = com.example.ui.i18n.L10n.pick(
            lang,
            "تم حفظ صوت التذكير: $soundName",
            "Reminder sound updated: $soundName",
            "Eslatma ovozi saqlandi: $soundName"
        )
    }

    fun importCustomReminderSound(type: String, uri: Uri) = viewModelScope.launch {
        val lang = preferences.value.language
        val result = withContext(Dispatchers.IO) {
            AdhanCatalog.importFromUri(getApplication(), uri)
        }
        when (result) {
            is AdhanImportResult.Success -> {
                repo.prefs.addCustomAdhanRecord(result.record)
                repo.prefs.setReminderSound(type, result.record.id)
                _statusBanner.value = com.example.ui.i18n.L10n.pick(
                    lang,
                    "تم تخصيص الصوت من جهازك بنجاح: ${result.record.displayName}",
                    "Custom reminder sound imported: ${result.record.displayName}",
                    "Qurilmadan maxsus eslatma ovozi tanlandi: ${result.record.displayName}"
                )
            }
            is AdhanImportResult.Error -> {
                _statusBanner.value = com.example.ui.i18n.L10n.pick(lang, result.messageAr, result.messageEn)
            }
        }
    }

    fun sendTestReminderAlert(type: String): Boolean {
        val prefs = preferences.value
        val lang = prefs.language
        val (title, body) = NoorAlarmScheduler.reminderCopy(type, lang)
        val voiceId = when (type) {
            "morning" -> prefs.morningReminderSoundId
            "evening" -> prefs.eveningReminderSoundId
            "tahajjud" -> prefs.tahajjudReminderSoundId
            else -> AdhanCatalog.SYSTEM_DEFAULT_ID
        }
        val notifId = when (type) {
            "morning" -> NoorAlarmScheduler.REQ_MORNING_ADHKAR
            "evening" -> NoorAlarmScheduler.REQ_EVENING_ADHKAR
            "tahajjud" -> NoorAlarmScheduler.REQ_TAHAJJUD_REMINDER
            else -> 9998
        }
        val sent = NoorNotificationHelper.showNotification(
            context = getApplication(),
            notificationId = notifId,
            channelId = NoorNotificationHelper.CHANNEL_REMINDER,
            title = title,
            body = body
        )
        if (voiceId.isNotBlank() && voiceId != AdhanCatalog.SYSTEM_DEFAULT_ID) {
            AdhanPlaybackService.startReminderSound(
                context = getApplication(),
                voiceId = voiceId,
                title = title,
                body = body,
                isTestAlarm = true
            )
        }
        _statusBanner.value = if (sent) {
            com.example.ui.i18n.L10n.pick(
                lang,
                "تم إرسال تذكير تجريبي مع الصوت المخصص ✓",
                "Test reminder sent with your custom sound ✓",
                "Maxsus ovozli sinov eslatmasi yuborildi ✓"
            )
        } else {
            com.example.ui.i18n.L10n.pick(
                lang,
                "يرجى منح إذن الإشعارات أولاً",
                "Please grant notification permission first",
                "Avval bildirishnoma ruxsatini bering"
            )
        }
        return sent
    }

    fun sendTestNotification(): Boolean {
        val lang = preferences.value.language
        val sent = NoorNotificationHelper.showNotification(
            context = getApplication(),
            notificationId = 9999,
            channelId = NoorNotificationHelper.CHANNEL_PRAYER,
            title = com.example.ui.i18n.L10n.pick(lang, "تطبيق نور — تنبيه تجريبي", "Noor — Test Notification", "NOOR — Sinov bildirishnomasi"),
            body = com.example.ui.i18n.L10n.pick(
                lang,
                "نظام تنبيهات الصلاة والأذكار والتهجد يعمل بنجاح ودقة.",
                "Prayer, Adhkar & Tahajjud notifications are active and working properly.",
                "Namoz, zikr va Tahajjud bildirishnomalari faol va to'g'ri ishlamoqda."
            )
        )
        _statusBanner.value = if (sent) {
            com.example.ui.i18n.L10n.pick(lang, "تم إرسال إشعار تجريبي بنجاح", "Test notification sent", "Sinov bildirishnomasi yuborildi")
        } else {
            com.example.ui.i18n.L10n.pick(lang, "يرجى منح إذن الإشعارات أولاً", "Please grant notification permission first", "Avval bildirishnoma ruxsatini bering")
        }
        return sent
    }

    // --- Adhkar ---
    fun toggleFavoriteDhikr(dhikrId: String) = viewModelScope.launch {
        val isFav = favoriteDhikrIds.value.contains(dhikrId)
        repo.toggleFavoriteDhikr(dhikrId, isFav)
    }

    fun incrementDhikr(dhikr: DhikrItem) = viewModelScope.launch {
        val current = adhkarProgressMap.value[dhikr.id]?.currentCount ?: 0
        if (current < dhikr.targetCount) {
            triggerHaptic(light = true)
            repo.incrementDhikrProgress(dhikr.id, current, dhikr.targetCount)
            if (current + 1 >= dhikr.targetCount) {
                cloudRepo.incrementDailyAnalytics(adhkarDelta = 1L, section = "adhkar")
            }
        }
    }

    fun markDhikrCompleted(dhikr: DhikrItem) = viewModelScope.launch {
        triggerHaptic(light = false)
        repo.markDhikrCompletedDirectly(dhikr.id, dhikr.targetCount)
        cloudRepo.incrementDailyAnalytics(adhkarDelta = 1L, section = "adhkar")
    }

    fun recordAdhkarSessionState(categoryId: String, completedItems: Int, totalItems: Int, lastDhikrIndex: Int) =
        viewModelScope.launch {
            repo.recordAdhkarSessionState(categoryId, completedItems, totalItems, lastDhikrIndex)
        }

    fun resetTodayAdhkar() = viewModelScope.launch {
        repo.resetDhikrProgressForToday()
    }

    fun toggleSpeakDhikr(dhikr: DhikrItem) {
        dhikrTtsHelper.speakOrStop(dhikr.id, dhikr.textAr)
    }

    fun stopSpeakingDhikr() {
        dhikrTtsHelper.stop()
    }

    // --- NOOR 3.0 Support, Reports, Audio Quality, Telemetry & Auth Actions ---

    fun setAudioQualityLabel(quality: String) = viewModelScope.launch {
        repo.prefs.setAudioQualityLabel(quality)
    }

    fun trackSectionVisit(section: String) = viewModelScope.launch {
        cloudRepo.incrementDailyAnalytics(section = section)
    }

    fun submitSupportTicket(ticketType: String, subject: String, message: String) = viewModelScope.launch {
        val isAr = preferences.value.language == AppLanguage.ARABIC
        if (subject.trim().length < 2 || message.trim().length < 3) {
            _statusBanner.value = if (isAr) "يرجى إدخال العنوان والتفاصيل" else "Please enter both subject and details"
            return@launch
        }
        val res = cloudRepo.submitSupportTicket(ticketType, subject, message, preferences.value.userName)
        _statusBanner.value = if (res.isSuccess) {
            if (isAr) "تم إرسال رسالتك لفريق دعم نور بنجاح" else "Your message has been submitted to NOOR Support"
        } else {
            if (isAr) "يرجى تسجيل الدخول لإرسال تذكرة الدعم" else "Please sign in to submit a support message"
        }
    }

    fun submitContentReport(targetType: String, targetId: String, reason: String) = viewModelScope.launch {
        val isAr = preferences.value.language == AppLanguage.ARABIC
        val res = cloudRepo.submitUserReport(targetType, targetId, reason)
        _statusBanner.value = if (res.isSuccess) {
            if (isAr) "تم إرسال البلاغ للمراجعة" else "Report submitted for review"
        } else {
            if (isAr) "تعذّر إرسال البلاغ حالياً" else "Unable to submit report right now"
        }
    }

    fun signOut(context: Context) = viewModelScope.launch {
        FirebaseAuthHelper.signOut(context)
        val isAr = preferences.value.language == AppLanguage.ARABIC
        _statusBanner.value = if (isAr) "تم تسجيل الخروج بنجاح" else "Signed out successfully"
    }

    fun showBannerMessage(message: String) {
        _statusBanner.value = message
    }

    // --- Electronic Tasbih & Custom Dhikr ---
    fun incrementTasbih(
        timbre: com.example.domain.tasbih.BeadMaterialTimbre = com.example.domain.tasbih.BeadMaterialTimbre.AMBER
    ) {
        tasbihHydrated = true
        val prefs = preferences.value
        val nextCount = _liveTasbihCount.value + 1
        _liveTasbihCount.value = nextCount
        val target = prefs.activeTasbihTarget.coerceAtLeast(1)

        if (prefs.tasbihVibrationEnabled) {
            triggerHaptic(light = nextCount % target != 0)
        }
        if (prefs.tasbihSoundEnabled) {
            playClickTone(isMilestone = nextCount % target == 0, timbre = timbre, isReverse = false)
        }

        viewModelScope.launch {
            repo.recordTasbihTap(prefs.activeTasbihPresetId, nextCount, target)

            if (nextCount == target) {
                val (labelAr, labelEn) = resolveActiveTasbihLabels(prefs)
                repo.saveCompletedTasbihSession(
                    presetId = prefs.activeTasbihPresetId,
                    labelAr = labelAr,
                    labelEn = labelEn,
                    count = nextCount,
                    target = target
                )
            }
        }
    }

    fun decrementTasbih(
        timbre: com.example.domain.tasbih.BeadMaterialTimbre = com.example.domain.tasbih.BeadMaterialTimbre.AMBER
    ) {
        tasbihHydrated = true
        val current = _liveTasbihCount.value
        if (current <= 0) return
        val prefs = preferences.value
        val prevCount = (current - 1).coerceAtLeast(0)
        _liveTasbihCount.value = prevCount

        if (prefs.tasbihVibrationEnabled) {
            triggerHaptic(light = true)
        }
        if (prefs.tasbihSoundEnabled) {
            playClickTone(isMilestone = false, timbre = timbre, isReverse = true)
        }

        viewModelScope.launch {
            repo.prefs.saveActiveTasbih(prefs.activeTasbihPresetId, prevCount, prefs.activeTasbihTarget)
        }
    }

    private fun resolveActiveTasbihLabels(prefs: UserPreferences): Pair<String, String> {
        val builtIn = AdhkarCatalog.tasbihPresets.firstOrNull { it.id == prefs.activeTasbihPresetId }
        if (builtIn != null) return builtIn.textAr to builtIn.textEn
        val custom = customDhikrPresets.value.firstOrNull { it.id == prefs.activeTasbihPresetId }
        if (custom != null) return custom.textAr to custom.textEn
        return prefs.customTasbihText to prefs.customTasbihText
    }

    fun selectTasbihPreset(presetId: String, target: Int, customText: String? = null) {
        tasbihHydrated = true
        val prefs = preferences.value
        val prevCount = _liveTasbihCount.value
        _liveTasbihCount.value = 0
        viewModelScope.launch {
            if (prevCount > 0 && prevCount != prefs.activeTasbihTarget) {
                val (labelAr, labelEn) = resolveActiveTasbihLabels(prefs)
                repo.saveCompletedTasbihSession(
                    presetId = prefs.activeTasbihPresetId,
                    labelAr = labelAr,
                    labelEn = labelEn,
                    count = prevCount,
                    target = prefs.activeTasbihTarget
                )
            }
            repo.prefs.saveActiveTasbih(presetId, 0, target.coerceAtLeast(1), customText)
        }
    }

    fun addCustomDhikrPreset(textAr: String, textEn: String, target: Int) = viewModelScope.launch {
        repo.addCustomDhikrPreset(textAr, textEn, target)
    }

    fun deleteCustomDhikrPreset(id: String) = viewModelScope.launch {
        repo.deleteCustomDhikrPreset(id)
    }

    fun updateTasbihTarget(target: Int) = viewModelScope.launch {
        val prefs = preferences.value
        repo.prefs.saveActiveTasbih(prefs.activeTasbihPresetId, _liveTasbihCount.value, target.coerceIn(1, 10000))
    }

    fun resetTasbihCounter() {
        tasbihHydrated = true
        val prefs = preferences.value
        val prevCount = _liveTasbihCount.value
        _liveTasbihCount.value = 0
        viewModelScope.launch {
            if (prevCount > 0 && prevCount != prefs.activeTasbihTarget) {
                val (labelAr, labelEn) = resolveActiveTasbihLabels(prefs)
                repo.saveCompletedTasbihSession(
                    presetId = prefs.activeTasbihPresetId,
                    labelAr = labelAr,
                    labelEn = labelEn,
                    count = prevCount,
                    target = prefs.activeTasbihTarget
                )
            }
            repo.prefs.saveActiveTasbih(prefs.activeTasbihPresetId, 0, prefs.activeTasbihTarget)
        }
    }

    fun setTasbihVibration(enabled: Boolean) = viewModelScope.launch {
        repo.prefs.setTasbihVibration(enabled)
    }

    fun setTasbihSound(enabled: Boolean) = viewModelScope.launch {
        repo.prefs.setTasbihSound(enabled)
    }

    // --- Custom Daily Worship Routines ---
    fun addCustomRoutine(titleAr: String, titleEn: String, category: String = "general") = viewModelScope.launch {
        repo.addCustomRoutine(titleAr, titleEn, category)
    }

    fun toggleCustomRoutineToday(routine: CustomRoutineEntity) = viewModelScope.launch {
        repo.toggleCustomRoutineToday(routine)
    }

    fun deleteCustomRoutine(id: Long) = viewModelScope.launch {
        repo.deleteCustomRoutine(id)
    }

    fun todayDateKey(): String = repo.todayDateKey()

    // --- Holy Quran ---
    fun loadSurah(surahNumber: Int) = viewModelScope.launch {
        _surahReaderState.value = SurahReaderState.Loading
        val reciter = preferences.value.quranReciter
        when (val res = repo.loadSurahVerses(surahNumber, reciter)) {
            is SurahLoadResult.Success -> {
                _surahReaderState.value = SurahReaderState.Ready(surahNumber, res.verses, res.isOfflineCached)
                // Preserve exact saved lastReadAyah when reopening the same Surah!
                // Never overwrite lastReadAyah back to 1 on load.
            }
            is SurahLoadResult.Error -> {
                _surahReaderState.value = SurahReaderState.Error(surahNumber, res.messageAr, res.messageEn)
            }
        }
    }

    fun searchQuranVerses(query: String) = viewModelScope.launch {
        _verseSearchResults.value = repo.searchVersesOffline(query)
    }

    fun saveLastRead(surahNumber: Int, ayahNumber: Int) = viewModelScope.launch {
        repo.recordQuranReadingProgress(surahNumber, ayahNumber, versesDelta = 1)
        cloudRepo.incrementDailyAnalytics(quranVersesDelta = 1L, section = "quran")
    }

    fun toggleBookmark(surahNumber: Int, ayahNumber: Int, ayahText: String) = viewModelScope.launch {
        val surah = SurahCatalog.findSurah(surahNumber)
        val exists = quranBookmarks.value.any { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }
        repo.toggleBookmark(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            surahNameAr = surah.nameAr,
            surahNameEn = surah.nameEn,
            ayahText = ayahText,
            currentlyBookmarked = exists
        )
    }

    fun setQuranFontSize(sizeSp: Int) = viewModelScope.launch {
        repo.prefs.setQuranFontSize(sizeSp)
    }

    fun setQuranReadingTheme(theme: QuranReadingTheme) = viewModelScope.launch {
        repo.prefs.setQuranReadingTheme(theme)
    }

    fun setQuranDisplayMode(mode: QuranDisplayMode) = viewModelScope.launch {
        repo.prefs.setQuranDisplayMode(mode)
    }

    fun setQuranReciter(reciter: QuranReciter) = viewModelScope.launch {
        _selectedMushafCustomReciterId.value = null
        val prevAudio = quranAudioPlayer.state.value
        val shouldRestartActive = (prevAudio.isPlaying || prevAudio.isBuffering) &&
            prevAudio.playingSurah != null &&
            prevAudio.playingAyah != null
        val targetSurah = prevAudio.playingSurah
        val targetAyah = prevAudio.playingAyah

        // Stop any active or in-flight session BEFORE switching reciter so two Sheikh voices never overlap
        if (shouldRestartActive) {
            cancelPendingPlaybackRequests()
            quranAudioPlayer.stop()
        }

        repo.prefs.setQuranReciter(reciter)
        val currentState = _surahReaderState.value
        if (currentState is SurahReaderState.Ready) {
            val updatedVerses = currentState.verses.map { v ->
                v.copy(audioUrl = SurahCatalog.audioUrlForAyah(v.surahNumber, v.numberInSurah, reciter))
            }
            _surahReaderState.value = currentState.copy(verses = updatedVerses)
        }

        if (shouldRestartActive && targetSurah != null && targetAyah != null) {
            playFromMushafAyah(
                surahNumber = targetSurah,
                startAyah = targetAyah,
                explicitReciter = reciter
            )
        }
    }

    /**
     * Selects an Admin-uploaded Custom Reciter directly inside the Quran Mushaf reader.
     * Playing inside the Mushaf will stream the uploaded file/URL and automatically track
     * and highlight Ayahs across the Surah proportionally to playback progress.
     */
    fun selectMushafCustomReciter(reciter: CustomReciterItem) {
        val prevAudio = quranAudioPlayer.state.value
        val shouldRestartActive = (prevAudio.isPlaying || prevAudio.isBuffering) &&
            prevAudio.playingSurah != null
        val targetSurah = prevAudio.playingSurah
        val targetAyah = prevAudio.playingAyah ?: 1

        _selectedMushafCustomReciterId.value = reciter.id
        if (shouldRestartActive && targetSurah != null) {
            cancelPendingPlaybackRequests()
            quranAudioPlayer.stop()
            playFromMushafAyah(surahNumber = targetSurah, startAyah = targetAyah)
        }
    }

    fun playQuranVerse(
        verse: QuranVerse,
        allVerses: List<QuranVerse>,
        repeatPerAyah: Int = 1
    ) {
        val surah = SurahCatalog.findSurah(verse.surahNumber)
        val reciter = preferences.value.quranReciter
        if (_selectedMushafCustomReciterId.value != null) {
            playFromMushafAyah(
                surahNumber = verse.surahNumber,
                startAyah = verse.numberInSurah,
                preloadedVerses = allVerses
            )
            return
        }
        if (repeatPerAyah <= 1 && allVerses.isNotEmpty()) {
            playSurahVerseByVerse(
                surahNumber = verse.surahNumber,
                verses = allVerses,
                reciter = reciter,
                startAyah = verse.numberInSurah
            )
            return
        }
        cancelPendingPlaybackRequests()
        quranAudioPlayer.playAyah(
            surahNumber = verse.surahNumber,
            ayahNumber = verse.numberInSurah,
            audioUrl = verse.audioUrl,
            titleAr = "سورة ${surah.nameAr} • آية ${verse.numberInSurah}",
            titleEn = "${surah.nameEn} • Ayah ${verse.numberInSurah}",
            reciterNameAr = reciter.nameAr,
            reciterNameEn = reciter.nameEn,
            repeatRemaining = repeatPerAyah,
            onCompleted = {
                val nextIndex = allVerses.indexOfFirst { it.numberInSurah == verse.numberInSurah } + 1
                if (nextIndex in allVerses.indices) {
                    playQuranVerse(allVerses[nextIndex], allVerses, repeatPerAyah)
                }
            }
        )
    }

    // --- Continuous Uninterrupted Sheikh Recitation (Engine A: Full-Surah Stream + Verified Timestamps; Engine B: Gapless Queue) ---
    fun playSurahVerseByVerse(
        surahNumber: Int,
        verses: List<QuranVerse>,
        reciter: QuranReciter = preferences.value.quranReciter,
        startAyah: Int = 1
    ) {
        if (verses.isEmpty()) {
            playFullSurahWithReciter(surahNumber, reciter)
            return
        }
        playFromMushafAyah(
            surahNumber = surahNumber,
            startAyah = startAyah,
            endAyah = null,
            repeatRange = false,
            explicitReciter = reciter,
            preloadedVerses = verses
        )
    }

    /**
     * Real-time verified word position (`1..N`) inside the currently playing Ayah.
     * Strictly derived from verified millisecond timing segments — returns `null` when unavailable.
     */
    val activeWordPosition: StateFlow<Int?> = combine(
        audioState,
        _activeSurahWordTimings,
        preferences.map { it.quranReciter }
    ) { st, cached, reciter ->
        if (!st.isPlaying || st.playingSurah == null || st.playingAyah == null) return@combine null
        if (st.isContinuousStream) return@combine st.activeWordPosition
        val expectedKey = "${reciter.id}_${st.playingSurah}"
        if (cached == null || cached.first != expectedKey) return@combine null
        cached.second[st.playingAyah]?.findWordPositionAt(st.currentPositionMs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val hasVerifiedWordTimingsForCurrentPlayback: StateFlow<Boolean> = combine(
        audioState,
        _activeSurahWordTimings,
        preferences.map { it.quranReciter }
    ) { st, cached, reciter ->
        val s = st.playingSurah ?: return@combine false
        val a = st.playingAyah ?: return@combine false
        if (st.isContinuousStream && st.activeWordPosition != null) return@combine true
        val expectedKey = "${reciter.id}_$s"
        cached != null && cached.first == expectedKey && cached.second[a]?.segments?.isNotEmpty() == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun updateMushafHighlightStyle(style: MushafHighlightStyle) {
        val clamped = style.copy(alpha = style.alpha.coerceIn(0.15f, 0.50f))
        _mushafHighlightStyle.value = clamped
        mushafPrefs.edit()
            .putString("highlight_color", clamped.colorPreset.key)
            .putFloat("highlight_alpha", clamped.alpha)
            .putBoolean("highlight_pulse", clamped.pulseAnimated)
            .apply()
    }

    /**
     * Loads verified Quran verses for ALL Surahs appearing on a specific Madinah Mushaf page (`1..604`).
     * Supports multi-Surah pages such as Page 604 (Surahs 112, 113, 114).
     */
    suspend fun loadVersesForMushafPage(pageNumber: Int): List<QuranVerse> = withContext(Dispatchers.IO) {
        val meta = MadinahMushafPageCatalog.pageMeta(pageNumber)
        val reciter = preferences.value.quranReciter
        val pageVerses = mutableListOf<QuranVerse>()
        for (surah in meta.surahsOnPage) {
            when (val res = repo.loadSurahVerses(surah.number, reciter)) {
                is SurahLoadResult.Success -> {
                    val onPage = res.verses.filter { it.pageNumber == pageNumber }
                    if (onPage.isNotEmpty()) {
                        pageVerses += onPage
                    } else if (meta.surahsOnPage.size == 1) {
                        // Fallback if page metadata on cached verses is missing
                        pageVerses += res.verses
                    }
                }
                is SurahLoadResult.Error -> Unit
            }
        }
        pageVerses.sortedWith(compareBy({ it.surahNumber }, { it.numberInSurah }))
    }

    /**
     * Interactive Mushaf: "Read from this Ayah" (continuous uninterrupted playback across Ayah, page & Surah boundaries)
     * or "Repeat Ayah Range" (seamless in-stream looping between `startAyah..endAyah`).
     *
     * Priority:
     * 1. **Engine A (Primary)**: Single continuous full-Surah MP3 + verified millisecond Ayah/word timestamps
     *    from Quran.com v4 (`playContinuousSurahManifest`) — ZERO stops or re-buffering between Ayahs!
     * 2. **Engine B (Fallback)**: Gapless `setNextMediaPlayer` pre-buffered per-Ayah queue for reciters
     *    without continuous chapter timestamps or when offline without cached manifests.
     */
    fun playFromMushafAyah(
        surahNumber: Int,
        startAyah: Int,
        endAyah: Int? = null,
        repeatRange: Boolean = false,
        explicitReciter: QuranReciter? = null,
        preloadedVerses: List<QuranVerse>? = null
    ): Job {
        val activeCustomReciter = if (explicitReciter == null) {
            _selectedMushafCustomReciterId.value?.let { cid ->
                customReciters.value.firstOrNull { it.id == cid && it.hasPlayableAudio() }
            }
        } else {
            null
        }
        val reciter = explicitReciter ?: preferences.value.quranReciter
        val sMeta = SurahCatalog.findSurah(surahNumber)
        val clampedStart = startAyah.coerceIn(1, sMeta.verseCount)
        val clampedEndAyah = endAyah?.coerceIn(clampedStart, sMeta.verseCount)
        val activeReciterKey = activeCustomReciter?.let { "custom_${it.id}" } ?: reciter.id
        val activeReciterNameEn = activeCustomReciter?.let { it.nameEn.ifBlank { it.nameAr } } ?: reciter.nameEn
        val signature = "$activeReciterKey:$surahNumber:$clampedStart:$clampedEndAyah:$repeatRange"
        val nowMs = System.currentTimeMillis()
        val existingJob = activePlaybackRequestJob
        val curAudio = quranAudioPlayer.state.value

        // Deduplicate rapid repeated Play taps for the exact same request
        if (signature == lastPlaySignature &&
            (nowMs - lastPlayRequestAtMs) < 350L &&
            (existingJob?.isActive == true || curAudio.isPlaying || curAudio.isBuffering)
        ) {
            return existingJob ?: viewModelScope.launch { }
        }
        lastPlaySignature = signature
        lastPlayRequestAtMs = nowMs

        val myToken = cancelPendingPlaybackRequests()

        // Immediately silence any conflicting active session (different Surah/reciter/non-continuous)
        // before suspending on network or DB calls.
        if (curAudio.hasActiveSession &&
            (curAudio.playingSurah != surahNumber || curAudio.subtitleEn != activeReciterNameEn)
        ) {
            quranAudioPlayer.stop()
        }

        val job = viewModelScope.launch {
            val isAr = preferences.value.language == AppLanguage.ARABIC

            // Persist the user's chosen starting position immediately
            saveLastRead(surahNumber, clampedStart)

            // Load verses for accurate Ayah -> Mushaf page mapping
            val surahVerses = preloadedVerses?.takeIf { it.isNotEmpty() && it.first().surahNumber == surahNumber }
                ?: when (val res = repo.loadSurahVerses(surahNumber, reciter)) {
                    is SurahLoadResult.Success -> res.verses
                    is SurahLoadResult.Error -> emptyList()
                }
            if (myToken != playbackRequestToken.get()) return@launch

            val pageByAyah = surahVerses.associate { it.numberInSurah to it.pageNumber.takeIf { p -> p in 1..604 } }

            // Engine Custom: Admin-Uploaded Custom Reciter inside the Mushaf with Automatic Ayah Sync!
            if (activeCustomReciter != null) {
                val ayahWeights = if (surahVerses.isNotEmpty()) {
                    surahVerses.map { it.numberInSurah to it.textUthmani.length.coerceAtLeast(15) }
                } else {
                    (1..sMeta.verseCount).map { it to 40 }
                }
                val customManifest = QuranWordTimingRepository.buildProportionalSurahManifest(
                    reciterId = "custom_${activeCustomReciter.id}",
                    surahNumber = surahNumber,
                    audioSource = activeCustomReciter.resolvePlayableSource(),
                    ayahWeights = ayahWeights
                )
                val rangeLabel = if (repeatRange && clampedEndAyah != null) {
                    if (isAr) "تكرار ${sMeta.nameAr} ($clampedStart–$clampedEndAyah)"
                    else "Loop ${sMeta.nameEn} ($clampedStart–$clampedEndAyah)"
                } else {
                    null
                }
                quranAudioPlayer.playContinuousSurahManifest(
                    manifest = customManifest,
                    surahTitleAr = "سورة ${sMeta.nameAr}",
                    surahTitleEn = sMeta.nameEn,
                    reciterNameAr = activeCustomReciter.nameAr,
                    reciterNameEn = activeCustomReciter.nameEn.ifBlank { activeCustomReciter.nameAr },
                    pageResolver = { _, ayahNum -> pageByAyah[ayahNum] ?: sMeta.startPage },
                    startAyah = clampedStart,
                    repeatRangeStartAyah = if (repeatRange && clampedEndAyah != null) clampedStart else null,
                    repeatRangeEndAyah = if (repeatRange && clampedEndAyah != null) clampedEndAyah else null,
                    repeatRangeLabel = rangeLabel,
                    initialSpeed = preferences.value.audioPlaybackSpeed,
                    onAyahChanged = { s, a, _ -> saveLastRead(s, a) },
                    onSurahCompleted = null
                )
                return@launch
            }

            // Engine A: Continuous Uninterrupted Full-Surah Stream + Verified Millisecond Timestamps
            val manifest = if (quranWordTimingRepository.supportsVerifiedWordTimings(reciter)) {
                quranWordTimingRepository.getContinuousSurahManifest(reciter, surahNumber)
            } else {
                null
            }
            if (myToken != playbackRequestToken.get()) return@launch

            if (manifest != null && manifest.ayahs.isNotEmpty() && manifest.audioUrl.isNotBlank()) {
                val rangeLabel = if (repeatRange && clampedEndAyah != null) {
                    if (isAr) "تكرار ${sMeta.nameAr} ($clampedStart–$clampedEndAyah)"
                    else "Loop ${sMeta.nameEn} ($clampedStart–$clampedEndAyah)"
                } else {
                    null
                }
                quranAudioPlayer.playContinuousSurahManifest(
                    manifest = manifest,
                    surahTitleAr = "سورة ${sMeta.nameAr}",
                    surahTitleEn = sMeta.nameEn,
                    reciterNameAr = reciter.nameAr,
                    reciterNameEn = reciter.nameEn,
                    pageResolver = { _, ayahNum -> pageByAyah[ayahNum] ?: sMeta.startPage },
                    startAyah = clampedStart,
                    repeatRangeStartAyah = if (repeatRange && clampedEndAyah != null) clampedStart else null,
                    repeatRangeEndAyah = if (repeatRange && clampedEndAyah != null) clampedEndAyah else null,
                    repeatRangeLabel = rangeLabel,
                    initialSpeed = preferences.value.audioPlaybackSpeed,
                    onAyahChanged = { s, a, _ -> saveLastRead(s, a) },
                    onSurahCompleted = if (!repeatRange && surahNumber < 114) {
                        { playFromMushafAyah(surahNumber + 1, 1, explicitReciter = reciter) }
                    } else {
                        null
                    }
                )
                return@launch
            }

            // Engine B: Gapless pre-buffered per-Ayah queue (used by Sheikh Yasser Al-Dosari & others)
            val tracks = mutableListOf<AudioTrackItem>()

            suspend fun appendSurahTracks(
                sNum: Int,
                existing: List<QuranVerse>? = null,
                includeOpeningBismillah: Boolean = true
            ) {
                val meta = SurahCatalog.findSurah(sNum)
                val verses = existing?.takeIf { it.isNotEmpty() }
                    ?: when (val res = repo.loadSurahVerses(sNum, reciter)) {
                        is SurahLoadResult.Success -> res.verses
                        is SurahLoadResult.Error -> (1..meta.verseCount).map { aNum ->
                            QuranVerse(
                                surahNumber = sNum,
                                numberInSurah = aNum,
                                globalNumber = aNum,
                                textUthmani = "",
                                tafsirAr = "",
                                audioUrl = SurahCatalog.audioUrlForAyah(sNum, aNum, reciter),
                                pageNumber = meta.startPage
                            )
                        }
                    }
                tracks += buildSurahTracksWithBismillah(
                    surahNumber = sNum,
                    verses = verses,
                    reciter = reciter,
                    includeOpeningBismillah = includeOpeningBismillah
                )
            }

            // Include opening Bismillah when starting a Surah from Ayah 1 (except Al-Fatihah & At-Tawbah),
            // and ALWAYS between consecutive Surahs (especially short Surahs in Juz Amma!).
            val includeStartBismillah = !repeatRange && clampedStart == 1 && surahNumber != 1 && surahNumber != 9
            appendSurahTracks(surahNumber, surahVerses, includeOpeningBismillah = includeStartBismillah)
            if (myToken != playbackRequestToken.get()) return@launch
            if (!repeatRange && surahNumber < 114) {
                val nextSurahEnd = if (surahNumber >= 78) minOf(114, surahNumber + 10) else minOf(114, surahNumber + 2)
                for (nextS in (surahNumber + 1)..nextSurahEnd) {
                    appendSurahTracks(nextS, includeOpeningBismillah = true)
                    if (myToken != playbackRequestToken.get()) return@launch
                }
            }

            if (tracks.isEmpty() || myToken != playbackRequestToken.get()) return@launch
            val startIdx = if (includeStartBismillah) {
                tracks.indexOfFirst { it.surahNumber == surahNumber && it.ayahNumber == 0 }.coerceAtLeast(0)
            } else {
                tracks.indexOfFirst { it.surahNumber == surahNumber && it.ayahNumber == clampedStart }.coerceAtLeast(0)
            }

            if (repeatRange && clampedEndAyah != null) {
                val endIdx = tracks.indexOfLast { it.surahNumber == surahNumber && it.ayahNumber == clampedEndAyah }
                    .takeIf { it >= startIdx } ?: startIdx
                val label = if (isAr) {
                    "تكرار ${sMeta.nameAr} ($clampedStart–$clampedEndAyah)"
                } else {
                    "Loop ${sMeta.nameEn} ($clampedStart–$clampedEndAyah)"
                }
                quranAudioPlayer.playAyahRange(
                    queue = tracks,
                    rangeStartIndex = startIdx,
                    rangeEndIndex = endIdx,
                    rangeLabel = label,
                    initialSpeed = preferences.value.audioPlaybackSpeed
                )
            } else {
                quranAudioPlayer.playTrackQueue(
                    queue = tracks,
                    startIndex = startIdx,
                    initialSpeed = preferences.value.audioPlaybackSpeed
                )
            }
        }
        activePlaybackRequestJob = job
        return job
    }

    /**
     * Plays a single selected Ayah once from the Interactive Mushaf contextual menu.
     */
    fun playSingleMushafAyah(surahNumber: Int, ayahNumber: Int, pageNumber: Int? = null) {
        cancelPendingPlaybackRequests()
        val reciter = preferences.value.quranReciter
        val surah = SurahCatalog.findSurah(surahNumber)
        val url = SurahCatalog.audioUrlForAyah(surahNumber, ayahNumber, reciter)
        quranAudioPlayer.playAyah(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            audioUrl = url,
            titleAr = "سورة ${surah.nameAr} · آية $ayahNumber",
            titleEn = "${surah.nameEn} · Ayah $ayahNumber",
            reciterNameAr = reciter.nameAr,
            reciterNameEn = reciter.nameEn,
            repeatRemaining = 1,
            pageNumber = pageNumber ?: surah.startPage
        )
    }

    /**
     * Adds an Ayah (or short memorization segment starting at that Ayah) directly to NOOR's Hifz plans.
     */
    fun addAyahToMemorizationPlan(surahNumber: Int, ayahNumber: Int) = viewModelScope.launch {
        val surah = SurahCatalog.findSurah(surahNumber)
        val clampedStart = ayahNumber.coerceIn(1, surah.verseCount)
        val clampedEnd = (clampedStart + 4).coerceAtMost(surah.verseCount)
        val isAr = preferences.value.language == AppLanguage.ARABIC
        ecosystemRepo.saveHifzPlan(
            HifzPlanEntity(
                title = "حفظ سورة ${surah.nameAr} (آية $clampedStart - $clampedEnd)",
                targetSurahNumber = surahNumber,
                fromAyah = clampedStart,
                toAyah = clampedEnd,
                currentAyah = clampedStart,
                dailyAyahTarget = (clampedEnd - clampedStart + 1).coerceIn(1, 10)
            )
        )
        _statusBanner.value = if (isAr) {
            "تمت إضافة سورة ${surah.nameAr} (من الآية $clampedStart) إلى خطط الحفظ"
        } else {
            "Added ${surah.nameEn} (from Ayah $clampedStart) to Memorization Plans"
        }
    }

    fun jumpToQuranAudioAyah(
        surahNumber: Int,
        ayahNumber: Int,
        verses: List<QuranVerse>
    ) {
        if (!quranAudioPlayer.jumpToAyahInQueue(surahNumber, ayahNumber)) {
            playSurahVerseByVerse(
                surahNumber = surahNumber,
                verses = verses,
                reciter = preferences.value.quranReciter,
                startAyah = ayahNumber
            )
        }
    }

    // --- Yasser Al-Dosari & Islamic Audio Library ---
    fun playFullSurahWithReciter(surahNumber: Int, reciter: QuranReciter = preferences.value.quranReciter) {
        val currentReader = _surahReaderState.value
        if (currentReader is SurahReaderState.Ready && currentReader.surahNumber == surahNumber && currentReader.verses.isNotEmpty()) {
            playSurahVerseByVerse(surahNumber, currentReader.verses, reciter, startAyah = 1)
            return
        }
        cancelPendingPlaybackRequests()
        val queue = AudioLibraryCatalog.surahTracksForReciter(reciter)
        val idx = queue.indexOfFirst { it.surahNumber == surahNumber }.coerceAtLeast(0)
        quranAudioPlayer.playTrackQueue(queue, idx, preferences.value.audioPlaybackSpeed)
    }

    fun playAudioTrack(track: AudioTrackItem, queue: List<AudioTrackItem> = listOf(track)) {
        cancelPendingPlaybackRequests()
        val idx = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        quranAudioPlayer.playTrackQueue(queue, idx, preferences.value.audioPlaybackSpeed)
    }

    fun playRecentAudioEntity(recent: AudioRecentEntity) {
        val track = AudioTrackItem(
            id = recent.trackId,
            titleAr = recent.titleAr,
            titleEn = recent.titleEn,
            subtitleAr = recent.reciterNameAr,
            subtitleEn = recent.reciterNameEn,
            audioUrl = recent.audioUrl,
            category = com.example.domain.model.AudioCategory.entries.firstOrNull { it.key == recent.categoryKey }
                ?: com.example.domain.model.AudioCategory.YASSER_RECITATIONS,
            surahNumber = recent.surahNumber
        )
        playAudioTrack(track)
    }

    fun toggleAudioPauseResume() = quranAudioPlayer.togglePauseResume()
    fun playNextAudio() = quranAudioPlayer.playNext()
    fun playPreviousAudio() = quranAudioPlayer.playPrevious()
    fun seekAudioTo(ms: Long) = quranAudioPlayer.seekTo(ms)
    fun cycleAudioRepeatMode() = quranAudioPlayer.cycleRepeatMode()
    fun setAudioSleepTimer(minutes: Int?) = quranAudioPlayer.setSleepTimerMinutes(minutes)
    fun stopQuranAudio() {
        cancelPendingPlaybackRequests()
        quranAudioPlayer.stop()
    }

    fun setAudioPlaybackSpeed(speed: Float) = viewModelScope.launch {
        quranAudioPlayer.setPlaybackSpeed(speed)
        repo.prefs.setAudioPlaybackSpeed(speed)
    }

    fun toggleFavoriteAudio(trackId: String) = viewModelScope.launch {
        val isFav = favoriteAudioIds.value.contains(trackId)
        repo.toggleFavoriteAudio(trackId, isFav)
    }

    fun createAudioPlaylist(name: String) = viewModelScope.launch {
        repo.createAudioPlaylist(name)
    }

    fun deleteAudioPlaylist(playlistId: Long) = viewModelScope.launch {
        repo.deleteAudioPlaylist(playlistId)
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: String) = viewModelScope.launch {
        repo.addTrackToPlaylist(playlistId, trackId)
        val isAr = preferences.value.language == AppLanguage.ARABIC
        _statusBanner.value = if (isAr) "تمت الإضافة إلى قائمة التشغيل" else "Added to playlist"
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) = viewModelScope.launch {
        repo.removeTrackFromPlaylist(playlistId, trackId)
    }

    fun downloadTrackOffline(track: AudioTrackItem) = viewModelScope.launch {
        if (_downloadingTrackIds.value.contains(track.id)) return@launch
        val isAr = preferences.value.language == AppLanguage.ARABIC
        _downloadingTrackIds.value = _downloadingTrackIds.value + track.id
        val ok = repo.downloadAudioTrackForOffline(track)
        _downloadingTrackIds.value = _downloadingTrackIds.value - track.id
        _downloadedAudioIds.value = repo.listDownloadedAudioIds()
        _statusBanner.value = if (ok) {
            if (isAr) "تم حفظ التلاوة للاستماع دون إنترنت" else "Recitation saved for offline listening"
        } else {
            if (isAr) "تعذّر تنزيل الملف الصوتي حالياً" else "Unable to download audio right now"
        }
    }

    fun deleteOfflineTrack(trackId: String) = viewModelScope.launch {
        repo.deleteCachedAudioTrack(trackId)
        _downloadedAudioIds.value = repo.listDownloadedAudioIds()
    }

    // --- Backup, Restore & Reset ---
    suspend fun buildBackupJson(): String = repo.exportBackupJson()

    fun restoreBackupJson(json: String) = viewModelScope.launch {
        val ok = repo.importBackupJson(json)
        val isAr = preferences.value.language == AppLanguage.ARABIC
        _statusBanner.value = if (ok) {
            if (isAr) "تمت استعادة النسخة الاحتياطية بنجاح" else "Backup restored successfully"
        } else {
            if (isAr) "ملف النسخة الاحتياطية غير صالح" else "Invalid backup file format"
        }
    }

    fun resetApplication() = viewModelScope.launch {
        _liveTasbihCount.value = 0
        quranAudioPlayer.stop()
        dhikrTtsHelper.stop()
        repo.resetAllAppData()
        _downloadedAudioIds.value = emptySet()
        val isAr = preferences.value.language == AppLanguage.ARABIC
        _statusBanner.value = if (isAr) "تمت إعادة ضبط التطبيق بالكامل" else "Application reset completed"
    }

    private fun triggerHaptic(light: Boolean) {
        try {
            val ctx = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = if (light) 22L else 70L
                val amplitude = if (light) 90 else 200
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (light) 22L else 70L)
            }
        } catch (_: Exception) {
        }
    }

    private fun playClickTone(
        isMilestone: Boolean,
        timbre: com.example.domain.tasbih.BeadMaterialTimbre = com.example.domain.tasbih.BeadMaterialTimbre.AMBER,
        isReverse: Boolean = false
    ) {
        try {
            com.example.domain.tasbih.RealisticBeadSoundPlayer.playBeadClick(
                timbre = timbre,
                isReverse = isReverse,
                isMilestone = isMilestone
            )
        } catch (_: Exception) {
        }
    }

    // --- Smart Update: Home Screen Shortcut Routing ---
    fun dispatchShortcutRoute(route: String?) {
        if (!route.isNullOrBlank()) {
            _pendingShortcutRoute.value = route
        }
    }

    fun consumeShortcutRoute() {
        _pendingShortcutRoute.value = null
    }

    // --- Smart Update: Persistent Prayer Notification & Quran Auto-Scroll Speed ---
    fun setPersistentPrayerNotificationEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.prefs.setPersistentPrayerNotificationEnabled(enabled)
        val updated = preferences.value.copy(persistentPrayerNotificationEnabled = enabled)
        PrayerNotificationController.updateOrCancel(getApplication(), updated)
    }

    fun setQuranAutoScrollSpeed(speed: Float) = viewModelScope.launch {
        repo.prefs.setQuranAutoScrollSpeed(speed)
    }

    // --- Smart Update: Advanced Adhan Sound Customization (Default: Sheikh Nasser Al-Qatami) ---
    fun setAdhanSoundEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.prefs.setAdhanSoundEnabled(enabled)
        NoorAlarmScheduler.rescheduleAll(getApplication(), repo.prefs.getPreferencesOnce())
    }

    fun setAdhanNotificationOnly(notificationOnly: Boolean) = viewModelScope.launch {
        repo.prefs.setAdhanNotificationOnly(notificationOnly)
        val isAr = preferences.value.language == AppLanguage.ARABIC
        _statusBanner.value = if (notificationOnly) {
            if (isAr) "تم تفعيل وضع الإشعار فقط (بدون تشغيل الأذان الكامل)" else "Notification-only mode enabled"
        } else {
            if (isAr) "تم تفعيل تشغيل الأذان الكامل تلقائياً عند دخول الوقت" else "Full automatic Adhan playback enabled"
        }
    }

    fun setAdhanVolume(volume: Float) = viewModelScope.launch {
        repo.prefs.setAdhanVolume(volume)
    }

    fun setAdhanRespectSilentMode(respect: Boolean) = viewModelScope.launch {
        repo.prefs.setAdhanRespectSilentMode(respect)
    }

    fun setDefaultAdhanVoice(voiceId: String) = viewModelScope.launch {
        repo.prefs.setDefaultAdhanVoice(voiceId)
        val isAr = preferences.value.language == AppLanguage.ARABIC
        val voice = AdhanCatalog.resolveVoice(voiceId, preferences.value.customAdhanRecords, getApplication())
        _statusBanner.value = if (isAr) {
            "تم اعتماد صوت الأذان: ${voice.nameAr}"
        } else {
            "Default Adhan voice saved: ${voice.nameEn}"
        }
    }

    fun setPrayerSpecificAdhanVoice(prayer: PrayerName, voiceId: String) = viewModelScope.launch {
        repo.prefs.setPrayerSpecificAdhanVoice(prayer, voiceId)
    }

    fun downloadAdhanForOffline(voice: AdhanVoiceOption) = viewModelScope.launch {
        val isAr = preferences.value.language == AppLanguage.ARABIC
        _adhanDownloadStates.value = _adhanDownloadStates.value + (voice.id to AdhanDownloadState.Downloading(0.05f))
        val result = withContext(Dispatchers.IO) {
            AdhanCatalog.downloadAndVerifyVoice(getApplication(), voice) { progress ->
                _adhanDownloadStates.value = _adhanDownloadStates.value + (voice.id to AdhanDownloadState.Downloading(progress))
            }
        }
        _adhanDownloadStates.value = _adhanDownloadStates.value + (voice.id to result)
        _statusBanner.value = when (result) {
            is AdhanDownloadState.VerifiedOffline -> if (isAr) {
                "تم تنزيل والتحقق من ${voice.nameAr} للعمل بدون إنترنت ✓"
            } else {
                "Downloaded & verified ${voice.nameEn} for offline playback ✓"
            }
            is AdhanDownloadState.Failed -> if (isAr) result.messageAr else result.messageEn
            else -> null
        }
    }

    fun deleteOfflineAdhanDownload(voiceId: String) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            AdhanCatalog.deleteOfflineDownload(getApplication(), voiceId)
        }
        _adhanDownloadStates.value = _adhanDownloadStates.value - voiceId
        val isAr = preferences.value.language == AppLanguage.ARABIC
        _statusBanner.value = if (isAr) "تم حذف النسخة المحفوظة محلياً" else "Deleted local offline copy"
    }

    fun previewOrStopAdhan(voice: AdhanVoiceOption) {
        val isAr = preferences.value.language == AppLanguage.ARABIC
        val resolved = AdhanCatalog.resolveVoice(voice.id, preferences.value.customAdhanRecords, getApplication())
        adhanAudioPlayer.playVoice(
            voice = resolved,
            isAlarmPlayback = false,
            volume = preferences.value.adhanVolume,
            onFallbackNotice = {
                _statusBanner.value = if (isAr) {
                    "تعذّر تشغيل الملف المحدد — تم تشغيل النغمة الافتراضية"
                } else {
                    "Selected Adhan source unavailable — playing default tone"
                }
            }
        )
    }

    fun stopAdhanPreview() {
        adhanAudioPlayer.stop()
    }

    fun testAdhanPlaybackNow(prayer: PrayerName = PrayerName.FAJR) {
        val isAr = preferences.value.language == AppLanguage.ARABIC
        val label = PrayerNotificationController.prayerLabel(prayer, isAr)
        AdhanPlaybackService.startAdhan(
            context = getApplication(),
            prayer = prayer,
            title = if (isAr) "تجربة أذان $label" else "Adhan Preview · $label",
            body = if (isAr) "اضغط إيقاف الأذان لإنهاء التجربة" else "Tap Stop Adhan to end test",
            isTestAlarm = true
        )
    }

    fun scheduleTestAdhanInSeconds(prayer: PrayerName = PrayerName.FAJR, delaySeconds: Int = 15) {
        val isAr = preferences.value.language == AppLanguage.ARABIC
        NoorAlarmScheduler.scheduleTestPrayerAlarm(getApplication(), prayer, delaySeconds, isAr)
        _statusBanner.value = if (isAr) {
            "تم جدولة أذان تجريبي بعد $delaySeconds ثانية — يمكنك إغلاق الشاشة الآن لاختبار التشغيل الخلفي"
        } else {
            "Test Adhan alarm scheduled in ${delaySeconds}s — you may background the app now"
        }
    }

    fun restoreAdhanDefaults() = viewModelScope.launch {
        repo.prefs.restoreAdhanDefaults()
        NoorAlarmScheduler.rescheduleAll(getApplication(), repo.prefs.getPreferencesOnce())
        val isAr = preferences.value.language == AppLanguage.ARABIC
        _statusBanner.value = if (isAr) {
            "تمت استعادة الإعدادات الافتراضية للأذان (الشيخ ناصر القطامي)"
        } else {
            "Adhan settings restored to defaults (Sheikh Nasser Al-Qatami)"
        }
    }

    fun stopActiveAdhanAlarm() {
        AdhanPlaybackService.stopAdhan(getApplication())
        adhanAudioPlayer.stop()
    }

    fun importCustomAdhanAudio(uri: Uri) = viewModelScope.launch {
        val isAr = preferences.value.language == AppLanguage.ARABIC
        val result = withContext(Dispatchers.IO) {
            AdhanCatalog.importFromUri(getApplication(), uri)
        }
        when (result) {
            is AdhanImportResult.Success -> {
                repo.prefs.addCustomAdhanRecord(result.record)
                repo.prefs.setDefaultAdhanVoice(result.record.id)
                _statusBanner.value = if (isAr) {
                    "تم استيراد صوت الأذان بنجاح: ${result.record.displayName}"
                } else {
                    "Imported Adhan audio: ${result.record.displayName}"
                }
            }
            is AdhanImportResult.Error -> {
                _statusBanner.value = if (isAr) result.messageAr else result.messageEn
            }
        }
    }

    fun deleteCustomAdhanRecord(record: CustomAdhanRecord) = viewModelScope.launch {
        if (adhanAudioPlayer.playingVoiceId.value == record.id) {
            adhanAudioPlayer.stop()
        }
        withContext(Dispatchers.IO) {
            AdhanCatalog.deleteImportedFile(record)
        }
        repo.prefs.removeCustomAdhanRecord(record.id)
        val isAr = preferences.value.language == AppLanguage.ARABIC
        _statusBanner.value = if (isAr) "تم حذف الملف الصوتي المستورد" else "Imported Adhan audio removed"
    }

    // --- Smart Update: Live On-Device Quran Audio Recognition (Listen & Follow) ---
    fun syncLiveTrackerSurah(surahNumber: Int, verses: List<QuranVerse>, initialAyah: Int? = null) {
        liveRecitationListener.loadSurahContext(surahNumber, verses, initialAyah)
    }

    fun startLiveRecitationTracking() {
        // Pause Quran media playback if active so microphone hears the reciter clearly
        if (quranAudioPlayer.state.value.isPlaying) {
            quranAudioPlayer.pause()
        }
        liveRecitationListener.startListening()
    }

    fun pauseLiveRecitationTracking() {
        liveRecitationListener.pauseListening()
    }

    fun resumeLiveRecitationTracking() {
        if (quranAudioPlayer.state.value.isPlaying) {
            quranAudioPlayer.pause()
        }
        liveRecitationListener.resumeListening()
    }

    fun stopLiveRecitationTracking() {
        liveRecitationListener.stopListening()
    }

    fun clearCrossSurahJumpTarget() {
        liveRecitationListener.clearCrossSurahJumpTarget()
    }

    fun setNonEssentialNotificationsEnabled(enabled: Boolean) {
        _nonEssentialNotificationsEnabled.value = enabled
        notifPrefs.edit().putBoolean("non_essential_notifications", enabled).apply()
    }

    fun recordAdImpression(adId: String) = viewModelScope.launch {
        if (adId.isBlank()) return@launch
        try {
            ecosystemRepo.recordAdImpressionLocal(adId)
            cloudRepo.recordAdImpression(adId)
        } catch (_: Exception) {
        }
    }

    fun recordAdClick(adId: String) = viewModelScope.launch {
        if (adId.isBlank()) return@launch
        try {
            ecosystemRepo.recordAdClickLocal(adId)
            cloudRepo.recordAdClick(adId)
        } catch (_: Exception) {
        }
    }

    fun markAdminMessageRead(messageId: String) = viewModelScope.launch {
        if (messageId.isBlank()) return@launch
        try {
            ecosystemRepo.markAdminMessageReadLocal(messageId)
            cloudRepo.markAdminMessageReadCloud(messageId)
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Keep quranAudioPlayer running in background via QuranAudioService so playback survives Activity closure!
        liveRecitationListener.stopListening()
        adhanAudioPlayer.stop()
        dhikrTtsHelper.shutdown()
    }

    companion object {
        /**
         * Builds the per-Ayah audio track queue for a Surah, inserting a dedicated Bismillah track
         * (`001001.mp3` by the exact same reciter, `ayahNumber = 0`) right before Ayah 1 whenever
         * `includeOpeningBismillah` is true and the Surah is neither Al-Fatihah (1) nor At-Tawbah (9).
         * Guarantees Sheikh Yasser Al-Dosari (and all Engine B reciters) recite Bismillah cleanly
         * before every Surah and between consecutive short Surahs without prematurely highlighting Ayah 1.
         */
        fun buildSurahTracksWithBismillah(
            surahNumber: Int,
            verses: List<QuranVerse>,
            reciter: QuranReciter,
            includeOpeningBismillah: Boolean = true
        ): List<AudioTrackItem> {
            val meta = SurahCatalog.findSurah(surahNumber)
            val result = mutableListOf<AudioTrackItem>()
            val firstPage = verses.firstOrNull()?.pageNumber?.takeIf { it in 1..604 } ?: meta.startPage

            if (includeOpeningBismillah && surahNumber != 1 && surahNumber != 9) {
                result += AudioTrackItem(
                    id = "bismillah_${reciter.id}_$surahNumber",
                    titleAr = "سورة ${meta.nameAr} · بسم الله الرحمن الرحيم",
                    titleEn = "${meta.nameEn} · Bismillah",
                    subtitleAr = reciter.nameAr,
                    subtitleEn = reciter.nameEn,
                    audioUrl = SurahCatalog.audioUrlForAyah(1, 1, reciter),
                    category = com.example.domain.model.AudioCategory.QURAN_RECITERS,
                    surahNumber = surahNumber,
                    ayahNumber = 0,
                    pageNumber = firstPage,
                    reciterId = reciter.id
                )
            }

            verses.forEach { v ->
                result += AudioTrackItem(
                    id = "ayah_${reciter.id}_${surahNumber}_${v.numberInSurah}",
                    titleAr = "سورة ${meta.nameAr} · آية ${v.numberInSurah}",
                    titleEn = "${meta.nameEn} · Ayah ${v.numberInSurah}",
                    subtitleAr = reciter.nameAr,
                    subtitleEn = reciter.nameEn,
                    audioUrl = SurahCatalog.audioUrlForAyah(surahNumber, v.numberInSurah, reciter),
                    category = com.example.domain.model.AudioCategory.QURAN_RECITERS,
                    surahNumber = surahNumber,
                    ayahNumber = v.numberInSurah,
                    pageNumber = v.pageNumber.takeIf { it in 1..604 } ?: meta.startPage,
                    reciterId = reciter.id
                )
            }
            return result
        }
    }
}
