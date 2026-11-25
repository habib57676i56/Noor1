package com.example.data.local.prefs

import android.content.Context
import com.example.domain.qada.QadaPrayerEntry
import com.example.domain.qada.QadaPrayerType
import com.example.domain.qada.QadaState
import com.example.domain.qada.QadaTrackerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Offline-first persistent store for Missed Prayers (قضاء الصلوات الفائتة).
 * Saves every single tap immediately to local SharedPreferences so records survive restarts.
 */
class QadaPrayerStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(loadFromPrefs())
    val state: StateFlow<QadaState> = _state.asStateFlow()

    private fun loadFromPrefs(): QadaState {
        val map = QadaPrayerType.entries.associateWith { type ->
            val rem = prefs.getInt("rem_${type.key}", 0).coerceAtLeast(0)
            val done = prefs.getInt("done_${type.key}", 0).coerceAtLeast(0)
            QadaPrayerEntry(type = type, remaining = rem, completed = done)
        }
        val pace = prefs.getInt(KEY_DAILY_PACE, 5).coerceIn(1, 50)
        val updated = prefs.getLong(KEY_LAST_UPDATED, 0L)
        return QadaState(entries = map, dailyPacePrayers = pace, lastUpdatedEpochMs = updated)
    }

    private fun save(next: QadaState) {
        _state.value = next
        val editor = prefs.edit()
        next.entries.forEach { (type, entry) ->
            editor.putInt("rem_${type.key}", entry.remaining.coerceAtLeast(0))
            editor.putInt("done_${type.key}", entry.completed.coerceAtLeast(0))
        }
        editor.putInt(KEY_DAILY_PACE, next.dailyPacePrayers.coerceIn(1, 50))
        editor.putLong(KEY_LAST_UPDATED, next.lastUpdatedEpochMs)
        editor.apply()
    }

    fun markPrayed(type: QadaPrayerType) {
        save(QadaTrackerEngine.markOnePrayed(_state.value, type))
    }

    fun adjustMissed(type: QadaPrayerType, delta: Int) {
        save(QadaTrackerEngine.addMissedOne(_state.value, type, delta))
    }

    fun setRemainingExact(type: QadaPrayerType, remaining: Int) {
        save(QadaTrackerEngine.setRemainingExact(_state.value, type, remaining))
    }

    fun addMissedPeriod(years: Int, months: Int, days: Int, includeWitr: Boolean) {
        save(QadaTrackerEngine.addMissedPeriod(_state.value, years, months, days, includeWitr))
    }

    fun completeFullDay(includeWitr: Boolean = false) {
        save(QadaTrackerEngine.completeFullDay(_state.value, includeWitr))
    }

    fun setDailyPace(prayersPerDay: Int) {
        save(_state.value.copy(dailyPacePrayers = prayersPerDay.coerceIn(1, 50), lastUpdatedEpochMs = System.currentTimeMillis()))
    }

    fun resetAll() {
        save(QadaState(dailyPacePrayers = _state.value.dailyPacePrayers, lastUpdatedEpochMs = System.currentTimeMillis()))
    }

    companion object {
        private const val PREFS_NAME = "noor_qada_prayers_v1"
        private const val KEY_DAILY_PACE = "daily_pace"
        private const val KEY_LAST_UPDATED = "last_updated"
    }
}
