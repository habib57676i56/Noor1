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
        BackupSnapshotEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NoorDatabase : RoomDatabase() {
    abstract fun noorDao(): NoorDao

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

        fun getInstance(context: Context): NoorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NoorDatabase::class.java,
                    "noor_islamic_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

