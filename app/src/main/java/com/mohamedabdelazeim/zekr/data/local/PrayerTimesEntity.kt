package com.mohamedabdelazeim.zekr.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_times")
data class PrayerTimesEntity(
    @PrimaryKey
    val date: Long, // timestamp ليوم الصلاة
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val hijriDate: String,
    val gregorianDate: String
)
