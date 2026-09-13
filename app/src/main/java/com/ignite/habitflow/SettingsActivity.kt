package com.ignite.habitflow

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SettingsScreen() }
    }
}

@Composable
private fun SettingsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("habitflow", Context.MODE_PRIVATE) }
    var theme by remember { mutableStateOf(prefs.getString("theme", "System") ?: "System") }
    var accent by remember { mutableStateOf(prefs.getString("accent", "Ink") ?: "Ink") }
    var defaultFocus by remember { mutableIntStateOf(prefs.getInt("default_focus", 25)) }

    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF171717))) {
        Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }, containerColor = Color(0xFFF7F7F5)) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("Appearance", fontSize = 20.sp)
                SettingChoice("Theme", listOf("System", "Light", "Dark"), theme) { theme = it; prefs.edit().putString("theme", it).apply() }
                SettingChoice("Accent", listOf("Ink", "Blue", "Green"), accent) { accent = it; prefs.edit().putString("accent", it).apply() }
                Text("Focus", fontSize = 20.sp)
                SettingChoice("Default focus", listOf("15", "25", "50"), defaultFocus.toString()) { defaultFocus = it.toInt(); prefs.edit().putInt("default_focus", defaultFocus).apply() }
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ignite HabitFlow", fontSize = 16.sp)
                        Text("Your tasks, habits and focus data are stored locally on this device.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingChoice(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Color.Gray, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { options.forEach { option -> FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option) }) } }
    }
}
