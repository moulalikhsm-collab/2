package com.example.data.db

import com.example.data.api.GeminiClient
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class EcoRepository(private val database: AppDatabase) {

    private val plantDao = database.plantDao()
    private val scanDao = database.scanDao()
    private val chatDao = database.chatDao()
    private val waterReminderDao = database.waterReminderDao()

    // --- Plants ---
    val allPlantsFlow: Flow<List<PlantEntity>> = plantDao.getAllPlantsFlow()

    suspend fun insertPlant(plant: PlantEntity): Long {
        return plantDao.insertPlant(plant)
    }

    suspend fun updatePlant(plant: PlantEntity) {
        plantDao.updatePlant(plant)
    }

    suspend fun updatePlantWatered(plantId: Long, timestamp: Long) {
        plantDao.updateLastWatered(plantId, timestamp)
    }

    suspend fun updatePlantHealthScore(plantId: Long, score: Int) {
        plantDao.updateHealthScore(plantId, score)
    }

    suspend fun deletePlant(plant: PlantEntity) {
        plantDao.deletePlant(plant)
        waterReminderDao.deleteRemindersForPlant(plant.id)
    }

    // --- Scans (Disease Detection) ---
    val allScansFlow: Flow<List<ScanEntity>> = scanDao.getAllScansFlow()

    suspend fun saveScan(scan: ScanEntity): Long {
        return scanDao.insertScan(scan)
    }

    suspend fun deleteScan(scan: ScanEntity) {
        scanDao.deleteScan(scan)
    }

    // --- Chat Messages ---
    val chatMessagesFlow: Flow<List<ChatMessageEntity>> = chatDao.getChatMessagesFlow()

    suspend fun saveMessage(role: String, content: String): Long {
        return chatDao.insertMessage(
            ChatMessageEntity(
                role = role,
                content = content,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearChatHistory() {
        chatDao.clearHistory()
    }

    // --- Water Reminders ---
    val allRemindersFlow: Flow<List<WaterReminderEntity>> = waterReminderDao.getAllRemindersFlow()

    suspend fun insertReminder(reminder: WaterReminderEntity): Long {
        return waterReminderDao.insertReminder(reminder)
    }

    suspend fun updateReminderStatus(reminderId: Long, completed: Boolean) {
        waterReminderDao.updateReminderStatus(reminderId, completed)
    }

    suspend fun deleteReminder(reminder: WaterReminderEntity) {
        waterReminderDao.deleteReminder(reminder)
    }

    // --- Gemini Callers ---

    suspend fun getPlantRecommendation(
        location: String,
        space: String,
        climate: String,
        experience: String
    ): String {
        val prompt = """
            I am looking for suitable plant recommendations for my home garden.
            Here is my setup:
            - Location: $location
            - Available Space: $space
            - Climate Condition: $climate
            - Gardening Experience Level: $experience
            
            Based on these details, recommend 3-4 suitable plants, vegetables, or herbs. 
            For each recommended plant, provide:
            1. Plant Name
            2. High-level description
            3. Detailed Care Instructions (watering, light, soil)
            4. Expected Growth Timeline (e.g., germinates in X days, harvest in Y days)
            5. Key Growth Prediction Tips for my setup.
            
            Please provide the response in a beautiful, structured format. Use clean markdown titles and bullet points. Make the instructions highly practical and friendly.
        """.trimIndent()

        val systemInstruction = """
            You are EcoFriend Plant Advisor, a premium gardening consultant. 
            Provide inspiring, clear, well-structured planting suggestions tailored perfectly to the user's setup, climate, and experience level.
        """.trimIndent()

        return GeminiClient.generateText(prompt, systemInstruction)
    }

    suspend fun getChatResponse(message: String, chatHistory: List<ChatMessageEntity>): String {
        // Build conversational context
        val contextBuilder = StringBuilder()
        contextBuilder.append("Here is the chat history between the User and you (EcoFriend Chatbot):\n\n")
        
        chatHistory.takeLast(10).forEach { msg ->
            val speaker = if (msg.role == "user") "User" else "EcoFriend"
            contextBuilder.append("$speaker: ${msg.content}\n\n")
        }
        
        contextBuilder.append("User's latest query: $message\n")
        contextBuilder.append("Respond as EcoFriend, a warm, professional, highly knowledgeable AI gardening assistant. Provide helpful, conversational guidance, caring advice, and precise answers about plant care, watering, diagnostics, soils, tools, or smart plantation. Be supportive and modern.")

        val systemInstruction = """
            You are EcoFriend Gardening AI Chatbot. You are a conversational, supportive, and friendly gardening expert. 
            Help users solve complex gardening problems, and keep responses engaging, concise, and structured.
        """.trimIndent()

        return GeminiClient.generateText(contextBuilder.toString(), systemInstruction)
    }

    suspend fun analyzePlantDisease(
        base64Image: String,
        presetName: String? = null
    ): String {
        val prompt = if (presetName != null) {
            """
                Analyze this leaf belonging to a $presetName plant. 
                Identify if there are any signs of diseases, deficiency, or pest damage.
                Please structure your response rigidly as a valid JSON object because it will be parsed directly by an Android app. Return the following fields:
                {
                  "plantName": "$presetName",
                  "diseaseName": "Name of disease detected (or 'Healthy' if in perfect condition)",
                  "confidence": 0.95, // float between 0.0 and 1.0 representing confidence
                  "treatment": "Bullet point checklist of dynamic actions to take to treat or protect the plant",
                  "notes": "Short, beautiful overall diagnostic summary"
                }
                Do not include any extra markdown formatting like ```json or ``` outside of the JSON block itself. Output ONLY the raw JSON string.
            """.trimIndent()
        } else {
            """
                Identify the plant species from this leaf image.
                Identify if there are any signs of diseases, deficiency, or pest damage.
                Please structure your response rigidly as a valid JSON object because it will be parsed directly by an Android app. Return the following fields:
                {
                  "plantName": "Identified Plant Name",
                  "diseaseName": "Name of disease detected (or 'Healthy' if in perfect condition)",
                  "confidence": 0.88, // float between 0.0 and 1.0 representing confidence
                  "treatment": "Bullet point checklist of dynamic actions to take to treat or protect the plant",
                  "notes": "Short, beautiful overall diagnostic summary"
                }
                Do not include any extra markdown formatting like ```json or ``` outside of the JSON block itself. Output ONLY the raw JSON string.
            """.trimIndent()
        }

        val rawResponse = GeminiClient.analyzeImageAndText(prompt, base64Image)
        return rawResponse
    }
}
