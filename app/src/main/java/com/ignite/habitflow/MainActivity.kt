package com.ignite.habitflow

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AppBackground = Color(0xFFF7F7F5)
private val Ink = Color(0xFF171717)
private val Muted = Color(0xFF777777)
private val Soft = Color(0xFFEDEDEB)
private val CardWhite = Color.White

private enum class Priority { LOW, MEDIUM, HIGH }
private enum class TaskFilter { ALL, TODAY, UPCOMING, COMPLETED }
private enum class Recurrence { NONE, DAILY, WEEKDAYS, WEEKLY, MONTHLY }
private data class Subtask(val id: Long, val title: String, val done: Boolean = false)
private data class Task(val id: Long, val title: String, val done: Boolean, val due: String = LocalDate.now().toString(), val category: String = "General", val priority: Priority = Priority.MEDIUM, val recurrence: Recurrence = Recurrence.NONE, val subtasks: List<Subtask> = emptyList())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { IgniteHabitFlowApp() } }
}

private class Store(context: Context) {
    private val prefs = context.getSharedPreferences("habitflow", Context.MODE_PRIVATE)
    fun loadTasks(): List<Task> = runCatching {
        val a = JSONArray(prefs.getString("tasks", "[]"))
        List(a.length()) { i ->
            val o = a.getJSONObject(i); val subs = mutableListOf<Subtask>()
            o.optJSONArray("subtasks")?.let { s -> for (j in 0 until s.length()) { val x = s.optJSONObject(j) ?: continue; subs += Subtask(x.optLong("id", j.toLong()), x.optString("title", "Subtask"), x.optBoolean("done", false)) } }
            Task(o.getLong("id"), o.getString("title"), o.optBoolean("done", false), o.optString("due", LocalDate.now().toString()), o.optString("category", "General"), runCatching { Priority.valueOf(o.optString("priority", "MEDIUM")) }.getOrDefault(Priority.MEDIUM), runCatching { Recurrence.valueOf(o.optString("recurrence", "NONE")) }.getOrDefault(Recurrence.NONE), subs)
        }
    }.getOrDefault(emptyList())
    fun saveTasks(list: List<Task>) { val a = JSONArray(); list.forEach { t -> val s = JSONArray(); t.subtasks.forEach { x -> s.put(JSONObject().put("id", x.id).put("title", x.title).put("done", x.done)) }; a.put(JSONObject().put("id", t.id).put("title", t.title).put("done", t.done).put("due", t.due).put("category", t.category).put("priority", t.priority.name).put("recurrence", t.recurrence.name).put("subtasks", s)) }; prefs.edit().putString("tasks", a.toString()).apply() }
    fun saveFocus(minutes: Int) = prefs.edit().putInt("focus", prefs.getInt("focus", 0) + minutes).apply()
    fun loadFocusHistory(): List<Int> = runCatching { val a = JSONArray(prefs.getString("focus_history", "[]")); List(a.length()) { a.getInt(it) } }.getOrDefault(emptyList())
    fun addFocusHistory(minutes: Int) { val h = loadFocusHistory().takeLast(29) + minutes; val a = JSONArray(); h.forEach(a::put); prefs.edit().putString("focus_history", a.toString()).apply() }
}

private fun parseDate(value: String): LocalDate = runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
private fun prettyDate(value: String): String = parseDate(value).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
private fun nextDue(date: LocalDate, recurrence: Recurrence): LocalDate = when (recurrence) {
    Recurrence.NONE -> date
    Recurrence.DAILY -> date.plusDays(1)
    Recurrence.WEEKDAYS -> { var d = date.plusDays(1); while (d.dayOfWeek.value > 5) d = d.plusDays(1); d }
    Recurrence.WEEKLY -> date.plusWeeks(1)
    Recurrence.MONTHLY -> date.plusMonths(1)
}

