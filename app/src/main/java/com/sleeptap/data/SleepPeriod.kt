package com.sleeptap.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SleepPeriod(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sleepOnset: Long,
    val wakeTime: Long,
    val confidence: Float,
    val date: String
)
