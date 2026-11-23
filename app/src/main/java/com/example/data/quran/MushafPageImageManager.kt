package com.example.data.quran

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import com.example.data.local.catalog.MadinahMushafPageCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

data class MushafDownloadProgress(
    val cachedPagesCount: Int = 0,
    val totalPages: Int = MadinahMushafPageCatalog.TOTAL_PAGES,
    val isDownloading: Boolean = false,
    val batchCompleted: Int = 0,
    val batchTotal: Int = 0,
    val currentPage: Int = 0,
    val storageSizeMb: Double = 0.0,
    val statusAr: String = "",
    val statusEn: String = "",
    val errorAr: String = "",
    val errorEn: String = ""
)

sealed class MushafPageLoadResult {
    data class Ready(val pageNumber: Int, val bitmap: Bitmap, val fromOfflineCache: Boolean) : MushafPageLoadResult()
    data class Unavailable(val pageNumber: Int, val reasonAr: String, val reasonEn: String) : MushafPageLoadResult()
}

/**
 * Offline-first Madinah Mushaf 604-Page Image Manager.
 *
 * Features:
 * - Downloads authentic King Fahd Complex Madinah Mushaf page images from verified mirrors
 *   and validates binary PNG/JPEG/WebP signatures before persisting locally.
 * - Stores cached pages permanently in app-private storage (`filesDir/mushaf_pages/page_001.png`..`page_604.png`)
 *   for 100% offline reading after download or local ZIP import.
 * - Low-RAM safe bounded LRU Bitmap cache (max 6 pages, downsampled cleanly for Syria/budget devices).
 * - Supports zero-internet local ZIP archive import via Android Storage Access Framework (SAF).
 */
class MushafPageImageManager(private val appContext: Context) {

    private val pagesDir: File = File(appContext.filesDir, "mushaf_pages").apply { mkdirs() }

    // Memory-safe LRU cache capped at 6 decoded Mushaf pages (~6-10 MB) for low-RAM compatibility
    private val memoryCache = object : LruCache<Int, Bitmap>(6) {
        override fun sizeOf(key: Int, value: Bitmap): Int = 1
    }

    private val _progress = MutableStateFlow(scanCacheSnapshot())
    val progress: StateFlow<MushafDownloadProgress> = _progress.asStateFlow()

    @Volatile
    private var activeBatchJob: Job? = null

    fun pageFile(pageNumber: Int): File {
        val clamped = pageNumber.coerceIn(1, MadinahMushafPageCatalog.TOTAL_PAGES)
        return File(pagesDir, "page_${clamped.toString().padStart(3, '0')}.png")
    }

    fun isPageCachedOffline(pageNumber: Int): Boolean {
        val f = pageFile(pageNumber)
        return f.exists() && f.length() >= MIN_VALID_IMAGE_BYTES
    }

    fun refreshCacheStats() {
        val current = _progress.value
        val snapshot = scanCacheSnapshot()
        _progress.value = current.copy(
            cachedPagesCount = snapshot.cachedPagesCount,
            storageSizeMb = snapshot.storageSizeMb
        )
    }

    private fun scanCacheSnapshot(): MushafDownloadProgress {
        var count = 0
        var bytes = 0L
        pagesDir.listFiles()?.forEach { f ->
            if (f.isFile && f.name.startsWith("page_") && f.length() >= MIN_VALID_IMAGE_BYTES) {
                count++
                bytes += f.length()
            }
        }
        return MushafDownloadProgress(
            cachedPagesCount = count,
            storageSizeMb = bytes / (1024.0 * 1024.0)
        )
    }

