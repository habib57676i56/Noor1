package com.example

import com.example.data.remote.QuranSearchHit
import com.example.domain.quran.QuranRootAndQiraatEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoorQuranUniverseTest {

    @Test
    fun rootFamilies_haveRichDerivativesAndTrilingualMetadata() {
        val families = QuranRootAndQiraatEngine.rootFamilies
        assertTrue(families.size >= 12)
        families.forEach { fam ->
            assertTrue(fam.rootAr.isNotBlank())
            assertTrue(fam.titleAr.isNotBlank() && fam.titleEn.isNotBlank() && fam.titleUz.isNotBlank())
            assertTrue("Root ${fam.id} should have >=4 derivatives", fam.derivatives.size >= 4)
            fam.derivatives.forEach { d ->
                assertTrue(d.wordAr.isNotBlank() && d.searchQueryPlain.isNotBlank())
            }
        }
    }

    @Test
    fun qiraatComparisonsAndGoldenSanad_areCompleteAndAuthentic() {
        val comparisons = QuranRootAndQiraatEngine.qiraatComparisons
        assertTrue(comparisons.size >= 7)
        comparisons.forEach { cmp ->
            assertTrue(cmp.surahNumber in 1..114)
            assertTrue(cmp.hafsWordAr.isNotBlank() && cmp.altWordAr.isNotBlank())
            assertTrue(cmp.miracleNoteAr.isNotBlank() && cmp.miracleNoteEn.isNotBlank() && cmp.miracleNoteUz.isNotBlank())
        }

        val sanad = QuranRootAndQiraatEngine.goldenSanadChain
        assertTrue(sanad.size >= 8)
        assertEquals(0, sanad.first().tier)
        assertEquals(1, sanad[1].tier)
        assertTrue(sanad.drop(2).all { it.tier == 2 })
    }

    @Test
    fun analyzeRootDistribution_computesAccurateMeccanMadinanAndTopSurahs() {
        val sampleHits = listOf(
            // Al-Fatihah (1, Meccan) x2
            QuranSearchHit(1, "الفاتحة", "Al-Fatihah", 1, 1, "بسم الله الرحمن الرحيم"),
            QuranSearchHit(1, "الفاتحة", "Al-Fatihah", 3, 3, "الرحمن الرحيم"),
            // Al-Baqarah (2, Madinan) x3
            QuranSearchHit(2, "البقرة", "Al-Baqarah", 163, 170, "الرحمن الرحيم"),
            QuranSearchHit(2, "البقرة", "Al-Baqarah", 178, 185, "رحمة"),
            QuranSearchHit(2, "البقرة", "Al-Baqarah", 218, 225, "رحمت الله"),
            // Maryam (19, Meccan) x1
            QuranSearchHit(19, "مريم", "Maryam", 2, 2252, "ذكر رحمت ربك")
        )
        val stats = QuranRootAndQiraatEngine.analyzeRootDistribution(sampleHits)
        assertEquals(6, stats.totalMatches)
        assertEquals(3, stats.meccanCount)
        assertEquals(3, stats.madinanCount)
        assertEquals(3, stats.distinctSurahs)
        assertEquals("البقرة", stats.topSurahs.first().first)
        assertEquals(3, stats.topSurahs.first().second)
    }
}
