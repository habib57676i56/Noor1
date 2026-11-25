package com.example

import com.example.domain.astronomy.CosmicEarthGlobeEngine
import com.example.domain.quran.QuranMiraclesAndParablesEngine
import com.example.domain.quran.QuranicSoulRemedyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoorCosmicRemedyMiraclesTest {

    @Test
    fun cosmicEarthGlobeEngine_computesAccurateSubsolarQiblaGeodesicAndBeacons() {
        val now = System.currentTimeMillis()
        val sun = CosmicEarthGlobeEngine.computeSubsolarPoint(now)
        assertTrue("Subsolar latitude must be within tropics", sun.latitudeDeg in -24.0..24.0)
        assertTrue("Subsolar longitude must be in -180..180", sun.longitudeDeg in -180.0..180.0)

        // Subsolar point itself must be in daylight; its exact antipode must be in night
        assertTrue(CosmicEarthGlobeEngine.isPointInDaylight(sun.latitudeDeg, sun.longitudeDeg, sun))
        val antipodeLon = if (sun.longitudeDeg > 0) sun.longitudeDeg - 180.0 else sun.longitudeDeg + 180.0
        assertTrue(!CosmicEarthGlobeEngine.isPointInDaylight(-sun.latitudeDeg, antipodeLon, sun))

        // Qibla bearings: London (~119 deg ESE) and Tashkent (~240 deg WSW)
        val londonQibla = CosmicEarthGlobeEngine.qiblaBearingDegrees(51.5072, -0.1276)
        assertTrue("London Qibla expected ~119°, got $londonQibla", londonQibla in 115.0..123.0)

        val tashkentQibla = CosmicEarthGlobeEngine.qiblaBearingDegrees(41.2995, 69.2401)
        assertTrue("Tashkent Qibla expected ~240°, got $tashkentQibla", tashkentQibla in 236.0..244.0)

        // Great-circle distance from Madinah to Kaaba is ~339 km
        val madinahDist = CosmicEarthGlobeEngine.distanceToKaabaKm(24.4672, 39.6112)
        assertTrue("Madinah->Kaaba distance expected ~339km, got $madinahDist", madinahDist in 330.0..350.0)

        // Geodesic arc from London starts near London and ends at Kaaba
        val arc = CosmicEarthGlobeEngine.computeGeodesicToKaaba(51.5072, -0.1276, steps = 36)
        assertEquals(37, arc.size)
        assertTrue(abs(arc.first().latDeg - 51.5072) < 0.01)
        assertTrue(abs(arc.last().latDeg - CosmicEarthGlobeEngine.KAABA_LAT) < 0.01)
        assertTrue(abs(arc.last().lonDeg - CosmicEarthGlobeEngine.KAABA_LON) < 0.01)

        // Orthographic projection of camera center is visible at (0, 0, 1)
        val centerProj = CosmicEarthGlobeEngine.projectOrthographic(
            CosmicEarthGlobeEngine.KAABA_LAT,
            CosmicEarthGlobeEngine.KAABA_LON,
            CosmicEarthGlobeEngine.KAABA_LAT,
            CosmicEarthGlobeEngine.KAABA_LON
        )
        assertTrue(centerProj.isVisible)
        assertTrue(centerProj.z > 0.99f)

        // All 14 global city beacons evaluated including Makkah
        val beacons = CosmicEarthGlobeEngine.evaluateGlobalBeacons(now)
        assertEquals(14, beacons.size)
        assertTrue(beacons.any { it.id == "makkah" })
        assertTrue(beacons.any { it.isDaylight })
    }

    @Test
    fun quranicSoulRemedyEngine_prescriptionsHaveValidAudioAndTrilingualContent() {
        val list = QuranicSoulRemedyEngine.prescriptions
        assertTrue("Expected at least 8 emotional remedies", list.size >= 8)
        list.forEach { rx ->
            assertTrue(rx.moodAr.isNotBlank() && rx.moodEn.isNotBlank() && rx.moodUz.isNotBlank())
            assertTrue("Surah out of range for ${rx.id}", rx.surahNumber in 1..114)
            assertTrue("Global Ayah out of range for ${rx.id}", rx.globalAyahNumber in 1..6236)
            assertTrue(rx.ayahTextAr.isNotBlank())
            assertTrue(rx.ayahReflectionAr.isNotBlank() && rx.ayahReflectionEn.isNotBlank() && rx.ayahReflectionUz.isNotBlank())
            assertTrue(rx.propheticDuaAr.isNotBlank() && rx.propheticDuaEn.isNotBlank() && rx.duaSourceAr.isNotBlank())
            assertTrue(rx.dhikrTextAr.isNotBlank() && rx.dhikrTargetCount > 0)
        }
    }

    @Test
    fun quranMiraclesAndParablesEngine_hasAllCosmicSignsParablesAnd25Prophets() {
        val signs = QuranMiraclesAndParablesEngine.cosmicSigns
        assertTrue("Expected >= 7 cosmic signs", signs.size >= 7)
        signs.forEach { s ->
            assertTrue(s.surahNumber in 1..114)
            assertTrue(s.globalAyahNumber in 1..6236)
            assertTrue(s.titleAr.isNotBlank() && s.titleEn.isNotBlank() && s.titleUz.isNotBlank())
            assertTrue(s.ayahTextAr.isNotBlank() && s.reflectionAr.isNotBlank())
        }

        val parables = QuranMiraclesAndParablesEngine.parables
        assertTrue("Expected >= 5 Quranic parables", parables.size >= 5)
        parables.forEach { p ->
            assertTrue(p.surahNumber in 1..114)
            assertTrue(p.titleAr.isNotBlank() && p.ayahTextAr.isNotBlank() && p.imageryBreakdownAr.isNotBlank())
        }

        val prophets = QuranMiraclesAndParablesEngine.all25Prophets
        assertEquals("Must contain all 25 Prophets named in the Quran", 25, prophets.size)
        assertEquals(1, prophets.first().order)
        assertEquals(25, prophets.last().order)
        assertTrue("Last prophet #25 must be Muhammad ﷺ", prophets.last().nameEn.contains("Muhammad"))
        val topMentioned = prophets.maxByOrNull { it.mentionCountInQuran }!!
        assertEquals(136, topMentioned.mentionCountInQuran)
        assertTrue("Most mentioned prophet must be Musa", topMentioned.nameEn.contains("Musa"))
        prophets.forEach { p ->
            assertTrue(p.primarySurahNumber in 1..114)
            assertTrue(p.nameAr.isNotBlank() && p.nameEn.isNotBlank() && p.nameUz.isNotBlank())
            assertTrue(p.hallmarkAr.isNotBlank() && p.hallmarkEn.isNotBlank())
        }
    }
}
