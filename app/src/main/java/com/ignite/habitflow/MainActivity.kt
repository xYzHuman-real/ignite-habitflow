package com.ignite.habitflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

private val AppBackground = Color(0xFFF7F7F5)
private val Ink = Color(0xFF171717)
private val Muted = Color(0xFF777777)
private val Accent = Color(0xFF171717)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IgniteHabitFlowApp() }
    }
}

@Composable
private fun IgniteHabitFlowApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    MaterialTheme {
        Scaffold(
            containerColor = AppBackground,
            bottomBar = {
                GlassBottomBar(selectedTab) { selectedTab = it }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> HomeScreen()
                    1 -> TasksScreen()
                    2 -> HabitsScreen()
                    3 -> FocusScreen()
                }
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Good morning", color = Muted, fontSize = 15.sp)
        Text("Make today count.", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Today", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("0 tasks completed", color = Muted, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPill("Tasks", "0/0")
                    StatPill("Habits", "0/0")
                    StatPill("Focus", "0m")
                }
            }
        }

        Text("Quick actions", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("Add task", Icons.Outlined.Checklist, Modifier.weight(1f))
            QuickAction("Start focus", Icons.Outlined.PlayArrow, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFFF3F3F1)).padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(label, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    OutlinedButton(
        onClick = {},
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E2DF)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
    ) {
        Icon(icon, contentDescription = null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TasksScreen() {
    EmptySection("Tasks", "Keep the list simple. Add what matters today.", "Add your first task")
}

@Composable
private fun HabitsScreen() {
    EmptySection("Habits", "Build consistency one day at a time.", "Add your first habit")
}

@Composable
private fun EmptySection(title: String, subtitle: String, action: String) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, color = Ink, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Muted, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {},
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) { Text(action) }
    }
}

@Composable
private fun FocusScreen() {
    var seconds by remember { mutableIntStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running && seconds > 0) {
            delay(1000)
            seconds--
        }
        if (seconds == 0) running = false
    }

    val minutes = seconds / 60
    val remainingSeconds = seconds % 60

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Focus", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Text("25 minute session", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(30.dp))
        Text(
            String.format("%02d:%02d", minutes, remainingSeconds),
            color = Ink,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { running = !running },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) {
                Icon(if (running) Icons.Outlined.CheckCircleOutline else Icons.Outlined.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(if (running) "Running" else "Start")
            }
            OutlinedButton(
                onClick = { running = false; seconds = 25 * 60 },
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Outlined.Refresh, null)
                Spacer(Modifier.width(6.dp))
                Text("Reset")
            }
        }
    }
}

@Composable
private fun GlassBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        "Home" to Icons.Outlined.Home,
        "Tasks" to Icons.Outlined.Checklist,
        "Habits" to Icons.Outlined.CheckCircleOutline,
        "Focus" to Icons.Outlined.PlayArrow
    )

    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.86f))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEachIndexed { index, item ->
            val active = selected == index
            TextButton(
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (active) Color(0xFFEDEDEB) else Color.Transparent,
                    contentColor = Ink
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(item.second, contentDescription = item.first, Modifier.size(20.dp))
                    Text(item.first, fontSize = 10.sp)
                }
            }
        }
    }
}
