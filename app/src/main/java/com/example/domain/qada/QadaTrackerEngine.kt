package com.example.domain.qada

import kotlin.math.ceil
import kotlin.math.max

enum class QadaPrayerType(
    val key: String,
    val nameAr: String,
    val nameEn: String,
    val nameUz: String,
    val rakahs: Int,
    val isFard: Boolean = true
) {
    FAJR("fajr", "الفجر", "Fajr", "Bomdod", 2, true),
    DHUHR("dhuhr", "الظهر", "Dhuhr", "Peshin", 4, true),
    ASR("asr", "العصر", "Asr", "Asr", 4, true),
    MAGHRIB("maghrib", "المغرب", "Maghrib", "Shom", 3, true),
    ISHA("isha", "العشاء", "Isha", "Xufton", 4, true),
    WITR("witr", "الوتر", "Witr", "Vitr", 3, false)
}

data class QadaPrayerEntry(
    val type: QadaPrayerType,
    val remaining: Int = 0,
    val completed: Int = 0
) {
    val totalLogged: Int get() = remaining + completed
    val progress: Float
        get() = if (totalLogged <= 0) 0f else (completed.toFloat() / totalLogged.toFloat()).coerceIn(0f, 1f)
}

data class QadaState(
    val entries: Map<QadaPrayerType, QadaPrayerEntry> = QadaPrayerType.entries.associateWith { QadaPrayerEntry(it) },
    val dailyPacePrayers: Int = 5,
    val lastUpdatedEpochMs: Long = 0L
) {
    val totalRemaining: Int get() = entries.values.sumOf { it.remaining }
    val totalCompleted: Int get() = entries.values.sumOf { it.completed }
    val totalOverall: Int get() = totalRemaining + totalCompleted
    val progressFraction: Float
        get() = if (totalOverall <= 0) 0f else (totalCompleted.toFloat() / totalOverall.toFloat()).coerceIn(0f, 1f)
    val estimatedDaysToFinish: Int
        get() = QadaTrackerEngine.estimateCompletionDays(totalRemaining, dailyPacePrayers)
}

object QadaTrackerEngine {

    const val DAYS_PER_YEAR = 365
    const val DAYS_PER_MONTH = 30

    fun calculateDaysFromPeriod(years: Int, months: Int, days: Int): Int {
        val safeYears = max(0, years)
        val safeMonths = max(0, months)
        val safeDays = max(0, days)
        return safeYears * DAYS_PER_YEAR + safeMonths * DAYS_PER_MONTH + safeDays
    }

    fun addMissedPeriod(
        current: QadaState,
        years: Int,
        months: Int,
        days: Int,
        includeWitr: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): QadaState {
        val totalDays = calculateDaysFromPeriod(years, months, days)
        if (totalDays <= 0) return current
        val updated = current.entries.toMutableMap()
        QadaPrayerType.entries.forEach { type ->
            if (type.isFard || includeWitr) {
                val existing = updated[type] ?: QadaPrayerEntry(type)
                updated[type] = existing.copy(remaining = existing.remaining + totalDays)
            }
        }
        return current.copy(entries = updated, lastUpdatedEpochMs = nowMs)
    }

    fun markOnePrayed(
        current: QadaState,
        type: QadaPrayerType,
        nowMs: Long = System.currentTimeMillis()
    ): QadaState {
        val existing = current.entries[type] ?: QadaPrayerEntry(type)
        if (existing.remaining <= 0) return current
        val updated = current.entries.toMutableMap()
        updated[type] = existing.copy(
            remaining = existing.remaining - 1,
            completed = existing.completed + 1
        )
        return current.copy(entries = updated, lastUpdatedEpochMs = nowMs)
    }

    fun addMissedOne(
        current: QadaState,
        type: QadaPrayerType,
        delta: Int = 1,
        nowMs: Long = System.currentTimeMillis()
    ): QadaState {
        val existing = current.entries[type] ?: QadaPrayerEntry(type)
        val nextRemaining = (existing.remaining + delta).coerceAtLeast(0)
        val updated = current.entries.toMutableMap()
        updated[type] = existing.copy(remaining = nextRemaining)
        return current.copy(entries = updated, lastUpdatedEpochMs = nowMs)
    }

    fun setRemainingExact(
        current: QadaState,
        type: QadaPrayerType,
        remaining: Int,
        nowMs: Long = System.currentTimeMillis()
    ): QadaState {
        val existing = current.entries[type] ?: QadaPrayerEntry(type)
        val updated = current.entries.toMutableMap()
        updated[type] = existing.copy(remaining = remaining.coerceAtLeast(0))
        return current.copy(entries = updated, lastUpdatedEpochMs = nowMs)
    }

    fun completeFullDay(
        current: QadaState,
        includeWitr: Boolean = false,
        nowMs: Long = System.currentTimeMillis()
    ): QadaState {
        val updated = current.entries.toMutableMap()
        var anyChanged = false
        QadaPrayerType.entries.forEach { type ->
            if (type.isFard || includeWitr) {
                val existing = updated[type] ?: QadaPrayerEntry(type)
                if (existing.remaining > 0) {
                    updated[type] = existing.copy(
                        remaining = existing.remaining - 1,
                        completed = existing.completed + 1
                    )
                    anyChanged = true
                }
            }
        }
        return if (anyChanged) current.copy(entries = updated, lastUpdatedEpochMs = nowMs) else current
    }

    fun estimateCompletionDays(remainingTotal: Int, prayersPerDay: Int): Int {
        if (remainingTotal <= 0) return 0
        val pace = prayersPerDay.coerceAtLeast(1)
        return ceil(remainingTotal.toDouble() / pace.toDouble()).toInt()
    }
}
