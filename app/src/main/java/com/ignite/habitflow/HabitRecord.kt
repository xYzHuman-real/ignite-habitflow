package com.ignite.habitflow

import java.time.LocalDate
import java.time.YearMonth

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

    fun completionPercent(month: YearMonth): Int =
        HabitHistory.completionPercent(completedDates, month.atDay(1), month.atEndOfMonth())

    fun completionPercentThrough(month: YearMonth, end: LocalDate): Int {
        val boundedEnd = minOf(end, month.atEndOfMonth())
        if (boundedEnd.isBefore(month.atDay(1))) return 0
        return HabitHistory.completionPercent(completedDates, month.atDay(1), boundedEnd)
    }
}
