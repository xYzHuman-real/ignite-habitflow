package com.ignite.habitflow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private val CalendarInk = Color(0xFF171717)
private val CalendarMuted = Color(0xFF777777)
private val CalendarSoft = Color(0xFFEDEDEB)

/** Compact month grid. Tap a day to toggle that day's completion. */
@Composable
fun HabitMonthCalendar(
    month: YearMonth,
    completedDates: Set<String>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val days = month.lengthOfMonth()

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, color = CalendarMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
        var day = 1
        repeat(6) { row ->
            if (day <= days) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    repeat(7) { column ->
                        val index = row * 7 + column
                        if (index < leading || day > days) {
                            Box(Modifier.weight(1f).padding(2.dp))
                        } else {
                            val date = month.atDay(day++)
                            val completed = HabitHistory.isCompleted(completedDates, date)
                            Box(
                                Modifier.weight(1f).padding(2.dp).clip(CircleShape)
                                    .background(if (completed) CalendarInk else CalendarSoft)
                                    .clickable { onDateClick(date) }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    color = if (completed) Color.White else CalendarInk,
                                    fontSize = 12.sp,
                                    fontWeight = if (completed) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
