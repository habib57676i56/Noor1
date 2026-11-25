package com.example.domain.prayer

import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.PrayerName

/**
 * Enforces Islamic prayer-tracking time gating:
 * A daily Fard prayer can ONLY be marked as prayed once its scheduled time has entered
 * (`nowMillis >= schedule.timeFor(prayer)`). Upcoming prayers that have not yet entered
 * cannot be toggled.
 */
object PrayerTimeGate {
    fun hasPrayerEntered(
        schedule: DailyPrayerSchedule,
        prayer: PrayerName,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!prayer.isFard) return false
        return nowMillis >= schedule.timeFor(prayer)
    }
}
