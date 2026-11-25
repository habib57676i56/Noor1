package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoorApplication
import android.content.Intent
import com.example.audio.AudioPlaybackState
import com.example.audio.LiveRecitationState
import com.example.audio.QuranAudioService
import com.example.audio.QuranLiveRecitationListener
import com.example.audio.QuranRecitationRecorder
import com.example.audio.RecitationMistakeSoundPlayer
import com.example.audio.RecitationRecordingState
import com.example.audio.RecordingPlaybackState
import com.example.data.local.catalog.IslamicVideoCatalog
import com.example.data.local.catalog.IslamicVideoCategory
import com.example.data.local.catalog.IslamicVideoCollection
import com.example.data.local.catalog.IslamicVideoItem
import com.example.data.local.catalog.MadinahMushafPageCatalog
import com.example.data.local.catalog.QuranRadioStation
import com.example.data.local.catalog.SurahCatalog
import com.example.data.local.db.CustomQuizQuestionEntity
import com.example.data.local.db.FastingLogEntity
import com.example.data.local.db.HalaqahAttendanceEntity
import com.example.data.local.db.HalaqahCircleEntity
import com.example.data.local.db.HalaqahStudentEntity
import com.example.data.local.db.HalaqahTeacherEntity
import com.example.data.local.db.HifzPlanEntity
import com.example.data.local.db.HifzRevisionLogEntity
import com.example.data.local.db.KhatmPlanEntity
import com.example.data.local.db.KidsProgressEntity
import com.example.data.local.db.LibraryBookEntity
import com.example.data.local.db.LocalAdminAccountEntity
import com.example.data.local.db.LocalAiKnowledgeEntity
import com.example.data.local.db.QuizAttemptEntity
import com.example.data.local.db.QuranPersonalNoteEntity
import com.example.data.local.db.RecitationAssessmentEntity
import com.example.data.local.db.StudentRecitationRecordingEntity
import com.example.data.local.db.TajweedProgressEntity
import com.example.data.quran.IslamicVideoRepository
import com.example.data.quran.VideoEpisodesResult
import com.example.data.repository.StorageUsageSummary
import com.example.data.repository.SurahLoadResult
import com.example.domain.ai.AiIndexStats
import com.example.domain.ai.NoorAiAnswer
import com.example.domain.ai.NoorLocalAiEngine
import com.example.domain.model.AudioCategory
import com.example.domain.model.AudioTrackItem
import com.example.domain.model.QuranReciter
import com.example.domain.model.QuranVerse
import com.example.domain.quran.AssessmentStrictness
import com.example.domain.quran.ProgressiveRecitationState
import com.example.domain.quran.ProgressiveRecitationTracker
import com.example.domain.quran.QuranRecitationAssessmentEngine
import com.example.domain.quran.RecitationAssessmentReport
import com.example.domain.quran.RecitationOverallLevel
import com.example.security.LocalAdminRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class AdminSettingsState(
    val featureHalaqatEnabled: Boolean = true,
    val featureHifzAiEnabled: Boolean = true,
    val featureLocalAiEnabled: Boolean = true,
    val featureLibraryEnabled: Boolean = true,
    val featureKidsEnabled: Boolean = true,
    val featureQuizEnabled: Boolean = true,
    val featureRadioEnabled: Boolean = true,
    val sttPreferOffline: Boolean = false,
    val assessmentStrictness: AssessmentStrictness = AssessmentStrictness.BALANCED,
    val aiMinConfidencePercent: Int = 28,
    val autoApproveAdminImports: Boolean = true,
    val localModelModeLabel: String = "Local RAG Index (Offline Pure-Kotlin)",
    val appAnnouncementBanner: String = ""
) {
    companion object {
        fun fromMap(map: Map<String, String>): AdminSettingsState = AdminSettingsState(
            featureHalaqatEnabled = map["feature_halaqat"]?.toBooleanStrictOrNull() ?: true,
            featureHifzAiEnabled = map["feature_hifz_ai"]?.toBooleanStrictOrNull() ?: true,
            featureLocalAiEnabled = map["feature_local_ai"]?.toBooleanStrictOrNull() ?: true,
            featureLibraryEnabled = map["feature_library"]?.toBooleanStrictOrNull() ?: true,
            featureKidsEnabled = map["feature_kids"]?.toBooleanStrictOrNull() ?: true,
            featureQuizEnabled = map["feature_quiz"]?.toBooleanStrictOrNull() ?: true,
            featureRadioEnabled = map["feature_radio"]?.toBooleanStrictOrNull() ?: true,
            sttPreferOffline = map["stt_prefer_offline"]?.toBooleanStrictOrNull() ?: false,
            assessmentStrictness = AssessmentStrictness.fromKey(map["assessment_strictness"]),
            aiMinConfidencePercent = map["ai_min_confidence"]?.toIntOrNull()?.coerceIn(15, 65) ?: 28,
            autoApproveAdminImports = map["auto_approve_imports"]?.toBooleanStrictOrNull() ?: true,
            localModelModeLabel = map["local_model_label"].orEmpty().ifBlank { "Local RAG Index (Offline Pure-Kotlin)" },
            appAnnouncementBanner = map["app_announcement_banner"].orEmpty()
        )
    }
}

data class LocalAdminSession(
    val account: LocalAdminAccountEntity? = null
) {
    val isSignedIn: Boolean get() = account != null
    val role: LocalAdminRole get() = LocalAdminRole.fromKey(account?.role)
    val actorName: String get() = account?.username ?: "local_admin"
}

data class HifzComparisonResult(
    val surahNumber: Int,
    val ayahNumber: Int,
    val expectedText: String,
    val spokenText: String,
    val similarityPercent: Int,
    val status: String, // MATCHED, POSSIBLE_MISTAKE, UNCERTAIN
    val missingWordsHint: List<String>
)

data class KidsParentSettings(
    val dailyMinutesLimit: Int = 30,
    val quizOnlyMode: Boolean = false,
    val audioGuidanceEnabled: Boolean = true,
    val parentLockEnabled: Boolean = false
)

/**
 * Dedicated ViewModel for the NOOR Complete Islamic Ecosystem modules.
 * Keeps NoorViewModel clean and untouched while exposing reactive StateFlows for
 * Halaqat, Hifz AI, Local NOOR AI, Islamic Library, Tajweed, Kids, Fasting, Khatm,
 * Quiz, Personal Quran Notes, Radio, and Offline Local Super Admin.
 */
class EcosystemViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NoorApplication
    private val repo = app.ecosystemRepository
    private val mainRepo = app.repository
    private val audioPlayer = app.quranAudioPlayer
    private val hifzMicListener = QuranLiveRecitationListener(application.applicationContext)
    private val recitationRecorder = QuranRecitationRecorder(application.applicationContext)
    private val mistakeSoundPlayer = RecitationMistakeSoundPlayer()
    private val progressiveTracker = ProgressiveRecitationTracker()
    private val videoRepository = app.islamicVideoRepository

    // Halaqat flows
    val circles = repo.circles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val teachers = repo.teachers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val students = repo.students.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val attendance = repo.attendance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assessments = repo.assessments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Hifz AI flows (Recording + Speech-to-Text + Word-Level Assessment + Page Progressive Dictation)
    val hifzPlans = repo.hifzPlans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hifzRevisionLogs = repo.hifzRevisionLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hifzWeakVerses = repo.hifzWeakVerses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recitationRecordings = repo.recitationRecordings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hifzLiveState: StateFlow<LiveRecitationState> = hifzMicListener.state
    val recordingState: StateFlow<RecitationRecordingState> = recitationRecorder.recordingState
    val recordingPlaybackState: StateFlow<RecordingPlaybackState> = recitationRecorder.playbackState
    val quranAudioState: StateFlow<AudioPlaybackState> = audioPlayer.state
    val selectedReciter: StateFlow<QuranReciter> = mainRepo.prefs.preferencesFlow
        .map { it.quranReciter }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuranReciter.YASSER_AL_DOSARI)

    private val _hifzVerses = MutableStateFlow<List<QuranVerse>>(emptyList())
    val hifzVerses: StateFlow<List<QuranVerse>> = _hifzVerses.asStateFlow()

    private val _hifzPageMode = MutableStateFlow(true)
    val hifzPageMode: StateFlow<Boolean> = _hifzPageMode.asStateFlow()

    private val _hifzSelectedPage = MutableStateFlow(1)
    val hifzSelectedPage: StateFlow<Int> = _hifzSelectedPage.asStateFlow()

    private val _hifzVersesLoading = MutableStateFlow(false)
    val hifzVersesLoading: StateFlow<Boolean> = _hifzVersesLoading.asStateFlow()

    private val _hifzLoadError = MutableStateFlow<String?>(null)
    val hifzLoadError: StateFlow<String?> = _hifzLoadError.asStateFlow()

    private val _progressiveState = MutableStateFlow(ProgressiveRecitationState())
    val progressiveState: StateFlow<ProgressiveRecitationState> = _progressiveState.asStateFlow()

    private val _mistakeSoundEnabled = MutableStateFlow(true)
    val mistakeSoundEnabled: StateFlow<Boolean> = _mistakeSoundEnabled.asStateFlow()

    private val _hifzComparison = MutableStateFlow<HifzComparisonResult?>(null)
    val hifzComparison: StateFlow<HifzComparisonResult?> = _hifzComparison.asStateFlow()

    private val _recitationReport = MutableStateFlow<RecitationAssessmentReport?>(null)
    val recitationReport: StateFlow<RecitationAssessmentReport?> = _recitationReport.asStateFlow()

    // Islamic Videos flows
    private val _selectedVideoCollection = MutableStateFlow<IslamicVideoCollection?>(
        IslamicVideoCatalog.curatedCollections.firstOrNull()
    )
    val selectedVideoCollection: StateFlow<IslamicVideoCollection?> = _selectedVideoCollection.asStateFlow()

    private val _videoEpisodes = MutableStateFlow<List<IslamicVideoItem>>(emptyList())
    val videoEpisodes: StateFlow<List<IslamicVideoItem>> = _videoEpisodes.asStateFlow()

    val userVideos: StateFlow<List<IslamicVideoItem>> = videoRepository.publishedVideos

    private val _videosLoading = MutableStateFlow(false)
    val videosLoading: StateFlow<Boolean> = _videosLoading.asStateFlow()

    private val _videosError = MutableStateFlow<String?>(null)
    val videosError: StateFlow<String?> = _videosError.asStateFlow()

    private val _activeVideoItem = MutableStateFlow<IslamicVideoItem?>(null)
    val activeVideoItem: StateFlow<IslamicVideoItem?> = _activeVideoItem.asStateFlow()

    // Local NOOR AI flows
    val allAiKnowledge = repo.allAiKnowledge.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val approvedAiKnowledge = repo.approvedAiKnowledge.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val importedAiDocuments = repo.importedAiDocuments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiConversation = MutableStateFlow<List<NoorAiAnswer>>(emptyList())
    val aiConversation: StateFlow<List<NoorAiAnswer>> = _aiConversation.asStateFlow()

    private val _aiIndexStats = MutableStateFlow(AiIndexStats())
    val aiIndexStats: StateFlow<AiIndexStats> = _aiIndexStats.asStateFlow()

    // Library flows
    val libraryBooks = repo.libraryBooks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val libraryNotes = repo.libraryNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tajweed & Kids
    val tajweedProgress = repo.tajweedProgress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val kidsProgress = repo.kidsProgress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _kidsParentSettings = MutableStateFlow(KidsParentSettings())
    val kidsParentSettings: StateFlow<KidsParentSettings> = _kidsParentSettings.asStateFlow()

    // Fasting & Khatm
    val fastingLogs = repo.fastingLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val khatmPlans = repo.khatmPlans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val khatmLogs = repo.khatmLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Quiz
    val quizQuestions = repo.quizQuestions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val quizAttempts = repo.quizAttempts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Personal Quran Notes
    val noteCollections = repo.noteCollections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val personalNotes = repo.personalNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Local Offline Super Admin + Advanced Admin Settings
    val localAdmins = repo.localAdmins.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val localAuditLogs = repo.localAuditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val systemErrorLogs = repo.systemErrorLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val backupSnapshots = repo.backupSnapshots.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminSettings: StateFlow<AdminSettingsState> = repo.adminConfigEntries
        .map { list -> AdminSettingsState.fromMap(list.associate { it.configKey to it.configValue }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminSettingsState())

    private val _storageUsage = MutableStateFlow(StorageUsageSummary())
    val storageUsage: StateFlow<StorageUsageSummary> = _storageUsage.asStateFlow()

    private val _adminSession = MutableStateFlow(LocalAdminSession())
    val adminSession: StateFlow<LocalAdminSession> = _adminSession.asStateFlow()

    private val _hasSuperAdmin = MutableStateFlow<Boolean?>(null)
    val hasSuperAdmin: StateFlow<Boolean?> = _hasSuperAdmin.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val audioState: StateFlow<AudioPlaybackState> = audioPlayer.state

    init {
        videoRepository.getUserVideos()
        viewModelScope.launch {
            runCatching { repo.ensureSeeded() }
            _hasSuperAdmin.value = runCatching { repo.hasLocalSuperAdmin() }.getOrDefault(false)
            _aiIndexStats.value = runCatching { repo.reindexLocalKnowledge() }.getOrDefault(AiIndexStats())
            _storageUsage.value = runCatching { repo.calculateStorageUsage() }.getOrDefault(StorageUsageSummary())
            val cfg = runCatching { AdminSettingsState.fromMap(repo.getAllAdminConfigOnce()) }.getOrDefault(AdminSettingsState())
            hifzMicListener.setPreferOffline(cfg.sttPreferOffline)
            loadHifzSurah(1)
        }
        viewModelScope.launch {
            var lastTranscriptSeen = ""
            var lastMistakeEventSeen = 0
            hifzMicListener.state.collect { live ->
                val transcript = live.effectiveTranscript
                if (transcript != lastTranscriptSeen && transcript.isNotBlank()) {
                    lastTranscriptSeen = transcript
                    val nextState = progressiveTracker.processTranscript(
                        fullTranscript = transcript,
                        isPartialStreaming = live.isActive
                    )
                    _progressiveState.value = nextState
                    if (nextState.mistakeEventId > lastMistakeEventSeen) {
                        lastMistakeEventSeen = nextState.mistakeEventId
                        if (_mistakeSoundEnabled.value) {
                            mistakeSoundPlayer.playMistakeAlert()
                        }
                    }
                } else if (transcript.isBlank()) {
                    lastTranscriptSeen = ""
                }
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun postStatus(msg: String) {
        _statusMessage.value = msg
    }

    fun todayKey(): String = repo.todayKey()

    // ======================== HALAQAT ACTIONS ========================

    fun saveCircle(
        name: String,
        description: String,
        level: String,
        circleType: String,
        scheduleText: String,
        teacherId: Long?,
        existingId: Long = 0L
    ) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        repo.saveCircle(
            HalaqahCircleEntity(
                id = existingId,
                name = name.trim(),
                description = description.trim(),
                level = level,
                circleType = circleType,
                scheduleText = scheduleText.trim(),
                teacherId = teacherId
            )
        )
        postStatus("تم حفظ الحلقة بنجاح")
    }

    fun deleteCircle(id: Long) = viewModelScope.launch {
        repo.deleteCircle(id)
        postStatus("تم حذف الحلقة")
    }

    fun saveTeacher(
        fullName: String,
        phone: String,
        specialization: String,
        scheduleNotes: String,
        existingId: Long = 0L
    ) = viewModelScope.launch {
        if (fullName.isBlank()) return@launch
        repo.saveTeacher(
            HalaqahTeacherEntity(
                id = existingId,
                fullName = fullName.trim(),
                phone = phone.trim(),
                specialization = specialization.trim().ifBlank { "حفظ وتجويد" },
                scheduleNotes = scheduleNotes.trim()
            )
        )
        postStatus("تم حفظ بيانات المعلم")
    }

    fun deleteTeacher(id: Long) = viewModelScope.launch {
        repo.deleteTeacher(id)
        postStatus("تم حذف المعلم")
    }

    fun saveStudent(
        fullName: String,
        age: Int,
        guardianContact: String,
        circleId: Long?,
        teacherId: Long?,
        memorizedJuz: Int,
        memorizedPages: Int,
        revisionPages: Int,
        currentSurah: Int,
        teacherFeedback: String,
        existingId: Long = 0L
    ) = viewModelScope.launch {
        if (fullName.isBlank()) return@launch
        repo.saveStudent(
            HalaqahStudentEntity(
                id = existingId,
                fullName = fullName.trim(),
                age = age.coerceIn(4, 99),
                guardianContact = guardianContact.trim(),
                circleId = circleId,
                teacherId = teacherId,
                memorizedJuz = memorizedJuz.coerceIn(0, 30),
                memorizedPages = memorizedPages.coerceIn(0, 604),
                revisionPages = revisionPages.coerceIn(0, 604),
                currentSurahNumber = currentSurah.coerceIn(1, 114),
                teacherFeedback = teacherFeedback.trim()
            )
        )
        postStatus("تم حفظ سجل الطالب")
    }

    fun deleteStudent(id: Long) = viewModelScope.launch {
        repo.deleteStudent(id)
        postStatus("تم حذف الطالب")
    }

    fun markAttendance(circleId: Long, studentId: Long, status: String, notes: String = "") = viewModelScope.launch {
        repo.recordAttendance(
            HalaqahAttendanceEntity(
                circleId = circleId,
                studentId = studentId,
                dateKey = repo.todayKey(),
                status = status,
                notes = notes.trim()
            )
        )
        postStatus("تم تسجيل الحضور ($status)")
    }

    fun recordRecitationAssessment(
        studentId: Long,
        teacherId: Long?,
        circleId: Long?,
        surahNumber: Int,
        fromAyah: Int,
        toAyah: Int,
        newMemSummary: String,
        prevRevSummary: String,
        accuracyPercent: Int,
        tajweedObservations: String,
        mistakesToRevise: String,
        teacherComments: String,
        nextDateKey: String
    ) = viewModelScope.launch {
        repo.saveAssessment(
            RecitationAssessmentEntity(
                studentId = studentId,
                teacherId = teacherId,
                circleId = circleId,
                surahNumber = surahNumber.coerceIn(1, 114),
                fromAyah = fromAyah.coerceAtLeast(1),
                toAyah = toAyah.coerceAtLeast(fromAyah),
                newMemorizationSummary = newMemSummary.trim(),
                previousRevisionSummary = prevRevSummary.trim(),
                accuracyPercent = accuracyPercent.coerceIn(0, 100),
                tajweedObservations = tajweedObservations.trim(),
                mistakesToRevise = mistakesToRevise.trim(),
                teacherComments = teacherComments.trim(),
                assessmentDateKey = repo.todayKey(),
                nextAssessmentDateKey = nextDateKey.trim(),
                approvedByTeacher = true
            )
        )
        postStatus("تم حفظ تقييم التسميع بنجاح")
    }

    fun toggleAssessmentApproval(id: Long, approved: Boolean) = viewModelScope.launch {
        repo.setAssessmentApproval(id, approved)
    }

    fun deleteAssessment(id: Long) = viewModelScope.launch {
        repo.deleteAssessment(id)
    }

    // ======================== HIFZ AI ACTIONS ========================

    fun loadHifzSurah(surahNumber: Int, startAyah: Int? = null) = viewModelScope.launch {
        val clamped = surahNumber.coerceIn(1, 114)
        val surahMeta = SurahCatalog.findSurah(clamped)
        _hifzPageMode.value = false
        _hifzSelectedPage.value = surahMeta.startPage
        _hifzVersesLoading.value = true
        _hifzLoadError.value = null
        val reciter = selectedReciter.value
        val result = mainRepo.loadSurahVerses(clamped, reciter)
        val verses = SurahCatalog.getBundledSurahVerses(clamped, reciter)
            ?: when (result) {
                is SurahLoadResult.Success -> result.verses
                is SurahLoadResult.Error -> emptyList()
            }
        _hifzVerses.value = verses
        _hifzVersesLoading.value = false
        if (verses.isEmpty() && result is SurahLoadResult.Error) {
            _hifzLoadError.value = result.messageAr
        } else {
            hifzMicListener.clearTranscript()
            hifzMicListener.loadSurahContext(clamped, verses, startAyah ?: verses.firstOrNull()?.numberInSurah ?: 1)
            _progressiveState.value = progressiveTracker.loadVerses(
                verses = verses,
                titleAr = "سورة ${surahMeta.nameAr}",
                titleEn = "Surah ${surahMeta.nameEn}",
                startAtSurah = clamped,
                startAtAyah = startAyah
            )
        }
        _hifzComparison.value = null
        _recitationReport.value = null
    }

    /**
     * Loads verified Uthmani verses for ANY of the 604 Madinah Mushaf pages (covering all 114 Surahs),
     * including multi-Surah pages such as Page 604 (Al-Ikhlas, Al-Falaq, An-Nas).
     */
    fun loadHifzMushafPage(pageNumber: Int, startSurah: Int? = null, startAyah: Int? = null) = viewModelScope.launch {
        val clampedPage = pageNumber.coerceIn(1, MadinahMushafPageCatalog.TOTAL_PAGES)
        _hifzPageMode.value = true
        _hifzSelectedPage.value = clampedPage
        _hifzVersesLoading.value = true
        _hifzLoadError.value = null

        val meta = MadinahMushafPageCatalog.pageMeta(clampedPage)
        val reciter = selectedReciter.value
        val collected = withContext(Dispatchers.IO) {
            val list = mutableListOf<QuranVerse>()
            for (s in meta.surahsOnPage) {
                val surahVerses = when (val res = mainRepo.loadSurahVerses(s.number, reciter)) {
                    is SurahLoadResult.Success -> res.verses
                    is SurahLoadResult.Error -> SurahCatalog.getBundledSurahVerses(s.number, reciter).orEmpty()
                }
                val onPage = surahVerses.filter { it.pageNumber == clampedPage }
                if (onPage.isNotEmpty()) {
                    list += onPage
                } else if (meta.surahsOnPage.size == 1 && surahVerses.isNotEmpty()) {
                    list += surahVerses
                }
            }
            list.sortedWith(compareBy({ it.surahNumber }, { it.numberInSurah }))
        }

        _hifzVersesLoading.value = false
        if (collected.isEmpty()) {
            _hifzLoadError.value = "تعذّر تحميل صفحة المصحف $clampedPage. تأكد من الاتصال بالإنترنت أو حمّل المصحف دون اتصال."
            return@launch
        }
        _hifzVerses.value = collected
        hifzMicListener.clearTranscript()
        hifzMicListener.loadSurahContext(
            meta.primarySurahNumber,
            collected,
            startAyah ?: collected.first().numberInSurah
        )
        val surahsLabelAr = meta.surahsOnPage.joinToString(" · ") { it.nameAr }
        val surahsLabelEn = meta.surahsOnPage.joinToString(" · ") { it.nameEn }
        _progressiveState.value = progressiveTracker.loadVerses(
            verses = collected,
            titleAr = "صفحة $clampedPage — $surahsLabelAr (جزء ${meta.juzNumber})",
            titleEn = "Page $clampedPage — $surahsLabelEn (Juz ${meta.juzNumber})",
            startAtSurah = startSurah,
            startAtAyah = startAyah
        )
        _hifzComparison.value = null
        _recitationReport.value = null
    }

    fun toggleHifzScopeMode(pageMode: Boolean, currentSurah: Int) {
        if (pageMode) {
            val targetPage = SurahCatalog.findSurah(currentSurah).startPage
            loadHifzMushafPage(targetPage)
        } else {
            loadHifzSurah(currentSurah)
        }
    }

    fun resetProgressiveRecitation() {
        hifzMicListener.clearTranscript()
        _progressiveState.value = progressiveTracker.reset()
    }

    fun jumpProgressiveToAyah(surahNumber: Int, ayahNumber: Int) {
        _progressiveState.value = progressiveTracker.jumpToAyah(surahNumber, ayahNumber)
    }

    fun nudgeProgressiveSingleWord() {
        _progressiveState.value = progressiveTracker.nudgeRevealSingleWord()
    }

    fun toggleMistakeSoundEnabled() {
        val next = !_mistakeSoundEnabled.value
        _mistakeSoundEnabled.value = next
        mistakeSoundPlayer.setMuted(!next)
    }

    fun submitProgressiveDictation(spokenText: String) {
        if (spokenText.isBlank()) return
        val prevMistakeId = _progressiveState.value.mistakeEventId
        val next = progressiveTracker.processTranscript(spokenText, isPartialStreaming = false)
        _progressiveState.value = next
        if (next.mistakeEventId > prevMistakeId && _mistakeSoundEnabled.value) {
            mistakeSoundPlayer.playMistakeAlert()
        }
    }

    fun getMistakeAlertPlayCountForTest(): Int = mistakeSoundPlayer.playedCountForTest

    fun selectReciter(reciter: QuranReciter) = viewModelScope.launch {
        val prev = audioPlayer.state.value
        val wasPlayingSingleAyah = (prev.isPlaying || prev.isBuffering) && prev.playingSurah != null && prev.playingAyah != null
        val prevSurah = prev.playingSurah
        val prevAyah = prev.playingAyah
        if (wasPlayingSingleAyah) {
            audioPlayer.stop()
        }
        mainRepo.prefs.setQuranReciter(reciter)
        if (wasPlayingSingleAyah && prevSurah != null && prevAyah != null) {
            playReferenceAyah(prevSurah, prevAyah, explicitReciter = reciter)
        }
    }

    fun startHifzLiveMic(resetTranscript: Boolean = false) {
        if (audioPlayer.state.value.isPlaying) {
            audioPlayer.pause()
        }
        val wasRecording = recordingState.value.isRecordingOrPaused
        if (wasRecording) {
            recitationRecorder.stopRecording()
        }
        hifzMicListener.setPreferOffline(adminSettings.value.sttPreferOffline)
        // Give Android audio HAL 250ms to release MediaRecorder before acquiring SpeechRecognizer mic
        hifzMicListener.startListening(
            resetTranscript = resetTranscript,
            initialDelayMs = if (wasRecording) 250L else 0L
        )
    }

    fun pauseHifzLiveMic() = hifzMicListener.pauseListening()
    fun resumeHifzLiveMic() {
        if (audioPlayer.state.value.isPlaying) {
            audioPlayer.pause()
        }
        hifzMicListener.resumeListening()
    }
    fun retryHifzLiveMic() {
        startHifzLiveMic(resetTranscript = false)
    }
    fun stopHifzLiveMic(clearTranscript: Boolean = false) = hifzMicListener.stopListening(clearTranscript)
    fun clearHifzTranscript() = hifzMicListener.clearTranscript()
    fun clearRecitationReport() {
        _recitationReport.value = null
        _hifzComparison.value = null
    }
    fun setSttPreferOfflineLocal(preferOffline: Boolean) {
        hifzMicListener.setPreferOffline(preferOffline)
    }
    fun injectHifzSpeechResult(text: String) {
        hifzMicListener.injectRecognizedText(text, isFinal = true)
    }
    fun buildSystemSpeechIntent(): Intent = hifzMicListener.buildSystemSpeechIntent()
    fun isSpeechRecognitionAvailable(): Boolean = hifzMicListener.isSpeechServiceAvailable()
    fun inspectSttCapabilities() = hifzMicListener.inspectCapabilities()

    // Real MediaRecorder Audio Recording + Playback for Hifz Recitation
    fun startRecitationRecording(surahNumber: Int, ayahNumber: Int) {
        if (audioPlayer.state.value.isPlaying) {
            audioPlayer.pause()
        }
        if (hifzLiveState.value.isActive) {
            hifzMicListener.stopListening(clearTranscript = false)
        }
        val ok = recitationRecorder.startRecording("s${surahNumber}_a${ayahNumber}")
        if (ok) {
            postStatus("بدأ تسجيل تلاوتك بصيغة M4A")
        }
    }

    fun pauseRecitationRecording() = recitationRecorder.pauseRecording()
    fun resumeRecitationRecording() = recitationRecorder.resumeRecording()

    fun stopRecitationRecording(
        surahNumber: Int,
        fromAyah: Int,
        toAyah: Int,
        testMode: String = "LIVE_RECITATION",
        studentId: Long? = null,
        studentName: String = ""
    ) {
        val savedPath = recitationRecorder.stopRecording() ?: return
        val recState = recitationRecorder.recordingState.value
        val transcript = hifzLiveState.value.effectiveTranscript
        val report = _recitationReport.value
        viewModelScope.launch {
            repo.saveRecitationRecording(
                StudentRecitationRecordingEntity(
                    studentId = studentId,
                    studentName = studentName.ifBlank { "المستخدم الحالي" },
                    surahNumber = surahNumber,
                    fromAyah = fromAyah,
                    toAyah = toAyah,
                    testMode = testMode,
                    filePath = savedPath,
                    durationMs = recState.lastSavedDurationMs,
                    recognizedTranscript = transcript,
                    accuracyPercent = report?.accuracyPercent ?: 0,
                    suspectedMistakesSummary = report?.suspectedMistakeWords?.take(6)?.joinToString(" | ").orEmpty(),
                    uncertainCount = report?.recognitionUncertainWords?.size ?: 0,
                    teacherReviewStatus = "PENDING"
                )
            )
            refreshStorageUsage()
            postStatus("تم حفظ تسجيل التلاوة بنجاح (${recState.lastSavedDurationMs / 1000} ثانية)")
        }
    }

    fun playSavedRecording(filePath: String) {
        if (audioPlayer.state.value.isPlaying || audioPlayer.state.value.isBuffering) {
            audioPlayer.pause()
        }
        recitationRecorder.playRecording(filePath)
    }

    fun pauseSavedRecording() = recitationRecorder.pausePlayback()
    fun resumeSavedRecording() = recitationRecorder.resumePlayback()
    fun stopSavedRecording() = recitationRecorder.stopPlayback()

    fun deleteSavedRecording(entity: StudentRecitationRecordingEntity) = viewModelScope.launch {
        recitationRecorder.deleteRecordingFile(entity.filePath)
        repo.deleteRecitationRecording(entity.id)
        refreshStorageUsage()
        postStatus("تم حذف التسجيل الصوتي")
    }

    fun reviewStudentRecording(id: Long, status: String, comment: String) = viewModelScope.launch {
        repo.reviewRecitationRecording(id, status, comment)
        repo.logAudit(_adminSession.value.actorName, "REVIEW_RECORDING", "HIFZ", "INFO", "Recording #$id -> $status")
        postStatus("تم حفظ مراجعة المعلم على التسجيل")
    }

    /**
     * Full word-level sequence-alignment evaluation for one or multiple canonical Uthmani verses.
     * Explicitly distinguishes phonetic/ASR uncertainty from suspected mistakes and logs weak verses.
     */
    fun evaluateRecitedVerses(
        verses: List<QuranVerse>,
        spokenText: String,
        testMode: String = "LIVE_CHECK"
    ): RecitationAssessmentReport {
        val live = hifzLiveState.value
        val report = QuranRecitationAssessmentEngine.evaluate(
            verses = verses,
            spokenTranscript = spokenText,
            longPauseCount = live.pauseCount,
            longestPauseMs = live.longestPauseMs,
            strictness = adminSettings.value.assessmentStrictness
        )
        _recitationReport.value = report

        // Keep legacy HifzComparisonResult synchronized for any existing caller
        val firstVerse = verses.firstOrNull()
        if (firstVerse != null) {
            val legacyStatus = when (report.overallLevel) {
                RecitationOverallLevel.EXCELLENT -> "MATCHED"
                RecitationOverallLevel.RECOGNITION_UNCLEAR -> "UNCERTAIN"
                else -> "POSSIBLE_MISTAKE"
            }
            _hifzComparison.value = HifzComparisonResult(
                surahNumber = firstVerse.surahNumber,
                ayahNumber = firstVerse.numberInSurah,
                expectedText = verses.joinToString(" ") { it.textUthmani },
                spokenText = spokenText,
                similarityPercent = report.accuracyPercent,
                status = legacyStatus,
                missingWordsHint = report.omittedWords.take(5)
            )
        }

        if (verses.isNotEmpty() && report.overallLevel != RecitationOverallLevel.RECOGNITION_UNCLEAR) {
            viewModelScope.launch {
                repo.recordHifzRevision(
                    HifzRevisionLogEntity(
                        surahNumber = report.surahNumber,
                        fromAyah = report.fromAyah,
                        toAyah = report.toAyah,
                        sessionType = testMode,
                        accuracyPercent = report.accuracyPercent,
                        uncertainCount = report.recognitionUncertainWords.size,
                        possibleMistakeNotes = report.suspectedMistakeWords.take(5).joinToString(" ، "),
                        dateKey = repo.todayKey()
                    )
                )
                if (report.omittedWords.isNotEmpty() || report.replacedWords.isNotEmpty()) {
                    val weakAyahs = report.wordEvaluations
                        .filter {
                            it.status == com.example.domain.quran.WordEvaluationStatus.OMITTED ||
                                it.status == com.example.domain.quran.WordEvaluationStatus.REPLACED
                        }
                        .map { it.ayahNumber }
                        .distinct()
                        .ifEmpty { listOf(report.fromAyah) }
                    for (ayahNum in weakAyahs.take(3)) {
                        repo.recordWeakVerse(
                            surahNumber = report.surahNumber,
                            ayahNumber = ayahNum,
                            note = report.suspectedMistakeWords.take(3).joinToString(" ، ")
                        )
                    }
                }
            }
        }
        return report
    }

    fun evaluateRecitedVerse(verse: QuranVerse, spokenText: String) {
        evaluateRecitedVerses(listOf(verse), spokenText, testMode = "SINGLE_VERSE_CHECK")
    }

    fun saveHifzPlan(
        title: String,
        surahNumber: Int,
        fromAyah: Int,
        toAyah: Int,
        dailyTarget: Int
    ) = viewModelScope.launch {
        if (title.isBlank()) return@launch
        repo.saveHifzPlan(
            HifzPlanEntity(
                title = title.trim(),
                targetSurahNumber = surahNumber.coerceIn(1, 114),
                fromAyah = fromAyah.coerceAtLeast(1),
                toAyah = toAyah.coerceAtLeast(fromAyah),
                currentAyah = fromAyah.coerceAtLeast(1),
                dailyAyahTarget = dailyTarget.coerceIn(1, 50)
            )
        )
        postStatus("تم إنشاء خطة الحفظ")
    }

    fun advanceHifzPlan(plan: HifzPlanEntity) = viewModelScope.launch {
        val next = (plan.currentAyah + plan.dailyAyahTarget).coerceAtMost(plan.toAyah)
        repo.saveHifzPlan(
            plan.copy(
                currentAyah = next,
                isCompleted = next >= plan.toAyah
            )
        )
        repo.recordHifzRevision(
            HifzRevisionLogEntity(
                surahNumber = plan.targetSurahNumber,
                fromAyah = plan.currentAyah,
                toAyah = next,
                sessionType = "DAILY_HIFZ",
                accuracyPercent = 95,
                dateKey = repo.todayKey()
            )
        )
        postStatus("تم تحديث إنجاز الحفظ حتى الآية $next")
    }

    fun deleteHifzPlan(id: Long) = viewModelScope.launch {
        repo.deleteHifzPlan(id)
    }

    fun recordHifzSession(
        surahNumber: Int,
        fromAyah: Int,
        toAyah: Int,
        sessionType: String,
        accuracyPercent: Int,
        uncertainCount: Int = 0,
        notes: String = ""
    ) = viewModelScope.launch {
        repo.recordHifzRevision(
            HifzRevisionLogEntity(
                surahNumber = surahNumber,
                fromAyah = fromAyah,
                toAyah = toAyah,
                sessionType = sessionType,
                accuracyPercent = accuracyPercent.coerceIn(0, 100),
                uncertainCount = uncertainCount,
                possibleMistakeNotes = notes,
                dateKey = repo.todayKey()
            )
        )
        postStatus("تم تسجيل جلسة المراجعة ($accuracyPercent%)")
    }

    fun approveHifzLog(id: Long, approved: Boolean) = viewModelScope.launch {
        repo.setHifzLogApproval(id, approved)
    }

    fun markWeakVerseResolved(id: Long, resolved: Boolean) = viewModelScope.launch {
        repo.resolveWeakVerse(id, resolved)
    }

    fun playReferenceAyah(
        surahNumber: Int,
        ayahNumber: Int,
        explicitReciter: QuranReciter? = null
    ) {
        // Ensure zero audio overlap between user voice playback/mic and Sheikh playback
        recitationRecorder.stopPlayback()
        if (hifzLiveState.value.isActive) {
            hifzMicListener.stopListening(clearTranscript = false)
        }
        val surah = SurahCatalog.findSurah(surahNumber)
        val reciter = explicitReciter ?: selectedReciter.value
        val url = SurahCatalog.audioUrlForAyah(surahNumber, ayahNumber, reciter)
        audioPlayer.playAyah(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            audioUrl = url,
            titleAr = "سورة ${surah.nameAr} — آية $ayahNumber",
            titleEn = "Surah ${surah.nameEn} — Ayah $ayahNumber",
            reciterNameAr = reciter.nameAr,
            reciterNameEn = reciter.nameEn
        )
    }

    fun playRadioStation(station: QuranRadioStation) {
        recitationRecorder.stopPlayback()
        val track = AudioTrackItem(
            id = station.id,
            titleAr = station.nameAr,
            titleEn = station.nameEn,
            subtitleAr = "إذاعة نور المباشرة",
            subtitleEn = "NOOR Live Radio",
            audioUrl = station.streamUrl,
            category = AudioCategory.QURAN_RECITERS
        )
        QuranAudioService.updateNotification(app.applicationContext, station.nameAr, "بث مباشر", true)
        audioPlayer.playTrackQueue(listOf(track), 0)
        postStatus("جارٍ تشغيل: ${station.nameAr}")
    }

    fun stopAudio() {
        audioPlayer.stop()
        QuranAudioService.stopService(app.applicationContext)
    }

    // ======================== LOCAL NOOR AI ACTIONS ========================

    fun askLocalNoorAi(question: String) {
        val minConfidence = (adminSettings.value.aiMinConfidencePercent / 100f).coerceIn(0.18f, 0.65f)
        val answer = NoorLocalAiEngine.answerQuestion(
            query = question,
            approvedKnowledge = approvedAiKnowledge.value,
            approvedBooks = libraryBooks.value,
            conversationHistory = _aiConversation.value,
            minConfidenceThreshold = minConfidence
        )
        _aiConversation.value = listOf(answer) + _aiConversation.value
    }

    fun clearAiConversation() {
        _aiConversation.value = emptyList()
    }

    fun addOrUpdateAiKnowledge(
        category: String,
        titleAr: String,
        questionAr: String,
        answerAr: String,
        sourceRef: String,
        scholarlyNotes: String,
        verificationStatus: String,
        approvalStatus: String,
        keywords: String = "",
        existingId: String? = null
    ) = viewModelScope.launch {
        if (questionAr.isBlank() || answerAr.isBlank()) return@launch
        val saved = repo.saveAiKnowledge(
            LocalAiKnowledgeEntity(
                id = existingId ?: "ai_adm_${UUID.randomUUID().toString().take(10)}",
                category = category.uppercase(),
                titleAr = titleAr.trim().ifBlank { questionAr.trim().take(55) },
                titleEn = titleAr.trim().ifBlank { questionAr.trim().take(55) },
                questionAr = questionAr.trim(),
                answerAr = answerAr.trim(),
                sourceRef = sourceRef.trim().ifBlank { "مرجع محلي مضاف" },
                verificationStatus = verificationStatus,
                scholarlyNotes = scholarlyNotes.trim(),
                keywords = keywords.trim(),
                approvalStatus = approvalStatus,
                isUserAdded = true
            ),
            allowDuplicate = existingId != null
        )
        if (saved) {
            _aiIndexStats.value = repo.reindexLocalKnowledge()
            repo.logAudit(_adminSession.value.actorName, "UPSERT_AI_KNOWLEDGE", "AI_KB", "INFO", titleAr.take(50))
            postStatus("تم حفظ المعرفة وتحديث فهرس البحث المحلي بنجاح")
        } else {
            postStatus("تنبيه: هذا السؤال والإجابة موجودان مسبقاً (تم منع التكرار)")
        }
    }

    fun importKnowledgeDocumentBytes(fileName: String, bytes: ByteArray, category: String = "AUTO") = viewModelScope.launch {
        val autoApprove = adminSettings.value.autoApproveAdminImports && _adminSession.value.role.canApproveContent()
        val batch = repo.importAiDocumentBytes(
            fileName = fileName,
            bytes = bytes,
            category = category,
            autoApprove = autoApprove,
            actorName = _adminSession.value.actorName
        )
        _aiIndexStats.value = repo.reindexLocalKnowledge()
        repo.logAudit(
            _adminSession.value.actorName,
            "IMPORT_AI_DOCUMENT",
            "AI_KB",
            "INFO",
            "File=$fileName Chunks=${batch.items.size} Dup=${batch.skippedDuplicates} Lang=${batch.detectedLanguage} Cat=${batch.detectedCategory}"
        )
        val warn = batch.warningAr?.let { " — $it" }.orEmpty()
        postStatus("تمت فهرسة ${batch.items.size} مقطع من $fileName (تصنيف: ${batch.detectedCategory} · مكرر: ${batch.skippedDuplicates})$warn")
    }

    fun importKnowledgeFileContent(rawText: String, fileName: String, category: String) = viewModelScope.launch {
        val autoApprove = adminSettings.value.autoApproveAdminImports && _adminSession.value.role.canApproveContent()
        val batch = repo.importAiKnowledgeFromText(rawText, fileName, category, autoApprove)
        _aiIndexStats.value = repo.reindexLocalKnowledge()
        repo.logAudit(
            _adminSession.value.actorName,
            "IMPORT_KNOWLEDGE_FILE",
            "AI_KB",
            "INFO",
            "File=$fileName Added=${batch.items.size} Duplicates=${batch.skippedDuplicates}"
        )
        postStatus("تمت فهرسة ${batch.items.size} عنصر (تم تخطي ${batch.skippedDuplicates} مكرر)")
    }

    fun deleteImportedDocument(id: String) = viewModelScope.launch {
        repo.deleteImportedAiDocument(id)
        postStatus("تم حذف سجل المستند المستورد")
    }

    fun setAiKnowledgeApproval(id: String, status: String) = viewModelScope.launch {
        repo.setAiKnowledgeApproval(id, status)
        _aiIndexStats.value = repo.reindexLocalKnowledge()
        repo.logAudit(_adminSession.value.actorName, "AI_KB_APPROVAL", "AI_KB", "INFO", "$id -> $status")
        postStatus("تم تحديث حالة الاعتماد")
    }

    fun deleteAiKnowledge(id: String) = viewModelScope.launch {
        repo.deleteAiKnowledge(id)
        _aiIndexStats.value = repo.reindexLocalKnowledge()
        repo.logAudit(_adminSession.value.actorName, "DELETE_AI_KB", "AI_KB", "WARNING", id)
        postStatus("تم حذف العنصر من قاعدة المعرفة")
    }

    fun reindexKnowledgeBase() = viewModelScope.launch {
        val stats = repo.reindexLocalKnowledge()
        _aiIndexStats.value = stats
        repo.logAudit(_adminSession.value.actorName, "REINDEX_AI_KB", "AI_KB", "INFO", "Entries=${stats.totalEntries} Tokens=${stats.uniqueTokenCount}")
        postStatus("تمت إعادة بناء فهرس البحث المحلي: ${stats.totalEntries} مادة · ${stats.uniqueTokenCount} كلمة مفهرسة")
    }

    suspend fun exportAiKnowledgeJson(): String = repo.exportAiKnowledgeJson()

    suspend fun exportHalaqatReportText(): String = repo.buildHalaqatReportText()

    // ======================== ADVANCED ADMIN SETTINGS & DIAGNOSTICS ========================

    fun updateAdminSetting(key: String, value: String) = viewModelScope.launch {
        if (!_adminSession.value.role.canManageSettings()) {
            postStatus("إعدادات النظام المتقدمة مخصصة للمدير العام فقط")
            return@launch
        }
        repo.setAdminConfig(key, value, _adminSession.value.actorName)
        if (key == "stt_prefer_offline") {
            hifzMicListener.setPreferOffline(value.toBooleanStrictOrNull() ?: true)
        }
        repo.logAudit(_adminSession.value.actorName, "UPDATE_ADMIN_SETTING", "SETTINGS", "INFO", "$key=$value")
        postStatus("تم حفظ الإعداد بنجاح")
    }

    fun refreshStorageUsage() = viewModelScope.launch {
        _storageUsage.value = repo.calculateStorageUsage()
    }

    fun clearCachedQuranAudioStorage() = viewModelScope.launch {
        val freed = repo.clearCachedQuranAudio()
        refreshStorageUsage()
        repo.logAudit(_adminSession.value.actorName, "CLEAR_AUDIO_CACHE", "STORAGE", "INFO", "Freed $freed bytes")
        postStatus("تم تفريغ ذاكرة الصوت المؤقتة بنجاح")
    }

    // ======================== ISLAMIC LIBRARY ACTIONS ========================

    fun addOrImportLibraryBook(
        category: String,
        titleAr: String,
        authorAr: String,
        summaryAr: String,
        contentText: String,
        sourceRef: String,
        isImported: Boolean = false
    ) = viewModelScope.launch {
        if (titleAr.isBlank() || contentText.isBlank()) return@launch
        val safeContent = contentText.take(250_000) // Safe RAM cap for older Android devices
        repo.saveLibraryBook(
            LibraryBookEntity(
                id = "lib_usr_${UUID.randomUUID().toString().take(10)}",
                category = category.uppercase(),
                titleAr = titleAr.trim(),
                titleEn = titleAr.trim(),
                authorAr = authorAr.trim().ifBlank { "مؤلف غير محدد" },
                authorEn = authorAr.trim(),
                summaryAr = summaryAr.trim().ifBlank { safeContent.take(140) },
                contentText = safeContent,
                sourceReference = sourceRef.trim().ifBlank { "استيراد محلي" },
                isImported = isImported,
                approvalStatus = "APPROVED"
            )
        )
        postStatus("تمت إضافة الكتاب إلى مكتبة نور المحلية")
    }

    fun toggleBookBookmark(bookId: String, bookmarked: Boolean) = viewModelScope.launch {
        repo.toggleBookBookmark(bookId, bookmarked)
    }

    fun updateBookReadProgress(bookId: String, offset: Int) = viewModelScope.launch {
        repo.updateBookProgress(bookId, offset)
    }

    fun deleteLibraryBook(bookId: String) = viewModelScope.launch {
        repo.deleteLibraryBook(bookId)
        postStatus("تم حذف الكتاب من المكتبة")
    }

    fun addLibraryNote(bookId: String, noteText: String) = viewModelScope.launch {
        if (noteText.isBlank()) return@launch
        repo.addLibraryNote(bookId, noteText)
        postStatus("تم حفظ الملاحظة على الكتاب")
    }

    fun deleteLibraryNote(id: Long) = viewModelScope.launch {
        repo.deleteLibraryNote(id)
    }

    // ======================== TAJWEED & KIDS ACTIONS ========================

    fun recordTajweedLessonResult(ruleId: String, completed: Boolean, quizScorePercent: Int) = viewModelScope.launch {
        val current = tajweedProgress.value.firstOrNull { it.ruleId == ruleId }
        repo.saveTajweedProgress(
            TajweedProgressEntity(
                ruleId = ruleId,
                completed = completed || (current?.completed == true),
                quizScorePercent = maxOf(quizScorePercent, current?.quizScorePercent ?: 0),
                practicedCount = (current?.practicedCount ?: 0) + 1
            )
        )
        postStatus("تم حفظ تقدمك في درس التجويد")
    }

    fun completeKidsActivity(itemId: String, itemType: String, stars: Int = 3) = viewModelScope.launch {
        repo.saveKidsProgress(
            KidsProgressEntity(
                itemId = itemId,
                itemType = itemType,
                starsEarned = stars,
                completed = true
            )
        )
        postStatus("أحسنت! حصلت على $stars نجوم ⭐")
    }

    fun updateKidsParentSettings(settings: KidsParentSettings) {
        _kidsParentSettings.value = settings
        postStatus("تم تحديث إعدادات الوالدين")
    }

    // ======================== FASTING & KHATM ACTIONS ========================

    fun completeFastingDay(dateKey: String, fastType: String, notes: String = "") = viewModelScope.launch {
        try {
            val cleanDate = dateKey.trim().ifBlank { repo.todayKey() }
            val existing = fastingLogs.value.firstOrNull { it.dateKey == cleanDate }
            repo.saveFastingLog(
                FastingLogEntity(
                    dateKey = cleanDate,
                    fastType = fastType,
                    completed = true,
                    notes = notes
                )
            )
            if (existing != null && existing.fastType == fastType) {
                postStatus("تم تأكيد وتحديث صيام يوم $cleanDate بنجاح ✓")
            } else {
                postStatus("تقبل الله صيامك — تم حفظ إتمام الصيام ليوم $cleanDate ✓")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            repo.logError("FASTING_COMPLETE", e.message ?: "Failed to save fasting record", "ERROR")
            postStatus("تعذّر حفظ سجل الصيام حالياً، حاول مرة أخرى")
        }
    }

    fun removeFastingDay(dateKey: String) = viewModelScope.launch {
        try {
            repo.deleteFastingLog(dateKey)
            postStatus("تم حذف سجل الصيام ليوم $dateKey")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            repo.logError("FASTING_DELETE", e.message ?: "Failed to delete fasting record", "WARNING")
            postStatus("تعذّر حذف سجل الصيام")
        }
    }

    fun toggleFastingDay(dateKey: String, fastType: String, notes: String = "") = viewModelScope.launch {
        try {
            val existing = fastingLogs.value.firstOrNull { it.dateKey == dateKey }
            if (existing != null && existing.fastType == fastType) {
                repo.deleteFastingLog(dateKey)
                postStatus("تم إلغاء تسجيل الصيام ليوم $dateKey")
            } else {
                repo.saveFastingLog(FastingLogEntity(dateKey = dateKey, fastType = fastType, completed = true, notes = notes))
                postStatus("تقبل الله صيامك — تم التسجيل ✓")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            repo.logError("FASTING_TOGGLE", e.message ?: "Failed to toggle fasting", "ERROR")
            postStatus("تعذّر تحديث الصيام حالياً")
        }
    }

    fun createKhatmPlan(title: String, targetDateKey: String, dailyPages: Int) = viewModelScope.launch {
        if (title.isBlank()) {
            postStatus("يرجى إدخال اسم لخطة الختمة")
            return@launch
        }
        try {
            repo.saveKhatmPlan(
                KhatmPlanEntity(
                    title = title.trim(),
                    startDateKey = repo.todayKey(),
                    targetDateKey = targetDateKey.trim().ifBlank { repo.todayKey() },
                    currentPage = 1,
                    currentJuz = 1,
                    dailyTargetPages = dailyPages.coerceIn(1, 100),
                    totalPages = 604
                )
            )
            postStatus("تم إنشاء خطة الختمة بنجاح ✓")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            repo.logError("KHATM_CREATE", e.message ?: "Failed to create Khatm plan", "ERROR")
            postStatus("تعذّر إنشاء خطة الختمة، يرجى المحاولة مرة أخرى")
        }
    }

    fun advanceKhatm(plan: KhatmPlanEntity, pages: Int, allowExtraSameDay: Boolean = false) = viewModelScope.launch {
        try {
            val advanced = repo.advanceKhatmPlan(plan, pages.coerceAtLeast(1), allowExtraSameDay = allowExtraSameDay)
            if (advanced) {
                val nextPage = (plan.currentPage + pages.coerceAtLeast(1)).coerceIn(1, plan.totalPages.coerceIn(1, 604))
                if (nextPage >= plan.totalPages) {
                    postStatus("مبارك! أتممت الختمة كاملة بحمد الله ✓")
                } else {
                    postStatus("ما شاء الله! تم إتمام ورد اليوم والوصول إلى الصفحة $nextPage ✓")
                }
            } else {
                postStatus("تم تسجيل ورد اليوم لهذه الختمة مسبقاً — اضغط «ورد إضافي» إذا أردت الزيادة")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            repo.logError("KHATM_ADVANCE", e.message ?: "Failed to complete daily wird", "ERROR")
            postStatus("تعذّر تحديث الختمة حالياً، يرجى المحاولة مرة أخرى")
        }
    }

    fun syncKhatmWithPage(plan: KhatmPlanEntity, page: Int) = viewModelScope.launch {
        try {
            val safePage = page.coerceIn(1, plan.totalPages.coerceIn(1, 604))
            repo.syncKhatmPlanToPage(plan, safePage)
            postStatus("تمت مزامنة الختمة مع الصفحة $safePage ✓")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            repo.logError("KHATM_SYNC", e.message ?: "Failed to sync khatm page", "WARNING")
            postStatus("تعذّر مزامنة الصفحة حالياً")
        }
    }

    fun deleteKhatmPlan(id: Long) = viewModelScope.launch {
        try {
            repo.deleteKhatmPlan(id)
            postStatus("تم حذف خطة الختمة")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            repo.logError("KHATM_DELETE", e.message ?: "Failed to delete khatm plan", "WARNING")
            postStatus("تعذّر حذف خطة الختمة")
        }
    }

    // ======================== QUIZ ACTIONS ========================

    fun addCustomQuizQuestion(
        category: String,
        difficulty: String,
        questionAr: String,
        optionA: String,
        optionB: String,
        optionC: String,
        optionD: String,
        correctIndex: Int,
        explanationAr: String,
        sourceRef: String
    ) = viewModelScope.launch {
        if (questionAr.isBlank() || optionA.isBlank() || optionB.isBlank()) return@launch
        repo.saveQuizQuestion(
            CustomQuizQuestionEntity(
                id = "qz_adm_${UUID.randomUUID().toString().take(10)}",
                category = category.uppercase(),
                difficulty = difficulty.uppercase(),
                questionAr = questionAr.trim(),
                optionA = optionA.trim(),
                optionB = optionB.trim(),
                optionC = optionC.trim().ifBlank { "غير ذلك" },
                optionD = optionD.trim().ifBlank { "جميع ما سبق" },
                correctIndex = correctIndex.coerceIn(0, 3),
                explanationAr = explanationAr.trim(),
                sourceRef = sourceRef.trim(),
                isAdminCreated = true
            )
        )
        postStatus("تمت إضافة السؤال إلى بنك المسابقات")
    }

    fun deleteQuizQuestion(id: String) = viewModelScope.launch {
        repo.deleteQuizQuestion(id)
    }

    fun saveQuizAttempt(category: String, difficulty: String, score: Int, total: Int) = viewModelScope.launch {
        repo.recordQuizAttempt(
            QuizAttemptEntity(
                category = category,
                difficulty = difficulty,
                score = score,
                totalQuestions = total,
                dateKey = repo.todayKey()
            )
        )
    }

    // ======================== PERSONAL QURAN NOTES ACTIONS ========================

    fun createNoteCollection(name: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        repo.saveNoteCollection(name)
        postStatus("تم إنشاء مجموعة الملاحظات")
    }

    fun deleteNoteCollection(id: Long) = viewModelScope.launch {
        repo.deleteNoteCollection(id)
    }

    fun savePersonalNote(
        surahNumber: Int,
        ayahNumber: Int,
        noteText: String,
        collectionId: Long?,
        isFavorite: Boolean,
        existingId: Long = 0L
    ) = viewModelScope.launch {
        if (noteText.isBlank()) return@launch
        repo.savePersonalNote(
            QuranPersonalNoteEntity(
                id = existingId,
                surahNumber = surahNumber.coerceIn(1, 114),
                ayahNumber = ayahNumber.coerceAtLeast(1),
                collectionId = collectionId,
                noteText = noteText.trim(),
                isFavorite = isFavorite
            )
        )
        postStatus("تم حفظ ملاحظتك على الآية $ayahNumber")
    }

    fun toggleNoteFavorite(note: QuranPersonalNoteEntity) = viewModelScope.launch {
        repo.savePersonalNote(note.copy(isFavorite = !note.isFavorite))
    }

    fun deletePersonalNote(id: Long) = viewModelScope.launch {
        repo.deletePersonalNote(id)
    }

    // ======================== OFFLINE LOCAL SUPER ADMIN ACTIONS ========================

    fun setupInitialSuperAdmin(username: String, displayName: String, passcode: String) = viewModelScope.launch {
        val result = repo.createLocalAdminAccount(
            username = username,
            displayName = displayName,
            passcode = passcode,
            role = LocalAdminRole.SUPER_ADMIN,
            actorName = username.trim()
        )
        result.onSuccess { acct ->
            _hasSuperAdmin.value = true
            _adminSession.value = LocalAdminSession(acct)
            postStatus("تم تفعيل حساب المدير العام المحلي بنجاح")
        }.onFailure { err ->
            postStatus(err.message ?: "تعذّر إنشاء الحساب")
        }
    }

    fun signInLocalAdmin(username: String, passcode: String) = viewModelScope.launch {
        val acct = repo.authenticateLocalAdmin(username, passcode)
        if (acct != null) {
            _adminSession.value = LocalAdminSession(acct)
            postStatus("أهلاً بك يا ${acct.displayName}")
        } else {
            postStatus("اسم المستخدم أو رمز المرور غير صحيح")
        }
    }

    fun signOutLocalAdmin() {
        _adminSession.value = LocalAdminSession(null)
        postStatus("تم تسجيل الخروج من الإدارة المحلية")
    }

    fun createStaffAccount(username: String, displayName: String, passcode: String, role: LocalAdminRole) =
        viewModelScope.launch {
            if (!_adminSession.value.role.canManageAccounts()) {
                postStatus("هذه الصلاحية مخصصة للمدير العام فقط")
                return@launch
            }
            val result = repo.createLocalAdminAccount(
                username = username,
                displayName = displayName,
                passcode = passcode,
                role = role,
                actorName = _adminSession.value.actorName
            )
            result.onSuccess {
                postStatus("تم إنشاء حساب ${role.labelAr} بنجاح")
            }.onFailure {
                postStatus(it.message ?: "تعذّر إنشاء الحساب")
            }
        }

    fun deleteStaffAccount(id: Long) = viewModelScope.launch {
        if (!_adminSession.value.role.canManageAccounts()) return@launch
        if (_adminSession.value.account?.id == id) {
            postStatus("لا يمكن حذف الحساب النشط حالياً")
            return@launch
        }
        repo.deleteLocalAdminAccount(id, _adminSession.value.actorName)
        postStatus("تم حذف الحساب")
    }

    suspend fun exportLocalBackupJson(): String {
        return repo.buildFullEcosystemBackupJson(_adminSession.value.actorName)
    }

    fun restoreLocalBackupJson(rawJson: String) = viewModelScope.launch {
        runCatching {
            repo.restoreFromEcosystemBackupJson(rawJson, _adminSession.value.actorName)
        }.onSuccess { summary ->
            postStatus("تمت الاستعادة: ${summary.knowledgeImported} معرفة، ${summary.booksImported} كتاب، ${summary.studentsImported} طالب")
        }.onFailure { err ->
            repo.logError("BACKUP_RESTORE", err.message ?: "Invalid JSON")
            postStatus("فشل استيراد ملف النسخة الاحتياطية: تأكد من صحة ملف JSON")
        }
    }

    // ======================== ISLAMIC VIDEOS ACTIONS ========================

    fun loadVideoCollection(collection: IslamicVideoCollection, forceRefresh: Boolean = false) = viewModelScope.launch {
        _selectedVideoCollection.value = collection
        _videosLoading.value = true
        _videosError.value = null
        when (val res = videoRepository.loadEpisodes(collection, forceRefresh = forceRefresh)) {
            is VideoEpisodesResult.Success -> {
                _videoEpisodes.value = res.episodes
                if (_activeVideoItem.value == null && res.episodes.isNotEmpty()) {
                    _activeVideoItem.value = res.episodes.first()
                }
            }
            is VideoEpisodesResult.Error -> {
                _videoEpisodes.value = emptyList()
                _videosError.value = res.messageAr
            }
        }
        _videosLoading.value = false
    }

    fun selectVideoToPlay(item: IslamicVideoItem) {
        // Enforce single audio session across the entire app before playing video
        stopAudio()
        recitationRecorder.stopPlayback()
        if (hifzLiveState.value.isActive) {
            hifzMicListener.stopListening(clearTranscript = false)
        }
        _activeVideoItem.value = item
    }

    fun addUserVideoItem(
        title: String,
        streamUrl: String,
        isLocalUri: Boolean,
        categoryLabelAr: String = "فيديو مضاف",
        description: String = "",
        channelName: String = "",
        thumbnailUrl: String = "",
        durationLabel: String = "",
        category: IslamicVideoCategory = IslamicVideoCategory.MY_VIDEOS,
        uploaderName: String = "",
        uploaderAvatarUrl: String = ""
    ) {
        val cleanUrl = streamUrl.trim()
        if (cleanUrl.isBlank()) return
        val cleanTitle = title.trim().ifBlank { "فيديو إسلامي" }
        val cleanChannel = channelName.trim().ifBlank { uploaderName.trim().ifBlank { categoryLabelAr } }
        val cleanDesc = description.trim()
        val item = IslamicVideoItem(
            id = "pub_${UUID.randomUUID().toString().take(8)}",
            collectionId = "user_published",
            titleAr = cleanTitle,
            titleEn = cleanTitle,
            subtitleAr = cleanChannel,
            subtitleEn = cleanChannel.ifBlank { if (isLocalUri) "Local Device Video" else "Direct Video Link" },
            category = if (category == IslamicVideoCategory.ALL) IslamicVideoCategory.QURAN_LESSONS else category,
            streamUrl = cleanUrl,
            durationLabel = durationLabel.trim(),
            sourceLabel = if (isLocalUri) "NOOR Video" else "Web Stream",
            isLocalUri = isLocalUri,
            thumbnailUrl = thumbnailUrl.trim(),
            descriptionAr = cleanDesc,
            descriptionEn = cleanDesc,
            uploaderName = uploaderName.trim().ifBlank { cleanChannel },
            uploaderAvatarUrl = uploaderAvatarUrl.trim(),
            publishedAtMillis = System.currentTimeMillis(),
            isUserPublished = true
        )
        videoRepository.addUserVideo(item)
        selectVideoToPlay(item)
        postStatus("تم نشر الفيديو بنجاح مع الغلاف والوصف")
    }

    fun updatePublishedVideoItem(updated: IslamicVideoItem) {
        videoRepository.updateVideo(updated)
        if (_activeVideoItem.value?.id == updated.id) {
            _activeVideoItem.value = updated
        }
        postStatus("تم تحديث بيانات الفيديو بنجاح")
    }

    fun removeUserVideoItem(id: String) {
        videoRepository.removeUserVideo(id)
        if (_activeVideoItem.value?.id == id) {
            _activeVideoItem.value = _videoEpisodes.value.firstOrNull()
        }
        postStatus("تم حذف الفيديو")
    }

    override fun onCleared() {
        super.onCleared()
        hifzMicListener.stopListening()
        recitationRecorder.release()
        mistakeSoundPlayer.release()
    }
}
