package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.domain.calendar.HijriCalendarEngine
import com.example.domain.model.AppLanguage
import com.example.domain.model.PrayerName
import com.example.domain.prayer.PrayerTimeCalculator
import com.example.notifications.PrayerNotificationController
import com.example.shortcuts.NoorShortcutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Interactive Liquid Glass Home Screen Widget displaying:
 * - Current Hijri & Gregorian Date + City
 * - Next upcoming prayer + Live Chronometer countdown + Current prayer period
 * - All 5 daily prayer times (Fajr, Dhuhr, Asr, Maghrib, Isha) with next prayer highlighted
 * - Full Arabic RTL & English LTR support
 */
class PrayerTimesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, PrayerTimesWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                onUpdate(context, mgr, ids)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.widgets.ACTION_REFRESH_PRAYER_WIDGET"

        fun requestUpdate(context: Context) {
            runCatching {
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(ComponentName(context, PrayerTimesWidgetProvider::class.java))
                if (ids.isNotEmpty()) {
                    val intent = Intent(context, PrayerTimesWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            }
        }

        private suspend fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val prefs = UserPreferencesRepository(context).getPreferencesOnce()
            val isAr = prefs.language == AppLanguage.ARABIC
            val locale = if (isAr) Locale("ar") else Locale.ENGLISH
            val tz = TimeZone.getTimeZone(prefs.timezoneId)
            val now = System.currentTimeMillis()

            val todayCal = Calendar.getInstance(tz).apply { timeInMillis = now }
            val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }

            val todaySchedule = PrayerTimeCalculator.calculateForCalendar(
                todayCal, prefs.latitude, prefs.longitude, prefs.calculationMethod, prefs.asrMadhhab, prefs.offsets
            )
            val tomorrowSchedule = PrayerTimeCalculator.calculateForCalendar(
                tomorrowCal, prefs.latitude, prefs.longitude, prefs.calculationMethod, prefs.asrMadhhab, prefs.offsets
            )
            val nextInfo = PrayerTimeCalculator.computeNextPrayer(now, todaySchedule, tomorrowSchedule)

            val hijri = HijriCalendarEngine.gregorianToHijri(
                todayCal.get(Calendar.YEAR),
                todayCal.get(Calendar.MONTH) + 1,
                todayCal.get(Calendar.DAY_OF_MONTH),
                prefs.hijriAdjustmentDays
            )

            val dateFmt = SimpleDateFormat("EEE, d MMM", locale).apply { timeZone = tz }
            val timeFmt = SimpleDateFormat("hh:mm", locale).apply { timeZone = tz }

            val dateLine = if (isAr) {
                "${hijri.day} ${hijri.monthNameAr} ${hijri.year} هـ · ${prefs.cityNameAr}"
            } else {
                "${hijri.day} ${hijri.monthNameEn} ${hijri.year} AH · ${dateFmt.format(Date(now))}"
            }

            val nextName = PrayerNotificationController.prayerLabel(nextInfo.nextPrayer, isAr)
            val currentPeriod = resolveCurrentPrayerPeriod(now, todaySchedule, isAr)
            val nextBanner = if (isAr) {
                "القادمة: $nextName · $currentPeriod"
            } else {
                "Next: $nextName · $currentPeriod"
            }

            val remainingMs = (nextInfo.nextPrayerMillis - now).coerceAtLeast(1_000L)
            val chronometerBase = SystemClock.elapsedRealtime() + remainingMs

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(NoorShortcutManager.EXTRA_SHORTCUT_ROUTE, NoorShortcutManager.TARGET_PRAYER_TIMES)
            }
            val openPending = PendingIntent.getActivity(
                context,
                6101,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val refreshIntent = Intent(context, PrayerTimesWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPending = PendingIntent.getBroadcast(
                context,
                6102,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val highlight = 0xFFE8C973.toInt()
            val normal = 0xFFE6F3EC.toInt()

            for (appWidgetId in appWidgetIds) {
                val rv = RemoteViews(context.packageName, R.layout.widget_prayer_times)
                rv.setTextViewText(R.id.widget_prayer_date, dateLine)
                rv.setTextViewText(R.id.widget_prayer_next_title, nextBanner)
                rv.setChronometer(R.id.widget_prayer_countdown, chronometerBase, null, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    rv.setChronometerCountDown(R.id.widget_prayer_countdown, true)
                }

                rv.setTextViewText(R.id.widget_lbl_fajr, PrayerNotificationController.prayerLabel(PrayerName.FAJR, isAr))
                rv.setTextViewText(R.id.widget_lbl_dhuhr, PrayerNotificationController.prayerLabel(PrayerName.DHUHR, isAr))
                rv.setTextViewText(R.id.widget_lbl_asr, PrayerNotificationController.prayerLabel(PrayerName.ASR, isAr))
                rv.setTextViewText(R.id.widget_lbl_maghrib, PrayerNotificationController.prayerLabel(PrayerName.MAGHRIB, isAr))
                rv.setTextViewText(R.id.widget_lbl_isha, PrayerNotificationController.prayerLabel(PrayerName.ISHA, isAr))

                rv.setTextViewText(R.id.widget_time_fajr, timeFmt.format(Date(todaySchedule.fajrMillis)))
                rv.setTextViewText(R.id.widget_time_dhuhr, timeFmt.format(Date(todaySchedule.dhuhrMillis)))
                rv.setTextViewText(R.id.widget_time_asr, timeFmt.format(Date(todaySchedule.asrMillis)))
                rv.setTextViewText(R.id.widget_time_maghrib, timeFmt.format(Date(todaySchedule.maghribMillis)))
                rv.setTextViewText(R.id.widget_time_isha, timeFmt.format(Date(todaySchedule.ishaMillis)))

                rv.setTextColor(R.id.widget_lbl_fajr, if (nextInfo.nextPrayer == PrayerName.FAJR) highlight else normal)
                rv.setTextColor(R.id.widget_time_fajr, if (nextInfo.nextPrayer == PrayerName.FAJR) highlight else normal)
                rv.setTextColor(R.id.widget_lbl_dhuhr, if (nextInfo.nextPrayer == PrayerName.DHUHR) highlight else normal)
                rv.setTextColor(R.id.widget_time_dhuhr, if (nextInfo.nextPrayer == PrayerName.DHUHR) highlight else normal)
                rv.setTextColor(R.id.widget_lbl_asr, if (nextInfo.nextPrayer == PrayerName.ASR) highlight else normal)
                rv.setTextColor(R.id.widget_time_asr, if (nextInfo.nextPrayer == PrayerName.ASR) highlight else normal)
                rv.setTextColor(R.id.widget_lbl_maghrib, if (nextInfo.nextPrayer == PrayerName.MAGHRIB) highlight else normal)
                rv.setTextColor(R.id.widget_time_maghrib, if (nextInfo.nextPrayer == PrayerName.MAGHRIB) highlight else normal)
                rv.setTextColor(R.id.widget_lbl_isha, if (nextInfo.nextPrayer == PrayerName.ISHA) highlight else normal)
                rv.setTextColor(R.id.widget_time_isha, if (nextInfo.nextPrayer == PrayerName.ISHA) highlight else normal)

                rv.setOnClickPendingIntent(R.id.widget_prayer_root, openPending)
                rv.setOnClickPendingIntent(R.id.widget_prayer_refresh, refreshPending)
                appWidgetManager.updateAppWidget(appWidgetId, rv)
            }
        }

        private fun resolveCurrentPrayerPeriod(
            now: Long,
            schedule: com.example.domain.model.DailyPrayerSchedule,
            isAr: Boolean
        ): String {
            return when {
                now < schedule.fajrMillis -> if (isAr) "وقت قيام الليل" else "Qiyam Period"
                now < schedule.sunriseMillis -> if (isAr) "وقت الفجر" else "Fajr Period"
                now < schedule.dhuhrMillis -> if (isAr) "وقت الضحى" else "Duha Period"
                now < schedule.asrMillis -> if (isAr) "وقت الظهر" else "Dhuhr Period"
                now < schedule.maghribMillis -> if (isAr) "وقت العصر" else "Asr Period"
                now < schedule.ishaMillis -> if (isAr) "وقت المغرب" else "Maghrib Period"
                else -> if (isAr) "وقت العشاء" else "Isha Period"
            }
        }
    }
}
