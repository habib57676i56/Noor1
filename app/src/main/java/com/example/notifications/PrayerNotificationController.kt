package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.prefs.UserPreferences
import com.example.domain.model.AppLanguage
import com.example.domain.model.PrayerName
import com.example.domain.prayer.PrayerTimeCalculator
import com.example.ui.i18n.L10n
import java.util.Calendar
import java.util.TimeZone

/**
 * Manages the persistent, low-priority, battery-friendly Prayer Times Notification Panel.
 * Uses Android's native RemoteViews Chronometer countdown so the remaining time ticks live
 * every second in the status bar / notification shade even when NOOR is completely closed,
 * with zero background CPU wakeups.
 */
object PrayerNotificationController {

    const val CHANNEL_PERSISTENT_PRAYER = "noor_persistent_prayer_bar"
    const val NOTIFICATION_ID_PERSISTENT_PRAYER = 4301
    const val REQ_REFRESH_PERSISTENT = 4302
    const val ACTION_REFRESH_PERSISTENT_PRAYER = "com.example.notifications.ACTION_REFRESH_PERSISTENT_PRAYER"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_PERSISTENT_PRAYER,
                "NOOR Prayer Times Bar",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Compact persistent notification showing all 5 daily prayers and countdown"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun updateOrCancel(context: Context, prefs: UserPreferences) {
        if (!prefs.persistentPrayerNotificationEnabled) {
            cancel(context)
            return
        }
        if (!NoorNotificationHelper.hasNotificationPermission(context)) return
        ensureChannel(context)

        val now = System.currentTimeMillis()
        val tz = TimeZone.getTimeZone(prefs.timezoneId)
        val isAr = prefs.language == AppLanguage.ARABIC