    /**
     * Loads a Madinah Mushaf page bitmap from:
     * 1) In-memory LRU cache
     * 2) Local offline storage (`filesDir/mushaf_pages`)
     * 3) Authorized open Madinah Mushaf mirrors (auto-cached locally on success)
     */
    suspend fun loadPageBitmap(
        pageNumber: Int,
        maxWidthPx: Int = 1080
    ): MushafPageLoadResult = withContext(Dispatchers.IO) {
        val clamped = pageNumber.coerceIn(1, MadinahMushafPageCatalog.TOTAL_PAGES)
        memoryCache.get(clamped)?.let { cachedBmp ->
            if (!cachedBmp.isRecycled) {
                return@withContext MushafPageLoadResult.Ready(clamped, cachedBmp, fromOfflineCache = true)
            }
        }

        val localFile = pageFile(clamped)
        if (localFile.exists() && localFile.length() >= MIN_VALID_IMAGE_BYTES) {
            decodeSafeBitmap(localFile, maxWidthPx)?.let { bmp ->
                memoryCache.put(clamped, bmp)
                return@withContext MushafPageLoadResult.Ready(clamped, bmp, fromOfflineCache = true)
            } ?: run {
                // Corrupt file on disk -> remove and retry download
                runCatching { localFile.delete() }
            }
        }

        // Download from verified mirrors and persist locally
        val downloaded = downloadSinglePage(clamped)
        if (downloaded && localFile.exists()) {
            refreshCacheStats()
            decodeSafeBitmap(localFile, maxWidthPx)?.let { bmp ->
                memoryCache.put(clamped, bmp)
                return@withContext MushafPageLoadResult.Ready(clamped, bmp, fromOfflineCache = false)
            }
        }

        MushafPageLoadResult.Unavailable(
            pageNumber = clamped,
            reasonAr = "الصفحة $clamped غير محفوظة محلياً بعد وتعذّر تحميلها حالياً. يمكنك الاتصال بالإنترنت لتحميل صفحات مصحف المدينة أو استيراد حزمة المصحف المحلية أو التبديل مؤقتاً إلى النص العثماني.",
            reasonEn = "Page $clamped is not cached offline yet and could not be downloaded right now. Connect to download Madinah Mushaf pages, import an offline ZIP pack, or switch to Interactive Uthmani Text."
        )
    }

    /**
     * Prefetches adjacent pages quietly into offline storage when reading online.
     */
    suspend fun prefetchNeighborPages(centerPage: Int) = withContext(Dispatchers.IO) {
        val neighbors = listOf(centerPage + 1, centerPage + 2, centerPage - 1)
            .filter { it in 1..MadinahMushafPageCatalog.TOTAL_PAGES }
        for (p in neighbors) {
            if (!isActive) break
            if (!isPageCachedOffline(p)) {
                downloadSinglePage(p)
            }
        }
        refreshCacheStats()
    }

