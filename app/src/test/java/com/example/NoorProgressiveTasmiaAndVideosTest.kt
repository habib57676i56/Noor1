package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.RecitationMistakeSoundPlayer
import com.example.data.local.catalog.IslamicVideoCatalog
import com.example.data.local.catalog.IslamicVideoCategory
import com.example.data.local.catalog.IslamicVideoItem
import com.example.data.local.catalog.MadinahMushafPageCatalog
import com.example.data.local.catalog.SurahCatalog
import com.example.data.quran.IslamicVideoRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.QuranReciter
import com.example.domain.model.QuranVerse
import com.example.domain.quran.ProgressiveRecitationTracker
import com.example.ui.i18n.L10n
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoorProgressiveTasmiaAndVideosTest {

    @Test
    fun progressiveTracker_revealsWordByWord_andFreezesWithMistakeAlertOnWrongWord() {
        val fatihah = SurahCatalog.getBundledSurahVerses(1, QuranReciter.YASSER_AL_DOSARI)!!
        val tracker = ProgressiveRecitationTracker()

        val initial = tracker.loadVerses(
            verses = fatihah,
            titleAr = "سورة الفاتحة",
            titleEn = "Surah Al-Fatihah"
        )
        assertTrue(initial.totalWords >= 28)
        assertEquals(0, initial.revealedCount)
        assertEquals(0, initial.mistakeCount)
        assertFalse(initial.isCompleted)

        // Step 1: Recite Ayah 1 cleanly ("بسم الله الرحمن الرحيم")
        val afterAyah1 = tracker.processTranscript("بسم الله الرحمن الرحيم", isPartialStreaming = false)
        assertEquals(4, afterAyah1.revealedCount)
        assertEquals(0, afterAyah1.mistakeCount)
        assertFalse(afterAyah1.isMistakeActive)

        // Step 2: Recite Ayah 2 with an intentional wrong word at the end ("الحمد لله رب الكافرين")
        val afterMistake = tracker.processTranscript(
            "بسم الله الرحمن الرحيم الحمد لله رب الكافرين",
            isPartialStreaming = false
        )
        // "الحمد", "لله", "رب" succeed (4 + 3 = 7), "الكافرين" MUST NOT advance cursor beyond 7
        assertEquals(7, afterMistake.revealedCount)
        assertEquals(1, afterMistake.mistakeCount)
        assertEquals(1, afterMistake.mistakeEventId)
        assertTrue(afterMistake.isMistakeActive)
        assertEquals(2, afterMistake.lastMistakeAyah)

        // Step 3: Correct the mistake word ("العالمين") and continue writing cleanly
        val afterCorrection = tracker.processTranscript(
            "بسم الله الرحمن الرحيم الحمد لله رب الكافرين العالمين",
            isPartialStreaming = false
        )
        assertEquals(8, afterCorrection.revealedCount)
        assertFalse(afterCorrection.isMistakeActive)
        assertEquals(1, afterCorrection.mistakeCount)
    }

    @Test
    fun progressiveTracker_partialPrefixDoesNotFalseTriggerBeforeWordFinishes() {
        val fatihah = SurahCatalog.getBundledSurahVerses(1, QuranReciter.YASSER_AL_DOSARI)!!
        val tracker = ProgressiveRecitationTracker()
        tracker.loadVerses(fatihah, "سورة الفاتحة", "Al-Fatihah")

        // Streaming partial transcript stops mid-word on "الرح" before completing "الرحمن"
        val partialState = tracker.processTranscript("بسم الله الرح", isPartialStreaming = true)
        assertEquals(2, partialState.revealedCount)
        assertEquals(0, partialState.mistakeCount)

        // Once user completes "الرحمن الرحيم", it writes smoothly without false mistake alert
        val completedPartial = tracker.processTranscript("بسم الله الرحمن الرحيم", isPartialStreaming = false)
        assertEquals(4, completedPartial.revealedCount)
        assertEquals(0, completedPartial.mistakeCount)
    }

    @Test
    fun progressiveTracker_supportsMultiSurahMushafPage604ToCompletion() {
        val ikhlas = SurahCatalog.getBundledSurahVerses(112, QuranReciter.YASSER_AL_DOSARI)!!
        val falaq = SurahCatalog.getBundledSurahVerses(113, QuranReciter.YASSER_AL_DOSARI)!!
        val nas = SurahCatalog.getBundledSurahVerses(114, QuranReciter.YASSER_AL_DOSARI)!!
        val page604Verses = (ikhlas + falaq + nas)

        val tracker = ProgressiveRecitationTracker()
        val loaded = tracker.loadVerses(
            verses = page604Verses,
            titleAr = "صفحة 604 — الإخلاص · الفلق · الناس",
            titleEn = "Page 604 — Al-Ikhlas · Al-Falaq · An-Nas"
        )
        // Confirm all 3 Surahs on Page 604 are flagged with Surah header markers
        val surahHeaderCount = loaded.words.count { it.isFirstWordOfSurah }
        assertEquals(3, surahHeaderCount)

        // Speak every verse cleanly (simulating full page recitation)
        val fullSpoken = page604Verses.joinToString(" ") { it.textUthmani }
        val completed = tracker.processTranscript(fullSpoken, isPartialStreaming = false)
        assertTrue(completed.isCompleted)
        assertEquals(completed.totalWords, completed.revealedCount)
        assertEquals(0, completed.mistakeCount)

        val resetState = tracker.reset()
        assertEquals(0, resetState.revealedCount)
        assertFalse(resetState.isCompleted)
    }

    @Test
    fun mushafPageCatalog_coversAll114SurahsAcrossAll604Pages() {
        assertEquals(1, SurahCatalog.allSurahs.first().number)
        assertEquals(114, SurahCatalog.allSurahs.last().number)
        assertEquals(114, SurahCatalog.allSurahs.size)

        for (page in 1..MadinahMushafPageCatalog.TOTAL_PAGES) {
            val meta = MadinahMushafPageCatalog.pageMeta(page)
            assertTrue("Page $page must map to at least one Surah", meta.surahsOnPage.isNotEmpty())
            assertTrue(meta.juzNumber in 1..30)
        }
        val page604 = MadinahMushafPageCatalog.pageMeta(604)
        assertTrue(page604.surahsOnPage.map { it.number }.containsAll(listOf(112, 113, 114)))
    }

    @Test
    fun mistakeSoundPlayer_tracksTriggersAndRespectsMuteToggle() {
        val player = RecitationMistakeSoundPlayer()
        assertEquals(0, player.playedCountForTest)
        assertFalse(player.isMuted())

        player.playMistakeAlert()
        assertEquals(1, player.playedCountForTest)

        player.setMuted(true)
        assertTrue(player.isMuted())
        Thread.sleep(240L)
        player.playMistakeAlert()
        // Should NOT play while muted
        assertEquals(1, player.playedCountForTest)
        player.release()
    }

    @Test
    fun islamicVideoRepository_parsesArchiveMetadataDirectCdnUrlsAndPersistsUserVideos() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val collection = IslamicVideoCatalog.curatedCollections.first { it.category == IslamicVideoCategory.QURAN_LESSONS }
        val sampleJson = """
            {
              "server": "ia801500.us.archive.org",
              "dir": "/19/items/${collection.archiveItemId}",
              "files": [
                {"name": "lesson_02_harakat.mp4", "length": "325"},
                {"name": "lesson_02_harakat.ia.mp4", "length": "325"},
                {"name": "lesson_01_letters.mp4", "length": "240"},
                {"name": "cover.jpg", "length": ""}
              ]
            }
        """.trimIndent()

        val episodes = IslamicVideoRepository.parseArchiveMetadataJson(collection.archiveItemId, sampleJson, collection)
        assertEquals(2, episodes.size) // Deduplicated .ia.mp4 and ignored .jpg
        assertEquals(
            "https://ia801500.us.archive.org/19/items/${collection.archiveItemId}/lesson_01_letters.mp4",
            episodes[0].streamUrl
        )
        assertEquals("4:00", episodes[0].durationLabel)
        assertEquals(
            "https://ia801500.us.archive.org/19/items/${collection.archiveItemId}/lesson_02_harakat.mp4",
            episodes[1].streamUrl
        )
        assertEquals("5:25", episodes[1].durationLabel)

        val repo = IslamicVideoRepository(context)
        val added = repo.addUserVideo(
            IslamicVideoItem(
                id = "local_vid_1",
                collectionId = "user_saved",
                titleAr = "تجويد سورة الملك",
                titleEn = "Tajweed Lesson Surah Al-Mulk",
                subtitleAr = "فيديو محلي",
                subtitleEn = "Device Video",
                category = IslamicVideoCategory.MY_VIDEOS,
                streamUrl = "content://media/external/video/media/42",
                isLocalUri = true
            )
        )
        assertEquals(1, added.size)
        assertEquals(1, IslamicVideoRepository(context).getUserVideos().size)
        val afterRemove = repo.removeUserVideo("local_vid_1")
        assertTrue(afterRemove.isEmpty())
    }

    @Test
    fun islamicVideoCatalog_containsQuranLessonsAndIslamicCartoons() {
        val cats = IslamicVideoCatalog.curatedCollections.map { it.category }.toSet()
        assertTrue(IslamicVideoCategory.QURAN_LESSONS in cats)
        assertTrue(IslamicVideoCategory.ISLAMIC_CARTOONS in cats)
        assertTrue(IslamicVideoCategory.PROPHETS_STORIES in cats)
        assertTrue(IslamicVideoCatalog.curatedCollections.all { it.archiveItemId.isNotBlank() && it.isKidsSafe })
    }

    @Test
    fun uzbekLocalization_coversNewVideosAndProgressiveTasmiaStrings() {
        assertEquals("Sahifa bo'yicha (1–604)", L10n.pick(AppLanguage.UZBEK, "حسب الصفحة", "By Page (1–604)"))
        assertEquals("Aytganda yozish", L10n.pick(AppLanguage.UZBEK, "يكتب وراي", "Write-As-Recite"))
        assertEquals("NOOR islomiy videolari", L10n.pick(AppLanguage.UZBEK, "فيديوهات نور", "NOOR Islamic Videos"))
        assertEquals("Islomiy multfilmlar", L10n.pick(AppLanguage.UZBEK, "كرتون إسلامي", "Islamic Cartoons"))
    }
}