@Composable private fun IgniteHabitFlowApp() {
    val context = androidx.compose.ui.platform.LocalContext.current; val store = remember { Store(context) }; val habitStore = remember { HabitStoreV2(context) }
    var tab by remember { mutableIntStateOf(0) }; var tasks by remember { mutableStateOf(store.loadTasks()) }; var habits by remember { mutableStateOf(habitStore.load()) }; var focusHistory by remember { mutableStateOf(store.loadFocusHistory()) }
    fun saveTasks(v: List<Task>) { tasks = v; store.saveTasks(v) }; fun saveHabits(v: List<HabitRecord>) { habits = v; habitStore.save(v) }; fun completedFocus(m: Int) { store.saveFocus(m); store.addFocusHistory(m); focusHistory = store.loadFocusHistory() }
    MaterialTheme(colorScheme = lightColorScheme(background = AppBackground, surface = CardWhite, primary = Ink, onPrimary = Color.White, onBackground = Ink, onSurface = Ink)) {
        Scaffold(containerColor = AppBackground, bottomBar = { GlassBottomBar(tab) { tab = it } }) { padding -> Box(Modifier.padding(padding)) { when (tab) { 0 -> HomeScreen(tasks, habits, focusHistory) { tab = it }; 1 -> TasksScreen(tasks, ::saveTasks); 2 -> HabitsScreen(habits, ::saveHabits); 3 -> FocusScreen(focusHistory, ::completedFocus) } } }
    }
}

@Composable private fun HomeScreen(tasks: List<Task>, habits: List<HabitRecord>, focusHistory: List<Int>, navigate: (Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current; val today = LocalDate.now(); val dueToday = tasks.count { !it.done && parseDate(it.due) == today }; val doneToday = tasks.count { it.done && parseDate(it.due) == today }; val habitDone = habits.count { it.isComplete(today) }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())), color = Muted, fontSize = 14.sp); Text("Make today count.", fontSize = 32.sp, fontWeight = FontWeight.SemiBold) }; IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) { Icon(Icons.Outlined.Settings, "Settings") } }
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(CardWhite), elevation = CardDefaults.cardElevation(0.dp)) { Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Text("Today", fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text(if (dueToday == 0) "You're clear for today" else "$dueToday task${if (dueToday == 1) "" else "s"} due today", color = Muted); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatPill("Tasks", "$doneToday/${tasks.count { parseDate(it.due) == today }}", Modifier.weight(1f)); StatPill("Habits", "$habitDone/${habits.size}", Modifier.weight(1f)); StatPill("Focus", "${focusHistory.sum()}m", Modifier.weight(1f)) } } }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("Quick actions", fontSize = 18.sp, fontWeight = FontWeight.SemiBold); TextButton(onClick = { context.startActivity(Intent(context, InsightsActivity::class.java)) }) { Text("Insights") } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { QuickAction("Add task", Icons.Outlined.AddTask, Modifier.weight(1f)) { navigate(1) }; QuickAction("Start focus", Icons.Outlined.PlayArrow, Modifier.weight(1f)) { navigate(3) } }
        tasks.filter { !it.done }.sortedWith(compareBy({ parseDate(it.due) }, { -it.priority.ordinal })).take(3).forEach { TaskRow(it, {}, {}) }
    }
}

@Composable private fun StatPill(label: String, value: String, modifier: Modifier) { Column(modifier.clip(RoundedCornerShape(14.dp)).background(Soft).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontWeight = FontWeight.SemiBold, fontSize = 15.sp); Text(label, color = Muted, fontSize = 11.sp) } }
@Composable private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) { OutlinedButton(onClick = onClick, modifier = modifier.height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(label, fontWeight = FontWeight.Medium) } }

