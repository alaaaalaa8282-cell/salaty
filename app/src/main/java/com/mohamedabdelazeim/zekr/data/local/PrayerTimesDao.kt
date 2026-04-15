package com.mohamedabdelazeim.zekr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimesDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTimes(prayerTimes: PrayerTimesEntity)
    
    @Query("SELECT * FROM prayer_times WHERE date = :date")
    suspend fun getPrayerTimesByDate(date: Long): PrayerTimesEntity?
    
    @Query("SELECT * FROM prayer_times ORDER BY date DESC LIMIT 1")
    suspend fun getLatestPrayerTimes(): PrayerTimesEntity?
    
    @Query("SELECT * FROM prayer_times ORDER BY date DESC")
    fun getAllPrayerTimes(): Flow<List<PrayerTimesEntity>>
    
    @Query("DELETE FROM prayer_times WHERE date < :date")
    suspend fun deleteOldPrayerTimes(date: Long)
}
