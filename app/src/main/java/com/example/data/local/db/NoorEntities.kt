package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasbih_sessions")
data class TasbihSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val presetId: String,
    val labelAr: String,
    val labelEn: String,
    val count: Int,
    val target: Int,
    val dateKey: String,
    val timestampMillis: Long
)

@Entity(tableName = "adhkar_favorites")
data class AdhkarFavoriteEntity(
    @PrimaryKey val dhikrId: String,
    val savedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "adhkar_progress", primaryKeys = ["dhikrId", "dateKey"])
data class AdhkarProgressEntity(
    val dhikrId: String,
    val dateKey: String,
    val currentCount: Int,
    val targetCount: Int,
    val isCompleted: Boolean
)

@Entity(tableName = "quran_bookmarks", primaryKeys = ["surahNumber", "ayahNumber"])
data class QuranBookmarkEntity(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahNameAr: String,
    val surahNameEn: String,
    val ayahPreview: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "quran_verses_cache", primaryKeys = ["surahNumber", "numberInSurah"])
data class QuranVerseCacheEntity(
    val surahNumber: Int,
    val numberInSurah: Int,
    val globalNumber: Int,
    val textUthmani: String,
    val tafsirAr: String,
    val translationEn: String = "",
    val pageNumber: Int = 1,
    val juzNumber: Int = 1,
    val hizbQuarter: Int = 1
)

@Entity(tableName = "daily_activity_log")
data class DailyActivityEntity(
    @PrimaryKey val dateKey: String,
    val fajrPrayed: Boolean = false,
    val dhuhrPrayed: Boolean = false,
    val asrPrayed: Boolean = false,
    val maghribPrayed: Boolean = false,
    val ishaPrayed: Boolean = false,
    val tasbihCount: Int = 0,
    val adhkarCompleted: Int = 0,
    val quranVersesRead: Int = 0
) {
    val prayersCompletedCount: Int
        get() = listOf(fajrPrayed, dhuhrPrayed, asrPrayed, maghribPrayed, ishaPrayed).count { it }
}

// --- NOOR 2.0 New Persistent Entities ---

@Entity(tableName = "audio_favorites")
data class AudioFavoriteEntity(
    @PrimaryKey val trackId: String,
    val savedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "audio_recents")
data class AudioRecentEntity(
    @PrimaryKey val trackId: String,
    val titleAr: String,
    val titleEn: String,
    val reciterNameAr: String,
    val reciterNameEn: String,
    val categoryKey: String,
    val surahNumber: Int? = null,
    val audioUrl: String,
    val playedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "audio_playlists")
data class AudioPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameAr: String,
    val nameEn: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "audio_playlist_items", primaryKeys = ["playlistId", "trackId"])
data class AudioPlaylistItemEntity(
    val playlistId: Long,
    val trackId: String,
    val addedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_dhikr_presets")
data class CustomDhikrEntity(
    @PrimaryKey val id: String,
    val textAr: String,
    val textEn: String,
    val defaultTarget: Int = 33,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_worship_routines")
data class CustomRoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleAr: String,
    val titleEn: String,
    val category: String = "general",
    val completedDateKey: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

// --- NOOR 3.0 Persistent Entities (Non-Destructive Migration v2 -> v3) ---

@Entity(tableName = "adhkar_completion_history", primaryKeys = ["categoryId", "dateKey"])
data class AdhkarCompletionHistoryEntity(
    val categoryId: String,
    val dateKey: String,
    val completedItems: Int,
    val totalItems: Int,
    val isFullyCompleted: Boolean,
    val lastDhikrIndex: Int = 0,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "cms_content_local")
data class CmsContentLocalEntity(
    @PrimaryKey val id: String,
    val category: String,
    val subCategory: String = "",
    val titleAr: String,
    val titleEn: String = "",
    val bodyAr: String,
    val bodyEn: String = "",
    val transliteration: String = "",
    val sourceAr: String,
    val sourceEn: String = "",
    val gradingAr: String = "صحيح",
    val gradingEn: String = "Sahih",
    val repetitionCount: Int = 1,
    val audioUrl: String = "",
    val status: String = "DRAFT",
    val version: Int = 1,
    val scheduledAtMillis: Long = 0L,
    val updatedBy: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "cms_revisions_local")
data class CmsRevisionLocalEntity(
    @PrimaryKey val id: String,
    val contentId: String,
    val version: Int,
    val status: String,
    val bodyAr: String,
    val bodyEn: String = "",
    val sourceAr: String = "",
    val repetitionCount: Int = 1,
    val changeSummary: String = "",
    val editorUid: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "support_tickets_local")
data class SupportTicketLocalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userEmail: String = "",
    val userName: String = "",
    val ticketType: String,
    val subject: String,
    val message: String,
    val status: String = "OPEN",
    val adminReply: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_announcements_local")
data class AnnouncementLocalEntity(
    @PrimaryKey val id: String,
    val titleAr: String,
    val titleEn: String,
    val bodyAr: String,
    val bodyEn: String,
    val kind: String = "GENERAL",
    val active: Boolean = true,
    val isRead: Boolean = false,
    val scheduledAtMillis: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_audit_logs_local")
data class AdminAuditLogLocalEntity(
    @PrimaryKey val id: String,
    val actorUid: String,
    val actorEmail: String = "",
    val action: String,
    val category: String,
    val severity: String = "INFO",
    val details: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_error_logs")
data class SystemErrorLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tag: String,
    val message: String,
    val severity: String = "WARNING",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "backup_snapshots")
data class BackupSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val payloadJson: String,
    val sizeBytes: Long,
    val createdAtMillis: Long = System.currentTimeMillis()
)

