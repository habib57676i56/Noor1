package com.example.data.repository

import android.content.Context
import com.example.data.local.catalog.EcosystemContentCatalog
import com.example.data.local.db.AdminAdCardEntity
import com.example.data.local.db.AdminAuditLogLocalEntity
import com.example.data.local.db.AdminConfigEntryEntity
import com.example.data.local.db.AdminMessageEntity
import com.example.data.local.db.BackupSnapshotEntity
import com.example.data.local.db.CustomQuizQuestionEntity
import com.example.data.local.db.EcosystemDao
import com.example.data.local.db.FastingLogEntity
import com.example.data.local.db.HalaqahAttendanceEntity
import com.example.data.local.db.HalaqahCircleEntity
import com.example.data.local.db.HalaqahStudentEntity
import com.example.data.local.db.HalaqahTeacherEntity
import com.example.data.local.db.HifzPlanEntity
import com.example.data.local.db.HifzRevisionLogEntity
import com.example.data.local.db.HifzWeakVerseEntity
import com.example.data.local.db.ImportedAiDocumentEntity
import com.example.data.local.db.KhatmPlanEntity
import com.example.data.local.db.KhatmReadingLogEntity
import com.example.data.local.db.KidsProgressEntity
import com.example.data.local.db.LibraryBookEntity
import com.example.data.local.db.LibraryNoteEntity
import com.example.data.local.db.LocalAdminAccountEntity
import com.example.data.local.db.LocalAiKnowledgeEntity
import com.example.data.local.db.NoorDao
import com.example.data.local.db.QuizAttemptEntity
import com.example.data.local.db.QuranNoteCollectionEntity
import com.example.data.local.db.QuranPersonalNoteEntity
import com.example.data.local.db.RecitationAssessmentEntity
import com.example.data.local.db.StudentRecitationRecordingEntity
import com.example.data.local.db.SystemErrorLogEntity
import com.example.data.local.db.TajweedProgressEntity
import com.example.domain.ai.AiIndexStats
import com.example.domain.ai.DocumentContentExtractor
import com.example.domain.ai.ImportedKnowledgeBatch
import com.example.domain.ai.NoorLocalAiEngine
import com.example.security.LocalAdminRole
import com.example.security.LocalAdminSecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class StorageUsageSummary(
    val databaseBytes: Long = 0L,
    val recordingsBytes: Long = 0L,
    val adhanAudioBytes: Long = 0L,
    val cachedQuranAudioBytes: Long = 0L
) {
    val totalBytes: Long get() = databaseBytes + recordingsBytes + adhanAudioBytes + cachedQuranAudioBytes
    val totalMbFormatted: String get() = String.format(Locale.US, "%.2f MB", totalBytes / (1024.0 * 1024.0))
}

data class RestoreSummary(
    val knowledgeImported: Int,
    val booksImported: Int,
    val circlesImported: Int,
    val studentsImported: Int,
    val teachersImported: Int,
    val quizzesImported: Int,
    val notesImported: Int,
    val khatmPlansImported: Int
)

/**
 * Offline-first Repository for the NOOR Complete Islamic Ecosystem.
 * Handles idempotent local catalog seeding, Halaqat CRUD, Hifz AI tracking,
 * Local NOOR AI knowledge & import, Islamic Library, Tajweed, Kids, Fasting,
 * Khatm, Quizzes, Personal Quran Notes, Local Super Admin RBAC, and full JSON
 * local backup/restore & cross-device package export/import.
 */