@Composable private fun TasksScreen(tasks: List<Task>, update: (List<Task>) -> Unit) {
    var add by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf<Task?>(null) }; var filter by remember { mutableStateOf(TaskFilter.ALL) }; val today = LocalDate.now()
    val visible = tasks.filter { when (filter) { TaskFilter.ALL -> !it.done; TaskFilter.TODAY -> !it.done && parseDate(it.due) == today; TaskFilter.UPCOMING -> !it.done && parseDate(it.due).isAfter(today); TaskFilter.COMPLETED -> it.done } }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("Tasks", fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text("${tasks.count { !it.done }} remaining", color = Muted, fontSize = 14.sp) }; FilledIconButton(onClick = { add = true }, shape = RoundedCornerShape(16.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Ink, contentColor = Color.White)) { Icon(Icons.Outlined.Add, "Add task") } }
        Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(TaskFilter.ALL to "Open", TaskFilter.TODAY to "Today", TaskFilter.UPCOMING to "Upcoming", TaskFilter.COMPLETED to "Done").forEach { (f, label) -> FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(label, fontSize = 12.sp) }) } }; Spacer(Modifier.height(14.dp))
        if (visible.isEmpty()) EmptyState(if (filter == TaskFilter.COMPLETED) "No completed tasks" else "Nothing here", "Add a task when something matters.") { add = true } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) { items(visible, key = { it.id }) { task -> TaskRow(task, { update(tasks.map { if (it.id != task.id) it else if (!task.done && task.recurrence != Recurrence.NONE) task.copy(due = nextDue(parseDate(task.due), task.recurrence).toString(), done = false) else task.copy(done = !task.done) }) }, { editing = task }) } }
    }
    if (add) TaskDialog(null, { task -> update(tasks + task); add = false }, { add = false })
    editing?.let { old -> TaskDialog(old, { task -> update(tasks.map { if (it.id == old.id) task.copy(id = old.id) else it }); editing = null }, { editing = null }, { update(tasks.filterNot { it.id == old.id }); editing = null }) }
}

@Composable private fun TaskRow(task: Task, toggle: () -> Unit, edit: () -> Unit) {
    val subDone = task.subtasks.count { it.done }; val meta = buildString { append(task.category); append("  •  "); append(prettyDate(task.due)); append("  •  "); append(task.priority.name.lowercase().replaceFirstChar { it.uppercase() }); if (task.recurrence != Recurrence.NONE) { append("  •  "); append(task.recurrence.name.lowercase().replaceFirstChar { it.uppercase() }) } }
    Card(Modifier.fillMaxWidth().clickable { edit() }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(CardWhite), elevation = CardDefaults.cardElevation(0.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(task.done, { toggle() }, colors = CheckboxDefaults.colors(checkedColor = Ink)); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text(task.title, fontWeight = FontWeight.Medium); Text(meta, color = Muted, fontSize = 12.sp); if (task.subtasks.isNotEmpty()) Text("$subDone/${task.subtasks.size} subtasks", color = Muted, fontSize = 11.sp) } } }
}

