package com.ignite.habitflow

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AppBackground = Color(0xFFF7F7F5)
private val Ink = Color(0xFF171717)
private val Muted = Color(0xFF777777)
private val Soft = Color(0xFFEDEDEB)
private val CardWhite = Color.White

private data class Task(val id: Long, val title: String, val done: Boolean, val due: String = "Today")
private data class Habit(val id: Long, val title: String, val doneToday: Boolean, val streak: Int)

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
            Task(o.getLong("id"), o.getString("title"), o.getBoolean("done"), o.optString("due", "Today"))
        }
    }.getOrDefault(emptyList())
    fun saveTasks(list: List<Task>) {
        val a = JSONArray(); list.forEach { a.put(JSONObject().apply { put("id", it.id); put("title", it.title); put("done", it.done); put("due", it.due) }) }
        prefs.edit().putString("tasks", a.toString()).apply()
    }
    fun loadHabits(): List<Habit> = runCatching {
        val a = JSONArray(prefs.getString("habits", "[]"))
        List(a.length()) { i -> val o = a.getJSONObject(i); Habit(o.getLong("id"), o.getString("title"), o.getBoolean("done"), o.getInt("streak")) }
    }.getOrDefault(emptyList())
    fun saveHabits(list: List<Habit>) {
        val a = JSONArray(); list.forEach { a.put(JSONObject().apply { put("id", it.id); put("title", it.title); put("done", it.doneToday); put("streak", it.streak) }) }
        prefs.edit().putString("habits", a.toString()).apply()
    }
    fun saveFocus(minutes: Int) = prefs.edit().putInt("focus", prefs.getInt("focus", 0) + minutes).apply()
    fun focusMinutes() = prefs.getInt("focus", 0)
}

@Composable
private fun IgniteHabitFlowApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { Store(context) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var tasks by remember { mutableStateOf(store.loadTasks()) }
    var habits by remember { mutableStateOf(store.loadHabits()) }
    var focusMinutes by remember { mutableIntStateOf(store.focusMinutes()) }

    fun updateTasks(v: List<Task>) { tasks = v; store.saveTasks(v) }
    fun updateHabits(v: List<Habit>) { habits = v; store.saveHabits(v) }

    MaterialTheme(colorScheme = lightColorScheme(background = AppBackground, surface = CardWhite, primary = Ink, onPrimary = Color.White, onBackground = Ink, onSurface = Ink)) {
        Scaffold(containerColor = AppBackground, bottomBar = { GlassBottomBar(selectedTab) { selectedTab = it } }) { padding ->
            Box(Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> HomeScreen(tasks, habits, focusMinutes) { selectedTab = it }
                    1 -> TasksScreen(tasks, ::updateTasks)
                    2 -> HabitsScreen(habits, ::updateHabits)
                    3 -> FocusScreen { minutes -> store.saveFocus(minutes); focusMinutes = store.focusMinutes() }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(tasks: List<Task>, habits: List<Habit>, focus: Int, navigate: (Int) -> Unit) {
    val today = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    val completed = tasks.count { it.done }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(today, color = Muted, fontSize = 14.sp)
        Text("Make today count.", fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(CardWhite), elevation = CardDefaults.cardElevation(0.dp)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Today", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(if (tasks.isEmpty()) "No tasks yet" else "$completed of ${tasks.size} tasks completed", color = Muted, fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Tasks", "$completed/${tasks.size}", Modifier.weight(1f)); StatPill("Habits", "${habits.count { it.doneToday }}/${habits.size}", Modifier.weight(1f)); StatPill("Focus", "${focus}m", Modifier.weight(1f))
                }
            }
        }
        Text("Quick actions", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("Add task", Icons.Outlined.AddTask, Modifier.weight(1f)) { navigate(1) }
            QuickAction("Start focus", Icons.Outlined.PlayArrow, Modifier.weight(1f)) { navigate(3) }
        }
        if (tasks.isNotEmpty()) {
            Text("Next up", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            tasks.filterNot { it.done }.take(3).forEach { TaskRow(it, {}, {}) }
        }
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

@Composable
private fun TasksScreen(tasks: List<Task>, update: (List<Task>) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Tasks", fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text("${tasks.count { !it.done }} remaining", color = Muted, fontSize = 14.sp) }
            FilledIconButton(onClick = { showAdd = true }, shape = RoundedCornerShape(16.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Ink, contentColor = Color.White)) { Icon(Icons.Outlined.Add, "Add task") }
        }
        Spacer(Modifier.height(18.dp))
        if (tasks.isEmpty()) EmptyState("Nothing here yet", "Add a task when something matters.") { showAdd = true }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(tasks, key = { it.id }) { task -> TaskRow(task, { update(tasks.map { if (it.id == task.id) it.copy(done = !it.done) else it }) }, { editing = task })
                if (task == editing) { }
            }
        }
    }
    if (showAdd) TaskDialog(null, { title -> update(tasks + Task(System.currentTimeMillis(), title, false)); showAdd = false }, { showAdd = false })
    editing?.let { old -> TaskDialog(old, { title -> update(tasks.map { if (it.id == old.id) it.copy(title = title) else it }); editing = null }, { editing = null }, onDelete = { update(tasks.filterNot { it.id == old.id }); editing = null }) }
}

