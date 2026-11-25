package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.domain.quran.QuranMutashabihatEngine
import com.example.domain.seerah.SeerahAtlasEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoorMutashabihatAndSeerahTest {

    @Test
    fun mutashabihatEngine_curatedGroupsHaveValidVersesAndMnemonics() {
        val groups = QuranMutashabihatEngine.curatedGroups
        assertTrue(groups.size >= 10)
        groups.forEach { g ->
            assertTrue("Group ${g.id} must have >=2 verses", g.ayahs.size >= 2)
            assertTrue(g.titleAr.isNotBlank())
            assertTrue(g.titleEn.isNotBlank())
            assertTrue(g.titleUz.isNotBlank())
            assertTrue(g.mnemonicRuleAr.isNotBlank())
            g.ayahs.forEach { a ->
                assertTrue(a.surahNumber in 1..114)
                assertTrue(a.ayahNumber >= 1)
                assertTrue(a.ayahTextAr.isNotBlank())
            }
        }
    }

    @Test
    fun mutashabihatEngine_computeWordDiffsHighlightsDistinctiveWordsOnly() {
        val target = "وَسَارِعُوا إِلَىٰ مَغْفِرَةٍ مِّن رَّبِّكُمْ"
        val peer = "سَابِقُوا إِلَىٰ مَغْفِرَةٍ مِّن رَّبِّكُمْ"
        val diffs = QuranMutashabihatEngine.computeWordDiffs(target, listOf(peer))
        assertEquals(5, diffs.size)
        assertTrue("وَسَارِعُوا should be distinctive", diffs[0].isDistinctive)
        assertFalse("إِلَىٰ should be shared", diffs[1].isDistinctive)
        assertFalse("مَغْفِرَةٍ should be shared", diffs[2].isDistinctive)
        assertFalse("مِّن should be shared", diffs[3].isDistinctive)
        assertFalse("رَّبِّكُمْ should be shared", diffs[4].isDistinctive)
    }

    @Test
    fun seerahAtlasEngine_hijrahRouteAndHaversineDistancesAreAstronomicallyAccurate() {
        val waypoints = SeerahAtlasEngine.hijrahWaypoints
        assertEquals(7, waypoints.size)
        val directMakkahToMadinah = SeerahAtlasEngine.distanceKm(21.4225, 39.8262, 24.4672, 39.6112)
        assertTrue("Direct Makkah->Madinah should be ~330-345 km, got $directMakkahToMadinah", directMakkahToMadinah in 325.0..350.0)

        val totalHijrahKm = SeerahAtlasEngine.totalHijrahRouteKm()
        assertTrue("Hijrah coastal detour route should exceed direct distance (~380-480 km), got $totalHijrahKm", totalHijrahKm in 370.0..500.0)
    }

    @Test
    fun seerahAtlasEngine_sunnahRevivalPersistenceTogglesCleanly() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertTrue(SeerahAtlasEngine.chronicleEvents.size >= 8)
        assertTrue(SeerahAtlasEngine.shamailSunnahs.size >= 6)

        val afterAdd = SeerahAtlasEngine.toggleSunnahCompleted(ctx, "smile")
        assertTrue("smile" in afterAdd)
        assertTrue("smile" in SeerahAtlasEngine.loadCompletedSunnahs(ctx))

        val afterRemove = SeerahAtlasEngine.toggleSunnahCompleted(ctx, "smile")
        assertFalse("smile" in afterRemove)
    }
}
