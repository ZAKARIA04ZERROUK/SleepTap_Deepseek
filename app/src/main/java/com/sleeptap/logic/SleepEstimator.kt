package com.sleeptap.logic

import android.content.Context
import com.sleeptap.data.*
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Locale

class SleepEstimator(private val context: Context) {
    private val db = (context.applicationContext as SleepTapApp).db
    private val dao = db.eventDao()
    private val thresholdMinutes = 30L  // configurable later

    suspend fun analyzeRecent() {
        withContext(Dispatchers.IO) {
            val rawEpisodes = dao.getEpisodesRaw()
            if (rawEpisodes.size < 2) return@withContext

            var bestSleepOnset = 0L
            var bestWake = 0L
            var bestConfidence = 0f

            for (i in 1 until rawEpisodes.size) {
                val prev = rawEpisodes[i-1]
                val curr = rawEpisodes[i]
                val gap = curr.firstInteraction - prev.lastInteraction
                if (gap > TimeUnit.MINUTES.toMillis(thresholdMinutes)) {
                    // avoid accidental wake: episode must be meaningful
                    if (curr.count >= 3 || (curr.lastInteraction - curr.firstInteraction) > TimeUnit.MINUTES.toMillis(2)) {
                        val conf = calculateConfidence(gap, prev, curr)
                        if (conf > bestConfidence) {
                            bestConfidence = conf
                            bestSleepOnset = prev.lastInteraction
                            bestWake = curr.firstInteraction
                        }
                    }
                }
            }

            if (bestSleepOnset > 0 && bestWake > 0) {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(bestSleepOnset))
                if (dao.getSleepPeriodForDate(date) == null) {
                    dao.insertSleepPeriod(
                        SleepPeriod(
                            sleepOnset = bestSleepOnset,
                            wakeTime = bestWake,
                            confidence = bestConfidence,
                            date = date
                        )
                    )
                }
            }
        }
    }

    private fun calculateConfidence(gapMs: Long, prev: EpisodeRaw, curr: EpisodeRaw): Float {
        val hours = gapMs / 3_600_000f
        var conf = (hours / 8f).coerceIn(0f, 1f) * 0.7f
        if (prev.count > 3) conf += 0.2f
        if (curr.count > 5) conf += 0.1f
        return conf.coerceIn(0f, 1f)
    }
}
