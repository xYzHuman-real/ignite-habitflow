package com.ignite.habitflow

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class PremiumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PremiumScreen(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumScreen(context: Context) {
    val ink = Color(0xFF171717)
    val background = Color(0xFFF7F7F5)
    val active = remember { PremiumAccess.isPremium(context) }

    MaterialTheme(colorScheme = lightColorScheme(primary = ink, background = background, surface = Color.White)) {
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
                        Text(
                            if (active) "Premium is active on this device." else "Unlock the deeper parts of your productivity system when billing is enabled.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) Color(0xFFEDEDEB) else Color(0xFFF4F4F2)
                        ) {
                            Text(
                                if (active) "PREMIUM ACTIVE" else "PREMIUM NOT ACTIVE",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text("Premium features", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                PremiumFeature.values().forEach { feature ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(Color.White),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(feature.title, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Text(
                    "Pricing planned: ₹50/month · ₹200/year · ₹350 lifetime",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "Purchases are not enabled yet. Billing will be connected later; this screen never requests payment.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
