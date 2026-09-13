package com.ignite.habitflow

import java.time.LocalDate

/** Date-based completion history for habits. Stored as ISO dates (yyyy-MM-dd). */
object HabitHistory {
    fun toggle(completedDates: Set<String>, date: LocalDate): Set<String> {
        val key = date.toString()
        return if (key in completedDates) completedDates - key else completedDates + key
    }

    fun isCompleted(completedDates: Set<String>, date: LocalDate): Boolean =
        date.toString() in completedDates

    fun currentStreak(completedDates: Set<String>, today: LocalDate = LocalDate.now()): Int {
        var cursor = today
        if (!isCompleted(completedDates, cursor)) cursor = cursor.minusDays(1)
        var streak = 0
        while (isCompleted(completedDates, cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun completionPercent(completedDates: Set<String>, start: LocalDate, end: LocalDate): Int {
        if (end.isBefore(start)) return 0
        val total = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1
        val completed = (start..end).count { isCompleted(completedDates, it) }
        return (completed * 100 / total).coerceIn(0, 100)
    }
}
