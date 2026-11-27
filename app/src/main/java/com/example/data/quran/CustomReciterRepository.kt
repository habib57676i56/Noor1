package com.example.data.quran

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.domain.model.AudioCategory
import com.example.domain.model.AudioTrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Custom Reciter & Uploaded Audio entry added from the Super Admin Control Center.
 * Supports both locally uploaded audio files copied safely from phone storage via SAF
 * (`ActivityResultContracts.OpenDocument`) and optional HTTPS streaming URLs.
 */
data class CustomReciterItem(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val styleAr: String = "مرتّل خاص",
    val styleEn: String = "Custom Recitation",
    val surahTitleAr: String = "تلاوة مختارة",
    val surahTitleEn: String = "Featured Recitation",
    val surahNumber: Int = 1,
    val localAudioPath: String = "",
    val audioUrl: String = "",
    val fileName: String = "",
    val addedByEmail: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    /**
     * Resolves the playable source: prefers an existing local uploaded file, falling back to HTTPS URL.
     */
    fun resolvePlayableSource(): String {
        if (localAudioPath.isNotBlank() && File(localAudioPath).exists()) {
            return localAudioPath
        }
        return audioUrl.trim()
    }

    fun hasPlayableAudio(): Boolean = resolvePlayableSource().isNotBlank()

    fun isYasserIraqiMaqamTrack(): Boolean =
        styleEn.contains("Iraqi", ignoreCase = true) ||
            styleAr.contains("العراق") ||
            nameAr.contains("مقام العراق") ||
            nameEn.contains("Iraqi Maqam", ignoreCase = true)

    fun toAudioTrackItem(): AudioTrackItem {
        val playable = resolvePlayableSource()
        return AudioTrackItem(
            id = "custom_reciter_$id",
            titleAr = surahTitleAr.ifBlank { nameAr },
            titleEn = surahTitleEn.ifBlank { nameEn.ifBlank { nameAr } },
            subtitleAr = "$nameAr • $styleAr",
            subtitleEn = "${nameEn.ifBlank { nameAr }} • $styleEn",
            audioUrl = playable,
            category = AudioCategory.QURAN_RECITERS,
            surahNumber = surahNumber.takeIf { it in 1..114 },
            reciterId = "custom_$id",
            sourceAttribution = "NOOR Super Admin Custom Reciter"
        )
    }
}

/**
 * Local persistent store + audio file importer for Super Admin custom reciters.
 * Copies user-picked audio files from Android SAF into app-private storage so
 * playback works reliably offline and across restarts without storage permissions.
 */
class CustomReciterRepository(private val appContext: Context) {

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val audioDir = File(appContext.filesDir, "custom_reciters").apply { mkdirs() }

    private val _reciters = MutableStateFlow<List<CustomReciterItem>>(loadStoredReciters())
    val reciters: StateFlow<List<CustomReciterItem>> = _reciters.asStateFlow()

