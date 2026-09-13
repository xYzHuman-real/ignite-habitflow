package com.ignite.habitflow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** SharedPreferences persistence for date-aware habits, with V1 migration support. */
class HabitStoreV2(context: Context) {
    // Keep the original preference file so existing V1 habits can be migrated in place.
    private val prefs = context.getSharedPreferences("habitflow", Context.MODE_PRIVATE)
    private val key = "habits"

    fun load(): List<HabitRecord> {
        val raw = prefs.getString(key, "[]") ?: "[]"
        val migrated = HabitMigration.migrate(raw)
        if (migrated != raw) prefs.edit().putString(key, migrated).apply()

        val array = runCatching { JSONArray(migrated) }.getOrDefault(JSONArray())
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val dates = mutableSetOf<String>()
                item.optJSONArray("completedDates")?.let { history ->
                    for (j in 0 until history.length()) {
                        history.optString(j).takeIf { it.isNotBlank() }?.let(dates::add)
                    }
                }
                add(HabitRecord(item.optLong("id"), item.optString("title", "Habit"), dates))
            }
        }
    }

    fun save(habits: List<HabitRecord>) {
        val array = JSONArray()
        habits.forEach { habit ->
            val item = JSONObject().put("id", habit.id).put("title", habit.title)
            val dates = JSONArray()
            habit.completedDates.sorted().forEach(dates::put)
            item.put("completedDates", dates)
            array.put(item)
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    fun toggle(habit: HabitRecord, date: LocalDate): List<HabitRecord> =
        load().map { if (it.id == habit.id) it.toggle(date) else it }.also(::save)
}
