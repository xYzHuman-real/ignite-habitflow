package com.ignite.habitflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class PremiumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PremiumScreen() }
    }
}

@Composable
private fun PremiumScreen() {
    val ink = Color(0xFF171717)
    val background = Color(0xFFF7F7F5)
    MaterialTheme(colorScheme = lightColorScheme(primary = ink)) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Ignite Premium") }) },
            containerColor = background
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(Color.White),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("More room to build your system", fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                        Text("Premium features are being prepared for the full release.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
                Text("Planned Premium", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                listOf(
                    "Unlimited habits and categories",
                    "Advanced weekly, monthly and yearly insights",
                    "Custom focus and break presets",
                    "Focus history and deeper trends",
                    "Themes and home customization",
                    "Subtasks and recurring task controls"
                ).forEach { feature ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(Color.White),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) { Text("✓  $feature", Modifier.padding(16.dp), fontSize = 14.sp) }
                }
                Text(
                    "Pricing planned: ₹50/month · ₹200/year · ₹350 lifetime",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "Purchases are not enabled yet. No payment is requested from this screen.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
