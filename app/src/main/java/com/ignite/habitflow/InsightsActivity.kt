package com.ignite.habitflow

import android.os.Bundle
import android.content.Context
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
import org.json.JSONArray
import java.time.LocalDate
import java.time.YearMonth

private val InsightsBg = Color(0xFFF7F7F5)
private val InsightsInk = Color(0xFF171717)
private val InsightsMuted = Color(0xFF777777)

class InsightsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { InsightsScreen() }
    }
}

@Composable
private fun InsightsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("habitflow", Context.MODE_PRIVATE) }
    val tasks = remember { loadTaskStats(prefs.getString("tasks", "[]") ?: "[]") }
    val habits = remember { HabitStoreV2(context).load() }
    val focusHistory = remember { loadFocus(prefs.getString("focus_history", "[]") ?: "[]") }
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    val completedTasks = tasks.count { it.done }
    val taskRate = ProductivityStats.taskCompletionPercent(completedTasks, tasks.size)
    val habitRate = ProductivityStats.habitCompletionPercent(habits, today)
    val monthHabitRate = ProductivityStats.monthHabitPercent(habits, month, today)
    val bestStreak = habits.maxOfOrNull { it.streak(today) } ?: 0
    val focusMinutes = focusHistory.sum()

    MaterialTheme(colorScheme = lightColorScheme(background = InsightsBg, surface = Color.White, primary = InsightsInk, onBackground = InsightsInk, onSurface = InsightsInk)) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Text("Insights", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                Text("A clear view of your consistency.", color = InsightsMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
            item { InsightCard("Tasks", "$taskRate%", "$completedTasks of ${tasks.size} completed") }
            item { InsightCard("Today's habits", "$habitRate%", "${habits.count { it.isComplete(today) }} of ${habits.size} completed") }
            item { InsightCard("This month", "$monthHabitRate%", "Habit consistency so far") }
            item { InsightCard("Best current streak", "$bestStreak days", "Longest active habit streak") }
            item { InsightCard("Focus", "$focusMinutes min", "${focusHistory.size} completed sessions") }
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Keep it simple", fontWeight = FontWeight.SemiBold)
                        Text("Consistency matters more than a perfect day. Use these numbers to notice patterns, not to pressure yourself.", color = InsightsMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightCard(title: String, value: String, subtitle: String) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = InsightsMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private data class TaskStat(val done: Boolean)
private fun loadTaskStats(raw: String): List<TaskStat> = runCatching {
    val a = JSONArray(raw)
    List(a.length()) { i -> TaskStat(a.getJSONObject(i).optBoolean("done", false)) }
}.getOrDefault(emptyList())

private fun loadFocus(raw: String): List<Int> = runCatching {
    val a = JSONArray(raw)
    List(a.length()) { i -> a.getInt(i) }
}.getOrDefault(emptyList())
