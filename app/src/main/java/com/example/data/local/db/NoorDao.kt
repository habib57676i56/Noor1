package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoorDao {

    // --- Tasbih Sessions ---
    @Query("SELECT * FROM tasbih_sessions ORDER BY timestampMillis DESC LIMIT 60")
    fun observeTasbihSessions(): Flow<List<TasbihSessionEntity>>

    @Query("SELECT * FROM tasbih_sessions ORDER BY timestampMillis DESC")
    suspend fun getAllTasbihSessionsOnce(): List<TasbihSessionEntity>

    @Query("SELECT COALESCE(SUM(count), 0) FROM tasbih_sessions WHERE dateKey = :dateKey")
    fun observeTodayTasbihTotal(dateKey: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(count), 0) FROM tasbih_sessions")
    fun observeAllTimeTasbihTotal(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasbih_sessions")
    fun observeTasbihSessionCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasbihSession(session: TasbihSessionEntity)

    @Query("DELETE FROM tasbih_sessions")
    suspend fun clearTasbihSessions()

    // --- Custom Dhikr Presets ---
    @Query("SELECT * FROM custom_dhikr_presets ORDER BY createdAtMillis DESC")
    fun observeCustomDhikrPresets(): Flow<List<CustomDhikrEntity>>

    @Query("SELECT * FROM custom_dhikr_presets ORDER BY createdAtMillis DESC")
    suspend fun getAllCustomDhikrOnce(): List<CustomDhikrEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomDhikr(entity: CustomDhikrEntity)

    @Query("DELETE FROM custom_dhikr_presets WHERE id = :id")
    suspend fun deleteCustomDhikr(id: String)

    @Query("DELETE FROM custom_dhikr_presets")
    suspend fun clearCustomDhikrPresets()

    // --- Adhkar Favorites & Progress ---
    @Query("SELECT dhikrId FROM adhkar_favorites")
    fun observeFavoriteDhikrIds(): Flow<List<String>>

    @Query("SELECT * FROM adhkar_favorites")
    suspend fun getAllFavoritesOnce(): List<AdhkarFavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteDhikr(entity: AdhkarFavoriteEntity)

    @Query("DELETE FROM adhkar_favorites WHERE dhikrId = :dhikrId")
    suspend fun removeFavoriteDhikr(dhikrId: String)

    @Query("SELECT * FROM adhkar_progress WHERE dateKey = :dateKey")
    fun observeAdhkarProgressForDate(dateKey: String): Flow<List<AdhkarProgressEntity>>

    @Query("SELECT COUNT(*) FROM adhkar_progress WHERE isCompleted = 1")
    fun observeTotalCompletedAdhkar(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdhkarProgress(progress: AdhkarProgressEntity)

    @Query("DELETE FROM adhkar_progress WHERE dateKey = :dateKey")
    suspend fun resetAdhkarForDate(dateKey: String)

    @Query("DELETE FROM adhkar_favorites")
    suspend fun clearAdhkarFavorites()

    @Query("DELETE FROM adhkar_progress")
    suspend fun clearAdhkarProgress()

    // --- Quran Bookmarks & Offline Cache ---
    @Query("SELECT * FROM quran_bookmarks ORDER BY createdAtMillis DESC")
    fun observeQuranBookmarks(): Flow<List<QuranBookmarkEntity>>

    @Query("SELECT * FROM quran_bookmarks ORDER BY createdAtMillis DESC")
    suspend fun getAllBookmarksOnce(): List<QuranBookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuranBookmark(bookmark: QuranBookmarkEntity)

    @Query("DELETE FROM quran_bookmarks WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    suspend fun deleteQuranBookmark(surahNumber: Int, ayahNumber: Int)

    @Query("DELETE FROM quran_bookmarks")
    suspend fun clearQuranBookmarks()

    @Query("SELECT * FROM quran_verses_cache WHERE surahNumber = :surahNumber ORDER BY numberInSurah ASC")
    suspend fun getCachedSurahVerses(surahNumber: Int): List<QuranVerseCacheEntity>

    @Query("SELECT * FROM quran_verses_cache WHERE textUthmani LIKE '%' || :query || '%' OR translationEn LIKE '%' || :query || '%' LIMIT 50")
    suspend fun searchCachedVerses(query: String): List<QuranVerseCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuranVerses(verses: List<QuranVerseCacheEntity>)

    // --- Daily Activity Logs (Stats & Streaks) ---
    @Query("SELECT * FROM daily_activity_log WHERE dateKey = :dateKey")
    fun observeDailyActivity(dateKey: String): Flow<DailyActivityEntity?>

    @Query("SELECT * FROM daily_activity_log WHERE dateKey = :dateKey")
    suspend fun getDailyActivityOnce(dateKey: String): DailyActivityEntity?

    @Query("SELECT * FROM daily_activity_log ORDER BY dateKey DESC LIMIT 35")
    fun observeRecentDailyActivities(): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity_log ORDER BY dateKey DESC")
    suspend fun getAllDailyActivitiesOnce(): List<DailyActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyActivity(activity: DailyActivityEntity)

    @Query("DELETE FROM daily_activity_log")
    suspend fun clearDailyActivities()

    // --- Audio Library: Favorites, Recents, Playlists ---
    @Query("SELECT trackId FROM audio_favorites ORDER BY savedAtMillis DESC")
    fun observeFavoriteAudioIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteAudio(entity: AudioFavoriteEntity)

    @Query("DELETE FROM audio_favorites WHERE trackId = :trackId")
    suspend fun removeFavoriteAudio(trackId: String)

    @Query("DELETE FROM audio_favorites")
    suspend fun clearAudioFavorites()

    @Query("SELECT * FROM audio_recents ORDER BY playedAtMillis DESC LIMIT 25")
    fun observeRecentAudio(): Flow<List<AudioRecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentAudio(entity: AudioRecentEntity)

    @Query("DELETE FROM audio_recents")
    suspend fun clearRecentAudio()

    @Query("SELECT * FROM audio_playlists ORDER BY createdAtMillis DESC")
    fun observeAudioPlaylists(): Flow<List<AudioPlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioPlaylist(playlist: AudioPlaylistEntity): Long

    @Query("DELETE FROM audio_playlists WHERE id = :playlistId")
    suspend fun deleteAudioPlaylist(playlistId: Long)

    @Query("DELETE FROM audio_playlist_items WHERE playlistId = :playlistId")
    suspend fun deletePlaylistItemsForPlaylist(playlistId: Long)

    @Query("SELECT * FROM audio_playlist_items ORDER BY addedAtMillis ASC")
    fun observeAllPlaylistItems(): Flow<List<AudioPlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: AudioPlaylistItemEntity)

    @Query("DELETE FROM audio_playlist_items WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removePlaylistItem(playlistId: Long, trackId: String)

    @Query("DELETE FROM audio_playlists")
    suspend fun clearAudioPlaylists()

    @Query("DELETE FROM audio_playlist_items")
    suspend fun clearAudioPlaylistItems()

    // --- Custom Daily Worship Routines ---
    @Query("SELECT * FROM custom_worship_routines ORDER BY createdAtMillis ASC")
    fun observeCustomRoutines(): Flow<List<CustomRoutineEntity>>

    @Query("SELECT * FROM custom_worship_routines ORDER BY createdAtMillis ASC")
    suspend fun getAllCustomRoutinesOnce(): List<CustomRoutineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomRoutine(routine: CustomRoutineEntity): Long

    @Query("DELETE FROM custom_worship_routines WHERE id = :id")
    suspend fun deleteCustomRoutine(id: Long)

    @Query("DELETE FROM custom_worship_routines")
    suspend fun clearCustomRoutines()

    // --- NOOR 3.0: Adhkar Completion History ---
    @Query("SELECT * FROM adhkar_completion_history WHERE categoryId = :categoryId ORDER BY dateKey DESC LIMIT 30")
    fun observeAdhkarHistory(categoryId: String): Flow<List<AdhkarCompletionHistoryEntity>>

    @Query("SELECT * FROM adhkar_completion_history WHERE categoryId = :categoryId AND dateKey = :dateKey")
    suspend fun getAdhkarHistoryForDay(categoryId: String, dateKey: String): AdhkarCompletionHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdhkarHistory(entity: AdhkarCompletionHistoryEntity)

    @Query("DELETE FROM adhkar_completion_history")
    suspend fun clearAdhkarHistory()

    // --- NOOR 3.0: Local CMS Cache & Revisions ---
    @Query("SELECT * FROM cms_content_local ORDER BY updatedAtMillis DESC")
    fun observeLocalCmsContent(): Flow<List<CmsContentLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCmsContentLocal(item: CmsContentLocalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCmsContentLocalList(items: List<CmsContentLocalEntity>)

    @Query("DELETE FROM cms_content_local WHERE id = :id")
    suspend fun deleteCmsContentLocal(id: String)

    @Query("SELECT * FROM cms_revisions_local WHERE contentId = :contentId ORDER BY version DESC")
    fun observeCmsRevisionsLocal(contentId: String): Flow<List<CmsRevisionLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCmsRevisionLocal(revision: CmsRevisionLocalEntity)

    // --- NOOR 3.0: Local Support Tickets & Announcements ---
    @Query("SELECT * FROM support_tickets_local ORDER BY createdAtMillis DESC")
    fun observeLocalSupportTickets(): Flow<List<SupportTicketLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSupportTicketLocal(ticket: SupportTicketLocalEntity)

    @Query("SELECT * FROM app_announcements_local ORDER BY createdAtMillis DESC")
    fun observeLocalAnnouncements(): Flow<List<AnnouncementLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnnouncementsLocal(items: List<AnnouncementLocalEntity>)

    @Query("UPDATE app_announcements_local SET isRead = 1 WHERE id = :id")
    suspend fun markAnnouncementRead(id: String)

    // --- NOOR 3.0: Audit Logs, Error Logs & Rollback Snapshots ---
    @Query("SELECT * FROM admin_audit_logs_local ORDER BY createdAtMillis DESC LIMIT 100")
    fun observeLocalAuditLogs(): Flow<List<AdminAuditLogLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalAuditLog(log: AdminAuditLogLocalEntity)

    @Query("SELECT * FROM system_error_logs ORDER BY createdAtMillis DESC LIMIT 100")
    fun observeSystemErrorLogs(): Flow<List<SystemErrorLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSystemErrorLog(log: SystemErrorLogEntity)

    @Query("DELETE FROM system_error_logs")
    suspend fun clearSystemErrorLogs()

    @Query("SELECT * FROM backup_snapshots ORDER BY createdAtMillis DESC LIMIT 20")
    fun observeBackupSnapshots(): Flow<List<BackupSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupSnapshot(snapshot: BackupSnapshotEntity): Long

    @Query("DELETE FROM backup_snapshots WHERE id = :id")
    suspend fun deleteBackupSnapshot(id: Long)
}

