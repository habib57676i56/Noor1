package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TasbihSessionEntity::class,
        AdhkarFavoriteEntity::class,
        AdhkarProgressEntity::class,
        QuranBookmarkEntity::class,
        QuranVerseCacheEntity::class,
        DailyActivityEntity::class,
        AudioFavoriteEntity::class,
        AudioRecentEntity::class,
        AudioPlaylistEntity::class,
        AudioPlaylistItemEntity::class,
        CustomDhikrEntity::class,
        CustomRoutineEntity::class,
        AdhkarCompletionHistoryEntity::class,
        CmsContentLocalEntity::class,
        CmsRevisionLocalEntity::class,
        SupportTicketLocalEntity::class,
        AnnouncementLocalEntity::class,
        AdminAuditLogLocalEntity::class,
        SystemErrorLogEntity::class,
        BackupSnapshotEntity::class,
        // NOOR 4.0 Complete Islamic Ecosystem entities
        HalaqahCircleEntity::class,
        HalaqahTeacherEntity::class,
        HalaqahStudentEntity::class,
        HalaqahAttendanceEntity::class,
        RecitationAssessmentEntity::class,
        HifzPlanEntity::class,
        HifzRevisionLogEntity::class,
        HifzWeakVerseEntity::class,
        LocalAiKnowledgeEntity::class,
        LibraryBookEntity::class,
        LibraryNoteEntity::class,
        TajweedProgressEntity::class,
        KidsProgressEntity::class,
        FastingLogEntity::class,
        KhatmPlanEntity::class,
        KhatmReadingLogEntity::class,
        CustomQuizQuestionEntity::class,
        QuizAttemptEntity::class,
        QuranNoteCollectionEntity::class,
        QuranPersonalNoteEntity::class,
        LocalAdminAccountEntity::class,
        // NOOR v5 entities: Recitation recordings, Imported AI docs, Admin settings
        StudentRecitationRecordingEntity::class,
        ImportedAiDocumentEntity::class,
        AdminConfigEntryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class NoorDatabase : RoomDatabase() {
    abstract fun noorDao(): NoorDao
    abstract fun ecosystemDao(): EcosystemDao

    companion object {
        @Volatile
        private var INSTANCE: NoorDatabase? = null

        /**
         * Non-destructive migration from NOOR 1.0 (v1) to NOOR 2.0 (v2).
         * Preserves all existing bookmarks, tasbih sessions, adhkar progress,
         * favorites, and daily worship activity logs while adding new columns and tables.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE quran_verses_cache ADD COLUMN translationEn TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE quran_verses_cache ADD COLUMN pageNumber INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE quran_verses_cache ADD COLUMN juzNumber INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE quran_verses_cache ADD COLUMN hizbQuarter INTEGER NOT NULL DEFAULT 1"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS audio_favorites (
                        trackId TEXT NOT NULL PRIMARY KEY,
                        savedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS audio_recents (
                        trackId TEXT NOT NULL PRIMARY KEY,
                        titleAr TEXT NOT NULL,
                        titleEn TEXT NOT NULL,
                        reciterNameAr TEXT NOT NULL,
                        reciterNameEn TEXT NOT NULL,
                        categoryKey TEXT NOT NULL,
                        surahNumber INTEGER,
                        audioUrl TEXT NOT NULL,
                        playedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS audio_playlists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nameAr TEXT NOT NULL,
                        nameEn TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS audio_playlist_items (
                        playlistId INTEGER NOT NULL,
                        trackId TEXT NOT NULL,
                        addedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(playlistId, trackId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_dhikr_presets (
                        id TEXT NOT NULL PRIMARY KEY,
                        textAr TEXT NOT NULL,
                        textEn TEXT NOT NULL,
                        defaultTarget INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_worship_routines (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        titleAr TEXT NOT NULL,
                        titleEn TEXT NOT NULL,
                        category TEXT NOT NULL,
                        completedDateKey TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Non-destructive migration from NOOR 2.0 (v2) to NOOR 3.0 (v3).
         * Adds Adhkar session history, offline CMS cache & revisions, support tickets,
         * announcements, local audit/error logs, and rollback backup snapshots.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS adhkar_completion_history (
                        categoryId TEXT NOT NULL,
                        dateKey TEXT NOT NULL,
                        completedItems INTEGER NOT NULL,
                        totalItems INTEGER NOT NULL,
                        isFullyCompleted INTEGER NOT NULL,
                        lastDhikrIndex INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(categoryId, dateKey)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cms_content_local (
                        id TEXT NOT NULL PRIMARY KEY,
                        category TEXT NOT NULL,
                        subCategory TEXT NOT NULL,
                        titleAr TEXT NOT NULL,
                        titleEn TEXT NOT NULL,
                        bodyAr TEXT NOT NULL,
                        bodyEn TEXT NOT NULL,
                        transliteration TEXT NOT NULL,
                        sourceAr TEXT NOT NULL,
                        sourceEn TEXT NOT NULL,
                        gradingAr TEXT NOT NULL,
                        gradingEn TEXT NOT NULL,
                        repetitionCount INTEGER NOT NULL,
                        audioUrl TEXT NOT NULL,
                        status TEXT NOT NULL,
                        version INTEGER NOT NULL,
                        scheduledAtMillis INTEGER NOT NULL,
                        updatedBy TEXT NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cms_revisions_local (
                        id TEXT NOT NULL PRIMARY KEY,
                        contentId TEXT NOT NULL,
                        version INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        bodyAr TEXT NOT NULL,
                        bodyEn TEXT NOT NULL,
                        sourceAr TEXT NOT NULL,
                        repetitionCount INTEGER NOT NULL,
                        changeSummary TEXT NOT NULL,
                        editorUid TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS support_tickets_local (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        userEmail TEXT NOT NULL,
                        userName TEXT NOT NULL,
                        ticketType TEXT NOT NULL,
                        subject TEXT NOT NULL,
                        message TEXT NOT NULL,
                        status TEXT NOT NULL,
                        adminReply TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_announcements_local (
                        id TEXT NOT NULL PRIMARY KEY,
                        titleAr TEXT NOT NULL,
                        titleEn TEXT NOT NULL,
                        bodyAr TEXT NOT NULL,
                        bodyEn TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        active INTEGER NOT NULL,
                        isRead INTEGER NOT NULL,
                        scheduledAtMillis INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS admin_audit_logs_local (
                        id TEXT NOT NULL PRIMARY KEY,
                        actorUid TEXT NOT NULL,
                        actorEmail TEXT NOT NULL,
                        action TEXT NOT NULL,
                        category TEXT NOT NULL,
                        severity TEXT NOT NULL,
                        details TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS system_error_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tag TEXT NOT NULL,
                        message TEXT NOT NULL,
                        severity TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS backup_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        payloadJson TEXT NOT NULL,
                        sizeBytes INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Non-destructive migration from NOOR 3.0 (v3) to NOOR 4.0 Ecosystem (v4).
         * Adds Halaqat, Hifz AI, Local NOOR AI, Islamic Library, Tajweed, Kids, Fasting,
         * Khatm, Quiz, Personal Quran Notes, and Local Offline Super Admin tables.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS halaqah_circles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        level TEXT NOT NULL,
                        circleType TEXT NOT NULL,
                        scheduleText TEXT NOT NULL,
                        teacherId INTEGER,
                        active INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS halaqah_teachers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fullName TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        specialization TEXT NOT NULL,
                        scheduleNotes TEXT NOT NULL,
                        active INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS halaqah_students (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fullName TEXT NOT NULL,
                        age INTEGER NOT NULL,
                        guardianContact TEXT NOT NULL,
                        circleId INTEGER,
                        teacherId INTEGER,
                        memorizedJuz INTEGER NOT NULL,
                        memorizedPages INTEGER NOT NULL,
                        revisionPages INTEGER NOT NULL,
                        currentSurahNumber INTEGER NOT NULL,
                        currentAyahNumber INTEGER NOT NULL,
                        teacherFeedback TEXT NOT NULL,
                        active INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS halaqah_attendance (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        circleId INTEGER NOT NULL,
                        studentId INTEGER NOT NULL,
                        dateKey TEXT NOT NULL,
                        status TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        recordedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recitation_assessments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        studentId INTEGER NOT NULL,
                        teacherId INTEGER,
                        circleId INTEGER,
                        surahNumber INTEGER NOT NULL,
                        fromAyah INTEGER NOT NULL,
                        toAyah INTEGER NOT NULL,
                        newMemorizationSummary TEXT NOT NULL,
                        previousRevisionSummary TEXT NOT NULL,
                        accuracyPercent INTEGER NOT NULL,
                        tajweedObservations TEXT NOT NULL,
                        mistakesToRevise TEXT NOT NULL,
                        teacherComments TEXT NOT NULL,
                        assessmentDateKey TEXT NOT NULL,
                        nextAssessmentDateKey TEXT NOT NULL,
                        approvedByTeacher INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS hifz_plans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        targetSurahNumber INTEGER NOT NULL,
                        fromAyah INTEGER NOT NULL,
                        toAyah INTEGER NOT NULL,
                        currentAyah INTEGER NOT NULL,
                        dailyAyahTarget INTEGER NOT NULL,
                        revisionCycleDays INTEGER NOT NULL,
                        teacherApproved INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS hifz_revision_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        surahNumber INTEGER NOT NULL,
                        fromAyah INTEGER NOT NULL,
                        toAyah INTEGER NOT NULL,
                        sessionType TEXT NOT NULL,
                        accuracyPercent INTEGER NOT NULL,
                        uncertainCount INTEGER NOT NULL,
                        possibleMistakeNotes TEXT NOT NULL,
                        dateKey TEXT NOT NULL,
                        teacherApproved INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS hifz_weak_verses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        surahNumber INTEGER NOT NULL,
                        ayahNumber INTEGER NOT NULL,
                        missCount INTEGER NOT NULL,
                        lastMistakeNote TEXT NOT NULL,
                        resolved INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS local_ai_knowledge (
                        id TEXT NOT NULL PRIMARY KEY,
                        category TEXT NOT NULL,
                        titleAr TEXT NOT NULL,
                        titleEn TEXT NOT NULL,
                        questionAr TEXT NOT NULL,
                        questionEn TEXT NOT NULL,
                        answerAr TEXT NOT NULL,
                        answerEn TEXT NOT NULL,
                        sourceRef TEXT NOT NULL,
                        verificationStatus TEXT NOT NULL,
                        scholarlyNotes TEXT NOT NULL,
                        contentHash TEXT NOT NULL,
                        approvalStatus TEXT NOT NULL,
                        isUserAdded INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS library_books (
                        id TEXT NOT NULL PRIMARY KEY,
                        category TEXT NOT NULL,
                        titleAr TEXT NOT NULL,
                        titleEn TEXT NOT NULL,
                        authorAr TEXT NOT NULL,
                        authorEn TEXT NOT NULL,
                        summaryAr TEXT NOT NULL,
                        contentText TEXT NOT NULL,
                        sourceReference TEXT NOT NULL,
                        isImported INTEGER NOT NULL,
                        isBookmarked INTEGER NOT NULL,
                        lastReadCharOffset INTEGER NOT NULL,
                        totalLength INTEGER NOT NULL,
                        approvalStatus TEXT NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS library_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bookId TEXT NOT NULL,
                        noteText TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tajweed_progress (
                        ruleId TEXT NOT NULL PRIMARY KEY,
                        completed INTEGER NOT NULL,
                        quizScorePercent INTEGER NOT NULL,
                        practicedCount INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS kids_progress (
                        itemId TEXT NOT NULL PRIMARY KEY,
                        itemType TEXT NOT NULL,
                        starsEarned INTEGER NOT NULL,
                        completed INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fasting_logs (
                        dateKey TEXT NOT NULL PRIMARY KEY,
                        fastType TEXT NOT NULL,
                        completed INTEGER NOT NULL,
                        notes TEXT NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS khatm_plans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        startDateKey TEXT NOT NULL,
                        targetDateKey TEXT NOT NULL,
                        currentPage INTEGER NOT NULL,
                        currentJuz INTEGER NOT NULL,
                        dailyTargetPages INTEGER NOT NULL,
                        totalPages INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        reminderEnabled INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS khatm_reading_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        planId INTEGER NOT NULL,
                        dateKey TEXT NOT NULL,
                        pagesRead INTEGER NOT NULL,
                        reachedPage INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_quiz_questions (
                        id TEXT NOT NULL PRIMARY KEY,
                        category TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        questionAr TEXT NOT NULL,
                        questionEn TEXT NOT NULL,
                        optionA TEXT NOT NULL,
                        optionB TEXT NOT NULL,
                        optionC TEXT NOT NULL,
                        optionD TEXT NOT NULL,
                        correctIndex INTEGER NOT NULL,
                        explanationAr TEXT NOT NULL,
                        sourceRef TEXT NOT NULL,
                        isAdminCreated INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS quiz_attempts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        category TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        totalQuestions INTEGER NOT NULL,
                        dateKey TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS quran_note_collections (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        colorTag TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS quran_personal_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        surahNumber INTEGER NOT NULL,
                        ayahNumber INTEGER NOT NULL,
                        collectionId INTEGER,
                        noteText TEXT NOT NULL,
                        isFavorite INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS local_admin_accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        username TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        role TEXT NOT NULL,
                        passwordHash TEXT NOT NULL,
                        passwordSalt TEXT NOT NULL,
                        active INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Non-destructive migration from NOOR 4.0 (v4) to NOOR 5.0 (v5).
         * Adds keywords column to local_ai_knowledge and creates tables for
         * student recitation recordings, imported AI documents, and admin config/feature flags.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE local_ai_knowledge ADD COLUMN keywords TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS student_recitation_recordings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        studentId INTEGER,
                        studentName TEXT NOT NULL,
                        surahNumber INTEGER NOT NULL,
                        fromAyah INTEGER NOT NULL,
                        toAyah INTEGER NOT NULL,
                        testMode TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        durationMs INTEGER NOT NULL,
                        recognizedTranscript TEXT NOT NULL,
                        accuracyPercent INTEGER NOT NULL,
                        suspectedMistakesSummary TEXT NOT NULL,
                        uncertainCount INTEGER NOT NULL,
                        teacherReviewStatus TEXT NOT NULL,
                        teacherComment TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS imported_ai_documents (
                        id TEXT NOT NULL PRIMARY KEY,
                        fileName TEXT NOT NULL,
                        format TEXT NOT NULL,
                        detectedLanguage TEXT NOT NULL,
                        category TEXT NOT NULL,
                        chunkCount INTEGER NOT NULL,
                        charCount INTEGER NOT NULL,
                        importedBy TEXT NOT NULL,
                        warningNote TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS admin_config_entries (
                        configKey TEXT NOT NULL PRIMARY KEY,
                        configValue TEXT NOT NULL,
                        updatedBy TEXT NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): NoorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NoorDatabase::class.java,
                    "noor_islamic_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

