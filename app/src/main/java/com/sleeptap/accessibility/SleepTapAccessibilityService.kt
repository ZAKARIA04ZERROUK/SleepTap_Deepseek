package com.sleeptap.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.sleeptap.SleepTapApp
import com.sleeptap.data.InteractionEvent
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

class SleepTapAccessibilityService : AccessibilityService() {

    companion object {
        var instance: SleepTapAccessibilityService? = null
    }

    private val eventBuffer = ConcurrentLinkedQueue<InteractionEvent>()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentEpisodeId = -1L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val app = application as? SleepTapApp ?: return
        val db = app.db
        serviceScope.launch {
            while (isActive) {
                delay(30_000L)
                flushBuffer(db)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val validTypes = listOf(
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        )
        if (event.eventType in validTypes && currentEpisodeId != -1L) {
            eventBuffer.add(
                InteractionEvent(
                    timestamp = System.currentTimeMillis(),
                    eventType = eventTypeToString(event.eventType),
                    screenOnEpisodeId = currentEpisodeId
                )
            )
        }
    }

    override fun onInterrupt() {}

    private suspend fun flushBuffer(db: com.sleeptap.data.AppDatabase) {
        val events = mutableListOf<InteractionEvent>()
        while (eventBuffer.isNotEmpty()) {
            eventBuffer.poll()?.let { events.add(it) }
        }
        if (events.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                db.eventDao().insertAll(events)
            }
        }
    }

    fun startNewEpisode(episodeId: Long) {
        currentEpisodeId = episodeId
    }

    private fun eventTypeToString(type: Int): String = when (type) {
        AccessibilityEvent.TYPE_VIEW_CLICKED -> "click"
        AccessibilityEvent.TYPE_VIEW_SCROLLED -> "scroll"
        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "text"
        else -> "other"
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
    }
}
