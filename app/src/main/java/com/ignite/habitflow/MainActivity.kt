package com.ignite.habitflow

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val AppBackground = Color(0xFFF7F7F5)
private val Ink = Color(0xFF171717)
private val Muted = Color(0xFF777777)
private val Soft = Color(0xFFEDEDEB)
private val CardWhite = Color.White
private val DateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

private enum class Priority { LOW, MEDIUM, HIGH }
private enum class TaskFilter { ALL, TODAY, UPCOMING, COMPLETED }

private data class Task(
    val id: Long,
    val title: String,
    val done: Boolean,
    val due: String = LocalDate.now().toString(),
    val category: String = "General",
    val priority: Priority = Priority.MEDIUM
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IgniteHabitFlowApp() }
    }
}

private class Store(context: Context) {
    private val prefs = context.getSharedPreferences("habitflow", Context.MODE_PRIVATE)

    fun loadTasks(): List<Task> = runCatching {
        val a = JSONArray(prefs.getString("tasks", "[]"))
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Task(
                o.getLong("id"),
                o.getString("title"),
                o.getBoolean("done"),
                o.optString("due", LocalDate.now().toString()),
                o.optString("category", "General"),
                runCatching { Priority.valueOf(o.optString("priority", "MEDIUM")) }.getOrDefault(Priority.MEDIUM)
            )
        }
    }.getOrDefault(emptyList())

    fun saveTasks(list: List<Task>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject()
                .put("id", it.id)
                .put("title", it.title)
                .put("done", it.done)
                .put("due", it.due)
                .put("category", it.category)
                .put("priority", it.priority.name))
        }
        prefs.edit().putString("tasks", a.toString()).apply()
    }

    fun saveFocus(minutes: Int) = prefs.edit().putInt("focus", prefs.getInt("focus", 0) + minutes).apply()
    fun focusMinutes() = prefs.getInt("focus", 0)
    fun loadFocusHistory(): List<Int> = runCatching {
        val a = JSONArray(prefs.getString("focus_history", "[]"))
        List(a.length()) { a.getInt(it) }
    }.getOrDefault(emptyList())
    fun addFocusHistory(minutes: Int) {
        val history = loadFocusHistory().takeLast(29) + minutes
        val a = JSONArray(); history.forEach(a::put)
        prefs.edit().putString("focus_history", a.toString()).apply()
    }
}

@Composable
private fun IgniteHabitFlowApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { Store(context) }
    val habitStore = remember { HabitStoreV2(context) }
    var tab by remember { mutableIntStateOf(0) }
    var tasks by remember { mutableStateOf(store.loadTasks()) }
    var habits by remember { mutableStateOf(habitStore.load()) }
    var focus by remember { mutableIntStateOf(store.focusMinutes()) }
    var focusHistory by remember { mutableStateOf(store.loadFocusHistory()) }

    fun saveTasks(v: List<Task>) { tasks = v; store.saveTasks(v) }
    fun saveHabits(v: List<HabitRecord>) { habits = v; habitStore.save(v) }
    fun completedFocus(minutes: Int) {
        store.saveFocus(minutes)
        store.addFocusHistory(minutes)
        focus = store.focusMinutes()
        focusHistory = store.loadFocusHistory()
    }

    MaterialTheme(colorScheme = lightColorScheme(background = AppBackground, surface = CardWhite, primary = Ink, onPrimary = Color.White, onBackground = Ink, onSurface = Ink)) {
        Scaffold(containerColor = AppBackground, bottomBar = { GlassBottomBar(tab) { tab = it } }) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    0 -> HomeScreen(tasks, habits, focus, focusHistory) { tab = it }
                    1 -> TasksScreen(tasks, ::saveTasks)
                    2 -> HabitsScreen(habits, ::saveHabits)
                    3 -> FocusScreen(focusHistory, ::completedFocus)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(tasks: List<Task>, habits: List<HabitRecord>, focus: Int, focusHistory: List<Int>, navigate: (Int) -> Unit) {
    val today = LocalDate.now()
    val taskDoneToday = tasks.count { it.done && parseDate(it.due) == today }
    val dueToday = tasks.count { !it.done && parseDate(it.due) == today }
    val habitDone = habits.count { it.isComplete(today) }
    val totalFocus = focusHistory.sum()
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())), color = Muted, fontSize = 14.sp)
        Text("Make today count.", fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(CardWhite), elevation = CardDefaults.cardElevation(0.dp)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Today", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(if (dueToday == 0) "You're clear for today" else "$dueToday task${if (dueToday == 1) "" else "s"} due today", color = Muted, fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Tasks", "$taskDoneToday/${tasks.count { parseDate(it.due) == today }}", Modifier.weight(1f))
                    StatPill("Habits", "$habitDone/${habits.size}", Modifier.weight(1f))
                    StatPill("Focus", "${totalFocus}m", Modifier.weight(1f))
                }
            }
        }
        Text("Quick actions", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("Add task", Icons.Outlined.AddTask, Modifier.weight(1f)) { navigate(1) }
            QuickAction("Start focus", Icons.Outlined.PlayArrow, Modifier.weight(1f)) { navigate(3) }
        }
        tasks.filter { !it.done }.sortedWith(compareBy({ parseDate(it.due) }, { -it.priority.ordinal })).take(3).forEach { TaskRow(it, {}, {}) }
    }
}