    /**
     * Downloads a range of pages (e.g. a Surah, a Juz, or all 604 pages) to offline storage.
     */
    suspend fun downloadPagesRange(
        range: IntRange,
        labelAr: String,
        labelEn: String
    ): Int = withContext(Dispatchers.IO) {
        val pages = range.filter { it in 1..MadinahMushafPageCatalog.TOTAL_PAGES }
        if (pages.isEmpty()) return@withContext 0

        _progress.value = _progress.value.copy(
            isDownloading = true,
            batchCompleted = 0,
            batchTotal = pages.size,
            currentPage = pages.first(),
            statusAr = "جارٍ تحميل $labelAr (${pages.size} صفحة) للقراءة دون إنترنت...",
            statusEn = "Downloading $labelEn (${pages.size} pages) for offline reading...",
            errorAr = "",
            errorEn = ""
        )

        var successCount = 0
        var failCount = 0
        try {
            for ((index, page) in pages.withIndex()) {
                if (!isActive) break
                val ok = if (isPageCachedOffline(page)) true else downloadSinglePage(page)
                if (ok) successCount++ else failCount++
                val snap = scanCacheSnapshot()
                _progress.value = _progress.value.copy(
                    cachedPagesCount = snap.cachedPagesCount,
                    storageSizeMb = snap.storageSizeMb,
                    batchCompleted = index + 1,
                    batchTotal = pages.size,
                    currentPage = page
                )
            }
            val finalSnap = scanCacheSnapshot()
            _progress.value = finalSnap.copy(
                isDownloading = false,
                batchCompleted = pages.size,
                batchTotal = pages.size,
                statusAr = if (failCount == 0) {
                    "تم حفظ $labelAr بنجاح (${finalSnap.cachedPagesCount}/${MadinahMushafPageCatalog.TOTAL_PAGES} صفحة جاهزة دون إنترنت) ✓"
                } else {
                    "تم حفظ $successCount صفحة (تعذّر $failCount بسبب الشبكة — أعد المحاولة لاحقاً)"
                },
                statusEn = if (failCount == 0) {
                    "$labelEn saved offline (${finalSnap.cachedPagesCount}/${MadinahMushafPageCatalog.TOTAL_PAGES} pages ready) ✓"
                } else {
                    "Saved $successCount pages ($failCount failed due to network — retry later)"
                }
            )
            successCount
        } catch (e: CancellationException) {
            val snap = scanCacheSnapshot()
            _progress.value = snap.copy(
                isDownloading = false,
                statusAr = "تم إيقاف التحميل — المحفوظ حالياً: ${snap.cachedPagesCount} صفحة",
                statusEn = "Download paused — ${snap.cachedPagesCount} pages saved offline"
            )
            throw e
        } catch (e: Exception) {
            val snap = scanCacheSnapshot()
            _progress.value = snap.copy(
                isDownloading = false,
                errorAr = "تعذّر إكمال التحميل: ${e.localizedMessage ?: "خطأ في الشبكة"}",
                errorEn = "Download error: ${e.localizedMessage ?: "network error"}"
            )
            successCount
        }
    }

