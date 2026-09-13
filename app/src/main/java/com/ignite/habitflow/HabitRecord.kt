package com.ignite.habitflow

import java.time.LocalDate

/** Date-aware habit model used by the next HabitFlow habit UI. */
data class HabitRecord(
    val id: Long,
    val title: String,
    val completedDates: Set<String> = emptySet()
) {
    fun isComplete(date: LocalDate): Boolean =
        HabitHistory.isCompleted(completedDates, date)

    fun toggle(date: LocalDate): HabitRecord =
        copy(completedDates = HabitHistory.toggle(completedDates, date))

    fun streak(today: LocalDate = LocalDate.now()): Int =
        HabitHistory.currentStreak(completedDates, today)

    fun completionPercent(month: java.time.YearMonth): Int =
        HabitHistory.completionPercent(
            completedDates,
            month.atDay(1),
            month.atEndOfMonth()
        )
}
