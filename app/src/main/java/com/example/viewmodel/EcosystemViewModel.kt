package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoorApplication
import com.example.audio.AudioPlaybackState
import com.example.audio.LiveRecitationState
import com.example.audio.QuranAudioService
import com.example.audio.QuranLiveRecitationListener
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
import com.example.data.local.db.TajweedProgressEntity
import com.example.data.repository.SurahLoadResult
import com.example.domain.ai.NoorAiAnswer
import com.example.domain.ai.NoorLocalAiEngine
import com.example.domain.model.AudioCategory
import com.example.domain.model.AudioTrackItem
import com.example.domain.model.QuranReciter
import com.example.domain.model.QuranVerse
import com.example.domain.quran.QuranArabicPhoneticNormalizer
import com.example.security.LocalAdminRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

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

    // Halaqat flows
    val circles = repo.circles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val teachers = repo.teachers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val students = repo.students.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val attendance = repo.attendance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assessments = repo.assessments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Hifz AI flows
    val hifzPlans = repo.hifzPlans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hifzRevisionLogs = repo.hifzRevisionLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hifzWeakVerses = repo.hifzWeakVerses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hifzLiveState: StateFlow<LiveRecitationState> = hifzMicListener.state

    private val _hifzVerses = MutableStateFlow<List<QuranVerse>>(emptyList())
    val hifzVerses: StateFlow<List<QuranVerse>> = _hifzVerses.asStateFlow()

    private val _hifzComparison = MutableStateFlow<HifzComparisonResult?>(null)
    val hifzComparison: StateFlow<HifzComparisonResult?> = _hifzComparison.asStateFlow()

    // Local NOOR AI flows
    val allAiKnowledge = repo.allAiKnowledge.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val approvedAiKnowledge = repo.approvedAiKnowledge.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiConversation = MutableStateFlow<List<NoorAiAnswer>>(emptyList())
    val aiConversation: StateFlow<List<NoorAiAnswer>> = _aiConversation.asStateFlow()

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

    // Local Offline Super Admin
    val localAdmins = repo.localAdmins.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val localAuditLogs = repo.localAuditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val systemErrorLogs = repo.systemErrorLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val backupSnapshots = repo.backupSnapshots.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _adminSession = MutableStateFlow(LocalAdminSession())
    val adminSession: StateFlow<LocalAdminSession> = _adminSession.asStateFlow()

    private val _hasSuperAdmin = MutableStateFlow<Boolean?>(null)
    val hasSuperAdmin: StateFlow<Boolean?> = _hasSuperAdmin.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val audioState: StateFlow<AudioPlaybackState> = audioPlayer.state

    init {
        viewModelScope.launch {
            runCatching { repo.ensureSeeded() }
            _hasSuperAdmin.value = runCatching { repo.hasLocalSuperAdmin() }.getOrDefault(false)
            loadHifzSurah(1)
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

    fun loadHifzSurah(surahNumber: Int) = viewModelScope.launch {
        val clamped = surahNumber.coerceIn(1, 114)
        val reciter = QuranReciter.YASSER_AL_DOSARI
        val verses = SurahCatalog.getBundledSurahVerses(clamped, reciter)
            ?: when (val res = mainRepo.loadSurahVerses(clamped, reciter)) {
                is SurahLoadResult.Success -> res.verses
                is SurahLoadResult.Error -> emptyList()
            }
        _hifzVerses.value = verses
        if (verses.isNotEmpty()) {
            hifzMicListener.loadSurahContext(clamped, verses, verses.first().numberInSurah)
        }
        _hifzComparison.value = null
    }

    fun startHifzLiveMic() {
        hifzMicListener.startListening()
    }

    fun pauseHifzLiveMic() {
        hifzMicListener.pauseListening()
    }

    fun resumeHifzLiveMic() {
        hifzMicListener.resumeListening()
    }

    fun stopHifzLiveMic() {
        hifzMicListener.stopListening()
    }

    /**
     * Evaluates recited text against canonical Uthmani verse without marking uncertain recognition as a confirmed error.
     */
    fun evaluateRecitedVerse(verse: QuranVerse, spokenText: String) {
        val expectedTokens = QuranArabicPhoneticNormalizer.tokenize(verse.textUthmani)
        val spokenTokens = QuranArabicPhoneticNormalizer.tokenize(spokenText)
        if (spokenTokens.size < 2) {
            _hifzComparison.value = HifzComparisonResult(
                surahNumber = verse.surahNumber,
                ayahNumber = verse.numberInSurah,
                expectedText = verse.textUthmani,
                spokenText = spokenText,
                similarityPercent = 0,
                status = "UNCERTAIN",
                missingWordsHint = emptyList()
            )
            return
        }
        val spokenSet = spokenTokens.toSet()
        val matched = expectedTokens.count { exp ->
            exp in spokenSet || spokenTokens.any { s -> s.startsWith(exp.take(3)) && exp.length >= 3 }
        }
        val missing = expectedTokens.filter { exp ->
            exp !in spokenSet && spokenTokens.none { s -> s.startsWith(exp.take(3)) && exp.length >= 3 }
        }.take(4)

        val percent = if (expectedTokens.isEmpty()) 0 else ((matched * 100) / expectedTokens.size).coerceIn(0, 100)
        val status = when {
            percent >= 78 -> "MATCHED"
            percent >= 42 -> "POSSIBLE_MISTAKE"
            else -> "UNCERTAIN"
        }
        _hifzComparison.value = HifzComparisonResult(
            surahNumber = verse.surahNumber,
            ayahNumber = verse.numberInSurah,
            expectedText = verse.textUthmani,
            spokenText = spokenText,
            similarityPercent = percent,
            status = status,
            missingWordsHint = if (status == "POSSIBLE_MISTAKE") missing else emptyList()
        )
        if (status == "POSSIBLE_MISTAKE") {
            viewModelScope.launch {
                repo.recordWeakVerse(
                    surahNumber = verse.surahNumber,
                    ayahNumber = verse.numberInSurah,
                    note = "كلمات تحتاج مراجعة: ${missing.joinToString(" ، ")}"
                )
            }
        }
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

    fun playReferenceAyah(surahNumber: Int, ayahNumber: Int) {
        val surah = SurahCatalog.findSurah(surahNumber)
        val reciter = QuranReciter.YASSER_AL_DOSARI
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
        val answer = NoorLocalAiEngine.answerQuestion(
            query = question,
            approvedKnowledge = approvedAiKnowledge.value,
            approvedBooks = libraryBooks.value
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
                approvalStatus = approvalStatus,
                isUserAdded = true
            )
        )
        if (saved) {
            repo.logAudit(_adminSession.value.actorName, "UPSERT_AI_KNOWLEDGE", "AI_KB", "INFO", titleAr.take(50))
            postStatus("تم حفظ المعرفة في الفهرس المحلي بنجاح")
        } else {
            postStatus("تنبيه: هذا السؤال والإجابة موجودان مسبقاً (تم منع التكرار)")
        }
    }

    fun importKnowledgeFileContent(rawText: String, fileName: String, category: String) = viewModelScope.launch {
        val autoApprove = _adminSession.value.role == LocalAdminRole.SUPER_ADMIN
        val batch = repo.importAiKnowledgeFromText(rawText, fileName, category, autoApprove)
        repo.logAudit(
            _adminSession.value.actorName,
            "IMPORT_KNOWLEDGE_FILE",
            "AI_KB",
            "INFO",
            "File=$fileName Added=${batch.items.size} Duplicates=${batch.skippedDuplicates}"
        )
        postStatus("تمت فهرسة ${batch.items.size} عنصر (تم تخطي ${batch.skippedDuplicates} مكرر)")
    }

    fun setAiKnowledgeApproval(id: String, status: String) = viewModelScope.launch {
        repo.setAiKnowledgeApproval(id, status)
        repo.logAudit(_adminSession.value.actorName, "AI_KB_APPROVAL", "AI_KB", "INFO", "$id -> $status")
        postStatus("تم تحديث حالة الاعتماد")
    }

    fun deleteAiKnowledge(id: String) = viewModelScope.launch {
        repo.deleteAiKnowledge(id)
        repo.logAudit(_adminSession.value.actorName, "DELETE_AI_KB", "AI_KB", "WARNING", id)
        postStatus("تم حذف العنصر من قاعدة المعرفة")
    }

    fun reindexKnowledgeBase() = viewModelScope.launch {
        val total = repo.reindexLocalKnowledge()
        repo.logAudit(_adminSession.value.actorName, "REINDEX_AI_KB", "AI_KB", "INFO", "Indexed $total entries")
        postStatus("تمت إعادة فهرسة $total مادة معرفية بنجاح")
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

    fun toggleFastingDay(dateKey: String, fastType: String, notes: String = "") = viewModelScope.launch {
        val existing = fastingLogs.value.firstOrNull { it.dateKey == dateKey }
        if (existing != null && existing.fastType == fastType) {
            repo.deleteFastingLog(dateKey)
            postStatus("تم إلغاء تسجيل الصيام ليوم $dateKey")
        } else {
            repo.saveFastingLog(FastingLogEntity(dateKey = dateKey, fastType = fastType, completed = true, notes = notes))
            postStatus("تقبل الله صيامك — تم التسجيل")
        }
    }

    fun createKhatmPlan(title: String, targetDateKey: String, dailyPages: Int) = viewModelScope.launch {
        if (title.isBlank()) return@launch
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
        postStatus("تم إنشاء خطة الختمة بنجاح")
    }

    fun advanceKhatm(plan: KhatmPlanEntity, pages: Int) = viewModelScope.launch {
        repo.advanceKhatmPlan(plan, pages.coerceAtLeast(1))
        postStatus("ما شاء الله! تم تحديث صفحات الختمة")
    }

    fun syncKhatmWithPage(plan: KhatmPlanEntity, page: Int) = viewModelScope.launch {
        repo.syncKhatmPlanToPage(plan, page)
        postStatus("تمت مزامنة الختمة مع الصفحة $page")
    }

    fun deleteKhatmPlan(id: Long) = viewModelScope.launch {
        repo.deleteKhatmPlan(id)
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

    override fun onCleared() {
        super.onCleared()
        hifzMicListener.stopListening()
    }
}