@Composable private fun TaskDialog(task: Task?, onSave: (Task) -> Unit, onCancel: () -> Unit, onDelete: (() -> Unit)? = null) {
    var title by remember(task?.id) { mutableStateOf(task?.title ?: "") }; var category by remember(task?.id) { mutableStateOf(task?.category ?: "General") }; var due by remember(task?.id) { mutableStateOf(task?.due ?: LocalDate.now().toString()) }; var priority by remember(task?.id) { mutableStateOf(task?.priority ?: Priority.MEDIUM) }; var recurrence by remember(task?.id) { mutableStateOf(task?.recurrence ?: Recurrence.NONE) }; var subtasks by remember(task?.id) { mutableStateOf(task?.subtasks ?: emptyList()) }; var newSubtask by remember(task?.id) { mutableStateOf("") }; val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(onDismissRequest = onCancel, title = { Text(if (task == null) "New task" else "Edit task") }, text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.heightIn(max = 520.dp)) {
        OutlinedTextField(title, { title = it }, singleLine = true, label = { Text("Task") }, shape = RoundedCornerShape(14.dp)); OutlinedTextField(category, { category = it }, singleLine = true, label = { Text("Category") }, shape = RoundedCornerShape(14.dp))
        OutlinedButton(onClick = { val d = parseDate(due); DatePickerDialog(context, { _, y, m, day -> due = LocalDate.of(y, m + 1, day).toString() }, d.year, d.monthValue - 1, d.dayOfMonth).show() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Due: ${prettyDate(due)}") }
        Text("Priority", color = Muted, fontSize = 12.sp); Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { Priority.values().forEach { p -> FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) }) } }
        Text("Repeat", color = Muted, fontSize = 12.sp); Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { Recurrence.values().forEach { r -> FilterChip(selected = recurrence == r, onClick = { recurrence = r }, label = { Text(r.name.lowercase().replaceFirstChar { it.uppercase() }) }) } }
        Text("Subtasks", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        subtasks.forEach { sub -> Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(sub.done, { subtasks = subtasks.map { if (it.id == sub.id) it.copy(done = !it.done) else it } }, colors = CheckboxDefaults.colors(checkedColor = Ink)); Text(sub.title, Modifier.weight(1f), fontSize = 13.sp); IconButton(onClick = { subtasks = subtasks.filterNot { it.id == sub.id } }) { Icon(Icons.Outlined.Close, "Remove subtask") } } }
        Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(newSubtask, { newSubtask = it }, singleLine = true, modifier = Modifier.weight(1f), label = { Text("Add subtask") }, shape = RoundedCornerShape(14.dp)); Spacer(Modifier.width(6.dp)); IconButton(enabled = newSubtask.isNotBlank(), onClick = { subtasks = subtasks + Subtask(System.nanoTime(), newSubtask.trim()); newSubtask = "" }) { Icon(Icons.Outlined.Add, "Add subtask") } }
    } }, confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(Task(task?.id ?: System.currentTimeMillis(), title.trim(), task?.done ?: false, due, category.trim().ifBlank { "General" }, priority, recurrence, subtasks)) }) { Text("Save") } }, dismissButton = { Row { if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete") }; TextButton(onClick = onCancel) { Text("Cancel") } } })
}

@Composable private fun HabitsScreen(habits: List<HabitRecord>, update: (List<HabitRecord>) -> Unit) {
    var add by remember { mutableStateOf(false) }; var expanded by remember { mutableStateOf<Long?>(null) }; var month by remember { mutableStateOf(YearMonth.now()) }; val today = LocalDate.now()
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("Habits", fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text("Small actions, repeated", color = Muted, fontSize = 14.sp) }; FilledIconButton(onClick = { add = true }, shape = RoundedCornerShape(16.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Ink, contentColor = Color.White)) { Icon(Icons.Outlined.Add, "Add habit") } }; Spacer(Modifier.height(18.dp))
        if (habits.isEmpty()) EmptyState("Build a rhythm", "Create your first daily habit.") { add = true } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) { items(habits, key = { it.id }) { habit -> val open = expanded == habit.id; Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(CardWhite), elevation = CardDefaults.cardElevation(0.dp)) { Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(habit.isComplete(today), { update(habits.map { if (it.id == habit.id) it.toggle(today) else it }) }, colors = CheckboxDefaults.colors(checkedColor = Ink)); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text(habit.title, fontWeight = FontWeight.Medium); Text(if (habit.streak(today) == 0) "Start today" else "${habit.streak(today)} day streak", color = Muted, fontSize = 12.sp) }; TextButton(onClick = { expanded = if (open) null else habit.id }) { Text(if (open) "Hide" else "Calendar") } }
            if (open) { HorizontalDivider(Modifier.padding(vertical = 8.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year}", fontWeight = FontWeight.SemiBold); Row { IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Outlined.ChevronLeft, "Previous month") }; IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Outlined.ChevronRight, "Next month") } } }; val end = if (month == YearMonth.from(today)) today else month.atEndOfMonth(); Text("${habit.completionPercentThrough(month, end)}% completed", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)); HabitCalendarGrid(month, habit.completedDates, null, { date -> update(habits.map { if (it.id == habit.id) it.toggle(date) else it }) }); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { update(habits.filterNot { it.id == habit.id }); expanded = null }) { Text("Delete") } } }
        } } } }
    }
    if (add) { var title by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { add = false }, title = { Text("New habit") }, text = { OutlinedTextField(title, { title = it }, singleLine = true, label = { Text("Habit") }, shape = RoundedCornerShape(14.dp)) }, confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { update(habits + HabitRecord(System.currentTimeMillis(), title.trim())); add = false }) { Text("Create") } }, dismissButton = { TextButton(onClick = { add = false }) { Text("Cancel") } }) }
}

