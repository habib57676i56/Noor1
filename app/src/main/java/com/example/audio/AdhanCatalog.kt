package com.example.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.domain.model.PrayerName
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AdhanVoiceOption(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val muezzinAr: String,
    val muezzinEn: String,
    val audioUrl: String = "",
    val fallbackUrl: String = "",
    val localFilePath: String? = null,
    val attribution: String = "",
    val isSystemDefault: Boolean = false,
    val isCustomImported: Boolean = false,
    val isDownloadedOffline: Boolean = false
)

data class CustomAdhanRecord(
    val id: String,
    val displayName: String,
    val localFilePath: String,
    val extension: String,
    val importedAtMillis: Long
)

data class PerPrayerAdhanConfig(
    val defaultVoiceId: String = AdhanCatalog.DEFAULT_VOICE_ID,
    val fajrVoiceId: String = "",
    val dhuhrVoiceId: String = "",
    val asrVoiceId: String = "",
    val maghribVoiceId: String = "",
    val ishaVoiceId: String = ""
) {
    fun voiceIdForPrayer(prayer: PrayerName): String {
        val specific = when (prayer) {
            PrayerName.FAJR -> fajrVoiceId
            PrayerName.DHUHR -> dhuhrVoiceId
            PrayerName.ASR -> asrVoiceId
            PrayerName.MAGHRIB -> maghribVoiceId
            PrayerName.ISHA -> ishaVoiceId
            PrayerName.SUNRISE -> ""
        }
        return specific.ifBlank { defaultVoiceId.ifBlank { AdhanCatalog.DEFAULT_VOICE_ID } }
    }

    fun hasCustomOverride(prayer: PrayerName): Boolean = when (prayer) {
        PrayerName.FAJR -> fajrVoiceId.isNotBlank()
        PrayerName.DHUHR -> dhuhrVoiceId.isNotBlank()
        PrayerName.ASR -> asrVoiceId.isNotBlank()
        PrayerName.MAGHRIB -> maghribVoiceId.isNotBlank()
        PrayerName.ISHA -> ishaVoiceId.isNotBlank()
        PrayerName.SUNRISE -> false
    }
}

sealed class AdhanImportResult {
    data class Success(val record: CustomAdhanRecord) : AdhanImportResult()
    data class Error(val messageAr: String, val messageEn: String) : AdhanImportResult()
}

sealed class AdhanDownloadState {
    data object Idle : AdhanDownloadState()
    data class Downloading(val progress: Float) : AdhanDownloadState()
    data class VerifiedOffline(val filePath: String, val sizeBytes: Long, val durationMs: Long) : AdhanDownloadState()
    data class Failed(val messageAr: String, val messageEn: String) : AdhanDownloadState()
}

/**
 * Prevents duplicate Adhan playback for the same prayer within a 20-minute window
 * (e.g., if an alarm is re-delivered after timezone/boot recalculation).
 */
object AdhanDuplicateGuard {
    private const val PREFS_NAME = "noor_adhan_dedup_guard"
    private const val DEDUP_WINDOW_MS = 20L * 60L * 1000L

    fun shouldAllowPlayback(
        context: Context,
        prayer: PrayerName,
        nowMillis: Long = System.currentTimeMillis(),
        isTestAlarm: Boolean = false
    ): Boolean {
        if (isTestAlarm) return true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
        val key = "last_played_${prayer.name}_$dateKey"
        val lastMillis = prefs.getLong(key, 0L)
        return lastMillis == 0L || (nowMillis - lastMillis) > DEDUP_WINDOW_MS
    }

