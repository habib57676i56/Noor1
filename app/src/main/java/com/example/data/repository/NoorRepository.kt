package com.example.data.repository

import android.content.Context
import com.example.data.local.catalog.SurahCatalog
import com.example.data.local.db.AdhkarFavoriteEntity
import com.example.data.local.db.AdhkarProgressEntity
import com.example.data.local.db.AudioFavoriteEntity
import com.example.data.local.db.AudioPlaylistEntity
import com.example.data.local.db.AudioPlaylistItemEntity
import com.example.data.local.db.AudioRecentEntity
import com.example.data.local.db.CustomDhikrEntity
import com.example.data.local.db.CustomRoutineEntity
import com.example.data.local.db.DailyActivityEntity
import com.example.data.local.db.NoorDao
import com.example.data.local.db.QuranBookmarkEntity
import com.example.data.local.db.QuranVerseCacheEntity
import com.example.data.local.db.TasbihSessionEntity
import com.example.data.local.prefs.UserPreferences
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.data.remote.MosqueSearchResult
import com.example.data.remote.OverpassApiService
import com.example.data.remote.QuranApiService
import com.example.domain.model.AppLanguage
import com.example.domain.model.AsrMadhhab
import com.example.domain.model.AudioTrackItem
import com.example.domain.model.CalculationMethod
import com.example.domain.model.HomeDashboardConfig
import com.example.domain.model.IqamahDelays
import com.example.domain.model.PrayerMinuteOffsets
import com.example.domain.model.PrayerName
import com.example.domain.model.QuranDisplayMode
import com.example.domain.model.QuranReadingTheme
import com.example.domain.model.QuranReciter
import com.example.domain.model.QuranVerse
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class SurahLoadResult {
    data class Success(val verses: List<QuranVerse>, val isOfflineCached: Boolean) : SurahLoadResult()
    data class Error(val messageAr: String, val messageEn: String) : SurahLoadResult()
}

