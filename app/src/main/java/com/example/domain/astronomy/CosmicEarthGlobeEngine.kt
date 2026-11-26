package com.example.domain.astronomy

import com.example.domain.model.CalculationMethod
import com.example.domain.model.AsrMadhhab
import com.example.domain.prayer.PrayerTimeCalculator
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class SubsolarPoint(
    val latitudeDeg: Double,
    val longitudeDeg: Double
)

data class GeodesicPoint(
    val latDeg: Double,
    val lonDeg: Double
)

data class ProjectedSpherePoint(
    val x: Float, // -1..1
    val y: Float, // -1..1
    val z: Float, // > 0 means visible front hemisphere
    val isVisible: Boolean
)

data class GlobeCityBeacon(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val lat: Double,
    val lon: Double,
    val tzId: String,
    val isDaylight: Boolean,
    val isAdhanActiveNow: Boolean,
    val currentPhaseLabelAr: String,
    val currentPhaseLabelEn: String
)

/**
 * Real-Time 3D Spherical Earth, Solar Day/Night Terminator, Geodesic Great-Circle Qibla Arc,
 * and Global Prayer Wave Astronomy Engine.
 */
object CosmicEarthGlobeEngine {

    const val KAABA_LAT = 21.4225
    const val KAABA_LON = 39.8262

    private data class CitySeed(val id: String, val ar: String, val en: String, val lat: Double, val lon: Double, val tz: String)

    private val globalBeacons = listOf(
        CitySeed("makkah", "مكة المكرمة", "Makkah", 21.4225, 39.8262, "Asia/Riyadh"),
        CitySeed("madinah", "المدينة المنورة", "Madinah", 24.4672, 39.6112, "Asia/Riyadh"),
        CitySeed("quds", "القدس", "Al-Quds", 31.7767, 35.2345, "Asia/Jerusalem"),
        CitySeed("cairo", "القاهرة", "Cairo", 30.0444, 31.2357, "Africa/Cairo"),
        CitySeed("istanbul", "إسطنبول", "Istanbul", 41.0082, 28.9784, "Europe/Istanbul"),
        CitySeed("tashkent", "طشقند", "Tashkent", 41.2995, 69.2401, "Asia/Tashkent"),
        CitySeed("riyadh", "الرياض", "Riyadh", 24.7136, 46.6753, "Asia/Riyadh"),
        CitySeed("casablanca", "الدار البيضاء", "Casablanca", 33.5731, -7.5898, "Africa/Casablanca"),
        CitySeed("london", "لندن", "London", 51.5072, -0.1276, "Europe/London"),
        CitySeed("newyork", "نيويورك", "New York", 40.7128, -74.0060, "America/New_York"),
        CitySeed("karachi", "كراتشي", "Karachi", 24.8607, 67.0011, "Asia/Karachi"),
        CitySeed("kl", "كوالالمبور", "Kuala Lumpur", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        CitySeed("jakarta", "جاكرتا", "Jakarta", -6.2088, 106.8456, "Asia/Jakarta"),
        CitySeed("tokyo", "طوكيو", "Tokyo", 35.6762, 139.6503, "Asia/Tokyo")
    )

    /**
     * Computes the real-time geographic coordinate where the Sun is directly overhead right now.
     */
    fun computeSubsolarPoint(epochMillis: Long): SubsolarPoint {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = epochMillis }
        val dayOfYear = utc.get(Calendar.DAY_OF_YEAR)
        val utcHours = utc.get(Calendar.HOUR_OF_DAY) +
            utc.get(Calendar.MINUTE) / 60.0 +
            utc.get(Calendar.SECOND) / 3600.0

        // Solar declination approximation (accurate within ~0.3 deg)
        val declDeg = -23.44 * cos(2.0 * PI * (dayOfYear + 10.0) / 365.24)
        // Subsolar longitude: 12:00 UTC -> 0° lon; Earth rotates 15°/hour westward
        var lonDeg = (12.0 - utcHours) * 15.0
        while (lonDeg > 180.0) lonDeg -= 360.0
        while (lonDeg < -180.0) lonDeg += 360.0
        return SubsolarPoint(declDeg, lonDeg)
    }

