package com.example

import com.example.data.remote.NoorLiveOnlineService
import com.example.data.remote.QuranWordToken
import com.example.domain.astronomy.LunarAstronomyEngine
import com.example.domain.quran.QuranMorphologyIrabEngine
import com.example.domain.tasbih.BeadMaterialTimbre
import com.example.domain.tasbih.RealisticBeadSoundPlayer
import com.example.domain.tasbih.RealisticTasbihCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoorTasbihIrabAstronomyTest {

    // ---------- 1. REALISTIC 3D TASBIH: 20 LUXURY COLORS & PCM AUDIO ----------

    @Test
    fun realisticTasbihCatalog_hasExactly20UniqueLuxuryThemes() {
        val themes = RealisticTasbihCatalog.themes
        assertEquals(20, themes.size)
        assertEquals(20, themes.map { it.id }.toSet().size)
        themes.forEach { theme ->
            assertTrue("Arabic name blank for ${theme.id}", theme.nameAr.isNotBlank())
            assertTrue("English name blank for ${theme.id}", theme.nameEn.isNotBlank())
            assertTrue("Uzbek name blank for ${theme.id}", theme.nameUz.isNotBlank())
        }
        assertEquals("imperial_emerald", RealisticTasbihCatalog.byId("imperial_emerald").id)
        assertEquals(themes.first().id, RealisticTasbihCatalog.byId("non_existent_id").id)
    }

    @Test
    fun realisticBeadSoundPlayer_synthesizesNonZeroPcmForAllMaterialsAndMilestone() {
        BeadMaterialTimbre.entries.forEach { timbre ->
            val forward = RealisticBeadSoundPlayer.synthesizeBeadImpact(timbre, pitchFactor = 1.0, gain = 0.9)
            val reverse = RealisticBeadSoundPlayer.synthesizeBeadImpact(timbre, pitchFactor = 0.78, gain = 0.65)
            assertTrue("Forward PCM too short for $timbre", forward.size >= 512)
            assertEquals(forward.size, reverse.size)
            assertTrue("Forward PCM silent for $timbre", forward.any { abs(it.toInt()) > 1000 })
            assertTrue("Reverse PCM silent for $timbre", reverse.any { abs(it.toInt()) > 500 })
        }

        val chime = RealisticBeadSoundPlayer.synthesizeMilestoneChime()
        assertTrue(chime.size > 4000)
        assertTrue(chime.any { abs(it.toInt()) > 2000 })
    }

    // ---------- 2. QURAN WORD-BY-WORD I'RAB & SARF ENGINE ----------

    @Test
    fun quranMorphologyIrabEngine_returnsVerifiedScholarlyAnalysisForFatiha1() {
        val result = QuranMorphologyIrabEngine.analyzeAyah(
            surahNumber = 1,
            ayahNumber = 1,
            fullAyahText = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
            wordTokens = emptyList()
        )
        assertTrue(result.isScholarlyVerified)
        assertEquals(4, result.words.size)
        assertTrue(result.words.all { it.irabSummaryAr.isNotBlank() && it.sarfDetailsAr.isNotBlank() })
        assertTrue(result.sentenceNoteAr.isNotBlank())
    }

    @Test
    fun quranMorphologyIrabEngine_analyzesNonCatalogVerseWithRules() {
        val tokens = listOf(
            QuranWordToken(1, "قُلْ", "qul", "Say"),
            QuranWordToken(2, "يَعْمَلُونَ", "ya'maluna", "they work"),
            QuranWordToken(3, "فِي", "fi", "in"),
            QuranWordToken(4, "ٱللَّهِ", "Allahi", "Allah"),
            QuranWordToken(5, "ٱلْمُؤْمِنُونَ", "al-mu'minuna", "the believers")
        )
        val result = QuranMorphologyIrabEngine.analyzeAyah(
            surahNumber = 58,
            ayahNumber = 19,
            fullAyahText = "قُلْ يَعْمَلُونَ فِي ٱللَّهِ ٱلْمُؤْمِنُونَ",
            wordTokens = tokens,
            tafsirMuyassarAr = "تفسير تجريبي"
        )
        assertFalse(result.isScholarlyVerified)
        assertEquals(5, result.words.size)
        assertEquals("فعل أمر", result.words[0].partOfSpeechAr)
        assertEquals("فعل مضارع", result.words[1].partOfSpeechAr)
        assertEquals("حرف جر", result.words[2].partOfSpeechAr)
        assertTrue(result.words[3].partOfSpeechAr.contains("لفظ الجلالة"))
        assertEquals("مجرور", result.words[3].irabCaseAr)
        assertEquals("مرفوع بالواو", result.words[4].irabCaseAr)
        assertTrue(result.words[4].sarfRootAr.isNotBlank())
    }

    // ---------- 3. LUNAR ASTRONOMY & CRESCENT OBSERVATORY ENGINE ----------

    @Test
    fun lunarAstronomyEngine_computesValidNewFullAndArbitraryPhases() {
        val refNewMoon = 947_182_440_000L // 2000-01-06 18:14 UTC
        val newSnap = LunarAstronomyEngine.computeLunarPhase(refNewMoon)
        assertTrue(newSnap.ageDays in 0.0..0.5)
        assertTrue(newSnap.illuminationPercent in 0.0..1.0)
        assertEquals("New Moon", newSnap.phaseNameEn)
        assertTrue(newSnap.lunarMansionAr.isNotBlank())

        val halfCycleMs = (LunarAstronomyEngine.SYNODIC_MONTH_DAYS / 2.0 * 86_400_000.0).toLong()
        val fullSnap = LunarAstronomyEngine.computeLunarPhase(refNewMoon + halfCycleMs)
        assertTrue(fullSnap.illuminationPercent >= 99.0)
        assertEquals("Full Moon", fullSnap.phaseNameEn)
        assertTrue(fullSnap.lunarMansionEn.isNotBlank())

        assertTrue(LunarAstronomyEngine.sightingVerdictAr(2.0, 15).contains("مثالية"))
        assertTrue(LunarAstronomyEngine.sightingVerdictEn(2.0, 90).contains("Heavy cloud"))
        assertTrue(LunarAstronomyEngine.sightingVerdictUz(14.8, 10).contains("To'lin oy"))
    }

    // ---------- 4. LIVE QURAN.COM WORD-BY-WORD & OPEN-METEO SKY PARSERS ----------

    @Test
    fun liveOnlineService_parsesQuranComWordByWordAndSkyWeather() {
        val quranJson = """
            {
              "verse": {
                "text_uthmani": "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
                "words": [
                  { "char_type_name": "word", "text_uthmani": "قُلْ", "transliteration": { "text": "qul" }, "translation": { "text": "Say" } },
                  { "char_type_name": "word", "text_uthmani": "هُوَ", "transliteration": { "text": "huwa" }, "translation": { "text": "He" } },
                  { "char_type_name": "end", "text_uthmani": "١" }
                ]
              }
            }
        """.trimIndent()

        val parsedVerse = NoorLiveOnlineService.parseQuranComWordByWord(quranJson)
        assertEquals("قُلْ هُوَ ٱللَّهُ أَحَدٌ", parsedVerse.fullUthmaniText)
        assertEquals(2, parsedVerse.words.size)
        assertEquals("قُلْ", parsedVerse.words[0].uthmaniWord)
        assertEquals("Say", parsedVerse.words[0].meaningEn)

        val weatherJson = """
            {
              "current": { "temperature_2m": 24.5, "cloud_cover": 18, "relative_humidity_2m": 42 },
              "daily": { "sunrise": ["2026-11-25T06:12"], "sunset": ["2026-11-25T17:44"] }
            }
        """.trimIndent()

        val parsedWeather = NoorLiveOnlineService.parseSkySightingWeather(weatherJson)
        assertEquals(24.5, parsedWeather.temperatureC, 0.01)
        assertEquals(18, parsedWeather.cloudCoverPercent)
        assertEquals(42, parsedWeather.humidityPercent)
        assertEquals("06:12", parsedWeather.sunriseLocal)
        assertEquals("17:44", parsedWeather.sunsetLocal)
    }
}
