package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.prefs.UserPreferences
import com.example.domain.model.AppLanguage
import com.example.domain.model.PrayerName
import com.example.domain.prayer.PrayerTimeCalculator
import java.util.Calendar
import java.util.TimeZone

object NoorAlarmScheduler {

    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_BODY = "extra_body"
    const val EXTRA_CHANNEL = "extra_channel"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_IS_EXACT_PRAYER = "extra_is_exact_prayer"
    const val EXTRA_IS_TEST_ALARM = "extra_is_test_alarm"
    const val EXTRA_PRAYER_KEY = "extra_prayer_key"
    const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
    const val EXTRA_CUSTOM_VOICE_ID = "extra_custom_voice_id"

    private const val REQ_PRAYER_BASE = 2000
    private const val REQ_PRE_PRAYER_BASE = 2100
    const val REQ_MORNING_ADHKAR = 2201
    const val REQ_EVENING_ADHKAR = 2202
    const val REQ_QURAN_REMINDER = 2203
    const val REQ_TASBIH_REMINDER = 2204
    const val REQ_TAHAJJUD_REMINDER = 2205
    private const val REQ_TEST_ADHAN = 2299

    /**
     * Pure deterministic helper that computes the next daily trigger timestamp for (hour, minute).
     */
    fun computeNextTriggerMillis(
        hour: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, safeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis + 5_000L) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return cal.timeInMillis
    }

    fun reminderCopy(type: String, lang: AppLanguage): Pair<String, String> = when (type) {
        "morning" -> when (lang) {
            AppLanguage.ARABIC -> "تذكير صلاة الصبح وأذكار الصباح" to "أصبحنا وأصبح الملك لله — حافظ على صلاة الصبح وأذكار الصباح"
            AppLanguage.UZBEK -> "Bomdod va tonggi zikrlar eslatmasi" to "Bomdod namozi va tonggi zikrlarni o'qish vaqti keldi"
            AppLanguage.RUSSIAN -> "Напоминание о Фаджре и утренних азкарах" to "Начните утро с намаза Фаджр и поминания Аллаха"
            AppLanguage.ENGLISH -> "Morning Prayer & Adhkar Reminder" to "Begin your morning with Fajr prayer and remembrance of Allah"
        }
        "evening" -> when (lang) {
            AppLanguage.ARABIC -> "تذكير المساء وأذكار المساء" to "أمسينا وأمسى الملك لله — حافظ على ورد أذكار المساء والسكينة"
            AppLanguage.UZBEK -> "Kechki zikrlar eslatmasi" to "Kechki zikrlar va sokinlik virdini o'qish vaqti keldi"
            AppLanguage.RUSSIAN -> "Напоминание о вечерних азкарах" to "Время вечерних азкаров и спокойного поминания Аллаха"
            AppLanguage.ENGLISH -> "Evening Adhkar Reminder" to "Keep your evening Adhkar routine and peaceful remembrance"
        }
        "tahajjud" -> when (lang) {
            AppLanguage.ARABIC -> "تذكير صلاة التهجد وقيام الليل" to "قم في الثلث الأخير من الليل — وقت استجابة الدعاء ومناجاة الله"
            AppLanguage.UZBEK -> "Tahajjud va tungi namoz eslatmasi" to "Kechaning so'nggi uchdan bir qismida Tahajjud va duo vaqti keldi"
            AppLanguage.RUSSIAN -> "Напоминание о Тахаджуде и ночной молитве" to "Благословенная последняя треть ночи — время молитвы и дуа"
            AppLanguage.ENGLISH -> "Tahajjud & Qiyam Al-Layl Reminder" to "Rise for the blessed night prayer and heartfelt supplication"
        }
        "quran" -> when (lang) {
            AppLanguage.ARABIC -> "ورد القرآن اليومي" to "خصّص دقائق لتلاوة كتاب الله اليوم"
            AppLanguage.UZBEK -> "Kunlik Qur'on virdi" to "Bugun Qur'oni Karim tilovati uchun bir necha daqiqa ajrating"
            AppLanguage.RUSSIAN -> "Ежедневное чтение Корана" to "Уделите несколько минут чтению Священного Корана сегодня"
            AppLanguage.ENGLISH -> "Daily Quran Reading" to "Take a peaceful moment to read the Holy Quran"
        }
        else -> when (lang) {
            AppLanguage.ARABIC -> "تذكير التسبيح" to "سبحان الله والحمد لله والله أكبر"
            AppLanguage.UZBEK -> "Tasbeh eslatmasi" to "Subhanalloh, Alhamdulillah, Allohu Akbar"
            AppLanguage.RUSSIAN -> "Напоминание о тасбихе" to "Субханаллах, Альхамдулиллях, Аллаху Акбар"
            AppLanguage.ENGLISH -> "Tasbih Reminder" to "SubhanAllah, Alhamdulillah, Allahu Akbar"
        }
    }

    fun rescheduleAll(context: Context, prefs: UserPreferences) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val tz = TimeZone.getTimeZone(prefs.timezoneId)
        val now = System.currentTimeMillis()
        val lang = prefs.language

        val todayCal = Calendar.getInstance(tz)
        val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }

        val todaySchedule = PrayerTimeCalculator.calculateForCalendar(
            todayCal, prefs.latitude, prefs.longitude, prefs.calculationMethod, prefs.asrMadhhab, prefs.offsets
        )
        val tomorrowSchedule = PrayerTimeCalculator.calculateForCalendar(
            tomorrowCal, prefs.latitude, prefs.longitude, prefs.calculationMethod, prefs.asrMadhhab, prefs.offsets
        )

        // Schedule each Fard prayer & optional pre-prayer alert
        PrayerName.fardPrayers.forEachIndexed { idx, prayer ->
            val exactReq = REQ_PRAYER_BASE + idx
            val preReq = REQ_PRE_PRAYER_BASE + idx

            if (prefs.isPrayerNotificationEnabled(prayer)) {
                val targetTime = if (todaySchedule.timeFor(prayer) > now + 5_000L) {
                    todaySchedule.timeFor(prayer)
                } else {
                    tomorrowSchedule.timeFor(prayer)
                }
                val pName = prayerNameForLang(prayer, lang)
                val (prayerTitle, prayerBody) = when (lang) {
                    AppLanguage.ARABIC -> "حان الآن موعد صلاة $pName" to "حيّ على الصلاة — ${prefs.cityNameAr}"
                    AppLanguage.UZBEK -> "$pName namozi vaqti kirdi" to "Namozga shoshiling — ${prefs.cityNameEn}"
                    AppLanguage.RUSSIAN -> "Наступило время намаза $pName" to "Спешите к намазу — ${prefs.cityNameEn}"
                    AppLanguage.ENGLISH -> "Time for $pName Prayer" to "It is time for $pName in ${prefs.cityNameEn}"
                }

                scheduleSingleAlarm(
                    context = context,
                    alarmManager = alarmManager,
                    requestCode = exactReq,
                    triggerAtMillis = targetTime,
                    channelId = NoorNotificationHelper.CHANNEL_PRAYER,
                    title = prayerTitle,
                    body = prayerBody,
                    isExactPrayer = true,
                    prayerKey = prayer.name
                )

                if (prefs.prePrayerAlertMinutes > 0) {
                    val preTime = targetTime - prefs.prePrayerAlertMinutes * 60_000L
                    if (preTime > now + 5_000L) {
                        val (preTitle, preBody) = when (lang) {
                            AppLanguage.ARABIC -> "تذكير قبل صلاة $pName" to
                                "متبقي ${prefs.prePrayerAlertMinutes} دقيقة على دخول وقت صلاة $pName"
                            AppLanguage.UZBEK -> "$pName namozidan oldin eslatma" to
                                "$pName namozigacha ${prefs.prePrayerAlertMinutes} daqiqa qoldi"
                            AppLanguage.RUSSIAN -> "Скоро намаз $pName" to
                                "До намаза $pName осталось ${prefs.prePrayerAlertMinutes} мин."
                            AppLanguage.ENGLISH -> "Upcoming $pName Prayer" to
                                "${prefs.prePrayerAlertMinutes} minutes remaining until $pName"
                        }
                        scheduleSingleAlarm(
                            context = context,
                            alarmManager = alarmManager,
                            requestCode = preReq,
                            triggerAtMillis = preTime,
                            channelId = NoorNotificationHelper.CHANNEL_PRAYER,
                            title = preTitle,
                            body = preBody
                        )
                    } else {
                        cancelAlarm(context, alarmManager, preReq)
                    }
                } else {
                    cancelAlarm(context, alarmManager, preReq)
                }
            } else {
                cancelAlarm(context, alarmManager, exactReq)
                cancelAlarm(context, alarmManager, preReq)
            }
        }

        // Daily Morning, Evening, Tahajjud/Qiyam Al-Layl, Quran, and Tasbih reminders
        val (morningTitle, morningBody) = reminderCopy("morning", lang)
        scheduleDailyReminder(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQ_MORNING_ADHKAR,
            enabled = prefs.morningAdhkarReminderEnabled,
            hour = prefs.morningAdhkarHour,
            minute = prefs.morningAdhkarMinute,
            title = morningTitle,
            body = morningBody,
            reminderType = "morning",
            customVoiceId = prefs.morningReminderSoundId
        )

        val (eveningTitle, eveningBody) = reminderCopy("evening", lang)
        scheduleDailyReminder(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQ_EVENING_ADHKAR,
            enabled = prefs.eveningAdhkarReminderEnabled,
            hour = prefs.eveningAdhkarHour,
            minute = prefs.eveningAdhkarMinute,
            title = eveningTitle,
            body = eveningBody,
            reminderType = "evening",
            customVoiceId = prefs.eveningReminderSoundId
        )

        val (tahajjudTitle, tahajjudBody) = reminderCopy("tahajjud", lang)
        scheduleDailyReminder(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQ_TAHAJJUD_REMINDER,
            enabled = prefs.tahajjudReminderEnabled,
            hour = prefs.tahajjudReminderHour,
            minute = prefs.tahajjudReminderMinute,
            title = tahajjudTitle,
            body = tahajjudBody,
            reminderType = "tahajjud",
            customVoiceId = prefs.tahajjudReminderSoundId
        )

        val (quranTitle, quranBody) = reminderCopy("quran", lang)
        scheduleDailyReminder(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQ_QURAN_REMINDER,
            enabled = prefs.quranReminderEnabled,
            hour = prefs.quranReminderHour,
            minute = prefs.quranReminderMinute,
            title = quranTitle,
            body = quranBody,
            reminderType = "quran"
        )

        val (tasbihTitle, tasbihBody) = reminderCopy("tasbih", lang)
        scheduleDailyReminder(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQ_TASBIH_REMINDER,
            enabled = prefs.tasbihReminderEnabled,
            hour = prefs.tasbihReminderHour,
            minute = prefs.tasbihReminderMinute,
            title = tasbihTitle,
            body = tasbihBody,
            reminderType = "tasbih"
        )

        // Sync persistent prayer notification bar and Prayer Times Home Screen Widget
        PrayerNotificationController.updateOrCancel(context, prefs)
        com.example.widgets.PrayerTimesWidgetProvider.requestUpdate(context)
    }

    private fun scheduleDailyReminder(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        enabled: Boolean,
        hour: Int,
        minute: Int,
        title: String,
        body: String,
        reminderType: String? = null,
        customVoiceId: String? = null
    ) {
        if (!enabled) {
            cancelAlarm(context, alarmManager, requestCode)
            return
        }
        val triggerMillis = computeNextTriggerMillis(hour, minute)
        scheduleSingleAlarm(
            context = context,
            alarmManager = alarmManager,
            requestCode = requestCode,
            triggerAtMillis = triggerMillis,
            channelId = NoorNotificationHelper.CHANNEL_REMINDER,
            title = title,
            body = body,
            reminderType = reminderType,
            customVoiceId = customVoiceId
        )
    }

    fun scheduleTestPrayerAlarm(
        context: Context,
        prayer: PrayerName,
        delaySeconds: Int,
        isArabic: Boolean
    ): Long {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return 0L
        val triggerAtMillis = System.currentTimeMillis() + delaySeconds.coerceIn(5, 600) * 1000L
        val pNameAr = prayerNameForLang(prayer, AppLanguage.ARABIC)
        val pNameEn = prayerNameForLang(prayer, AppLanguage.ENGLISH)
        scheduleSingleAlarm(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQ_TEST_ADHAN,
            triggerAtMillis = triggerAtMillis,
            channelId = NoorNotificationHelper.CHANNEL_PRAYER,
            title = if (isArabic) "تجربة أذان صلاة $pNameAr" else "Scheduled Test Adhan · $pNameEn",
            body = if (isArabic) "اختبار الأذان التلقائي في الخلفية بصوت المؤذن المختار" else "Background Adhan verification with selected voice",
            isExactPrayer = true,
            prayerKey = prayer.name,
            isTestAlarm = true
        )
        return triggerAtMillis
    }

    private fun scheduleSingleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        triggerAtMillis: Long,
        channelId: String,
        title: String,
        body: String,
        isExactPrayer: Boolean = false,
        prayerKey: String? = null,
        isTestAlarm: Boolean = false,
        reminderType: String? = null,
        customVoiceId: String? = null
    ) {
        val intent = Intent(context, NoorAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_CHANNEL, channelId)
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(EXTRA_IS_EXACT_PRAYER, isExactPrayer)
            putExtra(EXTRA_IS_TEST_ALARM, isTestAlarm)
            if (prayerKey != null) {
                putExtra(EXTRA_PRAYER_KEY, prayerKey)
            }
            if (!reminderType.isNullOrBlank()) {
                putExtra(EXTRA_REMINDER_TYPE, reminderType)
            }
            if (!customVoiceId.isNullOrBlank()) {
                putExtra(EXTRA_CUSTOM_VOICE_ID, customVoiceId)
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canUseExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canUseExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, NoorAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun prayerNameForLang(prayer: PrayerName, lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> when (prayer) {
            PrayerName.FAJR -> "الفجر"
            PrayerName.SUNRISE -> "الشروق"
            PrayerName.DHUHR -> "الظهر"
            PrayerName.ASR -> "العصر"
            PrayerName.MAGHRIB -> "المغرب"
            PrayerName.ISHA -> "العشاء"
        }
        AppLanguage.UZBEK -> when (prayer) {
            PrayerName.FAJR -> "Bomdod"
            PrayerName.SUNRISE -> "Quyosh"
            PrayerName.DHUHR -> "Peshin"
            PrayerName.ASR -> "Asr"
            PrayerName.MAGHRIB -> "Shom"
            PrayerName.ISHA -> "Xufton"
        }
        AppLanguage.RUSSIAN -> when (prayer) {
            PrayerName.FAJR -> "Фаджр"
            PrayerName.SUNRISE -> "Восход"
            PrayerName.DHUHR -> "Зухр"
            PrayerName.ASR -> "Аср"
            PrayerName.MAGHRIB -> "Магриб"
            PrayerName.ISHA -> "Иша"
        }
        AppLanguage.ENGLISH -> when (prayer) {
            PrayerName.FAJR -> "Fajr"
            PrayerName.SUNRISE -> "Sunrise"
            PrayerName.DHUHR -> "Dhuhr"
            PrayerName.ASR -> "Asr"
            PrayerName.MAGHRIB -> "Maghrib"
            PrayerName.ISHA -> "Isha"
        }
    }
}