class EcosystemRepository(
    private val dao: EcosystemDao,
    private val noorDao: NoorDao,
    private val appContext: Context
) {
    // Reactive Flows
    val circles: Flow<List<HalaqahCircleEntity>> = dao.observeCircles()
    val teachers: Flow<List<HalaqahTeacherEntity>> = dao.observeTeachers()
    val students: Flow<List<HalaqahStudentEntity>> = dao.observeStudents()
    val attendance: Flow<List<HalaqahAttendanceEntity>> = dao.observeAttendance()
    val assessments: Flow<List<RecitationAssessmentEntity>> = dao.observeAssessments()

    val hifzPlans: Flow<List<HifzPlanEntity>> = dao.observeHifzPlans()
    val hifzRevisionLogs: Flow<List<HifzRevisionLogEntity>> = dao.observeHifzRevisionLogs()
    val hifzWeakVerses: Flow<List<HifzWeakVerseEntity>> = dao.observeWeakVerses()

    val allAiKnowledge: Flow<List<LocalAiKnowledgeEntity>> = dao.observeAllAiKnowledge()
    val approvedAiKnowledge: Flow<List<LocalAiKnowledgeEntity>> = dao.observeApprovedAiKnowledge()

    val libraryBooks: Flow<List<LibraryBookEntity>> = dao.observeLibraryBooks()
    val libraryNotes: Flow<List<LibraryNoteEntity>> = dao.observeAllLibraryNotes()

    val adminAdCards: Flow<List<AdminAdCardEntity>> = dao.observeAllAdCards()
    val enabledAdCards: Flow<List<AdminAdCardEntity>> = dao.observeEnabledAdCards()
    val adminMessages: Flow<List<AdminMessageEntity>> = dao.observeAllAdminMessages()
    val unreadAdminMessageCount: Flow<Int> = dao.observeUnreadAdminMessageCount()

    val tajweedProgress: Flow<List<TajweedProgressEntity>> = dao.observeTajweedProgress()
    val kidsProgress: Flow<List<KidsProgressEntity>> = dao.observeKidsProgress()

    val fastingLogs: Flow<List<FastingLogEntity>> = dao.observeFastingLogs()
    val khatmPlans: Flow<List<KhatmPlanEntity>> = dao.observeKhatmPlans()
    val khatmLogs: Flow<List<KhatmReadingLogEntity>> = dao.observeKhatmLogs()

    val quizQuestions: Flow<List<CustomQuizQuestionEntity>> = dao.observeQuizQuestions()
    val quizAttempts: Flow<List<QuizAttemptEntity>> = dao.observeQuizAttempts()

    val noteCollections: Flow<List<QuranNoteCollectionEntity>> = dao.observeNoteCollections()
    val personalNotes: Flow<List<QuranPersonalNoteEntity>> = dao.observePersonalNotes()

    val localAdmins: Flow<List<LocalAdminAccountEntity>> = dao.observeLocalAdmins()
    val localAuditLogs: Flow<List<AdminAuditLogLocalEntity>> = noorDao.observeLocalAuditLogs()
    val systemErrorLogs: Flow<List<SystemErrorLogEntity>> = noorDao.observeSystemErrorLogs()
    val backupSnapshots: Flow<List<BackupSnapshotEntity>> = noorDao.observeBackupSnapshots()

    // v5 Reactive Flows
    val recitationRecordings: Flow<List<StudentRecitationRecordingEntity>> = dao.observeRecitationRecordings()
    val importedAiDocuments: Flow<List<ImportedAiDocumentEntity>> = dao.observeImportedAiDocuments()
    val adminConfigEntries: Flow<List<AdminConfigEntryEntity>> = dao.observeAdminConfigEntries()

    fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /**
     * Idempotent first-launch seeding of authentic offline content.
     * Never overwrites user edits or admin additions (uses IGNORE conflict strategy).
     */
    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (dao.countAiKnowledge() == 0) {
            dao.insertAiKnowledgeIgnore(EcosystemContentCatalog.seedAiKnowledge())
        }
        if (dao.countLibraryBooks() == 0) {
            dao.insertLibraryBooksIgnore(EcosystemContentCatalog.seedLibraryBooks())
        }
        if (dao.countQuizQuestions() == 0) {
            dao.insertQuizQuestionsIgnore(EcosystemContentCatalog.seedQuizQuestions())
        }
    }

    // ======================== HALAQAT ========================

    suspend fun saveCircle(circle: HalaqahCircleEntity): Long = withContext(Dispatchers.IO) {
        dao.upsertCircle(circle)
    }

    suspend fun deleteCircle(id: Long) = withContext(Dispatchers.IO) { dao.deleteCircle(id) }

    suspend fun saveTeacher(teacher: HalaqahTeacherEntity): Long = withContext(Dispatchers.IO) {
        dao.upsertTeacher(teacher)
    }

    suspend fun deleteTeacher(id: Long) = withContext(Dispatchers.IO) { dao.deleteTeacher(id) }

    suspend fun saveStudent(student: HalaqahStudentEntity): Long = withContext(Dispatchers.IO) {
        dao.upsertStudent(student)
    }

    suspend fun deleteStudent(id: Long) = withContext(Dispatchers.IO) { dao.deleteStudent(id) }

    suspend fun recordAttendance(record: HalaqahAttendanceEntity): Long = withContext(Dispatchers.IO) {
        dao.insertAttendance(record)
    }

    suspend fun saveAssessment(assessment: RecitationAssessmentEntity): Long = withContext(Dispatchers.IO) {
        dao.upsertAssessment(assessment)
    }

    suspend fun setAssessmentApproval(id: Long, approved: Boolean) = withContext(Dispatchers.IO) {
        dao.setAssessmentApproval(id, approved)
    }

    suspend fun deleteAssessment(id: Long) = withContext(Dispatchers.IO) { dao.deleteAssessment(id) }

    // ======================== HIFZ AI ========================

    suspend fun saveHifzPlan(plan: HifzPlanEntity): Long = withContext(Dispatchers.IO) {
        dao.upsertHifzPlan(plan.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun deleteHifzPlan(id: Long) = withContext(Dispatchers.IO) { dao.deleteHifzPlan(id) }

    suspend fun recordHifzRevision(log: HifzRevisionLogEntity): Long = withContext(Dispatchers.IO) {
        dao.insertHifzRevisionLog(log)
    }

    suspend fun setHifzLogApproval(id: Long, approved: Boolean) = withContext(Dispatchers.IO) {
        dao.setHifzLogApproval(id, approved)
    }

    suspend fun recordWeakVerse(surahNumber: Int, ayahNumber: Int, note: String) = withContext(Dispatchers.IO) {
        val existing = dao.findWeakVerse(surahNumber, ayahNumber)
        if (existing != null) {
            dao.upsertWeakVerse(
                existing.copy(
                    missCount = existing.missCount + 1,
                    lastMistakeNote = note.ifBlank { existing.lastMistakeNote },
                    resolved = false,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        } else {
            dao.upsertWeakVerse(
                HifzWeakVerseEntity(
                    surahNumber = surahNumber,
                    ayahNumber = ayahNumber,
                    missCount = 1,
                    lastMistakeNote = note,
                    resolved = false
                )
            )
        }
    }

    suspend fun resolveWeakVerse(id: Long, resolved: Boolean) = withContext(Dispatchers.IO) {
        dao.setWeakVerseResolved(id, resolved)
    }

    // ======================== LOCAL NOOR AI ========================

    suspend fun saveAiKnowledge(item: LocalAiKnowledgeEntity, allowDuplicate: Boolean = false): Boolean =
        withContext(Dispatchers.IO) {
            val hash = LocalAdminSecurityManager.sha256Hex("${item.questionAr}|${item.answerAr}")
            if (!allowDuplicate) {
                val existing = dao.findAiKnowledgeByHash(hash)
                if (existing != null && existing.id != item.id) {
                    return@withContext false
                }
            }
            dao.upsertAiKnowledge(item.copy(contentHash = hash))
            true
        }

    suspend fun importAiKnowledgeFromText(
        rawText: String,
        fileName: String,
        category: String,
        autoApprove: Boolean
    ): ImportedKnowledgeBatch = withContext(Dispatchers.IO) {
        val existingHashes = dao.getAllAiKnowledgeOnce().map { it.contentHash }.filter { it.isNotBlank() }.toSet()
        val batch = NoorLocalAiEngine.parseImportedKnowledgeText(rawText, fileName, category, existingHashes)
        val toSave = if (autoApprove) batch.items.map { it.copy(approvalStatus = "APPROVED") } else batch.items
        if (toSave.isNotEmpty()) {
            dao.insertAiKnowledgeIgnore(toSave)
        }
        batch.copy(items = toSave)
    }

    suspend fun setAiKnowledgeApproval(id: String, status: String) = withContext(Dispatchers.IO) {
        dao.setAiKnowledgeApproval(id, status)
    }

    suspend fun deleteAiKnowledge(id: String) = withContext(Dispatchers.IO) {
        dao.deleteAiKnowledge(id)
    }

    suspend fun reindexLocalKnowledge(): AiIndexStats = withContext(Dispatchers.IO) {
        val items = dao.getAllAiKnowledgeOnce()
        items.forEach { entry ->
            val hash = LocalAdminSecurityManager.sha256Hex("${entry.questionAr}|${entry.answerAr}")
            if (entry.contentHash != hash) {
                dao.upsertAiKnowledge(entry.copy(contentHash = hash))
            }
        }
        val books = dao.getAllLibraryBooksOnce()
        NoorLocalAiEngine.rebuildSearchIndex(items, books)
    }

    suspend fun importAiDocumentBytes(
        fileName: String,
        bytes: ByteArray,
        category: String,
        autoApprove: Boolean,
        actorName: String
    ): ImportedKnowledgeBatch = withContext(Dispatchers.IO) {
        val extracted = DocumentContentExtractor.extract(fileName, bytes, defaultCategory = category)
        val existingHashes = dao.getAllAiKnowledgeOnce().map { it.contentHash }.filter { it.isNotBlank() }.toSet()
        val batch = NoorLocalAiEngine.parseExtractedDocumentToKnowledge(extracted, existingHashes, autoApprove)
        if (batch.items.isNotEmpty()) {
            dao.insertAiKnowledgeIgnore(batch.items)
        }
        dao.upsertImportedAiDocument(
            ImportedAiDocumentEntity(
                id = "doc_${UUID.randomUUID().toString().take(10)}",
                fileName = fileName,
                format = extracted.format.label,
                detectedLanguage = extracted.detectedLanguage,
                category = extracted.detectedCategory,
                chunkCount = batch.items.size,
                charCount = extracted.extractedText.length,
                importedBy = actorName,
                warningNote = extracted.warningAr.orEmpty()
            )
        )
        batch
    }

    suspend fun deleteImportedAiDocument(id: String) = withContext(Dispatchers.IO) {
        dao.deleteImportedAiDocument(id)
    }

    suspend fun exportAiKnowledgeJson(): String = withContext(Dispatchers.IO) {
        NoorLocalAiEngine.exportKnowledgeBaseJson(dao.getAllAiKnowledgeOnce())
    }

    // ======================== RECITATION RECORDINGS (v5) ========================

    suspend fun saveRecitationRecording(entity: StudentRecitationRecordingEntity): Long = withContext(Dispatchers.IO) {
        dao.upsertRecitationRecording(entity)
    }

    suspend fun reviewRecitationRecording(id: Long, status: String, comment: String) = withContext(Dispatchers.IO) {
        dao.updateRecordingTeacherReview(id, status, comment.trim())
    }

    suspend fun deleteRecitationRecording(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteRecitationRecording(id)
    }

    // ======================== ADMIN CONFIG, STORAGE & REPORTS (v5) ========================

    suspend fun setAdminConfig(key: String, value: String, actorName: String) = withContext(Dispatchers.IO) {
        dao.upsertAdminConfigEntry(
            AdminConfigEntryEntity(
                configKey = key,
                configValue = value,
                updatedBy = actorName,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun getAllAdminConfigOnce(): Map<String, String> = withContext(Dispatchers.IO) {
        dao.getAllAdminConfigEntriesOnce().associate { it.configKey to it.configValue }
    }

    suspend fun calculateStorageUsage(): StorageUsageSummary = withContext(Dispatchers.IO) {
        val dbFile = appContext.getDatabasePath("noor_islamic_db")
        val dbBytes = if (dbFile.exists()) dbFile.length() else 0L
        val recBytes = folderSizeBytes(File(appContext.filesDir, "hifz_recordings"))
        val adhanBytes = folderSizeBytes(File(appContext.filesDir, "adhan_downloads")) +
            folderSizeBytes(File(appContext.filesDir, "custom_adhan"))
        val cacheBytes = folderSizeBytes(File(appContext.filesDir, "audio_cache"))
        StorageUsageSummary(
            databaseBytes = dbBytes,
            recordingsBytes = recBytes,
            adhanAudioBytes = adhanBytes,
            cachedQuranAudioBytes = cacheBytes
        )
    }

    suspend fun clearCachedQuranAudio(): Long = withContext(Dispatchers.IO) {
        val dir = File(appContext.filesDir, "audio_cache")
        val before = folderSizeBytes(dir)
        dir.listFiles()?.forEach { runCatching { it.delete() } }
        before
    }

    suspend fun buildHalaqatReportText(): String = withContext(Dispatchers.IO) {
        val circlesList = dao.getAllCirclesOnce()
        val teachersList = dao.getAllTeachersOnce()
        val studentsList = dao.getAllStudentsOnce()
        val assessmentsList = dao.getAllAssessmentsOnce()
        val recordingsList = dao.getAllRecitationRecordingsOnce()
        buildString {
            appendLine("=== تقرير حلقات نور القرآنية (NOOR Halaqat Report) ===")
            appendLine("التاريخ: ${todayKey()}")
            appendLine("عدد الحلقات: ${circlesList.size} | المعلمون: ${teachersList.size} | الطلاب: ${studentsList.size} | التقييمات: ${assessmentsList.size} | التسجيلات: ${recordingsList.size}")
            appendLine()
            appendLine("--- الحلقات ---")
            circlesList.forEach { c ->
                val tName = teachersList.firstOrNull { it.id == c.teacherId }?.fullName ?: "غير محدد"
                appendLine("• ${c.name} (${c.level}/${c.circleType}) — المعلم: $tName — الموعد: ${c.scheduleText}")
            }
            appendLine()
            appendLine("--- الطلاب وتقدم الحفظ ---")
            studentsList.forEach { s ->
                val cName = circlesList.firstOrNull { it.id == s.circleId }?.name ?: "بدون حلقة"
                appendLine("• ${s.fullName} (العمر ${s.age}) | الحلقة: $cName | محفوظ: ${s.memorizedJuz} جزء (${s.memorizedPages} صفحة) | مراجعة: ${s.revisionPages} صفحة")
            }
            appendLine()
            appendLine("--- آخر تقييمات التسميع ---")
            assessmentsList.take(25).forEach { a ->
                val stName = studentsList.firstOrNull { it.id == a.studentId }?.fullName ?: "طالب #${a.studentId}"
                appendLine("• ${a.assessmentDateKey} | $stName | سورة ${a.surahNumber} (${a.fromAyah}-${a.toAyah}) | الدقة: ${a.accuracyPercent}% | معتمد: ${a.approvedByTeacher}")
            }
        }
    }

    private fun folderSizeBytes(dir: File): Long {
        if (!dir.exists()) return 0L
        var sum = 0L
        dir.listFiles()?.forEach { f ->
            sum += if (f.isDirectory) folderSizeBytes(f) else f.length()
        }
        return sum
    }

    // ======================== ISLAMIC LIBRARY ========================

    suspend fun saveLibraryBook(book: LibraryBookEntity) = withContext(Dispatchers.IO) {
        dao.upsertLibraryBook(
            book.copy(
                totalLength = book.contentText.length,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleBookBookmark(bookId: String, bookmarked: Boolean) = withContext(Dispatchers.IO) {
        dao.setBookBookmark(bookId, bookmarked)
    }

    suspend fun updateBookProgress(bookId: String, offset: Int) = withContext(Dispatchers.IO) {
        dao.updateBookProgress(bookId, offset)
    }

    suspend fun setBookApproval(bookId: String, status: String) = withContext(Dispatchers.IO) {
        dao.setBookApproval(bookId, status)
    }

    suspend fun deleteLibraryBook(bookId: String) = withContext(Dispatchers.IO) {
        dao.deleteLibraryBook(bookId)
    }

    suspend fun addLibraryNote(bookId: String, noteText: String) = withContext(Dispatchers.IO) {
        dao.insertLibraryNote(LibraryNoteEntity(bookId = bookId, noteText = noteText.trim()))
    }

    suspend fun deleteLibraryNote(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteLibraryNote(id)
    }

    // ======================== TAJWEED & KIDS ========================

    suspend fun saveTajweedProgress(progress: TajweedProgressEntity) = withContext(Dispatchers.IO) {
        dao.upsertTajweedProgress(progress.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun saveKidsProgress(progress: KidsProgressEntity) = withContext(Dispatchers.IO) {
        dao.upsertKidsProgress(progress.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    // ======================== FASTING & KHATM ========================

    suspend fun saveFastingLog(log: FastingLogEntity) = withContext(Dispatchers.IO) {
        val cleanDate = log.dateKey.trim().ifBlank { todayKey() }
        val cleanType = log.fastType.trim().uppercase().ifBlank { "SUNNAH" }
        dao.upsertFastingLog(
            log.copy(
                dateKey = cleanDate,
                fastType = cleanType,
                completed = true,
                notes = log.notes.trim().take(200),
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteFastingLog(dateKey: String) = withContext(Dispatchers.IO) {
        dao.deleteFastingLog(dateKey.trim())
    }

    suspend fun saveKhatmPlan(plan: KhatmPlanEntity): Long = withContext(Dispatchers.IO) {
        val total = plan.totalPages.coerceIn(1, 604)
        val clampedPage = plan.currentPage.coerceIn(1, total)
        val juz = ((clampedPage - 1) / 20 + 1).coerceIn(1, 30)
        dao.upsertKhatmPlan(
            plan.copy(
                title = plan.title.trim().take(120),
                totalPages = total,
                currentPage = clampedPage,
                currentJuz = juz,
                dailyTargetPages = plan.dailyTargetPages.coerceIn(1, 100),
                isCompleted = clampedPage >= total
            )
        )
    }

    /**
     * Advances a Khatm plan safely and records a Daily Wird log without creating duplicate
     * records for the same plan on the same day unless `allowExtraSameDay = true`.
     * Returns true if the plan was advanced, false if today's wird was already completed.
     */
    suspend fun advanceKhatmPlan(
        plan: KhatmPlanEntity,
        pagesAdded: Int,
        allowExtraSameDay: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val today = todayKey()
        val safeDelta = pagesAdded.coerceIn(1, 200)
        if (!allowExtraSameDay) {
            val existingToday = dao.getKhatmLogsForPlanOnDateOnce(plan.id, today)
            if (existingToday.isNotEmpty()) {
                return@withContext false
            }
        }
        val total = plan.totalPages.coerceIn(1, 604)
        val nextPage = (plan.currentPage + safeDelta).coerceIn(1, total)
        val nextJuz = ((nextPage - 1) / 20 + 1).coerceIn(1, 30)
        val done = nextPage >= total
        dao.updateKhatmPlan(
            plan.copy(
                currentPage = nextPage,
                currentJuz = nextJuz,
                isCompleted = done
            )
        )
        dao.insertKhatmLog(
            KhatmReadingLogEntity(
                planId = plan.id,
                dateKey = today,
                pagesRead = safeDelta,
                reachedPage = nextPage
            )
        )
        true
    }

    suspend fun syncKhatmPlanToPage(plan: KhatmPlanEntity, page: Int) = withContext(Dispatchers.IO) {
        val total = plan.totalPages.coerceIn(1, 604)
        val clamped = page.coerceIn(1, total)
        val juz = ((clamped - 1) / 20 + 1).coerceIn(1, 30)
        dao.updateKhatmPlan(
            plan.copy(
                currentPage = clamped,
                currentJuz = juz,
                isCompleted = clamped >= total
            )
        )
    }

    suspend fun deleteKhatmPlan(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteKhatmPlan(id)
    }

    // ======================== QUIZ ========================

    suspend fun saveQuizQuestion(q: CustomQuizQuestionEntity) = withContext(Dispatchers.IO) {
        dao.upsertQuizQuestion(q)
    }

    suspend fun deleteQuizQuestion(id: String) = withContext(Dispatchers.IO) {
        dao.deleteQuizQuestion(id)
    }

    suspend fun recordQuizAttempt(attempt: QuizAttemptEntity) = withContext(Dispatchers.IO) {
        dao.insertQuizAttempt(attempt)
    }

    // ======================== PERSONAL QURAN NOTES ========================

    suspend fun saveNoteCollection(name: String, colorTag: String = "GOLD"): Long = withContext(Dispatchers.IO) {
        dao.upsertNoteCollection(QuranNoteCollectionEntity(name = name.trim(), colorTag = colorTag))
    }

    suspend fun deleteNoteCollection(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteNoteCollection(id)
    }

    suspend fun savePersonalNote(note: QuranPersonalNoteEntity): Long = withContext(Dispatchers.IO) {
        dao.upsertPersonalNote(note.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun deletePersonalNote(id: Long) = withContext(Dispatchers.IO) {
        dao.deletePersonalNote(id)
    }

    // ======================== OFFLINE LOCAL SUPER ADMIN & RBAC ========================

    suspend fun hasLocalSuperAdmin(): Boolean = withContext(Dispatchers.IO) {
        dao.countActiveSuperAdmins() > 0
    }

    suspend fun createLocalAdminAccount(
        username: String,
        displayName: String,
        passcode: String,
        role: LocalAdminRole,
        actorName: String
    ): Result<LocalAdminAccountEntity> = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        if (cleanUser.length < 3) {
            return@withContext Result.failure(IllegalArgumentException("اسم المستخدم قصير جداً (٣ أحرف على الأقل)"))
        }
        if (!LocalAdminSecurityManager.isStrongEnoughPasscode(passcode)) {
            return@withContext Result.failure(IllegalArgumentException("رمز المرور يجب أن يكون ٦ أحرف أو أرقام على الأقل"))
        }
        if (dao.findActiveLocalAdminByUsername(cleanUser) != null) {
            return@withContext Result.failure(IllegalArgumentException("اسم المستخدم مستخدم مسبقاً"))
        }
        val digest = LocalAdminSecurityManager.hashPasscode(passcode)
        val entity = LocalAdminAccountEntity(
            username = cleanUser,
            displayName = displayName.trim().ifBlank { cleanUser },
            role = role.key,
            passwordHash = digest.hashHex,
            passwordSalt = digest.saltHex,
            active = true
        )
        val id = dao.upsertLocalAdmin(entity)
        logAudit(actorName, "CREATE_LOCAL_ADMIN", "SECURITY", "INFO", "Created ${role.key} account: $cleanUser")
        Result.success(entity.copy(id = id))
    }

    suspend fun authenticateLocalAdmin(username: String, passcode: String): LocalAdminAccountEntity? =
        withContext(Dispatchers.IO) {
            val account = dao.findActiveLocalAdminByUsername(username.trim()) ?: return@withContext null
            val ok = LocalAdminSecurityManager.verifyPasscode(passcode, account.passwordHash, account.passwordSalt)
            if (ok) {
                logAudit(account.username, "LOCAL_ADMIN_LOGIN", "SECURITY", "INFO", "Role=${account.role}")
                account
            } else {
                logAudit(username.trim(), "LOCAL_ADMIN_LOGIN_FAILED", "SECURITY", "WARNING", "Invalid passcode attempt")
                null
            }
        }

    suspend fun deleteLocalAdminAccount(id: Long, actorName: String) = withContext(Dispatchers.IO) {
        dao.deleteLocalAdmin(id)
        logAudit(actorName, "DELETE_LOCAL_ADMIN", "SECURITY", "WARNING", "Removed local account id=$id")
    }

    suspend fun logAudit(
        actor: String,
        action: String,
        category: String,
        severity: String = "INFO",
        details: String = ""
    ) {
        withContext(Dispatchers.IO) {
            noorDao.insertLocalAuditLog(
                AdminAuditLogLocalEntity(
                    id = "loc_${UUID.randomUUID().toString().take(12)}",
                    actorUid = actor,
                    actorEmail = "$actor@local.noor",
                    action = action,
                    category = category,
                    severity = severity,
                    details = details
                )
            )
        }
    }

    suspend fun logError(tag: String, message: String, severity: String = "WARNING") {
        withContext(Dispatchers.IO) {
            noorDao.insertSystemErrorLog(SystemErrorLogEntity(tag = tag, message = message, severity = severity))
        }
    }

    // ======================== LOCAL BACKUP / EXPORT & RESTORE / IMPORT ========================

    suspend fun buildFullEcosystemBackupJson(actorName: String): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("schemaVersion", 4)
        root.put("exportedAtMillis", System.currentTimeMillis())
        root.put("exportedBy", actorName)

        root.put("aiKnowledge", JSONArray().apply {
            dao.getAllAiKnowledgeOnce().forEach { k ->
                put(JSONObject().apply {
                    put("id", k.id)
                    put("category", k.category)
                    put("titleAr", k.titleAr)
                    put("titleEn", k.titleEn)
                    put("questionAr", k.questionAr)
                    put("questionEn", k.questionEn)
                    put("answerAr", k.answerAr)
                    put("answerEn", k.answerEn)
                    put("sourceRef", k.sourceRef)
                    put("verificationStatus", k.verificationStatus)
                    put("scholarlyNotes", k.scholarlyNotes)
                    put("approvalStatus", k.approvalStatus)
                })
            }
        })

        root.put("libraryBooks", JSONArray().apply {
            dao.getAllLibraryBooksOnce().forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("category", b.category)
                    put("titleAr", b.titleAr)
                    put("titleEn", b.titleEn)
                    put("authorAr", b.authorAr)
                    put("authorEn", b.authorEn)
                    put("summaryAr", b.summaryAr)
                    put("contentText", b.contentText)
                    put("sourceReference", b.sourceReference)
                    put("isImported", b.isImported)
                    put("approvalStatus", b.approvalStatus)
                })
            }
        })

        root.put("teachers", JSONArray().apply {
            dao.getAllTeachersOnce().forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("fullName", t.fullName)
                    put("phone", t.phone)
                    put("specialization", t.specialization)
                    put("scheduleNotes", t.scheduleNotes)
                })
            }
        })

        root.put("circles", JSONArray().apply {
            dao.getAllCirclesOnce().forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("description", c.description)
                    put("level", c.level)
                    put("circleType", c.circleType)
                    put("scheduleText", c.scheduleText)
                    put("teacherId", c.teacherId ?: JSONObject.NULL)
                })
            }
        })

        root.put("students", JSONArray().apply {
            dao.getAllStudentsOnce().forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("fullName", s.fullName)
                    put("age", s.age)
                    put("guardianContact", s.guardianContact)
                    put("circleId", s.circleId ?: JSONObject.NULL)
                    put("teacherId", s.teacherId ?: JSONObject.NULL)
                    put("memorizedJuz", s.memorizedJuz)
                    put("memorizedPages", s.memorizedPages)
                    put("revisionPages", s.revisionPages)
                    put("currentSurahNumber", s.currentSurahNumber)
                    put("currentAyahNumber", s.currentAyahNumber)
                    put("teacherFeedback", s.teacherFeedback)
                })
            }
        })

        root.put("quizQuestions", JSONArray().apply {
            dao.getAllQuizQuestionsOnce().forEach { q ->
                put(JSONObject().apply {
                    put("id", q.id)
                    put("category", q.category)
                    put("difficulty", q.difficulty)
                    put("questionAr", q.questionAr)
                    put("questionEn", q.questionEn)
                    put("optionA", q.optionA)
                    put("optionB", q.optionB)
                    put("optionC", q.optionC)
                    put("optionD", q.optionD)
                    put("correctIndex", q.correctIndex)
                    put("explanationAr", q.explanationAr)
                    put("sourceRef", q.sourceRef)
                })
            }
        })

        root.put("personalNotes", JSONArray().apply {
            dao.getAllPersonalNotesOnce().forEach { n ->
                put(JSONObject().apply {
                    put("surahNumber", n.surahNumber)
                    put("ayahNumber", n.ayahNumber)
                    put("noteText", n.noteText)
                    put("isFavorite", n.isFavorite)
                })
            }
        })

        root.put("khatmPlans", JSONArray().apply {
            dao.getAllKhatmPlansOnce().forEach { p ->
                put(JSONObject().apply {
                    put("title", p.title)
                    put("startDateKey", p.startDateKey)
                    put("targetDateKey", p.targetDateKey)
                    put("currentPage", p.currentPage)
                    put("currentJuz", p.currentJuz)
                    put("dailyTargetPages", p.dailyTargetPages)
                    put("totalPages", p.totalPages)
                    put("isCompleted", p.isCompleted)
                })
            }
        })

        val jsonStr = root.toString(2)
        noorDao.insertBackupSnapshot(
            BackupSnapshotEntity(
                label = "NOOR Ecosystem Backup (${todayKey()})",
                payloadJson = jsonStr,
                sizeBytes = jsonStr.toByteArray(Charsets.UTF_8).size.toLong()
            )
        )
        logAudit(actorName, "EXPORT_LOCAL_BACKUP", "BACKUP", "INFO", "Size=${jsonStr.length} chars")
        jsonStr
    }

    suspend fun restoreFromEcosystemBackupJson(rawJson: String, actorName: String): RestoreSummary =
        withContext(Dispatchers.IO) {
            require(rawJson.length <= 8_000_000) { "Backup file exceeds 8MB safety limit" }
            val root = JSONObject(rawJson)

            var kbCount = 0
            root.optJSONArray("aiKnowledge")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val q = o.optString("questionAr")
                    val a = o.optString("answerAr")
                    if (q.isBlank() || a.isBlank()) continue
                    dao.upsertAiKnowledge(
                        LocalAiKnowledgeEntity(
                            id = o.optString("id", "imp_${UUID.randomUUID().toString().take(8)}"),
                            category = o.optString("category", "GENERAL"),
                            titleAr = o.optString("titleAr", q.take(50)),
                            titleEn = o.optString("titleEn", ""),
                            questionAr = q,
                            questionEn = o.optString("questionEn", ""),
                            answerAr = a,
                            answerEn = o.optString("answerEn", ""),
                            sourceRef = o.optString("sourceRef", ""),
                            verificationStatus = o.optString("verificationStatus", "VERIFIED"),
                            scholarlyNotes = o.optString("scholarlyNotes", ""),
                            contentHash = LocalAdminSecurityManager.sha256Hex("$q|$a"),
                            approvalStatus = o.optString("approvalStatus", "APPROVED"),
                            isUserAdded = true
                        )
                    )
                    kbCount++
                }
            }

            var bookCount = 0
            root.optJSONArray("libraryBooks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val title = o.optString("titleAr")
                    val content = o.optString("contentText")
                    if (title.isBlank() || content.isBlank()) continue
                    dao.upsertLibraryBook(
                        LibraryBookEntity(
                            id = o.optString("id", "book_${UUID.randomUUID().toString().take(8)}"),
                            category = o.optString("category", "LESSONS"),
                            titleAr = title,
                            titleEn = o.optString("titleEn", title),
                            authorAr = o.optString("authorAr", ""),
                            authorEn = o.optString("authorEn", ""),
                            summaryAr = o.optString("summaryAr", ""),
                            contentText = content,
                            sourceReference = o.optString("sourceReference", ""),
                            isImported = o.optBoolean("isImported", true),
                            totalLength = content.length,
                            approvalStatus = o.optString("approvalStatus", "APPROVED")
                        )
                    )
                    bookCount++
                }
            }

            var teacherCount = 0
            root.optJSONArray("teachers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val name = o.optString("fullName")
                    if (name.isBlank()) continue
                    dao.upsertTeacher(
                        HalaqahTeacherEntity(
                            id = o.optLong("id", 0L),
                            fullName = name,
                            phone = o.optString("phone", ""),
                            specialization = o.optString("specialization", "حفظ وتجويد"),
                            scheduleNotes = o.optString("scheduleNotes", "")
                        )
                    )
                    teacherCount++
                }
            }

            var circleCount = 0
            root.optJSONArray("circles")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val name = o.optString("name")
                    if (name.isBlank()) continue
                    dao.upsertCircle(
                        HalaqahCircleEntity(
                            id = o.optLong("id", 0L),
                            name = name,
                            description = o.optString("description", ""),
                            level = o.optString("level", "BEGINNER"),
                            circleType = o.optString("circleType", "MEMORIZATION"),
                            scheduleText = o.optString("scheduleText", ""),
                            teacherId = if (o.isNull("teacherId")) null else o.optLong("teacherId")
                        )
                    )
                    circleCount++
                }
            }

            var studentCount = 0
            root.optJSONArray("students")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val name = o.optString("fullName")
                    if (name.isBlank()) continue
                    dao.upsertStudent(
                        HalaqahStudentEntity(
                            id = o.optLong("id", 0L),
                            fullName = name,
                            age = o.optInt("age", 12),
                            guardianContact = o.optString("guardianContact", ""),
                            circleId = if (o.isNull("circleId")) null else o.optLong("circleId"),
                            teacherId = if (o.isNull("teacherId")) null else o.optLong("teacherId"),
                            memorizedJuz = o.optInt("memorizedJuz", 0),
                            memorizedPages = o.optInt("memorizedPages", 0),
                            revisionPages = o.optInt("revisionPages", 0),
                            currentSurahNumber = o.optInt("currentSurahNumber", 1),
                            currentAyahNumber = o.optInt("currentAyahNumber", 1),
                            teacherFeedback = o.optString("teacherFeedback", "")
                        )
                    )
                    studentCount++
                }
            }

            var quizCount = 0
            root.optJSONArray("quizQuestions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val qAr = o.optString("questionAr")
                    if (qAr.isBlank()) continue
                    dao.upsertQuizQuestion(
                        CustomQuizQuestionEntity(
                            id = o.optString("id", "qz_${UUID.randomUUID().toString().take(8)}"),
                            category = o.optString("category", "KNOWLEDGE"),
                            difficulty = o.optString("difficulty", "EASY"),
                            questionAr = qAr,
                            questionEn = o.optString("questionEn", ""),
                            optionA = o.optString("optionA", ""),
                            optionB = o.optString("optionB", ""),
                            optionC = o.optString("optionC", ""),
                            optionD = o.optString("optionD", ""),
                            correctIndex = o.optInt("correctIndex", 0).coerceIn(0, 3),
                            explanationAr = o.optString("explanationAr", ""),
                            sourceRef = o.optString("sourceRef", ""),
                            isAdminCreated = true
                        )
                    )
                    quizCount++
                }
            }

            var noteCount = 0
            root.optJSONArray("personalNotes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val text = o.optString("noteText")
                    if (text.isBlank()) continue
                    dao.upsertPersonalNote(
                        QuranPersonalNoteEntity(
                            surahNumber = o.optInt("surahNumber", 1),
                            ayahNumber = o.optInt("ayahNumber", 1),
                            noteText = text,
                            isFavorite = o.optBoolean("isFavorite", false)
                        )
                    )
                    noteCount++
                }
            }

            var khatmCount = 0
            root.optJSONArray("khatmPlans")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val title = o.optString("title")
                    if (title.isBlank()) continue
                    dao.upsertKhatmPlan(
                        KhatmPlanEntity(
                            title = title,
                            startDateKey = o.optString("startDateKey", todayKey()),
                            targetDateKey = o.optString("targetDateKey", todayKey()),
                            currentPage = o.optInt("currentPage", 1),
                            currentJuz = o.optInt("currentJuz", 1),
                            dailyTargetPages = o.optInt("dailyTargetPages", 20),
                            totalPages = o.optInt("totalPages", 604),
                            isCompleted = o.optBoolean("isCompleted", false)
                        )
                    )
                    khatmCount++
                }
            }

            logAudit(
                actorName,
                "RESTORE_LOCAL_BACKUP",
                "BACKUP",
                "INFO",
                "Restored KB=$kbCount Books=$bookCount Circles=$circleCount Students=$studentCount"
            )
            RestoreSummary(kbCount, bookCount, circleCount, studentCount, teacherCount, quizCount, noteCount, khatmCount)
        }

    // ======================== v6 ADMIN AD CARDS (LOCAL / OFFLINE) ========================

    suspend fun saveAdCardLocal(card: AdminAdCardEntity) = withContext(Dispatchers.IO) {
        dao.upsertAdCard(card.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun deleteAdCardLocal(id: String) = withContext(Dispatchers.IO) {
        dao.deleteAdCard(id)
    }

    suspend fun recordAdImpressionLocal(id: String) = withContext(Dispatchers.IO) {
        dao.incrementAdImpression(id)
    }

    suspend fun recordAdClickLocal(id: String) = withContext(Dispatchers.IO) {
        dao.incrementAdClick(id)
    }

    // ======================== v6 ADMIN MESSAGES (LOCAL / OFFLINE) ========================

    suspend fun saveAdminMessageLocal(message: AdminMessageEntity) = withContext(Dispatchers.IO) {
        dao.upsertAdminMessage(message)
    }

    suspend fun markAdminMessageReadLocal(id: String) = withContext(Dispatchers.IO) {
        dao.markAdminMessageRead(id)
    }

    suspend fun deleteAdminMessageLocal(id: String) = withContext(Dispatchers.IO) {
        dao.deleteAdminMessage(id)
    }
}
