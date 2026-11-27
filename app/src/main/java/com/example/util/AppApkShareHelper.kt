package com.example.util

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.cloud.AppVersionGate
import com.example.data.cloud.NoorPlatformConstants
import com.example.domain.model.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Exports the currently installed NOOR APK into a FileProvider-backed cache path
 * and shares it via Android's native share sheet with the official Telegram channel attached.
 * Uses zero storage permissions.
 */
object AppApkShareHelper {

    const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    const val SHARED_APKS_DIR = "shared_apks"
    private const val COPY_BUFFER_BYTES = 64 * 1024

    fun fileProviderAuthority(packageName: String): String = "$packageName.fileprovider"

    fun buildApkFileName(versionTag: String): String {
        val cleaned = versionTag
            .trim()
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
            .ifBlank { NoorPlatformConstants.APP_VERSION_TAG }
        val normalizedTag = if (cleaned.startsWith("v", ignoreCase = true)) cleaned else "v$cleaned"
        return "NOOR_${normalizedTag}.apk"
    }

    fun buildShareSubject(lang: AppLanguage, versionTag: String): String = when (lang) {
        AppLanguage.ARABIC -> "تطبيق نور الإسلامي ($versionTag)"
        AppLanguage.UZBEK -> "NOOR Islomiy ilovasi ($versionTag)"
        AppLanguage.RUSSIAN -> "Исламское приложение NOOR ($versionTag)"
        AppLanguage.ENGLISH -> "NOOR Islamic Companion ($versionTag)"
    }

    fun buildChooserTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "مشاركة تطبيق نور (ملف APK)"
        AppLanguage.UZBEK -> "NOOR ilovasini ulashish (APK)"
        AppLanguage.RUSSIAN -> "Поделиться NOOR (APK)"
        AppLanguage.ENGLISH -> "Share NOOR App (APK)"
    }

    fun buildShareCaption(
        lang: AppLanguage,
        versionTag: String = NoorPlatformConstants.APP_VERSION_TAG,
        rawTelegramUrl: String? = NoorPlatformConstants.DEFAULT_UPDATE_CHANNEL_URL
    ): String {
        val officialTelegram = AppVersionGate.resolveSafeUpdateUrl(rawTelegramUrl)
        return when (lang) {
            AppLanguage.ARABIC ->
                "تطبيق نور — الرفيق الإسلامي المتكامل ($versionTag)\n" +
                    "المصحف الشريف المصور، مواقيت الصلاة بدقة عالية، الأذكار، القبلة والمسبحة.\n\n" +
                    "قم بتثبيت ملف APK المرفق مباشرةً.\n" +
                    "القناة الرسمية على تليجرام للتحديثات:\n$officialTelegram"

            AppLanguage.UZBEK ->
                "NOOR — Islomiy hamroh ilovasi ($versionTag)\n" +
                    "Madina Mushafi, aniq namoz vaqtlari, zikrlar, Qibla va tasbeh.\n\n" +
                    "Biriktirilgan APK faylni to'g'ridan-to'g'ri o'rnating.\n" +
                    "Rasmiy Telegram kanalimiz:\n$officialTelegram"

            AppLanguage.RUSSIAN ->
                "NOOR — Исламский спутник ($versionTag)\n" +
                    "Коран (Мусхаф Медины), точное время намаза, азкары, Кибла и тасбих.\n\n" +
                    "Установите прикреплённый APK-файл напрямую.\n" +
                    "Официальный Telegram-канал:\n$officialTelegram"

            AppLanguage.ENGLISH ->
                "NOOR — The Complete Islamic Companion ($versionTag)\n" +
                    "Madinah Mushaf, high-precision prayer times, Adhkar, Qibla & Tasbih.\n\n" +
                    "Install the attached APK file directly.\n" +
                    "Official Telegram channel for updates:\n$officialTelegram"
        }
    }

    suspend fun prepareAndShareApk(
        context: Context,
        lang: AppLanguage,
        versionTag: String = NoorPlatformConstants.APP_VERSION_TAG,
        rawTelegramUrl: String? = NoorPlatformConstants.DEFAULT_UPDATE_CHANNEL_URL
    ): Result<Unit> = runCatching {
        val apkFileName = buildApkFileName(versionTag)
        val exportedApk = withContext(Dispatchers.IO) {
            val sourcePath = context.applicationInfo.publicSourceDir
                ?.takeIf { it.isNotBlank() }
                ?: context.applicationInfo.sourceDir
            require(!sourcePath.isNullOrBlank()) { "Installed APK source path is unavailable." }

            val sourceFile = File(sourcePath)
            require(sourceFile.exists() && sourceFile.canRead() && sourceFile.length() > 0L) {
                "Installed APK file cannot be read."
            }

            val shareDir = File(context.cacheDir, SHARED_APKS_DIR).apply { mkdirs() }
            val targetFile = File(shareDir, apkFileName)

            // Prune stale APK files from previous versions to keep cache tidy
            shareDir.listFiles()?.forEach { existing ->
                if (existing.isFile && existing.name != apkFileName) {
                    runCatching { existing.delete() }
                }
            }

            // Copy fresh APK if missing or size changed (atomic write via temp file)
            if (!targetFile.exists() || targetFile.length() != sourceFile.length()) {
                val tempFile = File(shareDir, "$apkFileName.tmp")
                sourceFile.inputStream().buffered(COPY_BUFFER_BYTES).use { input ->
                    FileOutputStream(tempFile).buffered(COPY_BUFFER_BYTES).use { output ->
                        input.copyTo(output, COPY_BUFFER_BYTES)
                        output.flush()
                    }
                }
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
            }
            targetFile
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            fileProviderAuthority(context.packageName),
            exportedApk
        )
        val subject = buildShareSubject(lang, versionTag)
        val caption = buildShareCaption(lang, versionTag, rawTelegramUrl)
        val chooserTitle = buildChooserTitle(lang)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = APK_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, apkUri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, caption)
            clipData = ClipData.newRawUri(apkFileName, apkUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        withContext(Dispatchers.Main) {
            context.startActivity(chooser)
        }
    }
}
