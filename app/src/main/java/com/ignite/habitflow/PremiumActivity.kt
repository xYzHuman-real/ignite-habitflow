package com.ignite.habitflow

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
private fun PremiumScreen(activity: PremiumActivity) {
    val context = activity
    var active by remember { mutableStateOf(PremiumAccess.isPremium(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    val billing = remember {
        BillingManager(context, { enabled -> active = enabled; PremiumAccess.setPremiumForBilling(context, enabled) }) { message = it }
    }
    DisposableEffect(Unit) {
        billing.connect()
        onDispose { }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF171717), background = Color(0xFFF7F7F5), surface = Color.White)) {
        Scaffold(topBar = { TopAppBar(title = { Text("Ignite Premium") }) }, containerColor = Color(0xFFF7F7F5)) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                item {
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
                        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("More room to build your system", fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (active) "Premium is active on this Google Play account." else "Unlock the deeper parts of your productivity system.", color = Color.Gray, fontSize = 14.sp)
                            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFEDEDEB)) { Text(if (active) "PREMIUM ACTIVE" else "LIMITED-TIME LAUNCH OFFER", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                item { Text("Premium features", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                items(PremiumFeature.values().size) { index ->
                    val feature = PremiumFeature.values()[index]
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lock, null, tint = Color.Gray, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(12.dp)); Text(feature.title, fontSize = 14.sp)
                        }
                    }
                }
                item {
                    Text("Limited-time launch pricing", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                    Text("Get the launch price while the offer is available.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp, bottom = 8.dp))
                    PremiumPlan("Monthly", "₹199 / month", "₹49 / month", BillingManager.MONTHLY) { billing.buy(activity, it) }
                    PremiumPlan("Yearly", "₹399 / year", "₹199 / year", BillingManager.YEARLY) { billing.buy(activity, it) }
                    PremiumPlan("Lifetime", "₹849 once", "₹349 lifetime", BillingManager.LIFETIME) { billing.buy(activity, it) }
                }
                item { Text("Prices shown are the planned regular and launch prices. The final amount and offer availability are controlled by the products configured in Google Play Console. Purchases are processed by Google Play.", color = Color.Gray, fontSize = 12.sp) }
            }
        }
    }
    message?.let { text -> LaunchedEffect(text) { Toast.makeText(context, text, Toast.LENGTH_SHORT).show(); message = null } }
}

@Composable
private fun PremiumPlan(title: String, regularPrice: String, launchPrice: String, productId: String, onBuy: (String) -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(regularPrice, color = Color.Gray, fontSize = 12.sp, textDecoration = TextDecoration.LineThrough)
                Text(launchPrice, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("LIMITED-TIME OFFER", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = { onBuy(productId) }, shape = RoundedCornerShape(14.dp)) { Text("Choose") }
        }
    }
}
