package com.ignite.habitflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private val InsightsBg = Color(0xFFF7F7F5)
private val InsightsInk = Color(0xFF171717)
private val InsightsMuted = Color(0xFF777777)

private enum class InsightPeriod(val label: String) { WEEK("7 days"), MONTH("Month"), YEAR("Year") }

class InsightsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { InsightsScreen() }
    }
}

@Composable
private fun InsightsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("habitflow", android.content.Context.MODE_PRIVATE) }
    val tasks = remember { loadTaskStats(prefs.getString("tasks", "[]") ?: "[]") }
    val habits = remember { HabitStoreV2(context).load() }
    val focusRecords = remember { FocusHistory.load(context) }
    var period by remember { mutableStateOf(InsightPeriod.WEEK) }
    val today = LocalDate.now()
    val start = when (period) {
        InsightPeriod.WEEK -> today.minusDays(6)
        InsightPeriod.MONTH -> today.withDayOfMonth(1)
        InsightPeriod.YEAR -> today.withDayOfYear(1)
    }
    val end = today
    val habitRate = rangeHabitPercent(habits, start, end)
    val focusMinutes = FocusHistory.total(focusRecords, start, end)
    val focusSessions = FocusHistory.sessions(focusRecords, start, end)
    val completedTasks = tasks.count { it.done }
    val taskRate = ProductivityStats.taskCompletionPercent(completedTasks, tasks.size)
    val bestStreak = habits.maxOfOrNull { it.streak(today) } ?: 0
    val completedHabitsToday = habits.count { it.isComplete(today) }
    val periodLabel = when (period) {
        InsightPeriod.WEEK -> "Last 7 days"
        InsightPeriod.MONTH -> YearMonth.from(today).month.name.lowercase().replaceFirstChar { it.uppercase() }
        InsightPeriod.YEAR -> today.year.toString()
    }
    val averageFocus = if (focusSessions == 0) 0 else focusMinutes / focusSessions

    IgniteInsightsTheme {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text("Insights", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                Text("A clear view of your consistency.", color = InsightsMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    InsightPeriod.values().forEachIndexed { index, value ->
                        SegmentedButton(selected = period == value, onClick = { period = value }, shape = SegmentedButtonDefaults.itemShape(index, InsightPeriod.values().size)) {
                            Text(value.label, fontSize = 12.sp)
                        }
                    }
                }
            }
            item { InsightCard(periodLabel, "$habitRate%", "Habit consistency in this period") }
            item { InsightCard("Today’s habits", "$completedHabitsToday/${habits.size}", "Completed today") }
            item { InsightCard("Tasks", "$taskRate%", "$completedTasks of ${tasks.size} completed overall") }
            item { InsightCard("Focus", "$focusMinutes min", "$focusSessions sessions • $averageFocus min average") }
            item { InsightCard("Best current streak", "$bestStreak days", "Longest active habit streak") }
            if (period == InsightPeriod.YEAR) {
                item {
                    val monthly = FocusHistory.monthTotals(focusRecords, today.year)
                    InsightCard("Year focus", "${monthly.sum()} min", "${monthly.count { it > 0 }} active months")
                }
            }
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your rhythm", fontWeight = FontWeight.SemiBold)
                        val message = when {
                            habits.isEmpty() && tasks.isEmpty() -> "Add a habit or task to start building your baseline."
                            habitRate >= 80 -> "Excellent consistency. Keep protecting the routine that is working."
                            focusMinutes > 0 -> "You are building momentum. Use the next session to keep the streak alive."
                            else -> "Small, repeatable actions are the goal. Focus on the next useful step."
                        }
                        Text(message, color = InsightsMuted, fontSize = 13.sp)
                    }
                }
            }
            item {
                Text("Focus history", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Focus sessions are now stored with their completion date for accurate period totals. Older sessions without dates are migrated to the migration day rather than being given invented historical dates.", color = InsightsMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun IgniteInsightsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(background = InsightsBg, surface = Color.White, primary = InsightsInk, onBackground = InsightsInk, onSurface = InsightsInk), content = content)
}

@Composable
private fun InsightCard(title: String, value: String, subtitle: String) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = InsightsMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun rangeHabitPercent(habits: List<HabitRecord>, start: LocalDate, end: LocalDate): Int {
    if (habits.isEmpty() || end.isBefore(start)) return 0
    val totalDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
    val possible = habits.size * totalDays
    val completed = habits.sumOf { habit ->
        var date = start
        var count = 0
        while (!date.isAfter(end)) {
            if (habit.isComplete(date)) count++
            date = date.plusDays(1)
        }
        count
    }
    return if (possible == 0) 0 else (completed * 100 / possible).coerceIn(0, 100)
}

private data class TaskStat(val done: Boolean)
private fun loadTaskStats(raw: String): List<TaskStat> = runCatching {
    val a = org.json.JSONArray(raw)
    List(a.length()) { i -> TaskStat(a.getJSONObject(i).optBoolean("done", false)) }
}.getOrDefault(emptyList())