        val todayCal = Calendar.getInstance(tz).apply { timeInMillis = now }
        val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }

        val todaySchedule = PrayerTimeCalculator.calculateForCalendar(
            todayCal, prefs.latitude, prefs.longitude, prefs.calculationMethod, prefs.asrMadhhab, prefs.offsets
        )
        val tomorrowSchedule = PrayerTimeCalculator.calculateForCalendar(
            tomorrowCal, prefs.latitude, prefs.longitude, prefs.calculationMethod, prefs.asrMadhhab, prefs.offsets
        )
        val nextInfo = PrayerTimeCalculator.computeNextPrayer(now, todaySchedule, tomorrowSchedule)

        val fajrStr = L10n.formatSystemTime(context, todaySchedule.fajrMillis, prefs.timezoneId, prefs.language, compact = true)
        val dhuhrStr = L10n.formatSystemTime(context, todaySchedule.dhuhrMillis, prefs.timezoneId, prefs.language, compact = true)
        val asrStr = L10n.formatSystemTime(context, todaySchedule.asrMillis, prefs.timezoneId, prefs.language, compact = true)
        val maghribStr = L10n.formatSystemTime(context, todaySchedule.maghribMillis, prefs.timezoneId, prefs.language, compact = true)
        val ishaStr = L10n.formatSystemTime(context, todaySchedule.ishaMillis, prefs.timezoneId, prefs.language, compact = true)

        val nextName = prayerLabel(nextInfo.nextPrayer, isAr)
        val cityLabel = if (isAr) prefs.cityNameAr else prefs.cityNameEn
        val nextHeader = if (isAr) {
            "الصلاة القادمة: $nextName · $cityLabel"
        } else {
            "Next: $nextName · $cityLabel"
        }

        val remainingMs = (nextInfo.nextPrayerMillis - now).coerceAtLeast(1_000L)
        val chronometerBase = SystemClock.elapsedRealtime() + remainingMs

        val compactView = buildRemoteView(
            context = context,
            layoutId = R.layout.notification_prayer_bar_compact,
            isAr = isAr,
            nextHeader = nextHeader,
            chronometerBase = chronometerBase,
            nextPrayer = nextInfo.nextPrayer,
            fajrStr = fajrStr,
            dhuhrStr = dhuhrStr,
            asrStr = asrStr,
            maghribStr = maghribStr,
            ishaStr = ishaStr
        )

        val expandedView = buildRemoteView(
            context = context,
            layoutId = R.layout.notification_prayer_bar_expanded,
            isAr = isAr,
            nextHeader = nextHeader,
            chronometerBase = chronometerBase,
            nextPrayer = nextInfo.nextPrayer,
            fajrStr = fajrStr,
            dhuhrStr = dhuhrStr,
            asrStr = asrStr,
            maghribStr = maghribStr,
            ishaStr = ishaStr
        )

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_shortcut_route", "prayers")
        }
        val contentPending = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_PERSISTENT_PRAYER,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summaryFallback = if (isAr) {
            "الفجر $fajrStr · الظهر $dhuhrStr · العصر $asrStr · المغرب $maghribStr · العشاء $ishaStr"
        } else {
            "Fajr $fajrStr · Dhuhr $dhuhrStr · Asr $asrStr · Maghrib $maghribStr · Isha $ishaStr"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_PERSISTENT_PRAYER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(nextHeader)
            .setContentText(summaryFallback)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(compactView)
            .setCustomBigContentView(expandedView)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PERSISTENT_PRAYER, notification)
        } catch (_: SecurityException) {
        }

        scheduleNextRefresh(context, nextInfo.nextPrayerMillis + 2_000L)
    }

    private fun buildRemoteView(
        context: Context,
        layoutId: Int,
        isAr: Boolean,
        nextHeader: String,
        chronometerBase: Long,
        nextPrayer: PrayerName,
        fajrStr: String,
        dhuhrStr: String,
        asrStr: String,
        maghribStr: String,
        ishaStr: String
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, layoutId)
        rv.setTextViewText(R.id.notif_next_header, nextHeader)
        rv.setChronometer(R.id.notif_countdown, chronometerBase, null, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            rv.setChronometerCountDown(R.id.notif_countdown, true)
        }

        if (layoutId == R.layout.notification_prayer_bar_expanded) {
            rv.setTextViewText(R.id.notif_lbl_fajr, prayerLabel(PrayerName.FAJR, isAr))
            rv.setTextViewText(R.id.notif_lbl_dhuhr, prayerLabel(PrayerName.DHUHR, isAr))
            rv.setTextViewText(R.id.notif_lbl_asr, prayerLabel(PrayerName.ASR, isAr))
            rv.setTextViewText(R.id.notif_lbl_maghrib, prayerLabel(PrayerName.MAGHRIB, isAr))
            rv.setTextViewText(R.id.notif_lbl_isha, prayerLabel(PrayerName.ISHA, isAr))

            rv.setTextViewText(R.id.notif_time_fajr, fajrStr)
            rv.setTextViewText(R.id.notif_time_dhuhr, dhuhrStr)
            rv.setTextViewText(R.id.notif_time_asr, asrStr)
            rv.setTextViewText(R.id.notif_time_maghrib, maghribStr)
            rv.setTextViewText(R.id.notif_time_isha, ishaStr)

            val highlightColor = 0xFFE8C973.toInt()
            val normalColor = 0xFFDCEAE3.toInt()

            rv.setTextColor(R.id.notif_lbl_fajr, if (nextPrayer == PrayerName.FAJR) highlightColor else normalColor)
            rv.setTextColor(R.id.notif_time_fajr, if (nextPrayer == PrayerName.FAJR) highlightColor else normalColor)
            rv.setTextColor(R.id.notif_lbl_dhuhr, if (nextPrayer == PrayerName.DHUHR) highlightColor else normalColor)
            rv.setTextColor(R.id.notif_time_dhuhr, if (nextPrayer == PrayerName.DHUHR) highlightColor else normalColor)
            rv.setTextColor(R.id.notif_lbl_asr, if (nextPrayer == PrayerName.ASR) highlightColor else normalColor)
            rv.setTextColor(R.id.notif_time_asr, if (nextPrayer == PrayerName.ASR) highlightColor else normalColor)
            rv.setTextColor(R.id.notif_lbl_maghrib, if (nextPrayer == PrayerName.MAGHRIB) highlightColor else normalColor)
            rv.setTextColor(R.id.notif_time_maghrib, if (nextPrayer == PrayerName.MAGHRIB) highlightColor else normalColor)
            rv.setTextColor(R.id.notif_lbl_isha, if (nextPrayer == PrayerName.ISHA) highlightColor else normalColor)
            rv.setTextColor(R.id.notif_time_isha, if (nextPrayer == PrayerName.ISHA) highlightColor else normalColor)
        } else {
            val line = if (isAr) {
                "فجر $fajrStr · ظهر $dhuhrStr · عصر $asrStr · مغرب $maghribStr · عشاء $ishaStr"
            } else {
                "Fajr $fajrStr · Dhuhr $dhuhrStr · Asr $asrStr · Maghrib $maghribStr · Isha $ishaStr"
            }
            rv.setTextViewText(R.id.notif_compact_times, line)
        }
        return rv
    }

    private fun scheduleNextRefresh(context: Context, triggerAtMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NoorAlarmReceiver::class.java).apply {
            action = ACTION_REFRESH_PERSISTENT_PRAYER
        }
        val pi = PendingIntent.getBroadcast(
            context,
            REQ_REFRESH_PERSISTENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            am.setAndAllowWhileIdle(AlarmManager.RTC, triggerAtMillis, pi)
        } catch (_: SecurityException) {
        }
    }

    fun cancel(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_PERSISTENT_PRAYER)
        } catch (_: Exception) {
        }
    }

    fun prayerLabel(prayer: PrayerName, isArabic: Boolean): String = when (prayer) {
        PrayerName.FAJR -> if (isArabic) "الفجر" else "Fajr"
        PrayerName.SUNRISE -> if (isArabic) "الشروق" else "Sunrise"
        PrayerName.DHUHR -> if (isArabic) "الظهر" else "Dhuhr"
        PrayerName.ASR -> if (isArabic) "العصر" else "Asr"
        PrayerName.MAGHRIB -> if (isArabic) "المغرب" else "Maghrib"
        PrayerName.ISHA -> if (isArabic) "العشاء" else "Isha"
    }
}
