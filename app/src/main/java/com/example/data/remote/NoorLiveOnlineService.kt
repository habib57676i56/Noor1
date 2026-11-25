package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

// ===================== 1. LIVE HARAMAIN WEATHER =====================

data class CityLiveWeather(
    val cityKey: String,
    val temperatureC: Double,
    val humidityPercent: Int,
    val windKmh: Double,
    val weatherCode: Int
) {
    fun conditionAr(): String = when (weatherCode) {
        0 -> "سماء صافية"
        1, 2 -> "غائم جزئياً"
        3 -> "غائم"
        45, 48 -> "ضباب خفيف"
        51, 53, 55, 61, 63, 65 -> "أمطار رحمة"
        80, 81, 82 -> "زخات مطر"
        95, 96, 99 -> "عاصفة رعدية"
        else -> "معتدل"
    }

    fun conditionEn(): String = when (weatherCode) {
        0 -> "Clear Sky"
        1, 2 -> "Partly Cloudy"
        3 -> "Cloudy"
        45, 48 -> "Mist"
        51, 53, 55, 61, 63, 65 -> "Rain"
        80, 81, 82 -> "Rain Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Fair"
    }

    fun conditionUz(): String = when (weatherCode) {
        0 -> "Ochiq osmon"
        1, 2 -> "Qisman bulutli"
        3 -> "Bulutli"
        45, 48 -> "Tuman"
        51, 53, 55, 61, 63, 65 -> "Yomg'ir"
        80, 81, 82 -> "Jala"
        95, 96, 99 -> "Momaqaldiroq"
        else -> "Mo'tadil"
    }
}

data class HaramainWeatherBundle(
    val makkah: CityLiveWeather,
    val madinah: CityLiveWeather
)

// ===================== 2. GLOBAL QURAN SEARCH & MULTI-TAFSIR =====================

data class QuranSearchHit(
    val surahNumber: Int,
    val surahNameAr: String,
    val surahNameEn: String,
    val ayahNumberInSurah: Int,
    val globalAyahNumber: Int,
    val matchedText: String
)

data class AyahMultiTafsirBundle(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahNameAr: String,
    val surahNameEn: String,
    val uthmaniText: String,
    val tafsirMuyassarAr: String,
    val tafsirJalalaynAr: String,
    val englishSahih: String,
    val uzbekSodik: String,
    val audioUrl: String
)

// ===================== 3. LIVE HADITH ENCYCLOPEDIA (HadeethEnc) =====================

data class LiveHadithCategory(
    val id: String,
    val title: String,
    val hadithsCount: Int
)

data class LiveHadithSummary(
    val id: String,
    val title: String
)

data class LiveHadithDetail(
    val id: String,
    val title: String,
    val hadithText: String,
    val arabicText: String,
    val attribution: String,
    val grade: String,
    val explanation: String,
    val hints: List<String>,
    val reference: String
)

// ===================== 4. LIVE GOLD, SILVER & NISAB MARKET RATES =====================

data class LiveMetalPrices(
    val currencyCode: String,
    val gold24kPerGram: Double,
    val gold21kPerGram: Double,
    val gold18kPerGram: Double,
    val silverPerGram: Double,
    val goldNisab85g: Double,
    val silverNisab595g: Double,
    val usdRate: Double
)

/**
 * Real live-internet API client for NOOR's online flagship modules:
 * - Open-Meteo (Live Makkah & Madinah weather)
 * - AlQuran.cloud (Full 6,236-Ayah Search + Multi-Tafsir + Uzbek Shaykh Sodiq + Audio)
 * - HadeethEnc.com (Live Authentic Hadith Encyclopedia in Arabic, English & Uzbek)
 * - Gold-API + Open ER-API (Real-time World Gold 24K/21K/18K, Silver & Nisab in 7 currencies)
 */
object NoorLiveOnlineService {

