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
}
