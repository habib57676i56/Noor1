package com.example.data.remote

import com.example.domain.model.NearbyMosque
import com.example.domain.prayer.QiblaCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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
 * OpenStreetMap Overpass API client (open, free, no API key required)
 * Discovers real nearby mosques around given coordinates and calculates accurate Haversine distance & bearing.
 */
class OverpassApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun findNearbyMosques(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 5000
    ): MosqueSearchResult = withContext(Dispatchers.IO) {
        val query = String.format(
            Locale.US,
            """[out:json][timeout:15];(node["amenity"="place_of_worship"]["religion"="muslim"](around:%d,%.6f,%.6f);way["amenity"="place_of_worship"]["religion"="muslim"](around:%d,%.6f,%.6f););out center 35;""",
            radiusMeters, latitude, longitude,
            radiusMeters, latitude, longitude
        )

        try {
            val body = FormBody.Builder()
                .add("data", query)
                .build()
            val request = Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .header("User-Agent", "NOOR-Islamic-Companion/2.0 (Android)")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext MosqueSearchResult.Error(
                        messageAr = "تعذّر جلب المساجد القريبة حالياً. يمكنك فتح الخريطة المباشرة للبحث.",
                        messageEn = "Unable to query nearby mosques right now. You can open Maps directly."
                    )
                }
                val raw = response.body?.string().orEmpty()
                val json = JSONObject(raw)
                val elements = json.optJSONArray("elements")
                val list = mutableListOf<NearbyMosque>()

                if (elements != null) {
                    for (i in 0 until elements.length()) {
                        val el = elements.getJSONObject(i)
                        val id = el.optLong("id", i.toLong())
                        val lat = if (el.has("lat")) el.optDouble("lat", Double.NaN)
                        else el.optJSONObject("center")?.optDouble("lat", Double.NaN) ?: Double.NaN
                        val lon = if (el.has("lon")) el.optDouble("lon", Double.NaN)
                        else el.optJSONObject("center")?.optDouble("lon", Double.NaN) ?: Double.NaN
                        if (lat.isNaN() || lon.isNaN()) continue

                        val tags = el.optJSONObject("tags")
                        val name = tags?.optString("name:ar").takeUnless { it.isNullOrBlank() }
                            ?: tags?.optString("name").takeUnless { it.isNullOrBlank() }
                            ?: tags?.optString("name:en").takeUnless { it.isNullOrBlank() }
                            ?: "Mosque / مسجد"

                        val street = tags?.optString("addr:street").orEmpty()
                        val city = tags?.optString("addr:city").orEmpty()
                        val address = listOf(street, city).filter { it.isNotBlank() }.joinToString(" · ")

                        val distM = haversineMeters(latitude, longitude, lat, lon)
                        val bearing = bearingDegrees(latitude, longitude, lat, lon)

                        list.add(
                            NearbyMosque(
                                id = id,
                                name = name,
                                latitude = lat,
                                longitude = lon,
                                distanceMeters = distM,
                                bearingDegrees = bearing,
                                address = address
                            )
                        )
                    }
                }
                MosqueSearchResult.Success(list.sortedBy { it.distanceMeters })
            }
        } catch (_: Exception) {
            MosqueSearchResult.Error(
                messageAr = "يتطلب البحث عن المساجد القريبة اتصالاً بالإنترنت. يمكنك أيضاً فتح خرائط جوجل مباشرة.",
                messageEn = "Discovering nearby mosques requires an internet connection. You can also launch Maps directly."
            )
        }
    }

    companion object {
        private const val EARTH_RADIUS_M = 6_371_008.8

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