class NoorRepository(
    val dao: NoorDao,
    val prefs: UserPreferencesRepository,
    private val quranApi: QuranApiService,
    private val overpassApi: OverpassApiService = OverpassApiService(),
    private val appContext: Context? = null
) {

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun todayDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // Reactive Streams
    val userPreferences: Flow<UserPreferences> = prefs.preferencesFlow
    val tasbihSessions: Flow<List<TasbihSessionEntity>> = dao.observeTasbihSessions()
    val customDhikrPresets: Flow<List<CustomDhikrEntity>> = dao.observeCustomDhikrPresets()
    val allTimeTasbihTotal: Flow<Int> = dao.observeAllTimeTasbihTotal()
    val tasbihSessionCount: Flow<Int> = dao.observeTasbihSessionCount()
    val favoriteDhikrIds: Flow<List<String>> = dao.observeFavoriteDhikrIds()
    val totalCompletedAdhkar: Flow<Int> = dao.observeTotalCompletedAdhkar()
    val quranBookmarks: Flow<List<QuranBookmarkEntity>> = dao.observeQuranBookmarks()
    val recentDailyActivities: Flow<List<DailyActivityEntity>> = dao.observeRecentDailyActivities()

    // NOOR 2.0 Audio & Routine Streams
    val favoriteAudioIds: Flow<List<String>> = dao.observeFavoriteAudioIds()
    val recentAudioTracks: Flow<List<AudioRecentEntity>> = dao.observeRecentAudio()
    val audioPlaylists: Flow<List<AudioPlaylistEntity>> = dao.observeAudioPlaylists()
    val audioPlaylistItems: Flow<List<AudioPlaylistItemEntity>> = dao.observeAllPlaylistItems()
    val customRoutines: Flow<List<CustomRoutineEntity>> = dao.observeCustomRoutines()

    fun observeTodayTasbihTotal(dateKey: String = todayDateKey()): Flow<Int> =
        dao.observeTodayTasbihTotal(dateKey)

    fun observeAdhkarProgress(dateKey: String = todayDateKey()): Flow<List<AdhkarProgressEntity>> =
        dao.observeAdhkarProgressForDate(dateKey)

    fun observeDailyActivity(dateKey: String = todayDateKey()): Flow<DailyActivityEntity?> =
        dao.observeDailyActivity(dateKey)

    // --- Prayer Completion Tracking ---
    suspend fun togglePrayerLogged(prayer: PrayerName, dateKey: String = todayDateKey()) {
        if (!prayer.isFard) return
        val current = dao.getDailyActivityOnce(dateKey) ?: DailyActivityEntity(dateKey = dateKey)
        val updated = when (prayer) {
            PrayerName.FAJR -> current.copy(fajrPrayed = !current.fajrPrayed)
            PrayerName.DHUHR -> current.copy(dhuhrPrayed = !current.dhuhrPrayed)
            PrayerName.ASR -> current.copy(asrPrayed = !current.asrPrayed)
            PrayerName.MAGHRIB -> current.copy(maghribPrayed = !current.maghribPrayed)
            PrayerName.ISHA -> current.copy(ishaPrayed = !current.ishaPrayed)
            PrayerName.SUNRISE -> current
        }
        dao.upsertDailyActivity(updated)
    }

    // --- Tasbih & Custom Dhikr Persistence ---
    suspend fun recordTasbihTap(
        presetId: String,
        newCount: Int,
        target: Int,
        customText: String? = null
    ) {
        prefs.saveActiveTasbih(presetId, newCount, target, customText)
        val dateKey = todayDateKey()
        val current = dao.getDailyActivityOnce(dateKey) ?: DailyActivityEntity(dateKey = dateKey)
        dao.upsertDailyActivity(current.copy(tasbihCount = current.tasbihCount + 1))
    }

    suspend fun saveCompletedTasbihSession(
        presetId: String,
        labelAr: String,
        labelEn: String,
        count: Int,
        target: Int
    ) {
        if (count <= 0) return
        dao.insertTasbihSession(
            TasbihSessionEntity(
                presetId = presetId,
                labelAr = labelAr,
                labelEn = labelEn,
                count = count,
                target = target,
                dateKey = todayDateKey(),
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun addCustomDhikrPreset(textAr: String, textEn: String, target: Int) {
        val cleanAr = textAr.trim()
        if (cleanAr.isEmpty()) return
        val cleanEn = textEn.trim().ifEmpty { cleanAr }
        val id = "custom_${System.currentTimeMillis()}"
        dao.upsertCustomDhikr(
            CustomDhikrEntity(
                id = id,
                textAr = cleanAr,
                textEn = cleanEn,
                defaultTarget = target.coerceIn(1, 10000)
            )
        )
    }

    suspend fun deleteCustomDhikrPreset(id: String) {
        dao.deleteCustomDhikr(id)
    }

    // --- Adhkar Progress & Favorites ---
    suspend fun toggleFavoriteDhikr(dhikrId: String, currentlyFavorite: Boolean) {
        if (currentlyFavorite) {
            dao.removeFavoriteDhikr(dhikrId)
        } else {
            dao.addFavoriteDhikr(AdhkarFavoriteEntity(dhikrId = dhikrId))
        }
    }

    suspend fun incrementDhikrProgress(
        dhikrId: String,
        currentCount: Int,
        targetCount: Int,
        dateKey: String = todayDateKey()
    ) {
        val nextCount = (currentCount + 1).coerceAtMost(targetCount)
        val newlyCompleted = currentCount < targetCount && nextCount >= targetCount
        dao.upsertAdhkarProgress(
            AdhkarProgressEntity(
                dhikrId = dhikrId,
                dateKey = dateKey,
                currentCount = nextCount,
                targetCount = targetCount,
                isCompleted = nextCount >= targetCount
            )
        )
        if (newlyCompleted) {
            val act = dao.getDailyActivityOnce(dateKey) ?: DailyActivityEntity(dateKey = dateKey)
            dao.upsertDailyActivity(act.copy(adhkarCompleted = act.adhkarCompleted + 1))
        }
    }

    suspend fun resetDhikrProgressForToday(dateKey: String = todayDateKey()) {
        dao.resetAdhkarForDate(dateKey)
    }

    // --- Quran Loading (Offline-first: Room Cache -> Bundled -> AlQuran Cloud 3-edition API + Cache) ---
    suspend fun loadSurahVerses(
        surahNumber: Int,
        reciter: QuranReciter
    ): SurahLoadResult = withContext(Dispatchers.IO) {
        val surahMeta = SurahCatalog.findSurah(surahNumber)
        val bundledMap = SurahCatalog.getBundledSurahVerses(surahNumber, reciter)
            ?.associateBy { it.numberInSurah }
            .orEmpty()

        // 1. Check Room cache first (refresh if cached v1 rows lacked English translation for non-bundled Surahs, still offline-safe)
        val cached = dao.getCachedSurahVerses(surahNumber)
        if (cached.isNotEmpty()) {
            return@withContext SurahLoadResult.Success(
                verses = cached.map { c ->
                    val asbab = SurahCatalog.getAsbab(c.surahNumber, c.numberInSurah)
                    val bundledVerse = bundledMap[c.numberInSurah]
                    QuranVerse(
                        surahNumber = c.surahNumber,
                        numberInSurah = c.numberInSurah,
                        globalNumber = c.globalNumber,
                        textUthmani = c.textUthmani,
                        tafsirAr = c.tafsirAr,
                        audioUrl = SurahCatalog.audioUrlForAyah(c.surahNumber, c.numberInSurah, reciter),
                        translationEn = c.translationEn.ifBlank { bundledVerse?.translationEn ?: "" },
                        pageNumber = if (c.pageNumber > 0) c.pageNumber else surahMeta.startPage,
                        juzNumber = if (c.juzNumber > 0) c.juzNumber else surahMeta.startJuz,
                        hizbQuarter = if (c.hizbQuarter > 0) c.hizbQuarter else surahMeta.startHizb,
                        wordMeaningsAr = bundledVerse?.wordMeaningsAr ?: "",
                        asbabAlNuzulAr = asbab?.let { "${it.reasonAr} (${it.sourceAr})" } ?: "",
                        asbabAlNuzulEn = asbab?.let { "${it.reasonEn} (${it.sourceEn})" } ?: ""
                    )
                },
                isOfflineCached = true
            )
        }

        // 2. Check verified bundled Surahs (instant offline)
        val bundled = SurahCatalog.getBundledSurahVerses(surahNumber, reciter)
        if (bundled != null) {
            dao.insertQuranVerses(
                bundled.map { v ->
                    QuranVerseCacheEntity(
                        surahNumber = v.surahNumber,
                        numberInSurah = v.numberInSurah,
                        globalNumber = v.globalNumber,
                        textUthmani = v.textUthmani,
                        tafsirAr = v.tafsirAr,
                        translationEn = v.translationEn,
                        pageNumber = v.pageNumber,
                        juzNumber = v.juzNumber,
                        hizbQuarter = v.hizbQuarter
                    )
                }
            )
            return@withContext SurahLoadResult.Success(bundled, isOfflineCached = true)
        }

        // 3. Fetch authoritative Uthmani + Tafsir Al-Muyassar + Sahih International from AlQuran Cloud API and cache in Room
        try {
            val response = quranApi.fetchSurahWithTafsir(surahNumber)
            val editions = response.data
            if (editions != null && editions.size >= 2) {
                val uthmaniAyahs = editions[0].ayahs
                val tafsirMap = editions[1].ayahs.associateBy { it.numberInSurah }
                val englishMap = if (editions.size >= 3) {
                    editions[2].ayahs.associateBy { it.numberInSurah }
                } else {
                    emptyMap()
                }

                val verses = uthmaniAyahs.map { ayah ->
                    val asbab = SurahCatalog.getAsbab(surahNumber, ayah.numberInSurah)
                    QuranVerse(
                        surahNumber = surahNumber,
                        numberInSurah = ayah.numberInSurah,
                        globalNumber = ayah.number,
                        textUthmani = ayah.text,
                        tafsirAr = tafsirMap[ayah.numberInSurah]?.text ?: "",
                        audioUrl = SurahCatalog.audioUrlForAyah(surahNumber, ayah.numberInSurah, reciter),
                        translationEn = englishMap[ayah.numberInSurah]?.text ?: "",
                        pageNumber = ayah.page,
                        juzNumber = ayah.juz,
                        hizbQuarter = ayah.hizbQuarter,
                        asbabAlNuzulAr = asbab?.let { "${it.reasonAr} (${it.sourceAr})" } ?: "",
                        asbabAlNuzulEn = asbab?.let { "${it.reasonEn} (${it.sourceEn})" } ?: ""
                    )
                }
                dao.insertQuranVerses(
                    verses.map { v ->
                        QuranVerseCacheEntity(
                            surahNumber = v.surahNumber,
                            numberInSurah = v.numberInSurah,
                            globalNumber = v.globalNumber,
                            textUthmani = v.textUthmani,
                            tafsirAr = v.tafsirAr,
                            translationEn = v.translationEn,
                            pageNumber = v.pageNumber,
                            juzNumber = v.juzNumber,
                            hizbQuarter = v.hizbQuarter
                        )
                    }
                )
                SurahLoadResult.Success(verses, isOfflineCached = false)
            } else {
                SurahLoadResult.Error(
                    messageAr = "تعذّر تحميل نص السورة من المصدر الموثوق. يرجى التحقق من الاتصال بالإنترنت.",
                    messageEn = "Unable to load Surah text from the verified source. Please check your internet connection."
                )
            }
        } catch (_: Exception) {
            SurahLoadResult.Error(
                messageAr = "لا يتوفر اتصال بالإنترنت لتحميل هذه السورة لأول مرة. السور المحفوظة متاحة دائماً دون إنترنت.",
                messageEn = "No internet connection to download this Surah for the first time. Previously opened & bundled Surahs work fully offline."
            )
        }
    }

    suspend fun searchVersesOffline(query: String): List<QuranVerseCacheEntity> =
        withContext(Dispatchers.IO) {
            if (query.trim().length < 2) emptyList() else dao.searchCachedVerses(query.trim())
        }

    suspend fun toggleBookmark(
        surahNumber: Int,
        ayahNumber: Int,
        surahNameAr: String,
        surahNameEn: String,
        ayahText: String,
        currentlyBookmarked: Boolean
    ) {
        if (currentlyBookmarked) {
            dao.deleteQuranBookmark(surahNumber, ayahNumber)
        } else {
            dao.insertQuranBookmark(
                QuranBookmarkEntity(
                    surahNumber = surahNumber,
                    ayahNumber = ayahNumber,
                    surahNameAr = surahNameAr,
                    surahNameEn = surahNameEn,
                    ayahPreview = ayahText.take(90)
                )
            )
        }
    }

    suspend fun recordQuranReadingProgress(surahNumber: Int, ayahNumber: Int, versesDelta: Int = 1) {
        prefs.saveLastRead(surahNumber, ayahNumber)
        if (versesDelta > 0) {
            val dateKey = todayDateKey()
            val act = dao.getDailyActivityOnce(dateKey) ?: DailyActivityEntity(dateKey = dateKey)
            dao.upsertDailyActivity(act.copy(quranVersesRead = act.quranVersesRead + versesDelta))
        }
    }

    // --- NOOR 2.0 Audio Library (Favorites, Recents, Playlists & Authorized Offline App-Private Cache) ---
    suspend fun toggleFavoriteAudio(trackId: String, currentlyFavorite: Boolean) {
        if (currentlyFavorite) {
            dao.removeFavoriteAudio(trackId)
        } else {
            dao.addFavoriteAudio(AudioFavoriteEntity(trackId = trackId))
        }
    }

    suspend fun recordRecentAudio(track: AudioTrackItem) {
        dao.upsertRecentAudio(
            AudioRecentEntity(
                trackId = track.id,
                titleAr = track.titleAr,
                titleEn = track.titleEn,
                reciterNameAr = track.subtitleAr,
                reciterNameEn = track.subtitleEn,
                categoryKey = track.category.key,
                surahNumber = track.surahNumber,
                audioUrl = track.audioUrl,
                playedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun createAudioPlaylist(name: String): Long {
        val clean = name.trim().take(50)
        if (clean.isEmpty()) return -1L
        return dao.insertAudioPlaylist(AudioPlaylistEntity(nameAr = clean, nameEn = clean))
    }

    suspend fun deleteAudioPlaylist(playlistId: Long) {
        dao.deletePlaylistItemsForPlaylist(playlistId)
        dao.deleteAudioPlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: String) {
        dao.insertPlaylistItem(AudioPlaylistItemEntity(playlistId = playlistId, trackId = trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        dao.removePlaylistItem(playlistId, trackId)
    }

    private fun audioCacheDir(): File? {
        val ctx = appContext ?: return null
        val dir = File(ctx.filesDir, "audio_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun safeCacheFileName(trackId: String): String =
        trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".mp3"

    fun getCachedAudioPath(trackId: String): String? {
        val dir = audioCacheDir() ?: return null
        val file = File(dir, safeCacheFileName(trackId))
        return if (file.exists() && file.length() > 1024L) file.absolutePath else null
    }

    fun listDownloadedAudioIds(): Set<String> {
        val dir = audioCacheDir() ?: return emptySet()
        return dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".mp3") && it.length() > 1024L }
            ?.map { it.name.removeSuffix(".mp3") }
            ?.toSet()
            ?: emptySet()
    }

    suspend fun downloadAudioTrackForOffline(track: AudioTrackItem): Boolean = withContext(Dispatchers.IO) {
        if (!track.allowOfflineCache) return@withContext false
        val dir = audioCacheDir() ?: return@withContext false
        val target = File(dir, safeCacheFileName(track.id))
        val tmp = File(dir, safeCacheFileName(track.id) + ".part")
        try {
            val request = Request.Builder()
                .url(track.audioUrl)
                .header("User-Agent", "NOOR-Islamic-Companion/2.0")
                .build()
            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                body.byteStream().use { input ->
                    tmp.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            if (tmp.length() > 1024L) {
                if (target.exists()) target.delete()
                tmp.renameTo(target)
                true
            } else {
                tmp.delete()
                false
            }
        } catch (_: Exception) {
            tmp.delete()
            false
        }
    }

    suspend fun deleteCachedAudioTrack(trackId: String) = withContext(Dispatchers.IO) {
        val dir = audioCacheDir() ?: return@withContext
        File(dir, safeCacheFileName(trackId)).delete()
    }

    // --- Custom Daily Worship Routines ---
    suspend fun addCustomRoutine(titleAr: String, titleEn: String, category: String = "general") {
        val cleanAr = titleAr.trim()
        if (cleanAr.isEmpty()) return
        val cleanEn = titleEn.trim().ifEmpty { cleanAr }
        dao.upsertCustomRoutine(
            CustomRoutineEntity(
                titleAr = cleanAr,
                titleEn = cleanEn,
                category = category
            )
        )
    }

    suspend fun toggleCustomRoutineToday(routine: CustomRoutineEntity, dateKey: String = todayDateKey()) {
        val nextKey = if (routine.completedDateKey == dateKey) "" else dateKey
        dao.upsertCustomRoutine(routine.copy(completedDateKey = nextKey))
    }

    suspend fun deleteCustomRoutine(id: Long) {
        dao.deleteCustomRoutine(id)
    }

    // --- Nearby Mosques Finder (OpenStreetMap Overpass API) ---
    suspend fun findNearbyMosques(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 5000
    ): MosqueSearchResult = overpassApi.findNearbyMosques(latitude, longitude, radiusMeters)

    // --- Local Backup & Restore (JSON via SAF - zero cloud leak, zero storage permission) ---
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val p = prefs.getPreferencesOnce()
        val root = JSONObject()
        root.put("app", "NOOR")
        root.put("schemaVersion", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val prefsJson = JSONObject().apply {
            put("userName", p.userName)
            put("language", p.language.code)
            put("themeMode", p.themeMode.key)
            put("cityId", p.cityId)
            put("cityNameAr", p.cityNameAr)
            put("cityNameEn", p.cityNameEn)
            put("countryAr", p.countryAr)
            put("countryEn", p.countryEn)
            put("latitude", p.latitude)
            put("longitude", p.longitude)
            put("timezoneId", p.timezoneId)
            put("calculationMethod", p.calculationMethod.key)
            put("asrMadhhab", p.asrMadhhab.key)
            put("hijriAdjustmentDays", p.hijriAdjustmentDays)
            put("offsetFajr", p.offsets.fajr)
            put("offsetSunrise", p.offsets.sunrise)
            put("offsetDhuhr", p.offsets.dhuhr)
            put("offsetAsr", p.offsets.asr)
            put("offsetMaghrib", p.offsets.maghrib)
            put("offsetIsha", p.offsets.isha)
            put("iqamahEnabled", p.iqamahDelays.enabled)
            put("iqamahMosqueName", p.iqamahDelays.mosqueName)
            put("iqamahFajr", p.iqamahDelays.fajr)
            put("iqamahDhuhr", p.iqamahDelays.dhuhr)
            put("iqamahAsr", p.iqamahDelays.asr)
            put("iqamahMaghrib", p.iqamahDelays.maghrib)
            put("iqamahIsha", p.iqamahDelays.isha)
            put("lastReadSurah", p.lastReadSurah)
            put("lastReadAyah", p.lastReadAyah)
            put("quranFontSizeSp", p.quranFontSizeSp)
            put("quranReadingTheme", p.quranReadingTheme.key)
            put("quranDisplayMode", p.quranDisplayMode.key)
            put("quranReciter", p.quranReciter.id)
            put("dailyQuranGoalVerses", p.dailyQuranGoalVerses)
            put("dailyTasbihGoal", p.dailyTasbihGoal)
        }
        root.put("preferences", prefsJson)

        val bookmarksArr = JSONArray()
        dao.getAllBookmarksOnce().forEach { b ->
            bookmarksArr.put(JSONObject().apply {
                put("surahNumber", b.surahNumber)
                put("ayahNumber", b.ayahNumber)
                put("surahNameAr", b.surahNameAr)
                put("surahNameEn", b.surahNameEn)
                put("ayahPreview", b.ayahPreview)
                put("createdAtMillis", b.createdAtMillis)
            })
        }
        root.put("bookmarks", bookmarksArr)

        val favsArr = JSONArray()
        dao.getAllFavoritesOnce().forEach { favsArr.put(it.dhikrId) }
        root.put("favoriteAdhkar", favsArr)

        val sessionsArr = JSONArray()
        dao.getAllTasbihSessionsOnce().forEach { s ->
            sessionsArr.put(JSONObject().apply {
                put("presetId", s.presetId)
                put("labelAr", s.labelAr)
                put("labelEn", s.labelEn)
                put("count", s.count)
                put("target", s.target)
                put("dateKey", s.dateKey)
                put("timestampMillis", s.timestampMillis)
            })
        }
        root.put("tasbihSessions", sessionsArr)

        val customDhikrArr = JSONArray()
        dao.getAllCustomDhikrOnce().forEach { c ->
            customDhikrArr.put(JSONObject().apply {
                put("id", c.id)
                put("textAr", c.textAr)
                put("textEn", c.textEn)
                put("defaultTarget", c.defaultTarget)
            })
        }
        root.put("customDhikr", customDhikrArr)

        val routinesArr = JSONArray()
        dao.getAllCustomRoutinesOnce().forEach { r ->
            routinesArr.put(JSONObject().apply {
                put("titleAr", r.titleAr)
                put("titleEn", r.titleEn)
                put("category", r.category)
                put("completedDateKey", r.completedDateKey)
            })
        }
        root.put("customRoutines", routinesArr)

        val logsArr = JSONArray()
        dao.getAllDailyActivitiesOnce().forEach { d ->
            logsArr.put(JSONObject().apply {
                put("dateKey", d.dateKey)
                put("fajrPrayed", d.fajrPrayed)
                put("dhuhrPrayed", d.dhuhrPrayed)
                put("asrPrayed", d.asrPrayed)
                put("maghribPrayed", d.maghribPrayed)
                put("ishaPrayed", d.ishaPrayed)
                put("tasbihCount", d.tasbihCount)
                put("adhkarCompleted", d.adhkarCompleted)
                put("quranVersesRead", d.quranVersesRead)
            })
        }
        root.put("dailyActivities", logsArr)
        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.optString("app") != "NOOR") return@withContext false

            root.optJSONObject("preferences")?.let { pj ->
                val current = prefs.getPreferencesOnce()
                val restored = current.copy(
                    userName = pj.optString("userName", current.userName),
                    language = AppLanguage.fromCode(pj.optString("language", current.language.code)),
                    themeMode = ThemeMode.fromKey(pj.optString("themeMode", current.themeMode.key)),
                    cityId = pj.optString("cityId", current.cityId),
                    cityNameAr = pj.optString("cityNameAr", current.cityNameAr),
                    cityNameEn = pj.optString("cityNameEn", current.cityNameEn),
                    countryAr = pj.optString("countryAr", current.countryAr),
                    countryEn = pj.optString("countryEn", current.countryEn),
                    latitude = pj.optDouble("latitude", current.latitude),
                    longitude = pj.optDouble("longitude", current.longitude),
                    timezoneId = pj.optString("timezoneId", current.timezoneId),
                    calculationMethod = CalculationMethod.fromKey(pj.optString("calculationMethod", current.calculationMethod.key)),
                    asrMadhhab = AsrMadhhab.fromKey(pj.optString("asrMadhhab", current.asrMadhhab.key)),
                    hijriAdjustmentDays = pj.optInt("hijriAdjustmentDays", current.hijriAdjustmentDays),
                    offsets = PrayerMinuteOffsets(
                        fajr = pj.optInt("offsetFajr", 0),
                        sunrise = pj.optInt("offsetSunrise", 0),
                        dhuhr = pj.optInt("offsetDhuhr", 0),
                        asr = pj.optInt("offsetAsr", 0),
                        maghrib = pj.optInt("offsetMaghrib", 0),
                        isha = pj.optInt("offsetIsha", 0)
                    ),
                    iqamahDelays = IqamahDelays(
                        enabled = pj.optBoolean("iqamahEnabled", current.iqamahDelays.enabled),
                        mosqueName = pj.optString("iqamahMosqueName", current.iqamahDelays.mosqueName),
                        fajr = pj.optInt("iqamahFajr", current.iqamahDelays.fajr),
                        dhuhr = pj.optInt("iqamahDhuhr", current.iqamahDelays.dhuhr),
                        asr = pj.optInt("iqamahAsr", current.iqamahDelays.asr),
                        maghrib = pj.optInt("iqamahMaghrib", current.iqamahDelays.maghrib),
                        isha = pj.optInt("iqamahIsha", current.iqamahDelays.isha)
                    ),
                    lastReadSurah = pj.optInt("lastReadSurah", 1),
                    lastReadAyah = pj.optInt("lastReadAyah", 1),
                    quranFontSizeSp = pj.optInt("quranFontSizeSp", 24),
                    quranReadingTheme = QuranReadingTheme.fromKey(pj.optString("quranReadingTheme", "auto")),
                    quranDisplayMode = QuranDisplayMode.fromKey(pj.optString("quranDisplayMode", "study")),
                    quranReciter = QuranReciter.fromId(pj.optString("quranReciter", "yasser_dosari")),
                    dailyQuranGoalVerses = pj.optInt("dailyQuranGoalVerses", current.dailyQuranGoalVerses),
                    dailyTasbihGoal = pj.optInt("dailyTasbihGoal", current.dailyTasbihGoal),
                    homeConfig = HomeDashboardConfig()
                )
                prefs.restoreFromPreferences(restored)
            }

            root.optJSONArray("bookmarks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.insertQuranBookmark(
                        QuranBookmarkEntity(
                            surahNumber = o.getInt("surahNumber"),
                            ayahNumber = o.getInt("ayahNumber"),
                            surahNameAr = o.optString("surahNameAr"),
                            surahNameEn = o.optString("surahNameEn"),
                            ayahPreview = o.optString("ayahPreview"),
                            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
                        )
                    )
                }
            }

            root.optJSONArray("favoriteAdhkar")?.let { arr ->
                for (i in 0 until arr.length()) {
                    dao.addFavoriteDhikr(AdhkarFavoriteEntity(arr.getString(i)))
                }
            }

            root.optJSONArray("tasbihSessions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.insertTasbihSession(
                        TasbihSessionEntity(
                            presetId = o.optString("presetId"),
                            labelAr = o.optString("labelAr"),
                            labelEn = o.optString("labelEn"),
                            count = o.optInt("count"),
                            target = o.optInt("target"),
                            dateKey = o.optString("dateKey"),
                            timestampMillis = o.optLong("timestampMillis", System.currentTimeMillis())
                        )
                    )
                }
            }

            root.optJSONArray("customDhikr")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.upsertCustomDhikr(
                        CustomDhikrEntity(
                            id = o.optString("id", "custom_$i"),
                            textAr = o.optString("textAr"),
                            textEn = o.optString("textEn"),
                            defaultTarget = o.optInt("defaultTarget", 33)
                        )
                    )
                }
            }

            root.optJSONArray("customRoutines")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.upsertCustomRoutine(
                        CustomRoutineEntity(
                            titleAr = o.optString("titleAr"),
                            titleEn = o.optString("titleEn"),
                            category = o.optString("category", "general"),
                            completedDateKey = o.optString("completedDateKey", "")
                        )
                    )
                }
            }

            root.optJSONArray("dailyActivities")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.upsertDailyActivity(
                        DailyActivityEntity(
                            dateKey = o.getString("dateKey"),
                            fajrPrayed = o.optBoolean("fajrPrayed"),
                            dhuhrPrayed = o.optBoolean("dhuhrPrayed"),
                            asrPrayed = o.optBoolean("asrPrayed"),
                            maghribPrayed = o.optBoolean("maghribPrayed"),
                            ishaPrayed = o.optBoolean("ishaPrayed"),
                            tasbihCount = o.optInt("tasbihCount"),
                            adhkarCompleted = o.optInt("adhkarCompleted"),
                            quranVersesRead = o.optInt("quranVersesRead")
                        )
                    )
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun resetAllAppData() = withContext(Dispatchers.IO) {
        dao.clearTasbihSessions()
        dao.clearCustomDhikrPresets()
        dao.clearAdhkarFavorites()
        dao.clearAdhkarProgress()
        dao.clearAdhkarHistory()
        dao.clearQuranBookmarks()
        dao.clearDailyActivities()
        dao.clearAudioFavorites()
        dao.clearRecentAudio()
        dao.clearAudioPlaylists()
        dao.clearAudioPlaylistItems()
        dao.clearCustomRoutines()
        audioCacheDir()?.listFiles()?.forEach { it.delete() }
        prefs.clearAll()
    }

    // --- NOOR 3.0: Morning/Evening Adhkar Session History & Direct Item Completion ---

    fun observeAdhkarHistory(categoryId: String): Flow<List<com.example.data.local.db.AdhkarCompletionHistoryEntity>> =
        dao.observeAdhkarHistory(categoryId)

    suspend fun markDhikrCompletedDirectly(
        dhikrId: String,
        targetCount: Int,
        dateKey: String = todayDateKey()
    ) {
        dao.upsertAdhkarProgress(
            AdhkarProgressEntity(
                dhikrId = dhikrId,
                dateKey = dateKey,
                currentCount = targetCount,
                targetCount = targetCount,
                isCompleted = true
            )
        )
        val act = dao.getDailyActivityOnce(dateKey) ?: DailyActivityEntity(dateKey = dateKey)
        dao.upsertDailyActivity(act.copy(adhkarCompleted = act.adhkarCompleted + 1))
    }

    suspend fun recordAdhkarSessionState(
        categoryId: String,
        completedItems: Int,
        totalItems: Int,
        lastDhikrIndex: Int,
        dateKey: String = todayDateKey()
    ) {
        if (totalItems <= 0) return
        val safeCompleted = completedItems.coerceIn(0, totalItems)
        dao.upsertAdhkarHistory(
            com.example.data.local.db.AdhkarCompletionHistoryEntity(
                categoryId = categoryId,
                dateKey = dateKey,
                completedItems = safeCompleted,
                totalItems = totalItems,
                isFullyCompleted = safeCompleted >= totalItems,
                lastDhikrIndex = lastDhikrIndex.coerceIn(0, (totalItems - 1).coerceAtLeast(0)),
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    // --- NOOR 3.0: System Management (Snapshots, Storage, Error Logs, Cache) ---

    val backupSnapshots: Flow<List<com.example.data.local.db.BackupSnapshotEntity>> = dao.observeBackupSnapshots()
    val systemErrorLogs: Flow<List<com.example.data.local.db.SystemErrorLogEntity>> = dao.observeSystemErrorLogs()

    suspend fun createRollbackSnapshot(label: String): Long = withContext(Dispatchers.IO) {
        val json = exportBackupJson()
        val bytes = json.toByteArray(Charsets.UTF_8).size.toLong()
        dao.insertBackupSnapshot(
            com.example.data.local.db.BackupSnapshotEntity(
                label = label.trim().ifBlank { "Snapshot ${todayDateKey()}" },
                payloadJson = json,
                sizeBytes = bytes,
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteRollbackSnapshot(id: Long) = dao.deleteBackupSnapshot(id)

    suspend fun logSystemError(tag: String, message: String, severity: String = "WARNING") {
        dao.insertSystemErrorLog(
            com.example.data.local.db.SystemErrorLogEntity(
                tag = tag.take(40),
                message = message.take(500),
                severity = severity,
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearSystemErrorLogs() = dao.clearSystemErrorLogs()

    data class StorageMetrics(
        val databaseBytes: Long = 0L,
        val audioCacheBytes: Long = 0L,
        val otherFilesBytes: Long = 0L,
        val downloadedAudioCount: Int = 0
    ) {
        val totalBytes: Long get() = databaseBytes + audioCacheBytes + otherFilesBytes
    }

    suspend fun computeStorageMetrics(): StorageMetrics = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext StorageMetrics()
        val dbFile = ctx.getDatabasePath("noor_islamic_db")
        val dbBytes = listOf(
            dbFile,
            File(dbFile.path + "-wal"),
            File(dbFile.path + "-shm")
        ).sumOf { if (it.exists()) it.length() else 0L }

        val audioFiles = audioCacheDir()?.listFiles().orEmpty()
        val audioBytes = audioFiles.sumOf { if (it.isFile) it.length() else 0L }
        val cacheBytes = ctx.cacheDir?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L

        StorageMetrics(
            databaseBytes = dbBytes,
            audioCacheBytes = audioBytes,
            otherFilesBytes = cacheBytes,
            downloadedAudioCount = audioFiles.count { it.isFile && it.name.endsWith(".mp3") }
        )
    }

    suspend fun clearTemporaryCaches() = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext
        ctx.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        audioCacheDir()?.listFiles()?.forEach { it.delete() }
    }

    fun exportCmsItemsToJson(items: List<com.example.data.cloud.CmsContentDoc>): String {
        val root = JSONObject()
        root.put("app", "NOOR_CMS_EXPORT")
        root.put("version", 3)
        root.put("exportedAt", System.currentTimeMillis())
        val arr = JSONArray()
        items.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("category", c.category)
                put("subCategory", c.subCategory)
                put("titleAr", c.titleAr)
                put("titleEn", c.titleEn)
                put("bodyAr", c.bodyAr)
                put("bodyEn", c.bodyEn)
                put("transliteration", c.transliteration)
                put("sourceAr", c.sourceAr)
                put("sourceEn", c.sourceEn)
                put("gradingAr", c.gradingAr)
                put("gradingEn", c.gradingEn)
                put("repetitionCount", c.repetitionCount)
                put("audioUrl", c.audioUrl)
                put("status", c.status)
                put("version", c.version)
            })
        }
        root.put("items", arr)
        return root.toString(2)
    }

    fun parseCmsItemsFromJson(jsonString: String): List<com.example.data.cloud.CmsContentDoc> {
        return try {
            val root = JSONObject(jsonString)
            if (root.optString("app") != "NOOR_CMS_EXPORT") return emptyList()
            val arr = root.optJSONArray("items") ?: return emptyList()
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        com.example.data.cloud.CmsContentDoc(
                            id = o.optString("id", ""),
                            category = o.optString("category", "morning_adhkar"),
                            subCategory = o.optString("subCategory", ""),
                            titleAr = o.optString("titleAr", ""),
                            titleEn = o.optString("titleEn", ""),
                            bodyAr = o.optString("bodyAr", ""),
                            bodyEn = o.optString("bodyEn", ""),
                            transliteration = o.optString("transliteration", ""),
                            sourceAr = o.optString("sourceAr", ""),
                            sourceEn = o.optString("sourceEn", ""),
                            gradingAr = o.optString("gradingAr", "صحيح"),
                            gradingEn = o.optString("gradingEn", "Sahih"),
                            repetitionCount = o.optInt("repetitionCount", 1).coerceIn(1, 1000),
                            audioUrl = o.optString("audioUrl", ""),
                            status = o.optString("status", "DRAFT"),
                            version = o.optInt("version", 1).coerceAtLeast(1)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

