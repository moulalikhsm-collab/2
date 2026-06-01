package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val customName: String,
    val climate: String,
    val plantingDate: String,
    val waterFrequencyDays: Int,
    val lastWateredTimestamp: Long,
    val healthScore: Int
)

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantName: String,
    val date: String,
    val diseaseName: String,
    val confidence: Double,
    val treatment: String,
    val notes: String,
    val imageUrl: String? = null
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user", "model"
    val content: String,
    val timestamp: Long
)

@Entity(tableName = "water_reminders")
data class WaterReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val plantName: String,
    val dueDate: Long,
    val isCompleted: Boolean = false
)