    /**
     * Returns true if the given (lat, lon) is illuminated by the Sun (daylight hemisphere).
     */
    fun isPointInDaylight(latDeg: Double, lonDeg: Double, sun: SubsolarPoint): Boolean {
        val lat1 = Math.toRadians(latDeg)
        val lat2 = Math.toRadians(sun.latitudeDeg)
        val dLon = Math.toRadians(lonDeg - sun.longitudeDeg)
        val cosZenith = sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(dLon)
        return cosZenith >= -0.015 // includes slight atmospheric refraction
    }

    /**
     * Orthographic 3D projection of a sphere point (lat, lon) viewed from camera (centerLat, centerLon).
     */
    fun projectOrthographic(
        latDeg: Double,
        lonDeg: Double,
        centerLatDeg: Double,
        centerLonDeg: Double
    ): ProjectedSpherePoint {
        val phi = Math.toRadians(latDeg)
        val phi0 = Math.toRadians(centerLatDeg)
        val dLam = Math.toRadians(lonDeg - centerLonDeg)

        val x = (cos(phi) * sin(dLam)).toFloat()
        val y = (-(cos(phi0) * sin(phi) - sin(phi0) * cos(phi) * cos(dLam))).toFloat()
        val z = (sin(phi0) * sin(phi) + cos(phi0) * cos(phi) * cos(dLam)).toFloat()
        return ProjectedSpherePoint(x, y, z, z >= 0f)
    }

    /**
     * Generates smooth spherical slerp (Great-Circle Geodesic) waypoints from any city to the Holy Kaaba.
     */
    fun computeGeodesicToKaaba(startLat: Double, startLon: Double, steps: Int = 36): List<GeodesicPoint> {
        val lat1 = Math.toRadians(startLat)
        val lon1 = Math.toRadians(startLon)
        val lat2 = Math.toRadians(KAABA_LAT)
        val lon2 = Math.toRadians(KAABA_LON)

        val x1 = cos(lat1) * cos(lon1)
        val y1 = cos(lat1) * sin(lon1)
        val z1 = sin(lat1)

        val x2 = cos(lat2) * cos(lon2)
        val y2 = cos(lat2) * sin(lon2)
        val z2 = sin(lat2)

        val dot = (x1 * x2 + y1 * y2 + z1 * z2).coerceIn(-1.0, 1.0)
        val omega = kotlin.math.acos(dot)

        if (omega < 1e-4) {
            return listOf(GeodesicPoint(startLat, startLon), GeodesicPoint(KAABA_LAT, KAABA_LON))
        }

        val sinOmega = sin(omega)
        val result = mutableListOf<GeodesicPoint>()
        for (i in 0..steps) {
            val t = i.toDouble() / steps.toDouble()
            val a = sin((1.0 - t) * omega) / sinOmega
            val b = sin(t * omega) / sinOmega
            val x = a * x1 + b * x2
            val y = a * y1 + b * y2
            val z = a * z1 + b * z2
            val lat = Math.toDegrees(atan2(z, sqrt(x * x + y * y)))
            val lon = Math.toDegrees(atan2(y, x))
            result += GeodesicPoint(lat, lon)
        }
        return result
    }