    suspend fun addCustomReciterFromPhone(
        nameAr: String,
        nameEn: String,
        styleAr: String,
        styleEn: String,
        surahTitleAr: String,
        surahTitleEn: String,
        surahNumber: Int,
        pickedAudioUri: Uri?,
        remoteAudioUrl: String,
        addedByEmail: String
    ): Result<CustomReciterItem> = withContext(Dispatchers.IO) {
        val cleanNameAr = nameAr.trim()
        if (cleanNameAr.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("اسم القارئ مطلوب / Reciter name is required"))
        }
        val cleanRemoteUrl = remoteAudioUrl.trim()
        if (pickedAudioUri == null && cleanRemoteUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("يرجى اختيار ملف صوتي من الهاتف أو إدخال رابط / Pick an audio file or enter URL"))
        }

        val id = "crec_${UUID.randomUUID().toString().take(10)}"
        var savedLocalPath = ""
        var resolvedFileName = ""

        if (pickedAudioUri != null) {
            try {
                resolvedFileName = queryDisplayName(pickedAudioUri) ?: "recitation_$id.mp3"
                val ext = resolvedFileName.substringAfterLast('.', "mp3")
                    .lowercase()
                    .filter { it.isLetterOrDigit() }
                    .ifBlank { "mp3" }
                    .take(6)
                val destFile = File(audioDir, "$id.$ext")
                appContext.contentResolver.openInputStream(pickedAudioUri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output, bufferSize = 32 * 1024)
                    }
                } ?: return@withContext Result.failure(IllegalStateException("تعذّر قراءة الملف الصوتي المختار"))
                if (destFile.length() <= 0L) {
                    destFile.delete()
                    return@withContext Result.failure(IllegalStateException("الملف الصوتي فارغ"))
                }
                savedLocalPath = destFile.absolutePath
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

        val item = CustomReciterItem(
            id = id,
            nameAr = cleanNameAr.take(100),
            nameEn = nameEn.trim().ifBlank { cleanNameAr }.take(100),
            styleAr = styleAr.trim().ifBlank { "مرتّل خاص" }.take(60),
            styleEn = styleEn.trim().ifBlank { "Custom Recitation" }.take(60),
            surahTitleAr = surahTitleAr.trim().ifBlank { "تلاوة مختارة" }.take(120),
            surahTitleEn = surahTitleEn.trim().ifBlank { surahTitleAr.trim().ifBlank { "Featured Recitation" } }.take(120),
            surahNumber = surahNumber.coerceIn(1, 114),
            localAudioPath = savedLocalPath,
            audioUrl = cleanRemoteUrl.take(500),
            fileName = resolvedFileName.take(140),
            addedByEmail = addedByEmail.take(200),
            createdAtMillis = System.currentTimeMillis()
        )

        val updated = listOf(item) + _reciters.value
        saveStoredReciters(updated)
        _reciters.value = updated
        Result.success(item)
    }

    suspend fun deleteCustomReciter(reciterId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = _reciters.value.firstOrNull { it.id == reciterId }
        if (existing != null && existing.localAudioPath.isNotBlank()) {
            runCatching { File(existing.localAudioPath).delete() }
        }
        val updated = _reciters.value.filterNot { it.id == reciterId }
        saveStoredReciters(updated)
        _reciters.value = updated
        Result.success(Unit)
    }

    /**
     * Merges cloud-published reciters (from Firestore) with locally uploaded phone audio reciters
     * without overwriting local audio paths.
     */
    fun mergeWithCloudReciters(cloudReciters: List<CustomReciterItem>): List<CustomReciterItem> {
        val localById = _reciters.value.associateBy { it.id }
        val merged = LinkedHashMap<String, CustomReciterItem>()
        _reciters.value.forEach { merged[it.id] = it }
        cloudReciters.forEach { cloud ->
            val local = localById[cloud.id]
            if (local != null) {
                merged[cloud.id] = cloud.copy(
                    localAudioPath = local.localAudioPath.ifBlank { cloud.localAudioPath },
                    fileName = local.fileName.ifBlank { cloud.fileName }
                )
            } else if (cloud.hasPlayableAudio()) {
                merged[cloud.id] = cloud
            }
        }
        return merged.values.sortedByDescending { it.createdAtMillis }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadStoredReciters(): List<CustomReciterItem> {
        val raw = prefs.getString(KEY_RECITERS_JSON, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        CustomReciterItem(
                            id = o.optString("id"),
                            nameAr = o.optString("nameAr"),
                            nameEn = o.optString("nameEn"),
                            styleAr = o.optString("styleAr", "مرتّل خاص"),
                            styleEn = o.optString("styleEn", "Custom Recitation"),
                            surahTitleAr = o.optString("surahTitleAr", "تلاوة مختارة"),
                            surahTitleEn = o.optString("surahTitleEn", "Featured Recitation"),
                            surahNumber = o.optInt("surahNumber", 1),
                            localAudioPath = o.optString("localAudioPath", ""),
                            audioUrl = o.optString("audioUrl", ""),
                            fileName = o.optString("fileName", ""),
                            addedByEmail = o.optString("addedByEmail", ""),
                            createdAtMillis = o.optLong("createdAtMillis", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveStoredReciters(list: List<CustomReciterItem>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("nameAr", r.nameAr)
                    put("nameEn", r.nameEn)
                    put("styleAr", r.styleAr)
                    put("styleEn", r.styleEn)
                    put("surahTitleAr", r.surahTitleAr)
                    put("surahTitleEn", r.surahTitleEn)
                    put("surahNumber", r.surahNumber)
                    put("localAudioPath", r.localAudioPath)
                    put("audioUrl", r.audioUrl)
                    put("fileName", r.fileName)
                    put("addedByEmail", r.addedByEmail)
                    put("createdAtMillis", r.createdAtMillis)
                }
            )
        }
        prefs.edit().putString(KEY_RECITERS_JSON, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "noor_custom_reciters_store"
        private const val KEY_RECITERS_JSON = "custom_reciters_v1"
    }
}
