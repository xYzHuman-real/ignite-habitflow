package com.ignite.habitflow

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Converts the original V1 habit format into date-based completion history. */
object HabitMigration {
    fun migrate(json: String, today: LocalDate = LocalDate.now()): String {
        val source = runCatching { JSONArray(json) }.getOrDefault(JSONArray())
        val result = JSONArray()

        for (i in 0 until source.length()) {
            val old = source.optJSONObject(i) ?: continue
            val item = JSONObject()
            item.put("id", old.optLong("id"))
            item.put("title", old.optString("title", "Habit"))

            val dates = mutableSetOf<String>()
            val existing = old.optJSONArray("completedDates")
            if (existing != null) {
                for (j in 0 until existing.length()) {
                    existing.optString(j).takeIf { it.isNotBlank() }?.let(dates::add)
                }
            } else if (old.optBoolean("done", false)) {
                // Preserve V1's completed-today state during migration.
                dates.add(today.toString())
            }

            val history = JSONArray()
            dates.sorted().forEach(history::put)
            item.put("completedDates", history)
            result.put(item)
        }
        return result.toString()
    }
}
