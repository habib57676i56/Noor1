package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.catalog.IslamicVideoCatalog
import com.example.data.local.catalog.IslamicVideoCategory
import com.example.data.local.catalog.IslamicVideoItem
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.data.quran.IslamicVideoRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.PrayerName
import com.example.domain.prayer.PrayerTimeGate
import com.example.ui.i18n.L10n
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoorProfileVideosAdminAndPrayerGateTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("noor_videos_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun prayerTimeGate_allowsOnlyEnteredFardPrayers_andBlocksUpcomingAndSunrise() {
        val schedule = DailyPrayerSchedule(
            dateKey = "2026-11-25",
            epochDay = 20417L,
            fajrMillis = 1_000_000L,
            sunriseMillis = 2_000_000L,
            dhuhrMillis = 3_000_000L,
            asrMillis = 4_000_000L,
            maghribMillis = 5_000_000L,
            ishaMillis = 6_000_000L
        )

        // Mid-afternoon: after Dhuhr (3M) and before Asr (4M)
        val nowMillis = 3_500_000L

        assertTrue("Fajr has entered", PrayerTimeGate.hasPrayerEntered(schedule, PrayerName.FAJR, nowMillis))
        assertFalse("Sunrise is not a fard prayer", PrayerTimeGate.hasPrayerEntered(schedule, PrayerName.SUNRISE, nowMillis))
        assertTrue("Dhuhr has entered", PrayerTimeGate.hasPrayerEntered(schedule, PrayerName.DHUHR, nowMillis))
        assertFalse("Asr has NOT entered yet", PrayerTimeGate.hasPrayerEntered(schedule, PrayerName.ASR, nowMillis))
        assertFalse("Maghrib has NOT entered yet", PrayerTimeGate.hasPrayerEntered(schedule, PrayerName.MAGHRIB, nowMillis))
        assertFalse("Isha has NOT entered yet", PrayerTimeGate.hasPrayerEntered(schedule, PrayerName.ISHA, nowMillis))

        // Exact boundary: Asr enters at exact millisecond
        assertTrue(
            "Asr enters at exact scheduled time",
            PrayerTimeGate.hasPrayerEntered(schedule, PrayerName.ASR, 4_000_000L)
        )
        assertFalse(
            "Asr is blocked 1ms before scheduled time",
            PrayerTimeGate.hasPrayerEntered(schedule, PrayerName.ASR, 3_999_999L)
        )
    }

    @Test
    fun youtubeVideoPublish_persistsCoverAndMetadata_andSuperAdminCanEditAndDelete() {
        val repo = IslamicVideoRepository(context)
        assertTrue(repo.publishedVideos.value.isEmpty())

        val published = IslamicVideoItem(
            id = "pub_test_01",
            collectionId = "user_published",
            titleAr = "درس تجويد سورة الفاتحة",
            titleEn = "Surah Al-Fatiha Tajweed Lesson",
            subtitleAr = "قناة نور للقرآن",
            subtitleEn = "NOOR Quran Channel",
            category = IslamicVideoCategory.QURAN_LESSONS,
            streamUrl = "https://example.com/fatiha.mp4",
            durationLabel = "14:20",
            sourceLabel = "Web Stream",
            isLocalUri = false,
            thumbnailUrl = "content://media/picker/cover_fatiha.jpg",
            descriptionAr = "شرح مفصل لأحكام تلاوة سورة الفاتحة للمبتدئين",
            descriptionEn = "Detailed Tajweed lesson for Surah Al-Fatiha",
            uploaderName = "أسامة",
            uploaderAvatarUrl = "https://lh3.googleusercontent.com/avatar.png",
            publishedAtMillis = 1_700_000_000L,
            isUserPublished = true
        )

        repo.addUserVideo(published)

        // Reactive StateFlow updates immediately
        assertEquals(1, repo.publishedVideos.value.size)
        assertEquals("content://media/picker/cover_fatiha.jpg", repo.publishedVideos.value.first().thumbnailUrl)
        assertEquals("شرح مفصل لأحكام تلاوة سورة الفاتحة للمبتدئين", repo.publishedVideos.value.first().descriptionAr)
        assertEquals(IslamicVideoCategory.QURAN_LESSONS, repo.publishedVideos.value.first().category)

        // Fresh repository instance reloads full YouTube-style metadata from SharedPreferences
        val reloadedRepo = IslamicVideoRepository(context)
        val reloaded = reloadedRepo.publishedVideos.value.first()
        assertEquals("pub_test_01", reloaded.id)
        assertEquals("درس تجويد سورة الفاتحة", reloaded.titleAr)
        assertEquals("14:20", reloaded.durationLabel)
        assertEquals("أسامة", reloaded.uploaderName)
        assertEquals("https://lh3.googleusercontent.com/avatar.png", reloaded.uploaderAvatarUrl)

        // Super Admin edits the published video (title, cover, category, description)
        val edited = reloaded.copy(
            titleAr = "درس تجويد سورة الفاتحة — معدل",
            titleEn = "Surah Al-Fatiha Tajweed (Edited)",
            descriptionAr = "وصف محدّث من السوبر أدمن",
            thumbnailUrl = "https://example.com/new_cover.jpg",
            category = IslamicVideoCategory.PROPHETS_STORIES
        )
        repo.updateVideo(edited)
        val afterEdit = repo.publishedVideos.value.first()
        assertEquals("درس تجويد سورة الفاتحة — معدل", afterEdit.titleAr)
        assertEquals("وصف محدّث من السوبر أدمن", afterEdit.descriptionAr)
        assertEquals("https://example.com/new_cover.jpg", afterEdit.thumbnailUrl)
        assertEquals(IslamicVideoCategory.PROPHETS_STORIES, afterEdit.category)

        // Super Admin deletes the published video
        repo.removeUserVideo("pub_test_01")
        assertTrue(repo.publishedVideos.value.isEmpty())
    }

    @Test
    fun curatedCollections_provideRealArchiveCoverThumbnails() {
        assertTrue(IslamicVideoCatalog.curatedCollections.isNotEmpty())
        IslamicVideoCatalog.curatedCollections.forEach { col ->
            assertTrue(
                "Collection ${col.id} should expose Archive.org cover thumbnail URL",
                col.coverThumbnailUrl.startsWith("https://archive.org/services/img/") &&
                    col.coverThumbnailUrl.endsWith(col.archiveItemId)
            )
        }
    }

    @Test
    fun userPreferences_persistsDisplayNameAndCustomAvatarUri() = runBlocking {
        val prefsRepo = UserPreferencesRepository(context)
        prefsRepo.setUserName("أسامة النور")
        prefsRepo.setCustomAvatarUri("content://media/picker/0/avatar_99")

        val loaded = prefsRepo.getPreferencesOnce()
        assertEquals("أسامة النور", loaded.userName)
        assertEquals("content://media/picker/0/avatar_99", loaded.customAvatarUri)
    }

    @Test
    fun l10n_uzbekTranslationsCoverProfileVideosAdminAndPrayerGate() {
        assertEquals("Profil rasmi", L10n.pick(AppLanguage.UZBEK, "صورة الحساب", "Account Profile Photo"))
        assertEquals("Ismni o'zgartirish", L10n.pick(AppLanguage.UZBEK, "تغيير الاسم", "Change Name"))
        assertEquals("Video nashr qilish", L10n.pick(AppLanguage.UZBEK, "نشر الفيديو", "Publish Video"))
        assertEquals("Muqova rasmi", L10n.pick(AppLanguage.UZBEK, "صورة الغلاف", "Cover Photo"))
        assertEquals("Tahrirlash", L10n.pick(AppLanguage.UZBEK, "تعديل", "Edit"))
        assertEquals("O'chirish", L10n.pick(AppLanguage.UZBEK, "حذف", "Delete"))
        assertEquals("O'qildi ✓", L10n.pick(AppLanguage.UZBEK, "تمت الصلاة ✓", "Prayed ✓"))
        assertEquals("Vaqti hali kirmadi", L10n.pick(AppLanguage.UZBEK, "لم يدخل وقتها بعد", "Not entered yet"))
    }
}
