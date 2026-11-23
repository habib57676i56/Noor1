package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// =========================================================================
// 1. NOOR HALAQAT — Quran Learning Circles Management (Offline-First)
// =========================================================================

@Entity(tableName = "halaqah_circles")
data class HalaqahCircleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val level: String = "BEGINNER", // BEGINNER, INTERMEDIATE, ADVANCED, IJAZAH
    val circleType: String = "MEMORIZATION", // MEMORIZATION, REVISION, TAJWEED
    val scheduleText: String = "",
    val teacherId: Long? = null,
    val active: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "halaqah_teachers")
data class HalaqahTeacherEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val phone: String = "",
    val specialization: String = "حفظ وتجويد",
    val scheduleNotes: String = "",
    val active: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "halaqah_students")
data class HalaqahStudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val age: Int = 12,
    val guardianContact: String = "",
    val circleId: Long? = null,
    val teacherId: Long? = null,
    val memorizedJuz: Int = 0,
    val memorizedPages: Int = 0,
    val revisionPages: Int = 0,
    val currentSurahNumber: Int = 1,
    val currentAyahNumber: Int = 1,
    val teacherFeedback: String = "",
    val active: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "halaqah_attendance")
data class HalaqahAttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val circleId: Long,
    val studentId: Long,
    val dateKey: String, // YYYY-MM-DD
    val status: String = "PRESENT", // PRESENT, ABSENT, EXCUSED, LATE
    val notes: String = "",
    val recordedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "recitation_assessments")
data class RecitationAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val teacherId: Long? = null,
    val circleId: Long? = null,
    val surahNumber: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val newMemorizationSummary: String = "",
    val previousRevisionSummary: String = "",
    val accuracyPercent: Int = 90,
    val tajweedObservations: String = "",
    val mistakesToRevise: String = "",
    val teacherComments: String = "",
    val assessmentDateKey: String,
    val nextAssessmentDateKey: String = "",
    val approvedByTeacher: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

// =========================================================================
// 2. NOOR HIFZ AI — Memorization Schedules, Weak Verses & Revision History
// =========================================================================

@Entity(tableName = "hifz_plans")
data class HifzPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetSurahNumber: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val currentAyah: Int,
    val dailyAyahTarget: Int = 5,
    val revisionCycleDays: Int = 7,
    val teacherApproved: Boolean = false,
    val isCompleted: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "hifz_revision_logs")
data class HifzRevisionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surahNumber: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val sessionType: String = "LIVE_CHECK", // LIVE_CHECK, HIDDEN_TEST, COMPLETION, RANDOM_QUIZ
    val accuracyPercent: Int = 0,
    val uncertainCount: Int = 0,
    val possibleMistakeNotes: String = "",
    val dateKey: String,
    val teacherApproved: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "hifz_weak_verses")
data class HifzWeakVerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val missCount: Int = 1,
    val lastMistakeNote: String = "",
    val resolved: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

// =========================================================================
// 3. NOOR AI — Offline Knowledge Base & Content Approval
// =========================================================================

@Entity(tableName = "local_ai_knowledge")
data class LocalAiKnowledgeEntity(
    @PrimaryKey val id: String,
    val category: String, // QURAN, HADITH, FIQH, AQEEDAH, SEERAH, TAJWEED, WORSHIP, GENERAL
    val titleAr: String,
    val titleEn: String,
    val questionAr: String,
    val questionEn: String = "",
    val answerAr: String,
    val answerEn: String = "",
    val sourceRef: String,
    val verificationStatus: String = "VERIFIED", // VERIFIED, REVIEW, UNCERTAIN
    val scholarlyNotes: String = "",
    val keywords: String = "",
    val contentHash: String = "",
    val approvalStatus: String = "APPROVED", // APPROVED, PENDING, REJECTED
    val isUserAdded: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

// =========================================================================
// 4. NOOR LIBRARY — Offline Islamic Library & Personal Study Notes
// =========================================================================

@Entity(tableName = "library_books")
data class LibraryBookEntity(
    @PrimaryKey val id: String,
    val category: String, // TAFSEER, HADITH, FIQH, AQEEDAH, SEERAH, PROPHETS, COMPANIONS, LESSONS
    val titleAr: String,
    val titleEn: String,
    val authorAr: String,
    val authorEn: String,
    val summaryAr: String = "",
    val contentText: String,
    val sourceReference: String = "",
    val isImported: Boolean = false,
    val isBookmarked: Boolean = false,
    val lastReadCharOffset: Int = 0,
    val totalLength: Int = 0,
    val approvalStatus: String = "APPROVED",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "library_notes")
data class LibraryNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val noteText: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

// =========================================================================
// 5. NOOR TAJWEED & NOOR KIDS Progress
// =========================================================================

@Entity(tableName = "tajweed_progress")
data class TajweedProgressEntity(
    @PrimaryKey val ruleId: String,
    val completed: Boolean = false,
    val quizScorePercent: Int = 0,
    val practicedCount: Int = 0,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "kids_progress")
data class KidsProgressEntity(
    @PrimaryKey val itemId: String,
    val itemType: String, // SURAH, DUA, STORY, PILLAR, QUIZ
    val starsEarned: Int = 0,
    val completed: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

// =========================================================================
// 6. NOOR WORSHIP (Fasting) & NOOR KHATM
// =========================================================================

@Entity(tableName = "fasting_logs")
data class FastingLogEntity(
    @PrimaryKey val dateKey: String, // YYYY-MM-DD
    val fastType: String = "SUNNAH", // SUNNAH, WHITE_DAY, RAMADAN, QADA, VOLUNTARY
    val completed: Boolean = true,
    val notes: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "khatm_plans")
data class KhatmPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startDateKey: String,
    val targetDateKey: String,
    val currentPage: Int = 1,
    val currentJuz: Int = 1,
    val dailyTargetPages: Int = 20,
    val totalPages: Int = 604,
    val isCompleted: Boolean = false,
    val reminderEnabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "khatm_reading_logs")
data class KhatmReadingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val dateKey: String,
    val pagesRead: Int,
    val reachedPage: Int,
    val createdAtMillis: Long = System.currentTimeMillis()
)

