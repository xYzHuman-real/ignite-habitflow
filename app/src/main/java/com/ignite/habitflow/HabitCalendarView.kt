package com.ignite.habitflow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitCalendarView(completedDates: Set<String>, onToggle: (LocalDate) -> Unit) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()
    val first = month.atDay(1)
    val days = month.lengthOfMonth()
    val leading = first.dayOfWeek.value - 1
    val completed = (1..days).count { HabitHistory.isCompleted(completedDates, month.atDay(it)) }
    val percent = completed * 100 / days
    val ink = Color(0xFF171717)
    val soft = Color(0xFFEDEDEB)
    val muted = Color(0xFF777777)

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Outlined.ChevronLeft, "Previous month") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold)
                Text("$completed of $days days · $percent%", color = muted, fontSize = 12.sp)
            }
            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Outlined.ChevronRight, "Next month") }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("M","T","W","T","F","S","S").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = muted, fontSize = 12.sp) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            val rows = (leading + days + 6) / 7
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val index = row * 7 + col
                        val day = index - leading + 1
                        if (day in 1..days) {
                            val date = month.atDay(day)
                            val done = HabitHistory.isCompleted(completedDates, date)
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(RoundedCornerShape(10.dp)).background(if (done) ink else soft).clickable { onToggle(date) }, contentAlignment = Alignment.Center) {
                                Text("$day", color = if (done) Color.White else ink, fontSize = 12.sp, fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal)
                            }
                        } else Spacer(Modifier.weight(1f).aspectRatio(1f).padding(2.dp))
                    }
                }
            }
        }
        Text("Tap a day to mark it complete.", color = muted, fontSize = 12.sp)
    }
}