@Composable private fun FocusScreen(history: List<Int>, onComplete: (Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("habitflow", Context.MODE_PRIVATE) }
    var selected by remember { mutableIntStateOf(prefs.getInt("default_focus", 25)) }
    var shortBreak by remember { mutableIntStateOf(prefs.getInt("short_break", 5)) }
    var longBreak by remember { mutableIntStateOf(prefs.getInt("long_break", 15)) }
    var targetSessions by remember { mutableIntStateOf(prefs.getInt("session_count", 4)) }
    var phase by remember { mutableStateOf("Focus") }
    var completedSessions by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(selected * 60) }
    var running by remember { mutableStateOf(false) }

    fun resetPhase(nextPhase: String, minutes: Int) {
        phase = nextPhase
        seconds = minutes * 60
        running = false
    }

    LaunchedEffect(running, phase, selected, shortBreak, longBreak, targetSessions) {
        while (running && seconds > 0) { delay(1000); seconds-- }
        if (running && seconds == 0) {
            if (phase == "Focus") {
                onComplete(selected)
                completedSessions++
                if (completedSessions >= targetSessions) {
                    resetPhase("Long break", longBreak)
                    completedSessions = 0
                } else {
                    resetPhase("Short break", shortBreak)
                }
            } else {
                resetPhase("Focus", selected)
                running = true
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Spacer(Modifier.height(20.dp))
        Text("Focus", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Text(phase, color = Muted)
        Text(String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60), fontSize = 58.sp, fontWeight = FontWeight.SemiBold)
        if (phase == "Focus") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(15, 25, 50).forEach { m -> FilterChip(selected = selected == m, onClick = { if (!running) { selected = m; seconds = m * 60 } }, label = { Text("${m}m") }) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { running = !running }, shape = RoundedCornerShape(16.dp)) { Text(if (running) "Pause" else "Start") }
            OutlinedButton(onClick = { running = false; phase = "Focus"; completedSessions = 0; seconds = selected * 60 }, shape = RoundedCornerShape(16.dp)) { Text("Reset") }
        }
        Text("Session ${completedSessions + 1} of $targetSessions", color = Muted, fontSize = 13.sp)
        Text("Completed focus: ${history.sum()} minutes", color = Muted)
        Text("Sessions saved: ${history.size}", color = Muted, fontSize = 12.sp)
        Text("Breaks: ${shortBreak}m / ${longBreak}m", color = Muted, fontSize = 12.sp)
    }
}

@Composable private fun EmptyState(title: String, subtitle: String, action: () -> Unit) { Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)); TextButton(onClick = action, modifier = Modifier.padding(top = 8.dp)) { Text("Get started") } } }
@Composable private fun GlassBottomBar(selected: Int, onSelect: (Int) -> Unit) { NavigationBar(containerColor = Color.White.copy(alpha = .92f)) { listOf(Icons.Outlined.Home to "Home", Icons.Outlined.Checklist to "Tasks", Icons.Outlined.CheckCircleOutline to "Habits", Icons.Outlined.Timer to "Focus").forEachIndexed { i, pair -> NavigationBarItem(selected = selected == i, onClick = { onSelect(i) }, icon = { Icon(pair.first, pair.second) }, label = { Text(pair.second) }) } } }
