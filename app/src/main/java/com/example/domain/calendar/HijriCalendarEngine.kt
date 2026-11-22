package com.example.domain.calendar

import android.os.Build
import com.example.domain.model.HijriDateModel
import com.example.domain.model.IslamicOccasion
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar
import kotlin.math.floor

data class CalendarDayCell(
    val gregorianYear: Int,
    val gregorianMonth: Int,
    val gregorianDay: Int,
    val dayOfWeek: Int, // Calendar.SUNDAY .. SATURDAY
    val hijri: HijriDateModel,
    val isToday: Boolean,
    val isWhiteDay: Boolean,
    val isSunnahFastDay: Boolean,
    val isMonthStart: Boolean,
    val isRamadan: Boolean = false,
    val isLaylatAlQadrNight: Boolean = false,
    val occasionNameAr: String? = null,
    val occasionNameEn: String? = null
)

object HijriCalendarEngine {

    val hijriMonthsAr = listOf(
        "محرّم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوّال", "ذو القعدة", "ذو الحجة"
    )

    val hijriMonthsEn = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    val gregorianMonthsAr = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    val gregorianMonthsEn = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val weekDaysAr = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    val weekDaysEn = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    private data class OccasionDef(
        val id: String,
        val month: Int,
        val day: Int,
        val nameAr: String,
        val nameEn: String,
        val descAr: String,
        val descEn: String
    )

    private val occasions = listOf(
        OccasionDef("new_year", 1, 1, "رأس السنة الهجرية", "Islamic New Year", "بداية العام الهجري الجديد وذكرى الهجرة النبوية.", "Beginning of the new Hijri year."),
        OccasionDef("tasua", 1, 9, "يوم تاسوعاء", "Day of Tasu'a", "يُستحب صيامه مع عاشوراء اتباعاً للسنة النبوية.", "Recommended fasting day paired with Ashura."),
        OccasionDef("ashura", 1, 10, "يوم عاشوراء", "Day of Ashura", "يُستحب صيامه؛ يكفّر السنة التي قبله (صحيح مسلم).", "Recommended fasting day expiating the previous year (Sahih Muslim)."),
        OccasionDef("mawlid", 3, 12, "ذكرى المولد النبوي", "12 Rabi' al-Awwal", "ذكرى مولد النبي محمد ﷺ في شهر ربيع الأول.", "Historical date of the birth of Prophet Muhammad (PBUH)."),
        OccasionDef("isra_miraj", 7, 27, "الإسراء والمعراج", "Isra and Mi'raj", "ذكرى رحلة الإسراء والمعراج وفرض الصلوات الخمس.", "Night Journey and Ascension when five daily prayers were prescribed."),
        OccasionDef("shaban_mid", 8, 15, "النصف من شعبان", "Mid-Sha'ban", "من الأيام البيض في شعبان، شهر تُرفع فيه الأعمال.", "Middle White Day of Sha'ban, a month of abundant Sunnah fasting."),
        OccasionDef("ramadan_start", 9, 1, "بداية شهر رمضان", "Start of Ramadan", "أول أيام شهر الصيام والقرآن والرحمة والمغفرة.", "First day of the blessed month of fasting and Quran."),
        OccasionDef("last_ten_start", 9, 21, "بداية العشر الأواخر", "Last 10 Nights of Ramadan", "أولى الليالي الوترية المرجو فيها ليلة القدر.", "First odd night of the blessed last ten nights seeking Laylat al-Qadr."),
        OccasionDef("laylat_qadr", 9, 27, "ليلة السابع والعشرين (ليلة القدر)", "27th Night (Laylat al-Qadr)", "من أرجى الليالي الوترية لليلة القدر، خير من ألف شهر.", "Most anticipated odd night for Laylat al-Qadr, better than a thousand months."),
        OccasionDef("eid_fitr", 10, 1, "عيد الفطر المبارك", "Eid al-Fitr", "يوم الجائزة والفرح بعد إتمام صيام شهر رمضان (يحرم صيامه).", "Festival of Breaking the Fast following Ramadan (fasting prohibited)."),
        OccasionDef("arafah", 12, 9, "يوم عرفة", "Day of Arafah", "أعظم أيام الدعاء؛ صيامه لغير الحاج يكفر سنتين (صحيح مسلم).", "Supreme day of supplication; fasting expiates two years (Sahih Muslim)."),
        OccasionDef("eid_adha", 12, 10, "عيد الأضحى المبارك", "Eid al-Adha", "يوم النحر الأكبر وموسم الحج والأضاحي (يحرم صيامه).", "Festival of Sacrifice during the season of Hajj (fasting prohibited).")
    )