    /**
     * Imports a user-supplied local ZIP pack of Madinah Mushaf images (`001.png`..`604.png` or `page001.png`..`page604.png`)
     * directly into offline storage with zero internet required.
     * Guards against zip-slip traversal and validates binary image headers.
     */
    suspend fun importOfflineZipPack(uri: Uri): Int = withContext(Dispatchers.IO) {
        var imported = 0
        try {
            _progress.value = _progress.value.copy(
                isDownloading = true,
                statusAr = "جارٍ استيراد حزمة صفحات مصحف المدينة المحلية...",
                statusEn = "Importing local Madinah Mushaf ZIP pack...",
                errorAr = "",
                errorEn = ""
            )
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null && isActive) {
                        if (!entry.isDirectory) {
                            val name = entry.name.substringAfterLast('/').substringAfterLast('\\')
                            val pageNum = extractPageNumberFromFileName(name)
                            if (pageNum != null && pageNum in 1..MadinahMushafPageCatalog.TOTAL_PAGES) {
                                val bytes = readBoundedBytes(zis, MAX_SINGLE_PAGE_BYTES)
                                if (bytes != null && isValidImagePayload(bytes)) {
                                    writeAtomically(pageFile(pageNum), bytes)
                                    memoryCache.remove(pageNum)
                                    imported++
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            val snap = scanCacheSnapshot()
            _progress.value = snap.copy(
                isDownloading = false,
                statusAr = if (imported > 0) {
                    "تم استيراد $imported صفحة من المصحف المحلي بنجاح (${snap.cachedPagesCount}/604 جاهزة دون إنترنت) ✓"
                } else {
                    "لم يتم العثور على صور صفحات مرقمة (001.png..604.png) داخل ملف ZIP"
                },
                statusEn = if (imported > 0) {
                    "Imported $imported Mushaf pages offline (${snap.cachedPagesCount}/604 cached) ✓"
                } else {
                    "No numbered Mushaf page images (001.png..604.png) found inside the ZIP archive"
                }
            )
        } catch (e: Exception) {
            val snap = scanCacheSnapshot()
            _progress.value = snap.copy(
                isDownloading = false,
                errorAr = "فشل استيراد الحزمة المحلية: ${e.localizedMessage ?: "ملف غير صالح"}",
                errorEn = "Offline pack import failed: ${e.localizedMessage ?: "invalid file"}"
            )
        }
        imported
    }

    suspend fun clearOfflinePagesCache(): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        pagesDir.listFiles()?.forEach { f ->
            if (f.isFile && runCatching { f.delete() }.getOrDefault(false)) deleted++
        }
        memoryCache.evictAll()
        _progress.value = scanCacheSnapshot().copy(
            statusAr = "تم مسح $deleted صفحة مؤقتة من الذاكرة",
            statusEn = "Cleared $deleted cached Mushaf pages"
        )
        deleted
    }

    private fun downloadSinglePage(pageNumber: Int): Boolean {
        val urls = MadinahMushafPageCatalog.imageUrlsForPage(pageNumber)
        val destFile = pageFile(pageNumber)
        for (urlStr in urls) {
            val bytes = fetchImageBytes(urlStr)
            if (bytes != null && isValidImagePayload(bytes)) {
                return runCatching {
                    writeAtomically(destFile, bytes)
                    true
                }.getOrDefault(false)
            }
        }
        return false
    }

    private fun fetchImageBytes(urlStr: String): ByteArray? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 9000
                readTimeout = 12000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "NOOR-Islamic-Android/6.0")
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { stream ->
                readBoundedBytes(stream, MAX_SINGLE_PAGE_BYTES)
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun writeAtomically(dest: File, bytes: ByteArray) {
        val tmp = File(dest.parentFile, "${dest.name}.tmp")
        FileOutputStream(tmp).use { it.write(bytes) }
        if (dest.exists()) dest.delete()
        if (!tmp.renameTo(dest)) {
            FileOutputStream(dest).use { it.write(bytes) }
            tmp.delete()
        }
    }

    private fun decodeSafeBitmap(file: File, maxWidthPx: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sampleSize = 1
            while (bounds.outWidth / (sampleSize * 2) >= maxWidthPx) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (_: OutOfMemoryError) {
            memoryCache.evictAll()
            null
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val MIN_VALID_IMAGE_BYTES = 2048
        const val MAX_SINGLE_PAGE_BYTES = 3 * 1024 * 1024 // 3MB safety cap per page

        /**
         * Validates real PNG, JPEG, or WebP magic headers so HTML error pages or truncated files
         * are strictly rejected before saving to offline cache.
         */
        fun isValidImagePayload(bytes: ByteArray): Boolean {
            if (bytes.size < MIN_VALID_IMAGE_BYTES) return false
            // PNG: 89 50 4E 47 0D 0A 1A 0A
            val isPng = bytes.size >= 8 &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() &&
                bytes[3] == 0x47.toByte()
            // JPEG: FF D8 FF
            val isJpeg = bytes.size >= 3 &&
                bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() &&
                bytes[2] == 0xFF.toByte()
            // WebP: RIFF....WEBP
            val isWebp = bytes.size >= 12 &&
                bytes[0] == 'R'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() &&
                bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() &&
                bytes[9] == 'E'.code.toByte() &&
                bytes[10] == 'B'.code.toByte() &&
                bytes[11] == 'P'.code.toByte()
            return isPng || isJpeg || isWebp
        }

        /**
         * Extracts page number `1..604` from filenames like `001.png`, `page001.png`, `page_604.jpg`.
         */
        fun extractPageNumberFromFileName(name: String): Int? {
            val lower = name.lowercase()
            if (!lower.endsWith(".png") && !lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".webp")) {
                return null
            }
            val digits = Regex("(\\d{1,3})").findAll(lower).lastOrNull()?.groupValues?.get(1) ?: return null
            val num = digits.toIntOrNull() ?: return null
            return if (num in 1..MadinahMushafPageCatalog.TOTAL_PAGES) num else null
        }

        private fun readBoundedBytes(stream: java.io.InputStream, maxBytes: Int): ByteArray? {
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var total = 0
            while (true) {
                val read = stream.read(buf)
                if (read <= 0) break
                total += read
                if (total > maxBytes) return null
                out.write(buf, 0, read)
            }
            return out.toByteArray()
        }
    }
}
