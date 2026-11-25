package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AdhanCatalog
import com.example.data.local.prefs.UserPreferences
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.domain.model.AppLanguage
import com.example.notifications.NoorAlarmScheduler
import com.example.ui.components.formatReminderTime
import com.example.ui.i18n.L10n
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoorLiquidGlassAndRemindersTest {

    @Test
    fun computeNextTriggerMillis_schedulesSameDayWhenFuture_andRollsToTomorrowWhenPassed() {
        val tz = TimeZone.getTimeZone("UTC")
        // 01:00 UTC -> Tahajjud at 02:30 should trigger same day
        val earlyMorning = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 25, 1, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sameDayTrigger = NoorAlarmScheduler.computeNextTriggerMillis(
            hour = 2,
            minute = 30,
            nowMillis = earlyMorning,
            timeZone = tz
        )
        val sameDayCal = Calendar.getInstance(tz).apply { timeInMillis = sameDayTrigger }
        assertEquals(25, sameDayCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(2, sameDayCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, sameDayCal.get(Calendar.MINUTE))

        // 22:00 UTC -> Tahajjud at 02:30 should roll over to next day (Nov 26)
        val lateNight = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 25, 22, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val nextDayTrigger = NoorAlarmScheduler.computeNextTriggerMillis(
            hour = 2,
            minute = 30,
            nowMillis = lateNight,
            timeZone = tz
        )
        val nextDayCal = Calendar.getInstance(tz).apply { timeInMillis = nextDayTrigger }
        assertEquals(26, nextDayCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(2, nextDayCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, nextDayCal.get(Calendar.MINUTE))
    }

    @Test
    fun reminderCopy_supportsArabicEnglishAndUzbek_forMorningEveningAndTahajjud() {
        val (arTitle, arBody) = NoorAlarmScheduler.reminderCopy("tahajjud", AppLanguage.ARABIC)
        assertTrue(arTitle.contains("التهجد"))
        assertTrue(arBody.contains("الليل"))

        val (enTitle, enBody) = NoorAlarmScheduler.reminderCopy("tahajjud", AppLanguage.ENGLISH)
        assertTrue(enTitle.contains("Tahajjud"))
        assertTrue(enBody.isNotBlank())

        val (uzTitle, uzBody) = NoorAlarmScheduler.reminderCopy("tahajjud", AppLanguage.UZBEK)
        assertTrue(uzTitle.contains("Tahajjud"))
        assertTrue(uzBody.contains("Tahajjud"))

        val (uzMorningTitle, _) = NoorAlarmScheduler.reminderCopy("morning", AppLanguage.UZBEK)
        assertTrue(uzMorningTitle.contains("Bomdod"))

        val (uzEveningTitle, _) = NoorAlarmScheduler.reminderCopy("evening", AppLanguage.UZBEK)
        assertTrue(uzEveningTitle.contains("Kechki"))
    }

    @Test
    fun rescheduleAll_schedulesMorningEveningAndTahajjudAlarmsWithCustomSounds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        val prefs = UserPreferences(
            language = AppLanguage.ARABIC,
            morningAdhkarReminderEnabled = true,
            morningAdhkarHour = 5,
            morningAdhkarMinute = 45,
            morningReminderSoundId = "adhan_makkah",
            eveningAdhkarReminderEnabled = true,
            eveningAdhkarHour = 17,
            eveningAdhkarMinute = 15,
            eveningReminderSoundId = "adhan_madinah",
            tahajjudReminderEnabled = true,
            tahajjudReminderHour = 2,
            tahajjudReminderMinute = 30,
            tahajjudReminderSoundId = "adhan_alafasy"
        )

        NoorAlarmScheduler.rescheduleAll(context, prefs)

        val scheduledIntents = shadowAlarmManager.scheduledAlarms.mapNotNull { alarm ->
            shadowOf(alarm.operation).savedIntent
        }

        val morningIntent = scheduledIntents.firstOrNull {
            it.getStringExtra(NoorAlarmScheduler.EXTRA_REMINDER_TYPE) == "morning"
        }
        val eveningIntent = scheduledIntents.firstOrNull {
            it.getStringExtra(NoorAlarmScheduler.EXTRA_REMINDER_TYPE) == "evening"
        }
        val tahajjudIntent = scheduledIntents.firstOrNull {
            it.getStringExtra(NoorAlarmScheduler.EXTRA_REMINDER_TYPE) == "tahajjud"
        }

        assertNotNull("Morning reminder alarm must be scheduled", morningIntent)
        assertNotNull("Evening reminder alarm must be scheduled", eveningIntent)
        assertNotNull("Tahajjud reminder alarm must be scheduled", tahajjudIntent)

        assertEquals("adhan_makkah", morningIntent?.getStringExtra(NoorAlarmScheduler.EXTRA_CUSTOM_VOICE_ID))
        assertEquals("adhan_madinah", eveningIntent?.getStringExtra(NoorAlarmScheduler.EXTRA_CUSTOM_VOICE_ID))
        assertEquals("adhan_alafasy", tahajjudIntent?.getStringExtra(NoorAlarmScheduler.EXTRA_CUSTOM_VOICE_ID))
    }

    @Test
    fun userPreferencesRepository_persistsTahajjudTimeAndCustomReminderSounds() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = UserPreferencesRepository(context)

        repo.setReminder("tahajjud", enabled = true, hour = 3, minute = 15)
        repo.setReminder("morning", enabled = true, hour = 5, minute = 30)
        repo.setReminder("evening", enabled = false, hour = 18, minute = 0)
        repo.setReminderSound("tahajjud", "adhan_alafasy")
        repo.setReminderSound("morning", "adhan_makkah")
        repo.setReminderSound("evening", "adhan_madinah")

        val saved = repo.getPreferencesOnce()
        assertTrue(saved.tahajjudReminderEnabled)
        assertEquals(3, saved.tahajjudReminderHour)
        assertEquals(15, saved.tahajjudReminderMinute)
        assertEquals("adhan_alafasy", saved.tahajjudReminderSoundId)

        assertTrue(saved.morningAdhkarReminderEnabled)
        assertEquals(5, saved.morningAdhkarHour)
        assertEquals(30, saved.morningAdhkarMinute)
        assertEquals("adhan_makkah", saved.morningReminderSoundId)

        assertFalse(saved.eveningAdhkarReminderEnabled)
        assertEquals(18, saved.eveningAdhkarHour)
        assertEquals(0, saved.eveningAdhkarMinute)
        assertEquals("adhan_madinah", saved.eveningReminderSoundId)
    }

    @Test
    fun adhanCatalog_validatesMp3M4aAndWavAudioFormats() {
        assertEquals("mp3", AdhanCatalog.validateFormat("tahajjud.mp3", "audio/mpeg"))
        assertEquals("m4a", AdhanCatalog.validateFormat("morning.m4a", "audio/mp4"))
        assertEquals("wav", AdhanCatalog.validateFormat("evening.wav", "audio/wav"))
    }

    @Test
    fun formatReminderTime_andUzbekLocalization_workAcrossLanguages() {
        assertEquals("02:30 ص", formatReminderTime(2, 30, AppLanguage.ARABIC))
        assertEquals("05:30 م", formatReminderTime(17, 30, AppLanguage.ARABIC))
        assertEquals("02:30 AM", formatReminderTime(2, 30, AppLanguage.ENGLISH))
        assertEquals("17:30", formatReminderTime(17, 30, AppLanguage.UZBEK))

        assertEquals(
            "Tahajjud va tungi namoz (Qiyomul-layl)",
            L10n.pick(AppLanguage.UZBEK, "تذكير صلاة التهجد وقيام الليل", "Tahajjud & Qiyam Al-Layl")
        )
        assertEquals(
            "Qurilmadan audio fayl tanlash",
            L10n.pick(AppLanguage.UZBEK, "استيراد صوت", "Import Audio from Device")
        )
    }
}