    /**
     * Computes exact initial Qibla compass bearing in degrees clockwise from True North (0..360).
     */
    fun qiblaBearingDegrees(latDeg: Double, lonDeg: Double): Double {
        val phi1 = Math.toRadians(latDeg)
        val phi2 = Math.toRadians(KAABA_LAT)
        val dLam = Math.toRadians(KAABA_LON - lonDeg)
        val y = sin(dLam) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLam)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /**
     * Exact Great-Circle Haversine distance in km to the Holy Kaaba.
     */
    fun distanceToKaabaKm(latDeg: Double, lonDeg: Double): Double {
        val dLat = Math.toRadians(KAABA_LAT - latDeg)
        val dLon = Math.toRadians(KAABA_LON - lonDeg)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(latDeg)) * cos(Math.toRadians(KAABA_LAT)) * sin(dLon / 2).pow(2)
        return 6371.0 * 2.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /**
     * Evaluates live global city prayer & daylight beacons for the 3D Earth Globe.
     */
    fun evaluateGlobalBeacons(epochMillis: Long): List<GlobeCityBeacon> {
        val sun = computeSubsolarPoint(epochMillis)
        return globalBeacons.map { c ->
            val tz = TimeZone.getTimeZone(c.tz)
            val cal = Calendar.getInstance(tz).apply { timeInMillis = epochMillis }
            val schedule = PrayerTimeCalculator.calculateForDate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH),
                latitude = c.lat,
                longitude = c.lon,
                timeZone = tz,
                method = CalculationMethod.MWL,
                madhhab = AsrMadhhab.SHAFI
            )
            val prayers = listOf(
                Triple("الفجر", "Fajr", schedule.fajrMillis),
                Triple("الظهر", "Dhuhr", schedule.dhuhrMillis),
                Triple("العصر", "Asr", schedule.asrMillis),
                Triple("المغرب", "Maghrib", schedule.maghribMillis),
                Triple("العشاء", "Isha", schedule.ishaMillis)
            )
            val activeAdhan = prayers.firstOrNull { kotlin.math.abs(epochMillis - it.third) <= 15 * 60_000L }
            val daylight = isPointInDaylight(c.lat, c.lon, sun)
            val phaseAr = activeAdhan?.let { "أذان ${it.first} الآن" } ?: if (daylight) "نهار" else "ليل"
            val phaseEn = activeAdhan?.let { "${it.second} Adhan Now" } ?: if (daylight) "Day" else "Night"

            GlobeCityBeacon(
                id = c.id,
                nameAr = c.ar,
                nameEn = c.en,
                lat = c.lat,
                lon = c.lon,
                tzId = c.tz,
                isDaylight = daylight,
                isAdhanActiveNow = activeAdhan != null,
                currentPhaseLabelAr = phaseAr,
                currentPhaseLabelEn = phaseEn
            )
        }
    }

    /**
     * Smooth normalized sunlight factor in [0f, 1f]: 0f = deep night, 0.5f = twilight terminator, 1f = solar zenith.
     */
    fun sunlightFactor(latDeg: Double, lonDeg: Double, sun: SubsolarPoint): Float {
        val lat1 = Math.toRadians(latDeg)
        val lat2 = Math.toRadians(sun.latitudeDeg)
        val dLon = Math.toRadians(lonDeg - sun.longitudeDeg)
        val cosZenith = sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(dLon)
        return ((cosZenith + 0.18) / 1.18).toFloat().coerceIn(0f, 1f)
    }
}

data class GeoContinentPolygon(
    val id: String,
    val nameEn: String,
    val coordinates: List<GeodesicPoint>, // (lat, lon)
    val isDesertBelt: Boolean = false
)

/**
 * Genuine geographic coastline coordinates (latitude, longitude) for Earth's major landmasses,
 * enabling real-time 3D spherical projection and Vector-Satellite relief without static bitmap images.
 */
object WorldCoastlinesCatalog {
    private fun poly(id: String, name: String, desert: Boolean = false, vararg pts: Pair<Double, Double>) =
        GeoContinentPolygon(
            id = id,
            nameEn = name,
            coordinates = pts.map { (lat, lon) -> GeodesicPoint(lat, lon) },
            isDesertBelt = desert
        )

