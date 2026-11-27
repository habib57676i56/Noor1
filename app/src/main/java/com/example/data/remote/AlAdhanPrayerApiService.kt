package com.example.data.remote

import com.example.domain.model.AsrMadhhab
import com.example.domain.model.CalculationMethod
import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.PrayerMinuteOffsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Fetches verified official local prayer timings from the public AlAdhan API
 * (`https://api.aladhan.com/v1/timings/{dd-MM-yyyy}`) for the user's exact coordinates,
 * calculation authority, and juristic Asr school (`0 = Shafi'i/Standard`, `1 = Hanafi`),
 * with strict chronological validation and in-memory caching.
 *
 * Offline fallback is always provided instantaneously by [com.example.domain.prayer.PrayerTimeCalculator].
 */
object AlAdhanPrayerApiService {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private val cache = ConcurrentHashMap<String, DailyPrayerSchedule>()

    suspend fun fetchOfficialSchedule(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        timeZone: TimeZone,
        method: CalculationMethod,
        madhhab: AsrMadhhab
    ): DailyPrayerSchedule? = withContext(Dispatchers.IO) {
        val cacheKey = buildCacheKey(year, month, day, latitude, longitude, timeZone.id, method, madhhab)
        cache[cacheKey]?.let { return@withContext it }

        val url = buildTimingsUrl(
            year = year,
            month = month,
            day = day,
            latitude = latitude,
            longitude = longitude,
            method = method,
            madhhab = madhhab
        )

        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "NOOR-Islamic-Companion/4.2")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val parsed = parseTimingsResponse(
                    jsonBody = body,
                    year = year,
                    month = month,
                    day = day,
                    timeZone = timeZone
                )
                if (parsed != null) {
                    cache[cacheKey] = parsed
                }
                parsed
            }
        }.getOrNull()
    }

    fun applyOffsets(
        base: DailyPrayerSchedule,
        offsets: PrayerMinuteOffsets
    ): DailyPrayerSchedule {
        if (offsets.fajr == 0 && offsets.sunrise == 0 && offsets.dhuhr == 0 &&
            offsets.asr == 0 && offsets.maghrib == 0 && offsets.isha == 0
        ) {
            return base
        }
        return base.copy(
            fajrMillis = base.fajrMillis + offsets.fajr * 60_000L,
            sunriseMillis = base.sunriseMillis + offsets.sunrise * 60_000L,
            dhuhrMillis = base.dhuhrMillis + offsets.dhuhr * 60_000L,
            asrMillis = base.asrMillis + offsets.asr * 60_000L,
            maghribMillis = base.maghribMillis + offsets.maghrib * 60_000L,
            ishaMillis = base.ishaMillis + offsets.isha * 60_000L
        )
    }

    internal fun buildTimingsUrl(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod,
        madhhab: AsrMadhhab
    ): String {
        val dateStr = String.format(Locale.US, "%02d-%02d-%04d", day, month, year)
        val latStr = String.format(Locale.US, "%.4f", latitude)
        val lonStr = String.format(Locale.US, "%.4f", longitude)
        return "https://api.aladhan.com/v1/timings/$dateStr" +
            "?latitude=$latStr&longitude=$lonStr" +
            "&method=${method.aladhanMethodId}" +
            "&school=${madhhab.aladhanSchool}"
    }

    internal fun parseTimingsResponse(
        jsonBody: String,
        year: Int,
        month: Int,
        day: Int,
        timeZone: TimeZone
    ): DailyPrayerSchedule? {
        val afterTimingsKey = jsonBody.substringAfter("\"timings\"", "")
        if (afterTimingsKey.isBlank()) return null
        val timingsObject = afterTimingsKey.substringAfter("{", "").substringBefore("}", "")
        if (timingsObject.isBlank()) return null

        val fajr = parseHmToMillis(extractJsonString(timingsObject, "Fajr") ?: return null, year, month, day, timeZone) ?: return null
        val sunrise = parseHmToMillis(extractJsonString(timingsObject, "Sunrise") ?: return null, year, month, day, timeZone) ?: return null
        val dhuhr = parseHmToMillis(extractJsonString(timingsObject, "Dhuhr") ?: return null, year, month, day, timeZone) ?: return null
        val asr = parseHmToMillis(extractJsonString(timingsObject, "Asr") ?: return null, year, month, day, timeZone) ?: return null
        val maghrib = parseHmToMillis(extractJsonString(timingsObject, "Maghrib") ?: return null, year, month, day, timeZone) ?: return null
        val isha = parseHmToMillis(extractJsonString(timingsObject, "Isha") ?: return null, year, month, day, timeZone) ?: return null

        // Strict chronological sanity check before trusting remote response
        if (!(fajr < sunrise && sunrise < dhuhr && dhuhr < asr && asr < maghrib && maghrib < isha)) {
            return null
        }

        val midnightCal = Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
        return DailyPrayerSchedule(
            dateKey = dateKey,
            epochDay = midnightCal.timeInMillis / 86_400_000L,
            fajrMillis = fajr,
            sunriseMillis = sunrise,
            dhuhrMillis = dhuhr,
            asrMillis = asr,
            maghribMillis = maghrib,
            ishaMillis = isha
        )
    }

    private fun extractJsonString(jsonObjectBody: String, key: String): String? {
        val pattern = Regex(""""${Regex.escape(key)}"\s*:\s*"([^"]+)"""")
        return pattern.find(jsonObjectBody)?.groupValues?.getOrNull(1)
    }

    private fun parseHmToMillis(
        raw: String,
        year: Int,
        month: Int,
        day: Int,
        timeZone: TimeZone
    ): Long? {
        val clean = raw.trim().substringBefore(" ")
        val parts = clean.split(":")
        if (parts.size < 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        val cal = Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun buildCacheKey(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        tzId: String,
        method: CalculationMethod,
        madhhab: AsrMadhhab
    ): String {
        val latRound = String.format(Locale.US, "%.2f", latitude)
        val lonRound = String.format(Locale.US, "%.2f", longitude)
        return "$year-$month-$day|$latRound|$lonRound|$tzId|${method.key}|${madhhab.key}"
    }
}
