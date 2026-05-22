package com.sleeptap.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sleeptap.SleepTapApp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen() {
    val db = (LocalContext.current.applicationContext as SleepTapApp).db
    val sleepPeriods by db.eventDao().getAllSleepPeriods().collectAsState(initial = emptyList())
    val lastSleep = sleepPeriods.lastOrNull()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SleepTap", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        if (lastSleep != null) {
            val fmt = SimpleDateFormat("hh:mm a", Locale.US)
            Text("Last sleep: ${fmt.format(Date(lastSleep.sleepOnset))} → ${fmt.format(Date(lastSleep.wakeTime))}")
            val confidence = (lastSleep.confidence * 100).toInt()
            Text("Confidence: $confidence%")
        } else {
            Text("No sleep data yet. Keep the accessibility service enabled.")
        }
    }
}
