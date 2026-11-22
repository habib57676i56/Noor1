package com.example.audio

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.domain.model.PrayerName
import java.io.File
import java.util.Locale

data class AdhanVoiceOption(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val muezzinAr: String,
    val muezzinEn: String,
    val audioUrl: String = "",
    val localFilePath: String? = null,
    val attribution: String = "",
    val isSystemDefault: Boolean = false,
    val isCustomImported: Boolean = false
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

/**
 * Curated Adhan Voice Catalog & Zero-Permission SAF Audio Importer (MP3, M4A, WAV).
 */
object AdhanCatalog {
    const val DEFAULT_VOICE_ID = "adhan_maher_muaiqly"
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

    val builtInVoices: List<AdhanVoiceOption> = listOf(
        AdhanVoiceOption(
            id = "adhan_maher_muaiqly",
            nameAr = "أذان الشيخ ماهر المعيقلي",
            nameEn = "Sheikh Maher Al-Muaiqly Adhan",
            muezzinAr = "الشيخ ماهر المعيقلي · إمام الحرم المكي",
            muezzinEn = "Sheikh Maher Al-Muaiqly · Masjid al-Haram",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan20.mp3",
            attribution = "IslamCan Open Islamic Audio Archive"
        ),
        AdhanVoiceOption(
            id = "adhan_makkah",
            nameAr = "أذان المسجد الحرام — مكة المكرمة",
            nameEn = "Masjid al-Haram (Makkah) Adhan",
            muezzinAr = "الشيخ علي أحمد ملا · الأذان المكي",
            muezzinEn = "Sheikh Ali Ahmed Mulla · Makkah",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan1.mp3",
            attribution = "IslamCan Open Islamic Audio Archive"
        ),
        AdhanVoiceOption(
            id = "adhan_madinah",
            nameAr = "أذان المسجد النبوي — المدينة المنورة",
            nameEn = "Al-Masjid an-Nabawi (Madinah) Adhan",
            muezzinAr = "الأذان المدني الشريف",
            muezzinEn = "Traditional Madinah Adhan",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan2.mp3",
            attribution = "IslamCan Open Islamic Audio Archive"
        ),
        AdhanVoiceOption(
            id = "adhan_alafasy",
            nameAr = "أذان الشيخ مشاري راشد العفاسي",
            nameEn = "Sheikh Mishary Rashid Alafasy Adhan",
            muezzinAr = "الشيخ مشاري راشد العفاسي",
            muezzinEn = "Sheikh Mishary Rashid Alafasy",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan16.mp3",
            attribution = "IslamCan Open Islamic Audio Archive"
        ),
        AdhanVoiceOption(
            id = "adhan_qatami",
            nameAr = "أذان الشيخ ناصر القطامي",
            nameEn = "Sheikh Nasser Al-Qatami Adhan",
            muezzinAr = "الشيخ ناصر القطامي",
            muezzinEn = "Sheikh Nasser Al-Qatami",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan19.mp3",
            attribution = "IslamCan Open Islamic Audio Archive"
        ),
        AdhanVoiceOption(
            id = "adhan_aqsa",
            nameAr = "أذان المسجد الأقصى المبارك",
            nameEn = "Al-Aqsa Mosque (Al-Quds) Adhan",
            muezzinAr = "القدس الشريف",
            muezzinEn = "Al-Quds Traditional Adhan",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan3.mp3",
            attribution = "IslamCan Open Islamic Audio Archive"
        ),
        AdhanVoiceOption(
            id = "adhan_egypt",
            nameAr = "الأذان المصري التراثي",
            nameEn = "Classic Egyptian Adhan",
            muezzinAr = "المدرسة المصرية الأصيلة",
            muezzinEn = "Classic Cairo Style",
            audioUrl = "https://www.islamcan.com/audio/adhan/azan15.mp3",
            attribution = "IslamCan Open Islamic Audio Archive"
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

    fun allVoices(customRecords: List<CustomAdhanRecord>): List<AdhanVoiceOption> {
        val customOptions = customRecords.map { rec ->
            AdhanVoiceOption(
                id = rec.id,
                nameAr = rec.displayName,
                nameEn = rec.displayName,
                muezzinAr = "ملف صوتي مستورد (${rec.extension.uppercase(Locale.US)})",
                muezzinEn = "Imported Device Audio (${rec.extension.uppercase(Locale.US)})",
                localFilePath = rec.localFilePath,
                isCustomImported = true
            )
        }
        return builtInVoices + customOptions
    }

    fun resolveVoice(voiceId: String, customRecords: List<CustomAdhanRecord>): AdhanVoiceOption {
        val all = allVoices(customRecords)
        val matched = all.firstOrNull { it.id == voiceId }
        if (matched != null) {
            // If custom file was deleted from disk, safely fall back to built-in default
            if (matched.isCustomImported) {
                val path = matched.localFilePath
                if (path != null && File(path).exists() && File(path).length() > 0L) {
                    return matched
                }
                return builtInVoices.first()
            }
            return matched
        }
        return builtInVoices.first()
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