@Composable private fun TaskRow(task: Task, toggle: () -> Unit, edit: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { edit() }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(CardWhite), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.done, onCheckedChange = { toggle() }, colors = CheckboxDefaults.colors(checkedColor = Ink))
            Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text(task.title, fontWeight = FontWeight.Medium); Text(task.due, color = Muted, fontSize = 12.sp) }
        }
    }
}

@Composable private fun TaskDialog(task: Task?, onSave: (String) -> Unit, onCancel: () -> Unit, onDelete: (() -> Unit)? = null) {
    var title by remember(task?.id) { mutableStateOf(task?.title ?: "") }
    AlertDialog(onDismissRequest = onCancel, title = { Text(if (task == null) "New task" else "Edit task") }, text = { OutlinedTextField(title, { title = it }, singleLine = true, label = { Text("Task") }, shape = RoundedCornerShape(14.dp)) },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(title.trim()) }) { Text("Save") } },
        dismissButton = { Row { if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete") }; TextButton(onClick = onCancel) { Text("Cancel") } } })
}

@Composable
private fun HabitsScreen(habits: List<Habit>, update: (List<Habit>) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Habits", fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text("Small actions, repeated", color = Muted, fontSize = 14.sp) }
            FilledIconButton(onClick = { showAdd = true }, shape = RoundedCornerShape(16.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Ink, contentColor = Color.White)) { Icon(Icons.Outlined.Add, "Add habit") }
        }
        Spacer(Modifier.height(18.dp))
        if (habits.isEmpty()) EmptyState("Build a rhythm", "Create your first daily habit.") { showAdd = true }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(habits, key = { it.id }) { habit -> HabitRow(habit) { update(habits.map { if (it.id == habit.id) it.copy(doneToday = !it.doneToday, streak = if (!it.doneToday) it.streak + 1 else maxOf(0, it.streak - 1)) else it }) } } }
    }
    if (showAdd) {
        var name by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("New habit") }, text = { OutlinedTextField(name, { name = it }, label = { Text("Habit") }, singleLine = true, shape = RoundedCornerShape(14.dp)) }, confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { update(habits + Habit(System.currentTimeMillis(), name.trim(), false, 0)); showAdd = false }) { Text("Add") } }, dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } })
    }
}

@Composable private fun HabitRow(habit: Habit, toggle: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(CardWhite), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(habit.doneToday, { toggle() }, colors = CheckboxDefaults.colors(checkedColor = Ink)); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text(habit.title, fontWeight = FontWeight.Medium); Text(if (habit.streak == 0) "Start today" else "${habit.streak} day streak", color = Muted, fontSize = 12.sp) }
        }
    }
}

@Composable private fun EmptyState(title: String, subtitle: String, action: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, textAlign = TextAlign.Center); Button(onClick = action, shape = RoundedCornerShape(16.dp)) { Text("Get started") } }
}

@Composable
private fun FocusScreen(onCompleted: (Int) -> Unit) {
    var focusLength by remember { mutableIntStateOf(25) }; var seconds by remember { mutableIntStateOf(focusLength * 60) }; var running by remember { mutableStateOf(false) }; var completed by remember { mutableIntStateOf(0) }
    LaunchedEffect(running) { while (running && seconds > 0) { delay(1000); seconds-- }; if (seconds == 0) { running = false; completed++; onCompleted(focusLength); seconds = focusLength * 60 } }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Focus", fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text("One thing at a time.", color = Muted)
        Spacer(Modifier.height(32.dp)); Text(String.format("%02d:%02d", seconds / 60, seconds % 60), fontSize = 68.sp, fontWeight = FontWeight.Light)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Button(onClick = { running = !running }, shape = RoundedCornerShape(18.dp)) { Icon(if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(if (running) "Pause" else "Start") }; OutlinedButton(onClick = { running = false; seconds = focusLength * 60 }, shape = RoundedCornerShape(18.dp)) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Reset") } }
        Spacer(Modifier.height(10.dp)); Text("Session length", color = Muted, fontSize = 13.sp); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(15, 25, 50).forEach { m -> FilterChip(selected = focusLength == m, onClick = { focusLength = m; if (!running) seconds = m * 60 }, label = { Text("$m min") }) } }
        if (completed > 0) Text("$completed session${if (completed == 1) "" else "s"} completed this visit", color = Muted, fontSize = 13.sp)
    }
}

@Composable private fun GlassBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf("Home" to Icons.Outlined.Home, "Tasks" to Icons.Outlined.Checklist, "Habits" to Icons.Outlined.CheckCircleOutline, "Focus" to Icons.Outlined.PlayArrow)
    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = .88f)).padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEachIndexed { i, item -> TextButton(onClick = { onSelect(i) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.textButtonColors(containerColor = if (selected == i) Soft else Color.Transparent, contentColor = Ink)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(item.second, item.first, Modifier.size(20.dp)); Text(item.first, fontSize = 10.sp) } } }
    }
}
