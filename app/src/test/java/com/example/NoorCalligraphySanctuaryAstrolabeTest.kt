package com.example

import com.example.domain.astronomy.IslamicAstrolabeEngine
import com.example.domain.audio.IslamicSanctuaryAudioEngine
import com.example.domain.quran.QuranCalligraphyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoorCalligraphySanctuaryAstrolabeTest {

    @Test
    fun quranCalligraphyEngine_hasValidMasterpiecesAndIlluminationPalettes() {
        val pieces = QuranCalligraphyEngine.masterpieces
        assertTrue("Expected at least 6 masterpieces", pieces.size >= 6)
        pieces.forEach { p ->
            assertTrue(p.surahNumber in 1..114)
            assertTrue(p.globalAyahNumber in 1..6236)
            assertTrue(p.calligraphyTextAr.isNotBlank())
            assertTrue(p.titleAr.isNotBlank() && p.titleEn.isNotBlank() && p.titleUz.isNotBlank())
        }

        val themes = QuranCalligraphyEngine.themes
        assertEquals(5, themes.size)
        themes.forEach { t ->
            assertTrue(t.nameAr.isNotBlank() && t.nameEn.isNotBlank() && t.nameUz.isNotBlank())
        }
    }

    @Test
    fun islamicSanctuaryAudioEngine_synthesizesNonSilentClippedSafePcmAudio() {
        assertTrue(IslamicSanctuaryAudioEngine.presets.size >= 5)
        assertTrue(IslamicSanctuaryAudioEngine.sakinahRecitations.size >= 6)

        val synth = IslamicSanctuaryAudioEngine.SanctuarySynthesizer()
        val buf = ShortArray(2048)
        synth.renderBuffer(buf, rainLevel = 0.7f, breezeLevel = 0.4f, streamLevel = 0.5f, droneLevel = 0.5f)

        val maxAmp = buf.maxOf { abs(it.toInt()) }
        assertTrue("Expected audible non-zero PCM signal, got maxAmp=$maxAmp", maxAmp > 500)
        assertTrue("Signal must stay safely below hard clipping", maxAmp < Short.MAX_VALUE)

        // All-zero mix must produce exact silence
        val silentBuf = ShortArray(512)
        synth.renderBuffer(silentBuf, 0f, 0f, 0f, 0f)
        assertEquals(0, silentBuf.maxOf { abs(it.toInt()) })
    }

    @Test
    fun islamicAstrolabeEngine_computesValidSiderealTimeAnd15ArabicStars() {
        assertEquals(15, IslamicAstrolabeEngine.arabicFixedStars.size)
        assertTrue(IslamicAstrolabeEngine.arabicFixedStars.any { it.id == "sirius" })
        assertTrue(IslamicAstrolabeEngine.arabicFixedStars.any { it.id == "polaris" })

        val now = System.currentTimeMillis()
        // Test from Makkah (21.4225° N, 39.8262° E)
        val snap = IslamicAstrolabeEngine.computeSkySnapshot(now, 21.4225, 39.8262)
        assertTrue("LST must be in 0..24", snap.localSiderealTimeHours in 0.0..24.0)
        assertTrue("Sun altitude must be in -90..90", snap.sunAltitudeDeg in -90.0..90.0)
        assertTrue("Sun azimuth must be in 0..360", snap.sunAzimuthDeg in 0.0..360.0)
        assertEquals(15, snap.stars.size)

        // From Northern Hemisphere (Makkah lat ~21.4°), Polaris altitude must always be near observer latitude (~20°..23°)
        val polaris = snap.stars.first { it.star.id == "polaris" }
        assertTrue(
            "Polaris altitude from Makkah should be ~21.4°, got ${polaris.altitudeDeg}",
            polaris.altitudeDeg in 19.5..23.5
        )
        assertTrue("Polaris must be visible from Makkah", polaris.isVisibleNow)
        assertTrue("Formatted LST must match HH:MM:SS", IslamicAstrolabeEngine.formatSiderealTime(snap.localSiderealTimeHours).length == 8)
    }
}
