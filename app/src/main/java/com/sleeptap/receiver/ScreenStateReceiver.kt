package com.sleeptap.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleeptap.SleepTapApp
import com.sleeptap.accessibility.SleepTapAccessibilityService
import com.sleeptap.data.InteractionEvent
import com.sleeptap.logic.SleepEstimator
import kotlinx.coroutines.*

class ScreenStateReceiver : BroadcastReceiver() {
    private var screenOnEpisodeId = -1L
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val db = (context.applicationContext as SleepTapApp).db
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                screenOnEpisodeId = System.currentTimeMillis()
                SleepTapAccessibilityService.instance?.startNewEpisode(screenOnEpisodeId)
            }
            Intent.ACTION_SCREEN_OFF -> {
                scope.launch {
                    // Insert a dummy event to mark screen off
                    db.eventDao().insertScreenOffMarker(
                        InteractionEvent(
                            timestamp = System.currentTimeMillis(),
                            eventType = "screen_off",
                            screenOnEpisodeId = screenOnEpisodeId
                        )
                    )
                    // Delay to allow any accidental wake events to appear
                    delay(15_000L)
                    SleepEstimator(context).analyzeRecent()
                }
            }
            Intent.ACTION_USER_PRESENT -> { /* optional */ }
        }
    }
}
