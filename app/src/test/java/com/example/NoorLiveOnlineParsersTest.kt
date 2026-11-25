package com.example

import com.example.data.remote.NoorLiveOnlineService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoorLiveOnlineParsersTest {

    @Test
    fun parseOpenMeteoWeather_extractsTemperatureHumidityWindAndCode() {
        val sample = """
            {
              "current": {
                "temperature_2m": 34.6,
                "relative_humidity_2m": 42,
                "wind_speed_10m": 14.5,
                "weather_code": 0
              }
            }
        """.trimIndent()

        val parsed = NoorLiveOnlineService.parseOpenMeteoWeather("makkah", sample)
        assertEquals("makkah", parsed.cityKey)
        assertEquals(34.6, parsed.temperatureC, 0.001)
        assertEquals(42, parsed.humidityPercent)
        assertEquals(14.5, parsed.windKmh, 0.001)
        assertEquals("سماء صافية", parsed.conditionAr())
        assertEquals("Ochiq osmon", parsed.conditionUz())
    }

    @Test
    fun parseQuranSearchResponse_parsesSurahAndAyahMatches() {
        val sample = """
            {
              "code": 200,
              "data": {
                "count": 1,
                "matches": [
                  {
                    "number": 262,
                    "numberInSurah": 255,
                    "text": "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ",
                    "surah": {
                      "number": 2,
                      "name": "سُورَةُ البَقَرَةِ",
                      "englishName": "Al-Baqara"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val hits = NoorLiveOnlineService.parseQuranSearchResponse(sample)
        assertEquals(1, hits.size)
        assertEquals(2, hits[0].surahNumber)
        assertEquals(255, hits[0].ayahNumberInSurah)
        assertEquals("Al-Baqara", hits[0].surahNameEn)
    }

    @Test
    fun parseAyahMultiTafsir_extractsUthmaniMuyassarJalalaynEnglishUzbekAndAudio() {
        val sample = """
            {
              "code": 200,
              "data": [
                {"numberInSurah": 1, "text": "بِسْمِ ٱللَّهِ", "surah": {"number": 1, "name": "سورة الفاتحة", "englishName": "Al-Fatiha"}, "edition": {"identifier": "quran-uthmani"}},
                {"numberInSurah": 1, "text": "تفسير ميسر", "edition": {"identifier": "ar.muyassar"}},
                {"numberInSurah": 1, "text": "تفسير الجلالين", "edition": {"identifier": "ar.jalalayn"}},
                {"numberInSurah": 1, "text": "In the name of Allah", "edition": {"identifier": "en.sahih"}},
                {"numberInSurah": 1, "text": "Mehribon va rahmli Alloh nomi bilan", "edition": {"identifier": "uz.sodik"}},
                {"numberInSurah": 1, "audio": "https://cdn.islamic.network/quran/audio/128/ar.alafasy/1.mp3", "edition": {"identifier": "ar.alafasy"}}
              ]
            }
        """.trimIndent()

        val bundle = NoorLiveOnlineService.parseAyahMultiTafsir(sample)
        assertEquals(1, bundle.surahNumber)
        assertEquals(1, bundle.ayahNumber)
        assertEquals("تفسير ميسر", bundle.tafsirMuyassarAr)
        assertEquals("تفسير الجلالين", bundle.tafsirJalalaynAr)
        assertEquals("In the name of Allah", bundle.englishSahih)
        assertEquals("Mehribon va rahmli Alloh nomi bilan", bundle.uzbekSodik)
        assertTrue(bundle.audioUrl.endsWith("/1.mp3"))
    }

    @Test
    fun parseHadithEncResponses_extractsCategoriesListAndDetail() {
        val catsJson = """[{"id":"1","title":"القرآن وعلومه","hadeeths_count":"120"}]"""
        val cats = NoorLiveOnlineService.parseHadithCategories(catsJson)
        assertEquals(1, cats.size)
        assertEquals(120, cats[0].hadithsCount)

        val detailJson = """
            {
              "id": "55",
              "title": "إنما الأعمال بالنيات",
              "hadeeth": "إنما الأعمال بالنيات",
              "hadeeth_ar": "إنما الأعمال بالنيات",
              "attribution": "متفق عليه",
              "grade": "صحيح",
              "explanation": "النية شرط لقبول العمل",
              "hints": ["إخلاص النية لله", "العمل بغير نية لا يُقبل"]
            }
        """.trimIndent()
        val detail = NoorLiveOnlineService.parseHadithDetail(detailJson)
        assertEquals("صحيح", detail.grade)
        assertEquals("متفق عليه", detail.attribution)
        assertEquals(2, detail.hints.size)
    }

    @Test
    fun computeMetalPricesFromSpot_calculatesExact24k21k18kSilverAndNisab() {
        // Suppose XAU = 3110.34768 USD/oz -> $100.00/g 24K -> SAR (rate 3.75) = 375.00 SAR/g
        // 21K = 375 * 21/24 = 328.125 SAR/g, 18K = 375 * 18/24 = 281.25 SAR/g
        // XAG = 31.1034768 USD/oz -> $1.00/g -> 3.75 SAR/g
        val live = NoorLiveOnlineService.computeMetalPricesFromSpot(
            currencyCode = "SAR",
            goldOunceUsd = 3110.34768,
            silverOunceUsd = 31.1034768,
            usdToTargetRate = 3.75
        )
        assertEquals(375.0, live.gold24kPerGram, 0.01)
        assertEquals(328.125, live.gold21kPerGram, 0.01)
        assertEquals(281.25, live.gold18kPerGram, 0.01)
        assertEquals(3.75, live.silverPerGram, 0.01)
        assertEquals(375.0 * 85.0, live.goldNisab85g, 0.01)
        assertEquals(3.75 * 595.0, live.silverNisab595g, 0.01)
    }
}
