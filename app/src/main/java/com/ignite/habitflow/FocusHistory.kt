package com.ignite.habitflow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

/** Date-aware focus history. Old integer-only history is preserved and treated as today's legacy data. */
data class FocusRecord(val date: LocalDate, val minutes: Int)

object FocusHistory {
    private const val PREFS = "habitflow"
    private const val KEY = "focus_records"
    private const val LEGACY_KEY = "focus_history"

    fun load(context: Context, today: LocalDate = LocalDate.now()): List<FocusRecord> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null)
        if (!raw.isNullOrBlank()) {
            return runCatching {
                val array = JSONArray(raw)
                List(array.length()) { i ->
                    val o = array.getJSONObject(i)
                    FocusRecord(LocalDate.parse(o.getString("date")), o.optInt("minutes", 0).coerceAtLeast(0))
                }
            }.getOrDefault(emptyList())
        }

        // V1 stored only minute values, so there is no honest way to recover their original dates.
        // Keep the data visible by attributing legacy sessions to the migration day.
        val legacy = runCatching {
            val array = JSONArray(prefs.getString(LEGACY_KEY, "[]"))
            List(array.length()) { i -> array.optInt(i).coerceAtLeast(0) }
                .filter { it > 0 }
                .map { FocusRecord(today, it) }
        }.getOrDefault(emptyList())
        if (legacy.isNotEmpty()) save(context, legacy)
        return legacy
    }

    fun add(context: Context, minutes: Int, date: LocalDate = LocalDate.now()) {
        if (minutes <= 0) return
        val updated = load(context, date) + FocusRecord(date, minutes)
        save(context, updated.takeLast(500))
    }

    fun save(context: Context, records: List<FocusRecord>) {
        val array = JSONArray()
        records.takeLast(500).forEach {
            array.put(JSONObject().put("date", it.date.toString()).put("minutes", it.minutes))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, array.toString()).apply()
    }

    fun total(records: List<FocusRecord>, start: LocalDate, end: LocalDate): Int =
        records.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }.sumOf { it.minutes }

    fun sessions(records: List<FocusRecord>, start: LocalDate, end: LocalDate): Int =
        records.count { !it.date.isBefore(start) && !it.date.isAfter(end) }

    fun monthTotals(records: List<FocusRecord>, year: Int): List<Int> =
        (1..12).map { month ->
            val ym = YearMonth.of(year, month)
            total(records, ym.atDay(1), ym.atEndOfMonth())
        }
}
