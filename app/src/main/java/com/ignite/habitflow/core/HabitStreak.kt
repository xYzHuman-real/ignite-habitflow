package com.ignite.habitflow.core

import java.time.LocalDate

/**
 * Date-aware habit calculations used by the HabitFlow domain layer.
 * A streak counts consecutive completed calendar days ending today.
 */
object HabitStreak {
    fun current(completedDates: Set<LocalDate>, today: LocalDate = LocalDate.now()): Int {
        var cursor = today
        var streak = 0
        while (completedDates.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun toggle(
        completedDates: Set<LocalDate>,
        date: LocalDate,
        completed: Boolean
    ): Set<LocalDate> = if (completed) {
        completedDates + date
    } else {
        completedDates - date
    }

    fun completionPercent(
        completedDates: Set<LocalDate>,
        start: LocalDate,
        end: LocalDate
    ): Int {
        if (end.isBefore(start)) return 0
        val total = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1
        val completed = completedDates.count { !it.isBefore(start) && !it.isAfter(end) }
        return ((completed * 100.0) / total).toInt().coerceIn(0, 100)
    }
}