// =========================================================================
// 7. NOOR QUIZ — Offline Assessment & Custom Admin Quizzes
// =========================================================================

@Entity(tableName = "custom_quiz_questions")
data class CustomQuizQuestionEntity(
    @PrimaryKey val id: String,
    val category: String, // QURAN, HADITH, KNOWLEDGE, TAJWEED, MEMORIZATION, KIDS
    val difficulty: String = "EASY", // EASY, MEDIUM, HARD
    val questionAr: String,
    val questionEn: String = "",
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctIndex: Int, // 0..3
    val explanationAr: String = "",
    val sourceRef: String = "",
    val isAdminCreated: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val difficulty: String,
    val score: Int,
    val totalQuestions: Int,
    val dateKey: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

// =========================================================================
// 8. NOOR NOTES — Personal Quran Notes (Strictly Read-Only on Uthmani Text)
// =========================================================================

@Entity(tableName = "quran_note_collections")
data class QuranNoteCollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorTag: String = "GOLD",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "quran_personal_notes")
data class QuranPersonalNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val collectionId: Long? = null,
    val noteText: String,
    val isFavorite: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

// =========================================================================
// 9. NOOR OFFLINE SUPER ADMIN — Local RBAC Accounts (Salted PBKDF2 Hash)
// =========================================================================

@Entity(tableName = "local_admin_accounts")
data class LocalAdminAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val role: String, // SUPER_ADMIN, ADMIN, TEACHER, STUDENT
    val passwordHash: String,
    val passwordSalt: String,
    val active: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

// =========================================================================
// 10. NOOR v5 — Recitation Recordings, Imported AI Documents & Admin Settings
// =========================================================================

@Entity(tableName = "student_recitation_recordings")
data class StudentRecitationRecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long? = null,
    val studentName: String = "",
    val surahNumber: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val testMode: String = "LIVE_RECITATION",
    val filePath: String,
    val durationMs: Long = 0L,
    val recognizedTranscript: String = "",
    val accuracyPercent: Int = 0,
    val suspectedMistakesSummary: String = "",
    val uncertainCount: Int = 0,
    val teacherReviewStatus: String = "PENDING", // PENDING, APPROVED, NEEDS_REVISION
    val teacherComment: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "imported_ai_documents")
data class ImportedAiDocumentEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val format: String, // TXT, JSON, DOCX, PDF
    val detectedLanguage: String = "AR",
    val category: String = "GENERAL",
    val chunkCount: Int = 0,
    val charCount: Int = 0,
    val importedBy: String = "local_admin",
    val warningNote: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_config_entries")
data class AdminConfigEntryEntity(
    @PrimaryKey val configKey: String,
    val configValue: String,
    val updatedBy: String = "local_admin",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

// =========================================================================
// 11. NOOR v6 — Administrative Advertisement Cards & Admin-to-User Messaging
// =========================================================================

@Entity(tableName = "admin_ad_cards")
data class AdminAdCardEntity(
    @PrimaryKey val id: String,
    val titleAr: String,
    val titleEn: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val imageUriOrUrl: String = "",
    val destinationUrl: String = "",
    val placement: String = "HOME", // ONLY: HOME, ECOSYSTEM_HUB, AUDIO_LIBRARY
    val enabled: Boolean = true,
    val startAtMillis: Long = 0L,
    val endAtMillis: Long = 0L,
    val displayOrder: Int = 1,
    val impressionsCount: Long = 0L,
    val clicksCount: Long = 0L,
    val createdBy: String = "super_admin",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_messages")
data class AdminMessageEntity(
    @PrimaryKey val id: String,
    val targetType: String = "ALL", // ALL, GROUP, USER
    val targetGroup: String = "ALL", // ALL, ACTIVE, VERIFIED, ADMINS
    val targetUserUid: String = "",
    val targetUserEmail: String = "",
    val titleAr: String,
    val titleEn: String = "",
    val bodyAr: String,
    val bodyEn: String = "",
    val messageType: String = "SYSTEM", // SYSTEM, MAINTENANCE, FEATURE_UPDATE, REMINDER, EDUCATIONAL, DIRECT
    val priority: String = "NORMAL", // LOW, NORMAL, HIGH, URGENT
    val imageUriOrUrl: String = "",
    val scheduledAtMillis: Long = 0L,
    val sentByUid: String = "",
    val sentByEmail: String = "",
    val delivered: Boolean = true,
    val readByCurrentUser: Boolean = false,
    val readAtMillis: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis()
)


