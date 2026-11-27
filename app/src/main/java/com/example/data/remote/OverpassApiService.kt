package com.example.data.remote

import com.example.domain.model.NearbyMosque
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

sealed class MosqueSearchResult {
    data class Success(val mosques: List<NearbyMosque>) : MosqueSearchResult()
    data class Error(val messageAr: String, val messageEn: String) : MosqueSearchResult()
}

/**
 * High-accuracy multi-mirror Nearby Mosques Discovery Engine (100% open & free, zero API keys):
 * 1. Queries 3 independent OpenStreetMap Overpass mirrors using expanded tags
 *    (place_of_worship+muslim, building=mosque, amenity=mosque across nodes, ways & relations).
 * 2. Automatically expands search radius if the immediate radius is sparse.
 * 3. Falls back seamlessly to OpenStreetMap Nominatim & Photon Komoot geo-search if all Overpass
 *    mirrors are rate-limited/unavailable.
 * 4. Deduplicates overlapping OSM node+building entries and sorts strictly by exact Haversine distance.
 */
class OverpassApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(16, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun findNearbyMosques(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 5000
    ): MosqueSearchResult = withContext(Dispatchers.IO) {
        val safeRadius = radiusMeters.coerceIn(500, 30_000)

        // 1. Primary: Query expanded Overpass mirrors at requested radius
        val primary = queryOverpassMirrors(latitude, longitude, safeRadius)
        if (primary != null && primary.isNotEmpty()) {
            return@withContext MosqueSearchResult.Success(primary)
        }

        // 2. If Overpass succeeded with 0 mosques in a small radius, auto-expand up to 15 km
        if (primary != null && safeRadius < 15_000) {
            val expanded = queryOverpassMirrors(latitude, longitude, 15_000)
            if (expanded != null && expanded.isNotEmpty()) {
                return@withContext MosqueSearchResult.Success(expanded)
            }
        }

        // 3. Fallback A: OpenStreetMap Nominatim bounded viewbox search (multiple languages)
        val nominatimResults = queryNominatimMosques(latitude, longitude, maxOf(safeRadius, 12_000))
        if (nominatimResults.isNotEmpty()) {
            return@withContext MosqueSearchResult.Success(nominatimResults)
        }

        // 4. Fallback B: Photon (Komoot OpenStreetMap Geo-Search)
        val photonResults = queryPhotonMosques(latitude, longitude, maxOf(safeRadius, 15_000))
        if (photonResults.isNotEmpty()) {
            return@withContext MosqueSearchResult.Success(photonResults)
        }

        if (primary != null) {
            MosqueSearchResult.Success(emptyList())
        } else {
            MosqueSearchResult.Error(
                messageAr = "تعذّر الاتصال بخوادم خرائط المساجد حالياً. تأكد من الاتصال بالإنترنت أو افتح خرائط جوجل مباشرة.",
                messageEn = "Unable to reach mosque map servers right now. Check your connection or open Google Maps directly."
            )
        }
    }

    private fun queryOverpassMirrors(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): List<NearbyMosque>? {
        val query = buildOverpassQuery(latitude, longitude, radiusMeters)
        var anyMirrorSucceeded = false

        for (endpoint in OVERPASS_MIRRORS) {
            try {
                val body = FormBody.Builder()
                    .add("data", query)
                    .build()
                val request = Request.Builder()
                    .url(endpoint)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val raw = response.body?.string().orEmpty()
                    if (!raw.trimStart().startsWith("{")) return@use
                    anyMirrorSucceeded = true
                    val parsed = parseOverpassJson(raw, latitude, longitude)
                    if (parsed.isNotEmpty()) {
                        return deduplicateAndSort(parsed)
                    }
                }
            } catch (_: Exception) {
                // Try next mirror
            }
        }

        return if (anyMirrorSucceeded) emptyList() else null
    }

    private fun queryNominatimMosques(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): List<NearbyMosque> {
        val deltaLat = (radiusMeters / 111_000.0).coerceIn(0.03, 0.25)
        val cosLat = cos(Math.toRadians(latitude)).coerceAtLeast(0.2)
        val deltaLon = (radiusMeters / (111_000.0 * cosLat)).coerceIn(0.03, 0.30)
        val left = longitude - deltaLon
        val right = longitude + deltaLon
        val top = latitude + deltaLat
        val bottom = latitude - deltaLat

        val collected = mutableListOf<NearbyMosque>()
        val queries = listOf("mosque", "مسجد")
        for (q in queries) {
            try {
                val encodedQ = URLEncoder.encode(q, "UTF-8")
                val url = String.format(
                    Locale.US,
                    "https://nominatim.openstreetmap.org/search?format=jsonv2&q=%s&bounded=1&viewbox=%.5f,%.5f,%.5f,%.5f&limit=25&addressdetails=1",
                    encodedQ, left, top, right, bottom
                )
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept-Language", "ar,en")
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val raw = resp.body?.string().orEmpty()
                    collected.addAll(parseNominatimJson(raw, latitude, longitude, radiusMeters * 1.4))
                }
                if (collected.size >= 10) break
            } catch (_: Exception) {
                // Continue to next query
            }
        }
        return deduplicateAndSort(collected)
    }

    private fun queryPhotonMosques(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): List<NearbyMosque> {
        val collected = mutableListOf<NearbyMosque>()
        for (q in listOf("mosque", "مسجد")) {
            try {
                val encodedQ = URLEncoder.encode(q, "UTF-8")
                val url = String.format(
                    Locale.US,
                    "https://photon.komoot.io/api/?q=%s&lat=%.6f&lon=%.6f&limit=25",
                    encodedQ, latitude, longitude
                )
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val raw = resp.body?.string().orEmpty()
                    collected.addAll(parsePhotonJson(raw, latitude, longitude, radiusMeters * 1.6))
                }
                if (collected.size >= 8) break
            } catch (_: Exception) {
                // Ignore
            }
        }
        return deduplicateAndSort(collected)
    }

    companion object {
        private const val EARTH_RADIUS_M = 6_371_008.8
        private const val USER_AGENT = "NoorIslamicCompanion/3.8 (Android; contact: habiybullohy@gmail.com)"

        val OVERPASS_MIRRORS: List<String> = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
        )

        fun buildOverpassQuery(latitude: Double, longitude: Double, radiusMeters: Int): String =
            String.format(
                Locale.US,
                """[out:json][timeout:14];(nwr["amenity"="place_of_worship"]["religion"="muslim"](around:%d,%.6f,%.6f);nwr["building"="mosque"](around:%d,%.6f,%.6f);nwr["amenity"="mosque"](around:%d,%.6f,%.6f););out center 60;""",
                radiusMeters, latitude, longitude,
                radiusMeters, latitude, longitude,
                radiusMeters, latitude, longitude
            )

        fun parseOverpassJson(
            raw: String,
            originLat: Double,
            originLon: Double
        ): List<NearbyMosque> {
            val json = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
            val elements = json.optJSONArray("elements") ?: return emptyList()
            val list = mutableListOf<NearbyMosque>()

            for (i in 0 until elements.length()) {
                val el = elements.optJSONObject(i) ?: continue
                val id = el.optLong("id", i.toLong())
                val lat = if (el.has("lat")) el.optDouble("lat", Double.NaN)
                else el.optJSONObject("center")?.optDouble("lat", Double.NaN) ?: Double.NaN
                val lon = if (el.has("lon")) el.optDouble("lon", Double.NaN)
                else el.optJSONObject("center")?.optDouble("lon", Double.NaN) ?: Double.NaN
                if (lat.isNaN() || lon.isNaN()) continue

                val tags = el.optJSONObject("tags")
                val name = tags?.optString("name:ar").takeUnless { it.isNullOrBlank() }
                    ?: tags?.optString("name").takeUnless { it.isNullOrBlank() }
                    ?: tags?.optString("official_name").takeUnless { it.isNullOrBlank() }
                    ?: tags?.optString("name:en").takeUnless { it.isNullOrBlank() }
                    ?: tags?.optString("alt_name").takeUnless { it.isNullOrBlank() }
                    ?: "مسجد / Mosque"

                val street = tags?.optString("addr:street").orEmpty()
                val suburb = tags?.optString("addr:suburb").takeUnless { it.isNullOrBlank() }
                    ?: tags?.optString("addr:neighbourhood").takeUnless { it.isNullOrBlank() }
                    ?: tags?.optString("addr:district").orEmpty()
                val city = tags?.optString("addr:city").orEmpty()
                val address = listOf(street, suburb, city).filter { it.isNotBlank() }.distinct().joinToString(" · ")

                list.add(
                    NearbyMosque(
                        id = id,
                        name = name,
                        latitude = lat,
                        longitude = lon,
                        distanceMeters = haversineMeters(originLat, originLon, lat, lon),
                        bearingDegrees = bearingDegrees(originLat, originLon, lat, lon),
                        address = address
                    )
                )
            }
            return list
        }

        fun parseNominatimJson(
            raw: String,
            originLat: Double,
            originLon: Double,
            maxDistanceMeters: Double
        ): List<NearbyMosque> {
            val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
            val list = mutableListOf<NearbyMosque>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                val lon = obj.optString("lon").toDoubleOrNull() ?: continue
                val dist = haversineMeters(originLat, originLon, lat, lon)
                if (dist > maxDistanceMeters) continue
                val rawName = obj.optString("name").takeIf { it.isNotBlank() }
                    ?: obj.optString("display_name").substringBefore(",").trim().takeIf { it.isNotBlank() }
                    ?: "مسجد / Mosque"
                val addrObj = obj.optJSONObject("address")
                val address = listOfNotNull(
                    addrObj?.optString("road")?.takeIf { it.isNotBlank() },
                    addrObj?.optString("suburb")?.takeIf { it.isNotBlank() },
                    addrObj?.optString("city")?.takeIf { it.isNotBlank() }
                ).distinct().joinToString(" · ")

                list.add(
                    NearbyMosque(
                        id = obj.optLong("place_id", (i + 900_000).toLong()),
                        name = rawName,
                        latitude = lat,
                        longitude = lon,
                        distanceMeters = dist,
                        bearingDegrees = bearingDegrees(originLat, originLon, lat, lon),
                        address = address
                    )
                )
            }
            return list
        }

        fun parsePhotonJson(
            raw: String,
            originLat: Double,
            originLon: Double,
            maxDistanceMeters: Double
        ): List<NearbyMosque> {
            val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
            val features = root.optJSONArray("features") ?: return emptyList()
            val list = mutableListOf<NearbyMosque>()
            for (i in 0 until features.length()) {
                val feat = features.optJSONObject(i) ?: continue
                val coords = feat.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
                if (coords.length() < 2) continue
                val lon = coords.optDouble(0, Double.NaN)
                val lat = coords.optDouble(1, Double.NaN)
                if (lat.isNaN() || lon.isNaN()) continue
                val dist = haversineMeters(originLat, originLon, lat, lon)
                if (dist > maxDistanceMeters) continue
                val props = feat.optJSONObject("properties") ?: continue
                val name = props.optString("name").takeIf { it.isNotBlank() } ?: continue
                val address = listOfNotNull(
                    props.optString("street").takeIf { it.isNotBlank() },
                    props.optString("district").takeIf { it.isNotBlank() },
                    props.optString("city").takeIf { it.isNotBlank() }
                ).distinct().joinToString(" · ")

                list.add(
                    NearbyMosque(
                        id = props.optLong("osm_id", (i + 700_000).toLong()),
                        name = name,
                        latitude = lat,
                        longitude = lon,
                        distanceMeters = dist,
                        bearingDegrees = bearingDegrees(originLat, originLon, lat, lon),
                        address = address
                    )
                )
            }
            return list
        }

        /**
         * Deduplicates overlapping OSM node + building polygon entries within 45 meters,
         * preserving the entry with the most descriptive name & address, sorted closest-first.
         */
        fun deduplicateAndSort(mosques: List<NearbyMosque>): List<NearbyMosque> {
            val sorted = mosques.sortedBy { it.distanceMeters }
            val result = mutableListOf<NearbyMosque>()
            for (candidate in sorted) {
                val existingIdx = result.indexOfFirst { existing ->
                    haversineMeters(existing.latitude, existing.longitude, candidate.latitude, candidate.longitude) < 45.0
                }
                if (existingIdx == -1) {
                    result.add(candidate)
                } else {
                    val existing = result[existingIdx]
                    val existingGeneric = existing.name.contains("مسجد / Mosque") || existing.name.contains("Mosque / مسجد")
                    val candidateGeneric = candidate.name.contains("مسجد / Mosque") || candidate.name.contains("Mosque / مسجد")
                    if ((existingGeneric && !candidateGeneric) ||
                        (existing.address.isBlank() && candidate.address.isNotBlank())
                    ) {
                        result[existingIdx] = candidate
                    }
                }
            }
            return result
        }

        fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val phi1 = Math.toRadians(lat1)
            val phi2 = Math.toRadians(lat2)
            val dPhi = Math.toRadians(lat2 - lat1)
            val dLambda = Math.toRadians(lon2 - lon1)
            val a = sin(dPhi / 2.0) * sin(dPhi / 2.0) +
                cos(phi1) * cos(phi2) * sin(dLambda / 2.0) * sin(dLambda / 2.0)
            val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
            return EARTH_RADIUS_M * c
        }

        fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val phi1 = Math.toRadians(lat1)
            val phi2 = Math.toRadians(lat2)
            val dLambda = Math.toRadians(lon2 - lon1)
            val y = sin(dLambda) * cos(phi2)
            val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
            return ((atan2(y, x) * 180.0 / PI) + 360.0) % 360.0
        }
    }
}