@Composable private fun StatPill(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Soft).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 15.sp); Text(label, color = Muted, fontSize = 11.sp)
    }
}

@Composable private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) {
        Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(label, fontWeight = FontWeight.Medium)
    }
}

@Composable private fun TasksScreen(tasks: List<Task>, update: (List<Task>) -> Unit) {
    var add by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    val today = LocalDate.now()
    val visible = tasks.filter {
        when (filter) {
            TaskFilter.ALL -> !it.done
            TaskFilter.TODAY -> !it.done && parseDate(it.due) == today
            TaskFilter.UPCOMING -> !it.done && parseDate(it.due).isAfter(today)
            TaskFilter.COMPLETED -> it.done
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Tasks", fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text("${tasks.count { !it.done }} remaining", color = Muted, fontSize = 14.sp) }
            FilledIconButton(onClick = { add = true }, shape = RoundedCornerShape(16.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Ink, contentColor = Color.White)) { Icon(Icons.Outlined.Add, "Add task") }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(TaskFilter.ALL to "Open", TaskFilter.TODAY to "Today", TaskFilter.UPCOMING to "Upcoming", TaskFilter.COMPLETED to "Done").forEach { (f, label) ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(label, fontSize = 12.sp) })
            }
        }
        Spacer(Modifier.height(14.dp))
        if (visible.isEmpty()) EmptyState(if (filter == TaskFilter.COMPLETED) "No completed tasks" else "Nothing here", "Add a task when something matters.") { add = true }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(visible, key = { it.id }) { task ->
                TaskRow(task, { update(tasks.map { if (it.id == task.id) it.copy(done = !it.done) else it }) }, { editing = task })
            }
        }
    }
    if (add) TaskDialog(null, { task -> update(tasks + task); add = false }, { add = false })
    editing?.let { old -> TaskDialog(old, { task -> update(tasks.map { if (it.id == old.id) task.copy(id = old.id) else it }); editing = null }, { editing = null }, { update(tasks.filterNot { it.id == old.id }); editing = null }) }
}

@Composable private fun TaskRow(task: Task, toggle: () -> Unit, edit: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { edit() }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(CardWhite), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(task.done, { toggle() }, colors = CheckboxDefaults.colors(checkedColor = Ink))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.Medium)
                Text("${task.category}  •  ${prettyDate(task.due)}  •  ${task.priority.name.lowercase().replaceFirstChar { it.uppercase() }}", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable private fun TaskDialog(task: Task?, onSave: (Task) -> Unit, onCancel: () -> Unit, onDelete: (() -> Unit)? = null) {
    var title by remember(task?.id) { mutableStateOf(task?.title ?: "") }
    var category by remember(task?.id) { mutableStateOf(task?.category ?: "General") }
    var due by remember(task?.id) { mutableStateOf(task?.due ?: LocalDate.now().toString()) }
    var priority by remember(task?.id) { mutableStateOf(task?.priority ?: Priority.MEDIUM) }
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (task == null) "New task" else "Edit task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, singleLine = true, label = { Text("Task") }, shape = RoundedCornerShape(14.dp))
                OutlinedTextField(category, { category = it }, singleLine = true, label = { Text("Category") }, shape = RoundedCornerShape(14.dp))
                OutlinedButton(onClick = {
                    val d = parseDate(due)
                    DatePickerDialog(context, { _, y, m, day -> due = LocalDate.of(y, m + 1, day).toString() }, d.year, d.monthValue - 1, d.dayOfMonth).show()
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Due: ${prettyDate(due)}") }
                Text("Priority", color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Priority.values().forEach { p -> FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) }) }
                }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(Task(task?.id ?: System.currentTimeMillis(), title.trim(), task?.done ?: false, due, category.trim().ifBlank { "General" }, priority)) }) { Text("Save") } },
        dismissButton = { Row { if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete") }; TextButton(onClick = onCancel) { Text("Cancel") } } }
    )
}