    const val GRAMS_PER_TROY_OUNCE = 31.1034768

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private fun getStringSync(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "NOOR-Islamic-Android/1.0")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            return resp.body?.string() ?: error("Empty response body")
        }
    }

    // ---------- 1. LIVE HARAMAIN WEATHER ----------

    suspend fun fetchHaramainLiveWeather(): Result<HaramainWeatherBundle> = withContext(Dispatchers.IO) {
        runCatching {
            val makkahJson = getStringSync(
                "https://api.open-meteo.com/v1/forecast?latitude=21.4225&longitude=39.8262&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code"
            )
            val madinahJson = getStringSync(
                "https://api.open-meteo.com/v1/forecast?latitude=24.4672&longitude=39.6112&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code"
            )
            HaramainWeatherBundle(
                makkah = parseOpenMeteoWeather("makkah", makkahJson),
                madinah = parseOpenMeteoWeather("madinah", madinahJson)
            )
        }
    }

    internal fun parseOpenMeteoWeather(cityKey: String, json: String): CityLiveWeather {
        val current = JSONObject(json).getJSONObject("current")
        return CityLiveWeather(
            cityKey = cityKey,
            temperatureC = current.optDouble("temperature_2m", 0.0),
            humidityPercent = current.optInt("relative_humidity_2m", 0),
            windKmh = current.optDouble("wind_speed_10m", 0.0),
            weatherCode = current.optInt("weather_code", 0)
        )
    }

    // ---------- 2. GLOBAL FULL-QURAN SEARCH & MULTI-TAFSIR ----------

    suspend fun searchFullQuran(query: String, edition: String = "quran-simple-clean"): Result<List<QuranSearchHit>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val trimmed = query.trim()
                if (trimmed.isEmpty()) return@runCatching emptyList()
                val encoded = URLEncoder.encode(trimmed, "UTF-8").replace("+", "%20")
                val url = "https://api.alquran.cloud/v1/search/$encoded/all/$edition"
                val raw = getStringSync(url)
                parseQuranSearchResponse(raw)
            }
        }

    internal fun parseQuranSearchResponse(json: String): List<QuranSearchHit> {
        val root = JSONObject(json)
        if (root.optInt("code", 0) != 200) return emptyList()
        val data = root.optJSONObject("data") ?: return emptyList()
        val matches = data.optJSONArray("matches") ?: return emptyList()
        val results = mutableListOf<QuranSearchHit>()
        for (i in 0 until matches.length()) {
            val item = matches.optJSONObject(i) ?: continue
            val surah = item.optJSONObject("surah") ?: continue
            results += QuranSearchHit(
                surahNumber = surah.optInt("number", 1),
                surahNameAr = surah.optString("name", ""),
                surahNameEn = surah.optString("englishName", ""),
                ayahNumberInSurah = item.optInt("numberInSurah", 1),
                globalAyahNumber = item.optInt("number", 1),
                matchedText = item.optString("text", "")
            )
        }
        return results
    }

    suspend fun fetchAyahMultiTafsir(surahNumber: Int, ayahNumber: Int): Result<AyahMultiTafsirBundle> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url =
                    "https://api.alquran.cloud/v1/ayah/$surahNumber:$ayahNumber/editions/quran-uthmani,ar.muyassar,ar.jalalayn,en.sahih,uz.sodik,ar.alafasy"
                val raw = getStringSync(url)
                parseAyahMultiTafsir(raw)
            }
        }

    internal fun parseAyahMultiTafsir(json: String): AyahMultiTafsirBundle {
        val root = JSONObject(json)
        val data = root.getJSONArray("data")
        var surahNum = 1
        var ayahNum = 1
        var surahAr = ""
        var surahEn = ""
        var uthmani = ""
        var muyassar = ""
        var jalalayn = ""
        var english = ""
        var uzbek = ""
        var audio = ""

        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            val editionId = item.optJSONObject("edition")?.optString("identifier").orEmpty()
            val surahObj = item.optJSONObject("surah")
            if (surahObj != null) {
                surahNum = surahObj.optInt("number", surahNum)
                surahAr = surahObj.optString("name", surahAr)
                surahEn = surahObj.optString("englishName", surahEn)
            }
            ayahNum = item.optInt("numberInSurah", ayahNum)
            when (editionId) {
                "quran-uthmani" -> uthmani = item.optString("text", "")
                "ar.muyassar" -> muyassar = item.optString("text", "")
                "ar.jalalayn" -> jalalayn = item.optString("text", "")
                "en.sahih" -> english = item.optString("text", "")
                "uz.sodik" -> uzbek = item.optString("text", "")
                "ar.alafasy" -> audio = item.optString("audio", "")
            }
        }
        return AyahMultiTafsirBundle(
            surahNumber = surahNum,
            ayahNumber = ayahNum,
            surahNameAr = surahAr,
            surahNameEn = surahEn,
            uthmaniText = uthmani,
            tafsirMuyassarAr = muyassar,
            tafsirJalalaynAr = jalalayn,
            englishSahih = english,
            uzbekSodik = uzbek,
            audioUrl = audio
        )
    }

    // ---------- 3. LIVE HADITH ENCYCLOPEDIA (HadeethEnc.com) ----------

    suspend fun fetchHadithCategories(langCode: String): Result<List<LiveHadithCategory>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val safeLang = langCode.takeIf { it in setOf("ar", "en", "uz") } ?: "ar"
                val raw = getStringSync("https://hadeethenc.com/api/v1/categories/roots/?language=$safeLang")
                parseHadithCategories(raw)
            }
        }

    internal fun parseHadithCategories(json: String): List<LiveHadithCategory> {
        val arr = JSONArray(json)
        val out = mutableListOf<LiveHadithCategory>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            out += LiveHadithCategory(
                id = obj.optString("id", ""),
                title = obj.optString("title", ""),
                hadithsCount = obj.optString("hadeeths_count", "0").toIntOrNull() ?: 0
            )
        }
        return out
    }

    suspend fun fetchHadithsByCategory(
        langCode: String,
        categoryId: String,
        page: Int = 1,
        perPage: Int = 25
    ): Result<List<LiveHadithSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val safeLang = langCode.takeIf { it in setOf("ar", "en", "uz") } ?: "ar"
            val raw = getStringSync(
                "https://hadeethenc.com/api/v1/hadeeths/list/?language=$safeLang&category_id=$categoryId&page=$page&per_page=$perPage"
            )
            parseHadithList(raw)
        }
    }

    internal fun parseHadithList(json: String): List<LiveHadithSummary> {
        val data = JSONObject(json).optJSONArray("data") ?: return emptyList()
        val out = mutableListOf<LiveHadithSummary>()
        for (i in 0 until data.length()) {
            val obj = data.optJSONObject(i) ?: continue
            out += LiveHadithSummary(
                id = obj.optString("id", ""),
                title = obj.optString("title", "")
            )
        }
        return out
    }

    suspend fun fetchHadithDetail(langCode: String, hadithId: String): Result<LiveHadithDetail> =
        withContext(Dispatchers.IO) {
            runCatching {
                val safeLang = langCode.takeIf { it in setOf("ar", "en", "uz") } ?: "ar"
                val raw = getStringSync("https://hadeethenc.com/api/v1/hadeeths/one/?language=$safeLang&id=$hadithId")
                parseHadithDetail(raw)
            }
        }

    internal fun parseHadithDetail(json: String): LiveHadithDetail {
        val obj = JSONObject(json)
        val hintsArr = obj.optJSONArray("hints")
        val hints = mutableListOf<String>()
        if (hintsArr != null) {
            for (i in 0 until hintsArr.length()) {
                val h = hintsArr.optString(i, "").trim()
                if (h.isNotEmpty()) hints += h
            }
        }
        val primaryText = obj.optString("hadeeth", "")
        val arabicText = obj.optString("hadeeth_ar", "").ifBlank { primaryText }
        return LiveHadithDetail(
            id = obj.optString("id", ""),
            title = obj.optString("title", ""),
            hadithText = primaryText,
            arabicText = arabicText,
            attribution = obj.optString("attribution", ""),
            grade = obj.optString("grade", ""),
            explanation = obj.optString("explanation", ""),
            hints = hints,
            reference = obj.optString("reference", "")
        )
    }

    // ---------- 4. LIVE GOLD, SILVER & EXCHANGE RATES ----------

    suspend fun fetchLivePreciousMetalsPrices(currencyCode: String): Result<LiveMetalPrices> =
        withContext(Dispatchers.IO) {
            runCatching {
                val goldJson = getStringSync("https://api.gold-api.com/price/XAU")
                val silverJson = getStringSync("https://api.gold-api.com/price/XAG")
                val goldOunceUsd = JSONObject(goldJson).getDouble("price")
                val silverOunceUsd = JSONObject(silverJson).getDouble("price")

                val rate = if (currencyCode.equals("USD", ignoreCase = true)) {
                    1.0
                } else {
                    val fxJson = getStringSync("https://open.er-api.com/v6/latest/USD")
                    JSONObject(fxJson).getJSONObject("rates").optDouble(currencyCode.uppercase(), 1.0)
                }

                computeMetalPricesFromSpot(
                    currencyCode = currencyCode.uppercase(),
                    goldOunceUsd = goldOunceUsd,
                    silverOunceUsd = silverOunceUsd,
                    usdToTargetRate = rate
                )
            }
        }

    internal fun computeMetalPricesFromSpot(
        currencyCode: String,
        goldOunceUsd: Double,
        silverOunceUsd: Double,
        usdToTargetRate: Double
    ): LiveMetalPrices {
        val safeRate = if (usdToTargetRate > 0.0) usdToTargetRate else 1.0
        val gold24kGram = (goldOunceUsd / GRAMS_PER_TROY_OUNCE) * safeRate
        val gold21kGram = gold24kGram * (21.0 / 24.0)
        val gold18kGram = gold24kGram * (18.0 / 24.0)
        val silverGram = (silverOunceUsd / GRAMS_PER_TROY_OUNCE) * safeRate
        return LiveMetalPrices(
            currencyCode = currencyCode,
            gold24kPerGram = gold24kGram,
            gold21kPerGram = gold21kGram,
            gold18kPerGram = gold18kGram,
            silverPerGram = silverGram,
            goldNisab85g = gold24kGram * 85.0,
            silverNisab595g = silverGram * 595.0,
            usdRate = safeRate
        )
    }

    // ---------- 5. LIVE QURAN.COM V4 WORD-BY-WORD MORPHOLOGY TOKENS ----------

    suspend fun fetchQuranWordByWord(surahNumber: Int, ayahNumber: Int): Result<QuranWordByWordVerse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url =
                    "https://api.quran.com/api/v4/verses/by_key/$surahNumber:$ayahNumber?language=en&words=true&word_fields=text_uthmani,location&fields=text_uthmani"
                val raw = getStringSync(url)
                parseQuranComWordByWord(raw)
            }
        }

    internal fun parseQuranComWordByWord(json: String): QuranWordByWordVerse {
        val verse = JSONObject(json).getJSONObject("verse")
        val fullText = verse.optString("text_uthmani", "")
        val wordsArr = verse.optJSONArray("words")
        val tokens = mutableListOf<QuranWordToken>()
        if (wordsArr != null) {
            var pos = 1
            for (i in 0 until wordsArr.length()) {
                val w = wordsArr.optJSONObject(i) ?: continue
                if (w.optString("char_type_name", "word") != "word") continue
                val uthmani = w.optString("text_uthmani", w.optString("text", ""))
                val translit = w.optJSONObject("transliteration")?.optString("text").orEmpty()
                val meaning = w.optJSONObject("translation")?.optString("text").orEmpty()
                if (uthmani.isNotBlank()) {
                    tokens += QuranWordToken(
                        position = pos++,
                        uthmaniWord = uthmani,
                        transliteration = translit,
                        meaningEn = meaning
                    )
                }
            }
        }
        return QuranWordByWordVerse(fullUthmaniText = fullText, words = tokens)
    }

    // ---------- 6. LIVE SKY & CRESCENT SIGHTING WEATHER (Open-Meteo) ----------

    suspend fun fetchSkySightingWeather(latitude: Double, longitude: Double): Result<SkySightingWeather> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url =
                    "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,cloud_cover,relative_humidity_2m&daily=sunrise,sunset&timezone=auto&forecast_days=1"
                val raw = getStringSync(url)
                parseSkySightingWeather(raw)
            }
        }

    internal fun parseSkySightingWeather(json: String): SkySightingWeather {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        val daily = root.optJSONObject("daily")
        val sunrise = daily?.optJSONArray("sunrise")?.optString(0).orEmpty().substringAfter("T", "")
        val sunset = daily?.optJSONArray("sunset")?.optString(0).orEmpty().substringAfter("T", "")
        return SkySightingWeather(
            temperatureC = current.optDouble("temperature_2m", 0.0),
            cloudCoverPercent = current.optInt("cloud_cover", 0).coerceIn(0, 100),
            humidityPercent = current.optInt("relative_humidity_2m", 0),
            sunriseLocal = sunrise,
            sunsetLocal = sunset
        )
    }
}

data class QuranWordToken(
    val position: Int,
    val uthmaniWord: String,
    val transliteration: String,
    val meaningEn: String
)

data class QuranWordByWordVerse(
    val fullUthmaniText: String,
    val words: List<QuranWordToken>
)

data class SkySightingWeather(
    val temperatureC: Double,
    val cloudCoverPercent: Int,
    val humidityPercent: Int,
    val sunriseLocal: String,
    val sunsetLocal: String
)
