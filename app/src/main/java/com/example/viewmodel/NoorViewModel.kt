package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoorApplication
import com.example.audio.AudioPlaybackState
import com.example.audio.DhikrTtsHelper
import com.example.audio.QuranAudioPlayer
import com.example.audio.QuranAudioService
import com.example.data.cloud.AdminRole
import com.example.data.cloud.AppAnnouncementDoc
import com.example.data.cloud.CmsContentCategory
import com.example.data.cloud.CmsContentDoc
import com.example.data.cloud.FirebaseAuthHelper
import com.example.data.cloud.GlobalAppConfigDoc
import com.example.data.cloud.NoorCloudRepository
import com.example.data.cloud.SupportTicketDoc
import com.example.data.cloud.UserAccountDoc
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
import com.example.data.local.prefs.UserPreferences
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
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

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
    private val cloudRepo: NoorCloudRepository = (application as NoorApplication).cloudRepository
    private val quranAudioPlayer = QuranAudioPlayer()
    private val dhikrTtsHelper = DhikrTtsHelper(application)

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

        // Keep background media foreground service synced with live audio playback state
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
            }
        }

        // Sync user profile automatically when Google Sign-In state becomes active
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    val p = preferences.value
                    cloudRepo.syncCurrentUserProfile(p.language.code, p.cityNameEn)
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
                NoorAlarmScheduler.rescheduleAll(getApplication(), prefs)
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
        repo.prefs.setUserName(name)
    }

    fun setLanguage(lang: AppLanguage) = viewModelScope.launch {
        repo.prefs.setLanguage(lang)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        repo.prefs.setThemeMode(mode)
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

    fun togglePrayerLogged(prayer: PrayerName) = viewModelScope.launch {
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

    fun sendTestNotification(): Boolean {
        val isAr = preferences.value.language == AppLanguage.ARABIC
        val sent = NoorNotificationHelper.showNotification(
            context = getApplication(),
            notificationId = 9999,
            channelId = NoorNotificationHelper.CHANNEL_PRAYER,
            title = if (isAr) "تطبيق نور — تنبيه تجريبي" else "Noor — Test Notification",
            body = if (isAr) {
                "نظام تنبيهات الصلاة والأذكار يعمل بنجاح ودقة."
            } else {
                "Prayer & Adhkar notifications are active and working properly."
            }
        )
        _statusBanner.value = if (sent) {
            if (isAr) "تم إرسال إشعار تجريبي بنجاح" else "Test notification sent"
        } else {
            if (isAr) "يرجى منح إذن الإشعارات أولاً" else "Please grant notification permission first"
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
    fun incrementTasbih() {
        tasbihHydrated = true
        val prefs = preferences.value
        val nextCount = _liveTasbihCount.value + 1
        _liveTasbihCount.value = nextCount
        val target = prefs.activeTasbihTarget.coerceAtLeast(1)

        if (prefs.tasbihVibrationEnabled) {
            triggerHaptic(light = nextCount % target != 0)
        }
        if (prefs.tasbihSoundEnabled) {
            playClickTone(isMilestone = nextCount % target == 0)
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
                repo.recordQuranReadingProgress(surahNumber, 1, versesDelta = 1)
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
        repo.prefs.setQuranReciter(reciter)
        val currentState = _surahReaderState.value
        if (currentState is SurahReaderState.Ready) {
            val updatedVerses = currentState.verses.map { v ->
                v.copy(audioUrl = SurahCatalog.audioUrlForAyah(v.surahNumber, v.numberInSurah, reciter))
            }
            _surahReaderState.value = currentState.copy(verses = updatedVerses)
        }
    }

    fun playQuranVerse(
        verse: QuranVerse,
        allVerses: List<QuranVerse>,
        repeatPerAyah: Int = 1
    ) {
        val surah = SurahCatalog.findSurah(verse.surahNumber)
        val reciter = preferences.value.quranReciter
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

    // --- Yasser Al-Dosari & Islamic Audio Library ---
    fun playFullSurahWithReciter(surahNumber: Int, reciter: QuranReciter = preferences.value.quranReciter) {
        val queue = AudioLibraryCatalog.surahTracksForReciter(reciter)
        val idx = queue.indexOfFirst { it.surahNumber == surahNumber }.coerceAtLeast(0)
        quranAudioPlayer.playTrackQueue(queue, idx, preferences.value.audioPlaybackSpeed)
    }

    fun playAudioTrack(track: AudioTrackItem, queue: List<AudioTrackItem> = listOf(track)) {
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
    fun stopQuranAudio() = quranAudioPlayer.stop()

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

    private fun playClickTone(isMilestone: Boolean) {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_SYSTEM, 40)
            tone.startTone(
                if (isMilestone) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP,
                if (isMilestone) 95 else 30
            )
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        quranAudioPlayer.stop()
        dhikrTtsHelper.shutdown()
    }
}