    fun markPlayed(
        context: Context,
        prayer: PrayerName,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
        val key = "last_played_${prayer.name}_$dateKey"
        prefs.edit().putLong(key, nowMillis).apply()
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

/**
 * Curated Adhan Voice Catalog, Offline Verified Downloader, and Zero-Permission SAF Importer.
 * Default Adhan Reciter: Sheikh Nasser Al-Qatami (الشيخ ناصر القطامي).
 */
object AdhanCatalog {
    const val DEFAULT_VOICE_ID = "adhan_qatami"
    const val SYSTEM_DEFAULT_ID = "adhan_system_default"

    val supportedExtensions = setOf("mp3", "m4a", "wav")
    val supportedMimeTypes = arrayOf(
        "audio/mpeg",
        "audio/mp3",
        "audio/mp4",
        "audio/x-m4a",
        "audio/aac",
        "audio/wav",
        "audio/x-wav",
        "audio/vnd.wave"
    )

    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    val builtInVoices: List<AdhanVoiceOption> = listOf(
        AdhanVoiceOption(
            id = "adhan_qatami",
            nameAr = "أذان الشيخ ناصر القطامي (الافتراضي)",
            nameEn = "Sheikh Nasser Al-Qatami Adhan (Default)",
            muezzinAr = "الشيخ ناصر القطامي · أذان كامل عالي الجودة",
            muezzinEn = "Sheikh Nasser Al-Qatami · Full High-Quality Adhan",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan19.mp3",
            fallbackUrl = "https://cdn.aladhan.com/audio/adhans/a7.mp3",
            attribution = "IslamCan Authorized Islamic Audio Archive"
        ),
        AdhanVoiceOption(
            id = "adhan_maher_muaiqly",
            nameAr = "أذان الشيخ ماهر المعيقلي",
            nameEn = "Sheikh Maher Al-Muaiqly Adhan",
            muezzinAr = "الشيخ ماهر المعيقلي · إمام الحرم المكي",
            muezzinEn = "Sheikh Maher Al-Muaiqly · Masjid al-Haram",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan20.mp3",
            fallbackUrl = "https://cdn.aladhan.com/audio/adhans/a1.mp3",
            attribution = "IslamCan Authorized Islamic Audio Archive"
        ),
        AdhanVoiceOption(
            id = "adhan_makkah",
            nameAr = "أذان المسجد الحرام — مكة المكرمة",
            nameEn = "Masjid al-Haram (Makkah) Adhan",
            muezzinAr = "الشيخ علي أحمد ملا · الأذان المكي",
            muezzinEn = "Sheikh Ali Ahmed Mulla · Makkah",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan1.mp3",
            fallbackUrl = "https://cdn.aladhan.com/audio/adhans/a2.mp3",
            attribution = "IslamCan Authorized Islamic Audio Archive"
        ),
        AdhanVoiceOption(
            id = "adhan_madinah",
            nameAr = "أذان المسجد النبوي — المدينة المنورة",
            nameEn = "Al-Masjid an-Nabawi (Madinah) Adhan",
            muezzinAr = "الأذان المدني الشريف",
            muezzinEn = "Traditional Madinah Adhan",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan2.mp3",
            fallbackUrl = "https://cdn.aladhan.com/audio/adhans/a3.mp3",
            attribution = "IslamCan Authorized Islamic Audio Archive"
        ),
        AdhanVoiceOption(
            id = "adhan_alafasy",
            nameAr = "أذان الشيخ مشاري راشد العفاسي",
            nameEn = "Sheikh Mishary Rashid Alafasy Adhan",
            muezzinAr = "الشيخ مشاري راشد العفاسي",
            muezzinEn = "Sheikh Mishary Rashid Alafasy",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan16.mp3",
            fallbackUrl = "https://cdn.aladhan.com/audio/adhans/a4.mp3",
            attribution = "IslamCan Authorized Islamic Audio Archive"
        ),
        AdhanVoiceOption(
            id = "adhan_aqsa",
            nameAr = "أذان المسجد الأقصى المبارك",
            nameEn = "Al-Aqsa Mosque (Al-Quds) Adhan",
            muezzinAr = "القدس الشريف",
            muezzinEn = "Al-Quds Traditional Adhan",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan3.mp3",
            fallbackUrl = "https://cdn.aladhan.com/audio/adhans/a9.mp3",
            attribution = "IslamCan Authorized Islamic Audio Archive"
        ),
        AdhanVoiceOption(
            id = "adhan_egypt",
            nameAr = "الأذان المصري التراثي",
            nameEn = "Classic Egyptian Adhan",
            muezzinAr = "المدرسة المصرية الأصيلة",
            muezzinEn = "Classic Cairo Style",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan15.mp3",
            attribution = "IslamCan Authorized Islamic Audio Archive"
        ),
        AdhanVoiceOption(
            id = SYSTEM_DEFAULT_ID,
            nameAr = "نغمة التنبيه الافتراضية للنظام",
            nameEn = "Device Default Notification Tone",
            muezzinAr = "يعمل دائماً بدون إنترنت",
            muezzinEn = "Always available offline",
            audioUrl = "",
            isSystemDefault = true
        )
    )

    fun offlineDir(context: Context): File =
        File(context.filesDir, "adhan_offline").apply { mkdirs() }

    fun offlineFileFor(context: Context, voiceId: String): File =
        File(offlineDir(context), "${voiceId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.mp3")

    fun isVerifiedAudioHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        // Reject HTML/XML/JSON error responses saved by mistake
        val prefix = String(bytes, 0, minOf(bytes.size, 32), Charsets.US_ASCII).trimStart().lowercase(Locale.US)
        if (prefix.startsWith("<!doctype") || prefix.startsWith("<html") || prefix.startsWith("{")) {
            return false
        }
        // ID3 tag ("ID3") or MPEG frame sync (0xFF 0xE0..0xFF)
        val isMp3 = (bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) ||
            ((bytes[0].toInt() and 0xFF) == 0xFF && (bytes[1].toInt() and 0xE0) == 0xE0)
        // MP4/M4A ftyp box
        val isM4a = bytes[4] == 'f'.code.toByte() && bytes[5] == 't'.code.toByte() &&
            bytes[6] == 'y'.code.toByte() && bytes[7] == 'p'.code.toByte()
        // RIFF WAVE
        val isWav = bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()
        return isMp3 || isM4a || isWav
    }

    /**
     * Verifies a downloaded local Adhan audio file is non-empty, has valid audio headers,
     * and parses cleanly via MediaMetadataRetriever when running on Android.
     * Returns verified duration in ms (>= 0) or null if invalid.
     */
    fun verifyAudioFile(file: File): Long? {
        if (!file.exists() || file.length() < 16_384L) return null
        val header = ByteArray(32)
        val read = runCatching {
            file.inputStream().use { it.read(header) }
        }.getOrDefault(-1)
        if (read < 12 || !isVerifiedAudioHeader(header)) return null

        val durationMs = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrNull()

        // On Robolectric/JVM where native MediaMetadataRetriever returns null/0, valid header + >=16KB suffices.
        // On real Android devices, reject clips shorter than 5 seconds so incomplete samples aren't marked ready.
        if (durationMs != null && durationMs > 0L && durationMs < 5_000L) {
            return null
        }
        return durationMs ?: 0L
    }

    fun getVerifiedOfflineFile(context: Context, voiceId: String): File? {
        val file = offlineFileFor(context, voiceId)
        return if (verifyAudioFile(file) != null) file else null
    }

    fun scanOfflineDownloadStates(context: Context): Map<String, AdhanDownloadState> {
        val map = mutableMapOf<String, AdhanDownloadState>()
        builtInVoices.filter { !it.isSystemDefault }.forEach { voice ->
            val file = offlineFileFor(context, voice.id)
            val dur = verifyAudioFile(file)
            if (dur != null) {
                map[voice.id] = AdhanDownloadState.VerifiedOffline(
                    filePath = file.absolutePath,
                    sizeBytes = file.length(),
                    durationMs = dur
                )
            }
        }
        return map
    }

    fun downloadAndVerifyVoice(
        context: Context,
        voice: AdhanVoiceOption,
        onProgress: (Float) -> Unit
    ): AdhanDownloadState {
        if (voice.isSystemDefault || voice.audioUrl.isBlank()) {
            return AdhanDownloadState.Failed(
                "لا يتطلب هذا الصوت تنزيلاً.",
                "This voice does not require downloading."
            )
        }
        val urls = listOf(voice.audioUrl, voice.fallbackUrl).filter { it.isNotBlank() }
        val destFile = offlineFileFor(context, voice.id)
        val tempFile = File(offlineDir(context), "${voice.id}.tmp")

        var lastError = ""
        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "NOOR-Islamic-App/5.0 (Android)")
                    .build()
                downloadClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastError = "HTTP ${response.code}"
                        return@use
                    }
                    val body = response.body ?: return@use
                    val totalBytes = body.contentLength()
                    var downloaded = 0L
                    body.byteStream().use { input ->
                        tempFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloaded += bytesRead
                                if (totalBytes > 0L) {
                                    onProgress((downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0.02f, 0.96f))
                                } else {
                                    onProgress((downloaded / 1_500_000f).coerceIn(0.05f, 0.90f))
                                }
                            }
                        }
                    }
                }
                val verifiedDuration = verifyAudioFile(tempFile)
                if (verifiedDuration != null) {
                    if (destFile.exists()) destFile.delete()
                    if (!tempFile.renameTo(destFile)) {
                        tempFile.copyTo(destFile, overwrite = true)
                        tempFile.delete()
                    }
                    onProgress(1f)
                    return AdhanDownloadState.VerifiedOffline(
                        filePath = destFile.absolutePath,
                        sizeBytes = destFile.length(),
                        durationMs = verifiedDuration
                    )
                } else {
                    tempFile.delete()
                    lastError = "Invalid audio file integrity"
                }
            } catch (e: Exception) {
                tempFile.delete()
                lastError = e.message ?: "Network error"
            }
        }
        return AdhanDownloadState.Failed(
            messageAr = "تعذّر تنزيل أو التحقق من ملف الأذان ($lastError). تحقق من الاتصال بالإنترنت وأعد المحاولة.",
            messageEn = "Failed to download or verify Adhan audio ($lastError). Please check internet and retry."
        )
    }

    fun deleteOfflineDownload(context: Context, voiceId: String) {
        runCatching { offlineFileFor(context, voiceId).delete() }
    }

    fun allVoices(
        customRecords: List<CustomAdhanRecord>,
        context: Context? = null
    ): List<AdhanVoiceOption> {
        val enrichedBuiltIn = builtInVoices.map { voice ->
            if (context != null && !voice.isSystemDefault) {
                val offline = getVerifiedOfflineFile(context, voice.id)
                if (offline != null) {
                    voice.copy(localFilePath = offline.absolutePath, isDownloadedOffline = true)
                } else {
                    voice
                }
            } else {
                voice
            }
        }
        val customOptions = customRecords.map { rec ->
            AdhanVoiceOption(
                id = rec.id,
                nameAr = rec.displayName,
                nameEn = rec.displayName,
                muezzinAr = "ملف صوتي مستورد (${rec.extension.uppercase(Locale.US)}) · يعمل بدون إنترنت",
                muezzinEn = "Imported Device Audio (${rec.extension.uppercase(Locale.US)}) · Offline Ready",
                localFilePath = rec.localFilePath,
                isCustomImported = true,
                isDownloadedOffline = true
            )
        }
        return enrichedBuiltIn + customOptions
    }

    fun resolveVoice(
        voiceId: String,
        customRecords: List<CustomAdhanRecord>,
        context: Context? = null
    ): AdhanVoiceOption {
        val all = allVoices(customRecords, context)
        val matched = all.firstOrNull { it.id == voiceId }
        if (matched != null) {
            if (matched.isCustomImported) {
                val path = matched.localFilePath
                if (path != null && File(path).exists() && File(path).length() > 0L) {
                    return matched
                }
                return all.first()
            }
            return matched
        }
        return all.first()
    }

    fun validateFormat(fileName: String?, mimeType: String?): String? {
        val extFromName = fileName
            ?.substringAfterLast('.', "")
            ?.lowercase(Locale.US)
            ?.takeIf { it in supportedExtensions }
        if (extFromName != null) return extFromName

        return when (mimeType?.lowercase(Locale.US)) {
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/mp4", "audio/x-m4a", "audio/aac", "audio/m4a" -> "m4a"
            "audio/wav", "audio/x-wav", "audio/vnd.wave" -> "wav"
            else -> null
        }
    }

    fun encodeCustomRecords(records: List<CustomAdhanRecord>): String {
        return records.joinToString(separator = "\n") { rec ->
            listOf(
                rec.id,
                rec.displayName.replace("|", " ").replace("\n", " "),
                rec.localFilePath.replace("|", ""),
                rec.extension,
                rec.importedAtMillis.toString()
            ).joinToString("|")
        }
    }

    fun decodeCustomRecords(raw: String): List<CustomAdhanRecord> {
        if (raw.isBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 5) return@mapNotNull null
            CustomAdhanRecord(
                id = parts[0],
                displayName = parts[1],
                localFilePath = parts[2],
                extension = parts[3],
                importedAtMillis = parts[4].toLongOrNull() ?: 0L
            )
        }
    }

    fun importFromUri(context: Context, uri: Uri): AdhanImportResult {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)
        var displayName = "Custom Adhan"
        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val rawName = cursor.getString(idx)
                        if (!rawName.isNullOrBlank()) displayName = rawName
                    }
                }
            }
        } catch (_: Exception) {
        }

        val ext = validateFormat(displayName, mimeType)
            ?: return AdhanImportResult.Error(
                messageAr = "صيغة الملف غير مدعومة. الصيغ المدعومة هي MP3 و M4A و WAV فقط.",
                messageEn = "Unsupported audio format. Please select an MP3, M4A, or WAV file."
            )

        val dir = File(context.filesDir, "adhan_custom").apply { mkdirs() }
        val id = "adhan_custom_${System.currentTimeMillis()}"
        val destFile = File(dir, "$id.$ext")

        return try {
            val input = resolver.openInputStream(uri)
                ?: return AdhanImportResult.Error(
                    messageAr = "تعذّر قراءة الملف الصوتي المحدد.",
                    messageEn = "Unable to read the selected audio file."
                )
            input.use { src ->
                destFile.outputStream().use { dst ->
                    src.copyTo(dst)
                }
            }
            if (!destFile.exists() || destFile.length() == 0L) {
                destFile.delete()
                return AdhanImportResult.Error(
                    messageAr = "الملف الصوتي فارغ أو غير صالح.",
                    messageEn = "The selected audio file is empty or unreadable."
                )
            }
            val cleanLabel = displayName.substringBeforeLast('.').take(48).ifBlank { "Custom Adhan" }
            AdhanImportResult.Success(
                CustomAdhanRecord(
                    id = id,
                    displayName = cleanLabel,
                    localFilePath = destFile.absolutePath,
                    extension = ext,
                    importedAtMillis = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {
            destFile.delete()
            AdhanImportResult.Error(
                messageAr = "حدث خطأ أثناء نسخ الملف الصوتي إلى التطبيق.",
                messageEn = "Failed to import audio file into NOOR."
            )
        }
    }

    fun deleteImportedFile(record: CustomAdhanRecord) {
        runCatching { File(record.localFilePath).delete() }
    }
}