    fun gregorianToHijri(
        year: Int,
        month: Int, // 1..12
        day: Int,   // 1..31
        dayAdjustment: Int = 0
    ): HijriDateModel {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            add(Calendar.DAY_OF_MONTH, dayAdjustment)
        }
        val adjY = cal.get(Calendar.YEAR)
        val adjM = cal.get(Calendar.MONTH) + 1
        val adjD = cal.get(Calendar.DAY_OF_MONTH)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val localDate = LocalDate.of(adjY, adjM, adjD)
                val hijrah = HijrahDate.from(localDate)
                val hYear = hijrah.get(ChronoField.YEAR)
                val hMonth = hijrah.get(ChronoField.MONTH_OF_YEAR)
                val hDay = hijrah.get(ChronoField.DAY_OF_MONTH)
                HijriDateModel(
                    year = hYear,
                    month = hMonth,
                    day = hDay,
                    monthNameAr = hijriMonthsAr.getOrElse(hMonth - 1) { "" },
                    monthNameEn = hijriMonthsEn.getOrElse(hMonth - 1) { "" }
                )
            } catch (_: Exception) {
                kuwaitiFallback(adjY, adjM, adjD)
            }
        } else {
            kuwaitiFallback(adjY, adjM, adjD)
        }
    }

    /**
     * Converts a Hijri date (Umm al-Qura tabular/calendar) to Gregorian Calendar.
     * Supports interactive Hijri <-> Gregorian date converter.
     */
    fun hijriToGregorian(
        hYear: Int,
        hMonth: Int,
        hDay: Int,
        dayAdjustment: Int = 0
    ): Calendar {
        val safeYear = hYear.coerceIn(1350, 1550)
        val safeMonth = hMonth.coerceIn(1, 12)
        val safeDay = hDay.coerceIn(1, 30)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val clampedDay = safeDay.coerceAtMost(29)
                val baseHijrah = HijrahDate.of(safeYear, safeMonth, clampedDay)
                val maxDays = baseHijrah.lengthOfMonth()
                val finalHijrah = HijrahDate.of(safeYear, safeMonth, safeDay.coerceAtMost(maxDays))
                val localDate = LocalDate.from(finalHijrah).minusDays(dayAdjustment.toLong())
                return Calendar.getInstance().apply {
                    set(Calendar.YEAR, localDate.year)
                    set(Calendar.MONTH, localDate.monthValue - 1)
                    set(Calendar.DAY_OF_MONTH, localDate.dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
            } catch (_: Exception) {
                // Fall through to astronomical tabular conversion
            }
        }
        return hijriToGregorianTabular(safeYear, safeMonth, safeDay, dayAdjustment)
    }

    private fun hijriToGregorianTabular(
        hYear: Int,
        hMonth: Int,
        hDay: Int,
        dayAdjustment: Int
    ): Calendar {
        val jd = floor((11 * hYear + 3) / 30.0) + 354 * hYear + 30 * hMonth -
            floor((hMonth - 1) / 2.0) + hDay + 1948440 - 385
        val l = jd.toLong() + 68569
        val n = (4 * l) / 146097
        val l2 = l - (146097 * n + 3) / 4
        val i = (4000 * (l2 + 1)) / 1461001
        val l3 = l2 - (1461 * i) / 4 + 31
        val j = (80 * l3) / 2447
        val gDay = (l3 - (2447 * j) / 80).toInt()
        val l4 = j / 11
        val gMonth = (j + 2 - 12 * l4).toInt()
        val gYear = (100 * (n - 49) + i + l4).toInt()

        return Calendar.getInstance().apply {
            set(Calendar.YEAR, gYear)
            set(Calendar.MONTH, gMonth - 1)
            set(Calendar.DAY_OF_MONTH, gDay)
            add(Calendar.DAY_OF_MONTH, -dayAdjustment)
        }
    }

    private fun kuwaitiFallback(year: Int, month: Int, day: Int): HijriDateModel {
        val m = if (month <= 2) month + 12 else month
        val y = if (month <= 2) year - 1 else year
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5

        val l0 = jd - 1948439.5 + 10632.0
        val n = floor((l0 - 1.0) / 10631.0)
        val l1 = l0 - 10631.0 * n + 354.0
        val j = floor((10985.0 - l1) / 5316.0) * floor((50.0 * l1) / 17719.0) +
            floor(l1 / 5670.0) * floor((43.0 * l1) / 15238.0)
        val l2 = l1 - floor((30.0 - j) / 15.0) * floor((17719.0 * j) / 50.0) -
            floor(j / 16.0) * floor((15238.0 * j) / 43.0) + 29.0
        val hMonth = floor((24.0 * l2) / 709.0).toInt().coerceIn(1, 12)
        val hDay = (l2 - floor((709.0 * hMonth) / 24.0)).toInt().coerceIn(1, 30)
        val hYear = (30.0 * n + j - 30.0).toInt()

        return HijriDateModel(
            year = hYear,
            month = hMonth,
            day = hDay,
            monthNameAr = hijriMonthsAr.getOrElse(hMonth - 1) { "" },
            monthNameEn = hijriMonthsEn.getOrElse(hMonth - 1) { "" }
        )
    }

    fun buildMonthCells(
        gregorianYear: Int,
        gregorianMonth: Int, // 1..12
        dayAdjustment: Int
    ): List<CalendarDayCell> {
        val today = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, gregorianYear)
            set(Calendar.MONTH, gregorianMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cells = ArrayList<CalendarDayCell>(daysInMonth)

        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val hijri = gregorianToHijri(gregorianYear, gregorianMonth, day, dayAdjustment)
            val occ = occasions.firstOrNull { it.month == hijri.month && it.day == hijri.day }
            val isWhiteDay = hijri.day in 13..15
            val isMonOrThu = dow == Calendar.MONDAY || dow == Calendar.THURSDAY
            val isSpecialFast = (hijri.month == 1 && hijri.day in 9..10) || (hijri.month == 12 && hijri.day == 9)
            val isEid = (hijri.month == 10 && hijri.day == 1) || (hijri.month == 12 && hijri.day in 10..13)
            val isRamadan = hijri.month == 9
            val isLaylatAlQadrNight = isRamadan && hijri.day in listOf(21, 23, 25, 27, 29)

            cells.add(
                CalendarDayCell(
                    gregorianYear = gregorianYear,
                    gregorianMonth = gregorianMonth,
                    gregorianDay = day,
                    dayOfWeek = dow,
                    hijri = hijri,
                    isToday = today.get(Calendar.YEAR) == gregorianYear &&
                        (today.get(Calendar.MONTH) + 1) == gregorianMonth &&
                        today.get(Calendar.DAY_OF_MONTH) == day,
                    isWhiteDay = isWhiteDay && !isEid,
                    isSunnahFastDay = (isWhiteDay || isMonOrThu || isSpecialFast) && !isEid && !isRamadan,
                    isMonthStart = hijri.day == 1,
                    isRamadan = isRamadan,
                    isLaylatAlQadrNight = isLaylatAlQadrNight,
                    occasionNameAr = occ?.nameAr,
                    occasionNameEn = occ?.nameEn
                )
            )
        }
        return cells
    }

    fun getUpcomingOccasions(dayAdjustment: Int): List<IslamicOccasion> {
        val now = Calendar.getInstance()
        val todayHijri = gregorianToHijri(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1,
            now.get(Calendar.DAY_OF_MONTH),
            dayAdjustment
        )
        val todayDoy = (todayHijri.month - 1) * 29.53 + todayHijri.day

        return occasions.map { def ->
            val targetDoy = (def.month - 1) * 29.53 + def.day
            var diff = (targetDoy - todayDoy).toInt()
            if (diff < 0) diff += 354
            IslamicOccasion(
                id = def.id,
                nameAr = def.nameAr,
                nameEn = def.nameEn,
                descriptionAr = def.descAr,
                descriptionEn = def.descEn,
                hijriMonth = def.month,
                hijriDay = def.day,
                daysUntil = diff
            )
        }.sortedBy { it.daysUntil }
    }

    fun formatHijri(hijri: HijriDateModel, isArabic: Boolean): String =
        if (isArabic) "${hijri.day} ${hijri.monthNameAr} ${hijri.year} هـ"
        else "${hijri.day} ${hijri.monthNameEn} ${hijri.year} AH"

    fun formatGregorian(cal: Calendar, isArabic: Boolean): String {
        val dow = cal.get(Calendar.DAY_OF_WEEK) - 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        return if (isArabic) {
            "${weekDaysAr.getOrElse(dow) { "" }}، $day ${gregorianMonthsAr.getOrElse(month) { "" }} $year"
        } else {
            "${weekDaysEn.getOrElse(dow) { "" }}, ${gregorianMonthsEn.getOrElse(month) { "" }} $day, $year"
        }
    }
}
