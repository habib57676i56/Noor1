package com.example.data.quran

import android.content.Context
import com.example.data.local.catalog.IslamicVideoCatalog
import com.example.data.local.catalog.IslamicVideoCategory
import com.example.data.local.catalog.IslamicVideoCollection
import com.example.data.local.catalog.IslamicVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed class VideoEpisodesResult {
    data class Success(val episodes: List<IslamicVideoItem>, val fromCache: Boolean) : VideoEpisodesResult()
    data class Error(val messageAr: String, val messageEn: String) : VideoEpisodesResult()
}

/**
 * Resolves real direct playable `.mp4`/`.m4v` streams for [IslamicVideoCatalog] collections
 * via Archive.org's official Metadata JSON API (`https://archive.org/metadata/{itemId}`),
 * caching metadata locally so repeat visits work cleanly, and persisting user-picked local videos
 * (Android Photo Picker URIs) or custom educational stream URLs.
 */
class IslamicVideoRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val cacheDir = File(context.filesDir, "video_catalog_cache").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("noor_videos_prefs", Context.MODE_PRIVATE)

    suspend fun loadEpisodes(
        collection: IslamicVideoCollection,
        forceRefresh: Boolean = false
    ): VideoEpisodesResult = withContext(Dispatchers.IO) {
        val cacheFile = File(cacheDir, "${collection.archiveItemId}.json")
        if (!forceRefresh && cacheFile.exists()) {
            val cached = runCatching { cacheFile.readText() }.getOrNull()
            if (!cached.isNullOrBlank()) {
                val parsed = parseArchiveMetadataJson(collection.archiveItemId, cached, collection)
                if (parsed.isNotEmpty()) {
                    return@withContext VideoEpisodesResult.Success(parsed, fromCache = true)
                }
            }
        }

        val url = "https://archive.org/metadata/${collection.archiveItemId}"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "NOOR-Islamic-App/3.0")
            .build()

        val responseBody = runCatching {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                resp.body?.string()
            }
        }.getOrNull()

        if (!responseBody.isNullOrBlank()) {
            val episodes = parseArchiveMetadataJson(collection.archiveItemId, responseBody, collection)
            if (episodes.isNotEmpty()) {
                runCatching { cacheFile.writeText(responseBody) }
                return@withContext VideoEpisodesResult.Success(episodes, fromCache = false)
            }
        }

        // Fall back to cache even on force refresh if offline
        if (cacheFile.exists()) {
            val cached = runCatching { cacheFile.readText() }.getOrNull()
            if (!cached.isNullOrBlank()) {
                val parsed = parseArchiveMetadataJson(collection.archiveItemId, cached, collection)
                if (parsed.isNotEmpty()) {
                    return@withContext VideoEpisodesResult.Success(parsed, fromCache = true)
                }
            }
        }

        VideoEpisodesResult.Error(
            messageAr = "تعذّر تحميل قائمة حلقات الفيديو. تأكد من الاتصال بالإنترنت أو أضف فيديو من جهازك.",
            messageEn = "Could not load video episodes. Check your internet connection or pick a video from your device."
        )
    }

    fun getUserVideos(): List<IslamicVideoItem> {
        val raw = prefs.getString("user_videos_json", "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val list = mutableListOf<IslamicVideoItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val url = obj.optString("streamUrl")
            if (url.isBlank()) continue
            list.add(
                IslamicVideoItem(
                    id = obj.optString("id"),
                    collectionId = "user_saved",
                    titleAr = obj.optString("titleAr"),
                    titleEn = obj.optString("titleEn"),
                    subtitleAr = obj.optString("subtitleAr"),
                    subtitleEn = obj.optString("subtitleEn"),
                    category = IslamicVideoCategory.MY_VIDEOS,
                    streamUrl = url,
                    durationLabel = obj.optString("durationLabel"),
                    sourceLabel = obj.optString("sourceLabel", "Local"),
                    isLocalUri = obj.optBoolean("isLocalUri", true)
                )
            )
        }
        return list
    }

    fun addUserVideo(item: IslamicVideoItem): List<IslamicVideoItem> {
        val current = getUserVideos().filterNot { it.id == item.id || it.streamUrl == item.streamUrl }
        val updated = listOf(item) + current
        saveUserVideos(updated)
        return updated
    }

    fun removeUserVideo(id: String): List<IslamicVideoItem> {
        val updated = getUserVideos().filterNot { it.id == id }
        saveUserVideos(updated)
        return updated
    }

    private fun saveUserVideos(items: List<IslamicVideoItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("titleAr", item.titleAr)
                    put("titleEn", item.titleEn)
                    put("subtitleAr", item.subtitleAr)
                    put("subtitleEn", item.subtitleEn)
                    put("streamUrl", item.streamUrl)
                    put("durationLabel", item.durationLabel)
                    put("sourceLabel", item.sourceLabel)
                    put("isLocalUri", item.isLocalUri)
                }
            )
        }
        prefs.edit().putString("user_videos_json", arr.toString()).apply()
    }

    companion object {
        /**
         * Parses an Archive.org `/metadata/{id}` JSON payload into ordered playable `.mp4`/`.m4v` episodes
         * pointing directly to Archive.org's direct CDN node (`https://{server}{dir}/{encodedName}`),
         * avoiding 302 redirect pitfalls on legacy Android media stacks.
         */
        fun parseArchiveMetadataJson(
            archiveItemId: String,
            jsonText: String,
            collection: IslamicVideoCollection
        ): List<IslamicVideoItem> {
            val root = runCatching { JSONObject(jsonText) }.getOrNull() ?: return emptyList()
            val server = root.optString("server").trim()
            val dir = root.optString("dir").trim()
            val files = root.optJSONArray("files") ?: return emptyList()

            data class RawVideoCandidate(
                val name: String,
                val baseKey: String,
                val isDerivativeIaMp4: Boolean,
                val durationLabel: String
            )

            val candidates = mutableListOf<RawVideoCandidate>()
            for (i in 0 until files.length()) {
                val fileObj = files.optJSONObject(i) ?: continue
                val name = fileObj.optString("name").trim()
                if (name.isBlank()) continue
                val lower = name.lowercase()
                if (!lower.endsWith(".mp4") && !lower.endsWith(".m4v")) continue

                val baseKey = lower
                    .removeSuffix(".ia.mp4")
                    .removeSuffix(".mp4")
                    .removeSuffix(".m4v")
                val durationSec = fileObj.optString("length").toDoubleOrNull()?.toInt()
                val durationLabel = if (durationSec != null && durationSec > 0) {
                    "%d:%02d".format(durationSec / 60, durationSec % 60)
                } else {
                    ""
                }
                candidates.add(
                    RawVideoCandidate(
                        name = name,
                        baseKey = baseKey,
                        isDerivativeIaMp4 = lower.endsWith(".ia.mp4"),
                        durationLabel = durationLabel
                    )
                )
            }

            // Prefer original `.mp4` over `.ia.mp4` when both exist for the same base episode
            val deduped = candidates
                .groupBy { it.baseKey }
                .values
                .map { group -> group.firstOrNull { !it.isDerivativeIaMp4 } ?: group.first() }
                .sortedWith(compareBy({ extractLeadingNumber(it.name) }, { it.name }))

            return deduped.mapIndexed { idx, cand ->
                val encodedPath = cand.name.split("/").joinToString("/") { segment ->
                    URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
                }
                val directUrl = if (server.isNotBlank() && dir.isNotBlank()) {
                    "https://$server${if (dir.startsWith("/")) dir else "/$dir"}/$encodedPath"
                } else {
                    "https://archive.org/download/$archiveItemId/$encodedPath"
                }
                val cleanName = cand.name.substringAfterLast("/")
                    .removeSuffix(".ia.mp4")
                    .removeSuffix(".mp4")
                    .removeSuffix(".m4v")
                    .replace('_', ' ')
                    .trim()

                val episodePrefixAr = if (deduped.size > 1) "الحلقة ${idx + 1} · " else ""
                val episodePrefixEn = if (deduped.size > 1) "Ep ${idx + 1} · " else ""

                IslamicVideoItem(
                    id = "${collection.id}_$idx",
                    collectionId = collection.id,
                    titleAr = "$episodePrefixAr${cleanName.ifBlank { collection.titleAr }}",
                    titleEn = "$episodePrefixEn${cleanName.ifBlank { collection.titleEn }}",
                    subtitleAr = collection.speakerOrStudioAr,
                    subtitleEn = collection.speakerOrStudioEn,
                    category = collection.category,
                    streamUrl = directUrl,
                    durationLabel = cand.durationLabel,
                    sourceLabel = "Archive.org",
                    isLocalUri = false
                )
            }
        }

        private fun extractLeadingNumber(name: String): Int {
            val match = Regex("(\\d+)").find(name.substringAfterLast("/"))
            return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
        }
    }
}
