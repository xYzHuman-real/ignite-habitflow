package com.ignite.habitflow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val CalendarInk = Color(0xFF171717)
private val CalendarMuted = Color(0xFF777777)
private val CalendarSoft = Color(0xFFEDEDEB)

/** Reusable date-aware habit screen. State is owned here; persistence is delegated to HabitStoreV2. */
@Composable
fun HabitScreenV2(
    initialHabits: List<HabitRecord>,
    onHabitsChanged: (List<HabitRecord>) -> Unit
) {
    var habits by remember(initialHabits) { mutableStateOf(initialHabits) }
    var selectedHabitId by remember { mutableStateOf<Long?>(null) }
    var month by remember { mutableStateOf(YearMonth.now()) }

    fun replace(updated: List<HabitRecord>) {
        habits = updated
        onHabitsChanged(updated)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Habits", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Text("Build a rhythm, one day at a time.", color = CalendarMuted, fontSize = 14.sp)

        if (habits.isEmpty()) {
            Text("Create a habit to start tracking your consistency.", color = CalendarMuted)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(habits, key = { it.id }) { habit ->
                    val expanded = selectedHabitId == habit.id
                    HabitCardV2(
                        habit = habit,
                        expanded = expanded,
                        onToggleToday = {
                            replace(habits.map { if (it.id == habit.id) it.toggle(LocalDate.now()) else it })
                        },
                        onExpand = { selectedHabitId = if (expanded) null else habit.id }
                    )
                    if (expanded) {
                        HabitCalendarV2(
                            completedDates = habit.completedDates,
                            month = month,
                            onPrevious = { month = month.minusMonths(1) },
                            onNext = { month = month.plusMonths(1) },
                            onDateClick = { date ->
                                replace(habits.map { if (it.id == habit.id) it.toggle(date) else it })
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitCardV2(
    habit: HabitRecord,
    expanded: Boolean,
    onToggleToday: () -> Unit,
    onExpand: () -> Unit
) {
    val today = LocalDate.now()
    val streak = habit.streak(today)
    val complete = habit.isComplete(today)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(24.dp).background(if (complete) CalendarInk else CalendarSoft, CircleShape)
                    .clickable(onClick = onToggleToday),
                contentAlignment = Alignment.Center
            ) {
                if (complete) Text("✓", color = Color.White, fontSize = 14.sp)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.title, fontWeight = FontWeight.Medium)
                Text(
                    when {
                        streak > 0 -> "$streak day streak"
                        complete -> "Completed today"
                        else -> "Start today"
                    },
                    color = CalendarMuted,
                    fontSize = 12.sp
                )
            }
            Text(if (expanded) "Hide" else "Calendar", color = CalendarMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun HabitCalendarV2(
    completedDates: Set<String>,
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateClick: (LocalDate) -> Unit
) {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val cells = leading + month.lengthOfMonth()

    Column(
        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) { Icon(Icons.Outlined.ChevronLeft, "Previous month") }
            Text(
                month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " ${month.year}",
                Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNext) { Icon(Icons.Outlined.ChevronRight, "Next month") }
        }

        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = CalendarMuted, fontSize = 12.sp)
            }
        }

        for (row in 0 until ((cells + 6) / 7)) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    if (index < leading || index >= cells) {
                        Spacer(Modifier.weight(1f).size(38.dp))
                    } else {
                        val date = month.atDay(index - leading + 1)
                        val done = HabitHistory.isCompleted(completedDates, date)
                        val today = date == LocalDate.now()
                        Box(
                            Modifier.weight(1f).padding(2.dp).size(38.dp)
                                .background(if (done) CalendarInk else Color.Transparent, CircleShape)
                                .clickable { onDateClick(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                date.dayOfMonth.toString(),
                                color = if (done) Color.White else CalendarInk,
                                fontSize = 13.sp,
                                fontWeight = if (today) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Text(
            "${HabitHistory.completionPercent(completedDates, month.atDay(1), month.atEndOfMonth())}% completed this month",
            color = CalendarMuted,
            fontSize = 12.sp
        )
    }
}
