package com.ignite.habitflow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HabitCalendarGrid(
    month: YearMonth,
    completedDates: Set<String>,
    selectedDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val days = month.lengthOfMonth()
    val cells = leading + days
    val rows = (cells + 6) / 7
    val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")

    Column(modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            weekdays.forEach { Text(it, fontSize = 11.sp, color = Color.Gray) }
        }
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(7) { column ->
                    val index = row * 7 + column
                    if (index < leading || index >= cells) {
                        Box(Modifier.size(38.dp))
                    } else {
                        val date = month.atDay(index - leading + 1)
                        val completed = date.toString() in completedDates
                        val selected = date == selectedDate
                        Box(
                            Modifier
                                .size(38.dp)
                                .background(
                                    if (completed) Color(0xFF171717) else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { onDateClick(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                date.dayOfMonth.toString(),
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (completed) Color.White else Color(0xFF171717)
                            )
                        }
                    }
                }
            }
        }
    }
}