@Composable private fun HabitsScreen(habits: List<HabitRecord>, update: (List<HabitRecord>) -> Unit) {
    var add by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf<Long?>(null) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Habits", fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text("Small actions, repeated", color = Muted, fontSize = 14.sp) }
            FilledIconButton(onClick = { add = true }, shape = RoundedCornerShape(16.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Ink, contentColor = Color.White)) { Icon(Icons.Outlined.Add, "Add habit") }
        }
        Spacer(Modifier.height(18.dp))
        if (habits.isEmpty()) EmptyState("Build a rhythm", "Create your first daily habit.") { add = true }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(habits, key = { it.id }) { habit ->
                val open = expanded == habit.id
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(CardWhite), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(habit.isComplete(today), { update(habits.map { if (it.id == habit.id) it.toggle(today) else it }) }, colors = CheckboxDefaults.colors(checkedColor = Ink))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) { Text(habit.title, fontWeight = FontWeight.Medium); Text(if (habit.streak(today) == 0) "Start today" else "${habit.streak(today)} day streak", color = Muted, fontSize = 12.sp) }
                            TextButton(onClick = { expanded = if (open) null else habit.id }) { Text(if (open) "Hide" else "Calendar") }
                        }
                        if (open) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year}", fontWeight = FontWeight.SemiBold)
                                Row { IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Outlined.ChevronLeft, "Previous month") }; IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Outlined.ChevronRight, "Next month") } }
                            }
                            val end = if (month == YearMonth.from(today)) today else month.atEndOfMonth()
                            Text("${habit.completionPercentThrough(month, end)}% completed", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                            HabitCalendarGrid(month, habit.completedDates, null, { date -> update(habits.map { if (it.id == habit.id) it.toggle(date) else it }) })
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { update(habits.filterNot { it.id == habit.id }); expanded = null }) { Text("Delete") } }
                        }
                    }
                }
            }
        }
    }
    if (add) {
        var name by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { add = false }, title = { Text("New habit") }, text = { OutlinedTextField(name, { name = it }, label = { Text("Habit") }, singleLine = true, shape = RoundedCornerShape(14.dp)) }, confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { update(habits + HabitRecord(System.currentTimeMillis(), name.trim())); add = false }) { Text("Add") } }, dismissButton = { TextButton(onClick = { add = false }) { Text("Cancel") } })
    }
}

@Composable private fun EmptyState(title: String, subtitle: String, action: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, textAlign = TextAlign.Center); Button(onClick = action, shape = RoundedCornerShape(16.dp)) { Text("Get started") }
    }
}

@Composable private fun FocusScreen(history: List<Int>, onCompleted: (Int) -> Unit) {
    var length by remember { mutableIntStateOf(25) }
    var seconds by remember { mutableIntStateOf(1500) }
    var running by remember { mutableStateOf(false) }
    var completed by remember { mutableIntStateOf(0) }
    LaunchedEffect(running) {
        while (running && seconds > 0) { delay(1000); seconds-- }
        if (seconds == 0) { running = false; completed++; onCompleted(length); seconds = length * 60 }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Focus", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Text("One thing at a time.", color = Muted)
        Spacer(Modifier.height(28.dp))
        Text(String.format("%02d:%02d", seconds / 60, seconds % 60), fontSize = 68.sp, fontWeight = FontWeight.Light)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { running = !running }, shape = RoundedCornerShape(18.dp)) { Icon(if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(if (running) "Pause" else "Start") }
            OutlinedButton(onClick = { running = false; seconds = length * 60 }, shape = RoundedCornerShape(18.dp)) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Reset") }
        }
        Spacer(Modifier.height(8.dp)); Text("Session length", color = Muted, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(15, 25, 50).forEach { m -> FilterChip(selected = length == m, onClick = { length = m; if (!running) seconds = m * 60 }, label = { Text("$m min") }) } }
        if (completed > 0) Text("$completed session${if (completed == 1) "" else "s"} completed this visit", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(CardWhite), elevation = CardDefaults.cardElevation(0.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Focus history", fontWeight = FontWeight.SemiBold)
                Text("${history.size} completed sessions • ${history.sum()} minutes total", color = Muted, fontSize = 13.sp)
                if (history.isNotEmpty()) Text("Recent: " + history.takeLast(5).joinToString(" • ") { "$it min" }, color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable private fun GlassBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf("Home" to Icons.Outlined.Home, "Tasks" to Icons.Outlined.Checklist, "Habits" to Icons.Outlined.CheckCircleOutline, "Focus" to Icons.Outlined.PlayArrow)
    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = .88f)).padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(selected = selected == index, onClick = { onSelect(index) }, icon = { Icon(item.second, item.first) }, label = { Text(item.first, fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Ink, selectedTextColor = Ink, indicatorColor = Soft, unselectedIconColor = Muted, unselectedTextColor = Muted))
        }
    }
}

private fun parseDate(value: String): LocalDate = try { LocalDate.parse(value, DateFormatter) } catch (_: DateTimeParseException) { LocalDate.now() }
private fun prettyDate(value: String): String {
    val d = parseDate(value); val today = LocalDate.now()
    return when (d) { today -> "Today"; today.plusDays(1) -> "Tomorrow"; else -> d.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())) }
}
