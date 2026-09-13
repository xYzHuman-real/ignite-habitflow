package com.ignite.habitflow

import java.time.LocalDate
import java.time.YearMonth

/** Pure productivity calculations used by the dashboard and insights UI. */
object ProductivityStats {
    fun taskCompletionPercent(completed: Int, total: Int): Int =
        if (total <= 0) 0 else (completed * 100 / total).coerceIn(0, 100)

    fun habitCompletionPercent(habits: List<HabitRecord>, date: LocalDate = LocalDate.now()): Int {
        if (habits.isEmpty()) return 0
        return habits.count { it.isComplete(date) } * 100 / habits.size
    }

    fun monthHabitPercent(habits: List<HabitRecord>, month: YearMonth, today: LocalDate = LocalDate.now()): Int {
        if (habits.isEmpty()) return 0
        val end = if (month == YearMonth.from(today)) today else month.atEndOfMonth()
        if (end.isBefore(month.atDay(1))) return 0
        val days = java.time.temporal.ChronoUnit.DAYS.between(month.atDay(1), end).toInt() + 1
        val possible = habits.size * days
        val completed = habits.sumOf { habit ->
            var date = month.atDay(1)
            var count = 0
            while (!date.isAfter(end)) {
                if (habit.isComplete(date)) count++
                date = date.plusDays(1)
            }
            count
        }
        return if (possible == 0) 0 else completed * 100 / possible
    }
}
