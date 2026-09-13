package com.ignite.habitflow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

/** Date-aware focus history with a safe migration path from the original integer-only history. */
data class FocusRecord(val date: LocalDate, val minutes: Int)

object FocusHistory {
    private const val PREFS = "habitflow"
    private const val KEY = "focus_records"
    private const val LEGACY_KEY = "focus_history"
    private const val MIGRATED_COUNT_KEY = "focus_legacy_migrated_count"

    fun load(context: Context, today: LocalDate = LocalDate.now()): List<FocusRecord> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val records = runCatching {
            val raw = prefs.getString(KEY, "[]") ?: "[]"
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                FocusRecord(LocalDate.parse(o.getString("date")), o.optInt("minutes", 0).coerceAtLeast(0))
            }
        }.getOrDefault(emptyList()).toMutableList()

        // The old app stored only minute values. We cannot reconstruct their original dates.
        // Any old entries are therefore attributed to the first migration day, and any later
        // legacy entries created by the old UI are synchronized as today's sessions.
        val legacy = runCatching {
            val array = JSONArray(prefs.getString(LEGACY_KEY, "[]"))
            List(array.length()) { i -> array.optInt(i).coerceAtLeast(0) }.filter { it > 0 }
        }.getOrDefault(emptyList())
        val migratedCount = prefs.getInt(MIGRATED_COUNT_KEY, 0).coerceAtLeast(0)
        if (legacy.size > migratedCount) {
            legacy.drop(migratedCount).forEach { records += FocusRecord(today, it) }
            val trimmed = records.takeLast(500)
            save(context, trimmed)
            prefs.edit().putInt(MIGRATED_COUNT_KEY, legacy.size).apply()
            return trimmed
        }
        if (legacy.isNotEmpty() && migratedCount == 0 && records.isEmpty()) {
            legacy.forEach { records += FocusRecord(today, it) }
            val trimmed = records.takeLast(500)
            save(context, trimmed)
            prefs.edit().putInt(MIGRATED_COUNT_KEY, legacy.size).apply()
            return trimmed
        }
        return records
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
