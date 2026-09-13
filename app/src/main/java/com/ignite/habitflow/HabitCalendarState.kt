package com.ignite.habitflow

import java.time.LocalDate
import java.time.YearMonth

/** Pure state helpers for the habit calendar UI. */
object HabitCalendarState {
    fun monthFor(date: LocalDate): YearMonth = YearMonth.from(date)

    fun move(month: YearMonth, delta: Long): YearMonth = month.plusMonths(delta)

    fun toggle(completedDates: Set<String>, date: LocalDate): Set<String> =
        HabitHistory.toggle(completedDates, date)

    fun isCompleted(completedDates: Set<String>, date: LocalDate): Boolean =
        HabitHistory.isCompleted(completedDates, date)

    fun monthCompletion(completedDates: Set<String>, month: YearMonth): Int =
        HabitHistory.completionPercent(completedDates, month.atDay(1), month.atEndOfMonth())

    fun currentStreak(completedDates: Set<String>, today: LocalDate = LocalDate.now()): Int =
        HabitHistory.currentStreak(completedDates, today)
}
