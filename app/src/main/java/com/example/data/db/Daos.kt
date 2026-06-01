package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY lastWateredTimestamp DESC")
    fun getAllPlantsFlow(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants")
    suspend fun getAllPlantsList(): List<PlantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: PlantEntity): Long

    @Update
    suspend fun updatePlant(plant: PlantEntity)

    @Query("UPDATE plants SET lastWateredTimestamp = :timestamp WHERE id = :plantId")
    suspend fun updateLastWatered(plantId: Long, timestamp: Long)

    @Query("UPDATE plants SET healthScore = :score WHERE id = :plantId")
    suspend fun updateHealthScore(plantId: Long, score: Int)

    @Delete
    suspend fun deletePlant(plant: PlantEntity)
}

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY id DESC")
    fun getAllScansFlow(): Flow<List<ScanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity): Long

    @Delete
    suspend fun deleteScan(scan: ScanEntity)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatMessagesFlow(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

@Dao
interface WaterReminderDao {
    @Query("SELECT * FROM water_reminders ORDER BY dueDate ASC")
    fun getAllRemindersFlow(): Flow<List<WaterReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: WaterReminderEntity): Long

    @Query("UPDATE water_reminders SET isCompleted = :completed WHERE id = :reminderId")
    suspend fun updateReminderStatus(reminderId: Long, completed: Boolean)

    @Query("DELETE FROM water_reminders WHERE plantId = :plantId")
    suspend fun deleteRemindersForPlant(plantId: Long)

    @Delete
    suspend fun deleteReminder(reminder: WaterReminderEntity)
}
