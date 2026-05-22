package com.sleeptap.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<InteractionEvent>)

    @Insert
    suspend fun insertScreenOffMarker(marker: InteractionEvent)

    @Insert
    suspend fun insertSleepPeriod(period: SleepPeriod)

    @Query("SELECT * FROM SleepPeriod WHERE date = :date LIMIT 1")
    suspend fun getSleepPeriodForDate(date: String): SleepPeriod?

    @Query("SELECT * FROM SleepPeriod ORDER BY sleepOnset DESC")
    fun getAllSleepPeriods(): Flow<List<SleepPeriod>>

    // For analysis we need a helper: return episodes with start/end times and event count
    @Query("""
        SELECT screenOnEpisodeId,
               MIN(timestamp) AS firstInteraction,
               MAX(timestamp) AS lastInteraction,
               COUNT(*) AS count
        FROM InteractionEvent
        GROUP BY screenOnEpisodeId
        ORDER BY firstInteraction
    """)
    suspend fun getEpisodesRaw(): List<EpisodeRaw>
}

data class EpisodeRaw(
    val screenOnEpisodeId: Long,
    val firstInteraction: Long,
    val lastInteraction: Long,
    val count: Int
)