    val continents: List<GeoContinentPolygon> = listOf(
        // Africa
        poly(
            "africa", "Africa", desert = true,
            35.8 to -5.4, 36.8 to 3.0, 37.3 to 10.2, 32.9 to 13.2, 31.2 to 20.1,
            31.4 to 32.3, 27.9 to 34.5, 22.0 to 36.8, 15.6 to 39.4, 12.6 to 43.3,
            11.8 to 51.3, 4.5 to 48.0, 2.0 to 45.3, -1.6 to 41.5, -6.8 to 39.3,
            -10.5 to 40.5, -17.8 to 37.0, -25.9 to 32.6, -29.9 to 31.0, -34.8 to 20.0,
            -33.9 to 18.4, -26.6 to 15.1, -17.2 to 11.8, -12.3 to 13.5, -5.9 to 12.2,
            0.4 to 9.4, 4.0 to 9.7, 6.4 to 3.4, 5.5 to -0.2, 4.4 to -7.5,
            6.3 to -10.8, 8.5 to -13.2, 14.7 to -17.4, 21.0 to -17.0, 27.1 to -13.2,
            33.6 to -7.6
        ),
        // Madagascar
        poly(
            "madagascar", "Madagascar", desert = false,
            -12.0 to 49.3, -15.4 to 50.4, -20.3 to 48.6, -25.1 to 47.0, -25.4 to 44.2,
            -21.0 to 43.9, -15.7 to 46.3, -13.4 to 48.2
        ),
        // Arabian Peninsula & Levant
        poly(
            "arabia", "Arabian Peninsula", desert = true,
            31.5 to 34.5, 36.8 to 36.2, 37.1 to 41.5, 33.8 to 46.5, 30.0 to 48.5,
            26.2 to 50.6, 24.4 to 54.4, 25.3 to 56.4, 22.6 to 59.8, 17.0 to 54.1,
            15.0 to 50.5, 12.8 to 45.0, 12.7 to 43.5, 16.9 to 42.5, 21.5 to 39.1,
            28.5 to 34.8
        ),
        // Europe
        poly(
            "europe", "Europe", desert = false,
            36.0 to -5.6, 37.0 to -8.9, 43.5 to -9.2, 43.3 to -1.8, 47.5 to -3.0,
            49.5 to -1.9, 51.0 to 2.0, 53.5 to 8.6, 57.1 to 8.6, 55.6 to 12.6,
            60.4 to 5.3, 65.5 to 12.2, 70.6 to 23.6, 70.2 to 31.1, 66.5 to 41.0,
            68.5 to 56.0, 62.0 to 60.0, 54.0 to 58.0, 47.0 to 40.0, 44.6 to 33.5,
            46.4 to 30.7, 41.0 to 29.0, 40.6 to 22.9, 37.9 to 23.7, 36.4 to 22.5,
            39.6 to 19.9, 44.4 to 12.3, 40.4 to 18.1, 38.0 to 15.6, 41.9 to 12.5,
            44.4 to 8.9, 43.3 to 5.4, 41.4 to 2.2, 39.5 to -0.3, 36.7 to -4.4
        ),
        // Great Britain & Ireland
        poly(
            "britain", "Great Britain", desert = false,
            50.1 to -5.5, 51.4 to -3.2, 54.6 to -3.6, 58.6 to -5.0, 58.6 to -3.1,
            57.1 to -2.1, 55.0 to -1.4, 52.9 to 1.7, 51.1 to 1.4
        ),
        // Eurasia / Main Asia + India + SE Asia
        poly(
            "asia", "Asia", desert = false,
            37.0 to 27.2, 41.2 to 29.1, 41.0 to 40.0, 44.5 to 38.0, 47.0 to 40.0,
            54.0 to 58.0, 62.0 to 60.0, 68.5 to 56.0, 72.5 to 72.0, 77.5 to 104.0,
            73.0 to 126.0, 71.0 to 150.0, 66.0 to 179.0, 60.0 to 165.0, 52.0 to 156.5,
            53.0 to 141.0, 43.1 to 131.9, 39.8 to 124.2, 35.1 to 129.0, 37.5 to 126.5,
            39.0 to 121.6, 36.0 to 120.3, 31.2 to 121.9, 25.0 to 119.5, 22.3 to 114.2,
            20.0 to 106.5, 12.2 to 109.2, 8.6 to 104.7, 13.7 to 100.5, 1.3 to 103.8,
            5.4 to 100.3, 16.8 to 96.2, 22.3 to 91.8, 20.3 to 86.7, 13.1 to 80.3,
            8.1 to 77.5, 19.1 to 72.9, 23.0 to 68.9, 24.9 to 67.0, 25.3 to 60.5,
            27.1 to 56.3, 30.0 to 48.5, 33.8 to 46.5, 37.1 to 41.5, 36.8 to 36.2,
            36.2 to 29.6
        ),
        // Japan
        poly(
            "japan", "Japan", desert = false,
            31.4 to 130.6, 34.4 to 132.4, 35.7 to 139.8, 38.3 to 141.0, 41.5 to 141.5,
            45.4 to 141.9, 43.0 to 145.0, 40.5 to 140.0, 35.1 to 136.9, 33.6 to 130.4
        ),
        // Sumatra & Java / Borneo
        poly(
            "indonesia_west", "Sumatra & Java", desert = false,
            5.5 to 95.3, 1.5 to 102.0, -5.9 to 105.9, -8.4 to 114.4, -7.8 to 110.3,
            -6.1 to 105.5, 0.5 to 98.5
        ),
        poly(
            "borneo", "Borneo", desert = false,
            6.9 to 117.0, 1.0 to 118.9, -3.5 to 116.0, -3.2 to 110.2, 1.6 to 110.3,
            5.9 to 116.1
        ),
        // Australia
        poly(
            "australia", "Australia", desert = true,
            -12.4 to 130.8, -11.0 to 142.5, -19.2 to 146.8, -27.5 to 153.4, -33.9 to 151.2,
            -37.8 to 149.9, -38.4 to 144.6, -34.9 to 138.6, -32.0 to 133.6, -33.9 to 121.9,
            -32.0 to 115.8, -21.8 to 114.1, -17.9 to 122.2, -14.5 to 128.8
        ),
        // North America
        poly(
            "north_america", "North America", desert = false,
            71.3 to -156.8, 69.5 to -138.0, 68.0 to -110.0, 70.0 to -85.0, 62.5 to -77.5,
            53.5 to -55.8, 46.8 to -53.0, 44.6 to -63.6, 42.3 to -71.0, 40.7 to -74.0,
            35.2 to -75.5, 25.8 to -80.2, 24.6 to -81.8, 29.9 to -85.4, 29.3 to -89.4,
            26.0 to -97.2, 19.2 to -96.1, 21.5 to -87.1, 15.8 to -84.0, 9.0 to -79.5,
            7.4 to -77.8, 14.0 to -91.5, 19.0 to -104.3, 24.0 to -110.0, 32.7 to -117.2,
            37.8 to -122.5, 48.4 to -124.7, 57.0 to -135.3, 61.2 to -149.9, 55.2 to -162.7,
            64.5 to -165.4
        ),
        // Greenland
        poly(
            "greenland", "Greenland", desert = false,
            60.1 to -44.2, 65.6 to -37.6, 70.5 to -22.0, 81.5 to -18.0, 83.2 to -34.0,
            78.0 to -72.0, 69.2 to -51.1, 64.2 to -51.7
        ),
        // South America
        poly(
            "south_america", "South America", desert = false,
            12.2 to -71.3, 10.5 to -66.9, 6.8 to -58.2, 1.5 to -50.0, -1.5 to -45.0,
            -5.8 to -35.2, -13.0 to -38.5, -22.9 to -43.2, -25.5 to -48.5, -34.9 to -56.2,
            -42.8 to -64.9, -52.3 to -68.4, -55.0 to -67.0, -52.5 to -75.0, -41.5 to -73.6,
            -33.0 to -71.6, -23.6 to -70.4, -18.4 to -70.3, -12.0 to -77.1, -5.0 to -81.3,
            -2.2 to -79.9, 3.9 to -77.1, 8.9 to -76.5
        )
    )
}
