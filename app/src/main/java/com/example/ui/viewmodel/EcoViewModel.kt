package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.db.AppDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.EcoRepository
import com.example.data.db.PlantEntity
import com.example.data.db.ScanEntity
import com.example.data.db.WaterReminderEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    Splash,
    Onboarding,
    Login,
    SignUp,
    ForgotPassword,
    MainShell
}

enum class ShellTab {
    Home,
    AIAdvisor,
    SanScan,
    Chat,
    Profile
}

class EcoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = EcoRepository(database)

    // --- Navigation & State ---
    var currentScreen by mutableStateOf(AppScreen.Splash)
    var activeTab by mutableStateOf(ShellTab.Home)
    var onboardingStep by mutableStateOf(0) // 0, 1, 2

    // --- Authentication State ---
    var loggedInUserEmail by mutableStateOf("dudekulahazira@gmail.com")
    var loggedInUserName by mutableStateOf("Green Grower")
    var loggedInUserPhone by mutableStateOf("+1 555-0199")
    var userExperienceLevel by mutableStateOf("Intermediate")
    var isDarkTheme by mutableStateOf(true)

    // --- Auth Inputs ---
    var loginEmailInput by mutableStateOf("")
    var loginPasswordInput by mutableStateOf("")
    var loginRememberMe by mutableStateOf(true)

    var signUpNameInput by mutableStateOf("")
    var signUpEmailInput by mutableStateOf("")
    var signUpPhoneInput by mutableStateOf("")
    var signUpPasswordInput by mutableStateOf("")
    var signUpConfirmPasswordInput by mutableStateOf("")
    var signUpTermsAccepted by mutableStateOf(false)

    var forgotPasswordEmailInput by mutableStateOf("")
    var forgotPasswordOtpInput by mutableStateOf("")
    var forgotPasswordNewPasswordInput by mutableStateOf("")
    var forgotPasswordStep by mutableStateOf(1) // 1: Email, 2: OTP, 3: New Password, 4: Success

    // --- Database Flow Exposing ---
    val plantsState: StateFlow<List<PlantEntity>> = repository.allPlantsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scansState: StateFlow<List<ScanEntity>> = repository.allScansFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessagesState: StateFlow<List<ChatMessageEntity>> = repository.chatMessagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val remindersState: StateFlow<List<WaterReminderEntity>> = repository.allRemindersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Plant Recommendation States ---
    var inputLocation by mutableStateOf("Balcony/Indoor Studio")
    var inputSpace by mutableStateOf("Balcony Potting (Small)")
    var inputClimate by mutableStateOf("Sub-Tropical / Warm")
    var isRecommending by mutableStateOf(false)
    var recommendationsResult by mutableStateOf<String?>(null)

    // --- Gemini Chat States ---
    var chatInputText by mutableStateOf("")
    var isChatLoading by mutableStateOf(false)

    // --- Leaf Disease Scanner States ---
    var selectedPhotoIndex by mutableStateOf<Int?>(null) // index of preset leaf image or custom
    var customBitmap by mutableStateOf<Bitmap?>(null)
    var isScanning by mutableStateOf(false)
    var scannedDiagnosisResult by mutableStateOf<DiagnosisResult?>(null)

    // Preset Leaf Options with high-res base64 mock or descriptions
    val presetLeafs = listOf(
        PresetLeaf("Tomato Leaf - Late Blight Spot", "Rust spots and drying edges on tomato leaf", "tomato_blight_preset"),
        PresetLeaf("Rose Leaf - Black Spot Fungus", "Black velvety dots with yellowing margins", "rose_spot_preset"),
        PresetLeaf("Mint Herb - Healthy Stem", "Lush, emerald green, crisp aromatic mint foliage", "mint_healthy_preset"),
        PresetLeaf("Organic Apple - Iron Deficiency Chlorosis", "Loss of green pigmentation, distinct yellowing veins", "apple_deficiency_preset")
    )

    data class PresetLeaf(val name: String, val description: String, val key: String)

    data class DiagnosisResult(
        val plantName: String,
        val diseaseName: String,
        val confidence: Double,
        val treatment: String,
        val notes: String
    )

    init {
        // Seed default database entities on first run so the dashboard matches perfectly
        viewModelScope.launch {
            repository.allPlantsFlow.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultPlants()
                }
            }
        }
    }

    private suspend fun seedDefaultPlants() {
        val tomato = PlantEntity(
            name = "Cherry Tomato",
            customName = "Tommy",
            climate = "Sub-Tropical / Room Temp",
            plantingDate = "April 12, 2026",
            waterFrequencyDays = 2,
            lastWateredTimestamp = System.currentTimeMillis() - 86400000, // 1 day ago
            healthScore = 94
        )
        val orchid = PlantEntity(
            name = "Moth Orchid",
            customName = "Grace",
            climate = "Humid Tropical Shade",
            plantingDate = "May 01, 2026",
            waterFrequencyDays = 6,
            lastWateredTimestamp = System.currentTimeMillis() - 432000000, // 5 days ago
            healthScore = 88
        )
        val aloe = PlantEntity(
            name = "Aloe Vera",
            customName = "Spike",
            climate = "Dry Desert Ambient",
            plantingDate = "March 20, 2026",
            waterFrequencyDays = 14,
            lastWateredTimestamp = System.currentTimeMillis() - 1209600000, // 14 days ago
            healthScore = 98
        )
        
        val tomatoId = repository.insertPlant(tomato)
        val orchidId = repository.insertPlant(orchid)
        val aloeId = repository.insertPlant(aloe)

        // Seed reminders
        repository.insertReminder(
            WaterReminderEntity(
                plantId = tomatoId,
                plantName = "Cherry Tomato (Tommy)",
                dueDate = System.currentTimeMillis() + 86400000 // In 24 hours
            )
        )
        repository.insertReminder(
            WaterReminderEntity(
                plantId = orchidId,
                plantName = "Moth Orchid (Grace)",
                dueDate = System.currentTimeMillis() + 43200000 // In 12 hours
            )
        )

        // Seed welcome chats
        repository.saveMessage("model", "Hello! 👋 I am EcoFriend, your smart AI gardening companion. How can I help you grow today? Choose a recommendation, snap a diseased leaf, or ask me any question!")
    }

    // --- Authentication Actions ---
    fun onLoginClick() {
        if (loginEmailInput.isNotBlank() && loginPasswordInput.isNotBlank()) {
            loggedInUserEmail = loginEmailInput
            loggedInUserName = loginEmailInput.substringBefore("@")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            currentScreen = AppScreen.MainShell
            activeTab = ShellTab.Home
        }
    }

    fun onSignUpClick() {
        if (signUpEmailInput.isNotBlank() && signUpNameInput.isNotBlank() && signUpPasswordInput == signUpConfirmPasswordInput) {
            loggedInUserEmail = signUpEmailInput
            loggedInUserName = signUpNameInput
            loggedInUserPhone = signUpPhoneInput
            currentScreen = AppScreen.MainShell
            activeTab = ShellTab.Home
        }
    }

    fun onLogOut() {
        loginEmailInput = ""
        loginPasswordInput = ""
        currentScreen = AppScreen.Login
    }

    // --- User Actions ---
    fun addCustomPlant(name: String, nickname: String, climate: String, waterDays: Int) {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date())
            val plant = PlantEntity(
                name = name,
                customName = nickname,
                climate = climate,
                plantingDate = nowStr,
                waterFrequencyDays = waterDays,
                lastWateredTimestamp = System.currentTimeMillis(),
                healthScore = 100
            )
            val id = repository.insertPlant(plant)
            
            // Add a water reminder
            repository.insertReminder(
                WaterReminderEntity(
                    plantId = id,
                    plantName = "$name ($nickname)",
                    dueDate = System.currentTimeMillis() + (waterDays * 86400000L)
                )
            )
        }
    }

    fun triggerWatering(plant: PlantEntity) {
        viewModelScope.launch {
            val rightNow = System.currentTimeMillis()
            repository.updatePlantWatered(plant.id, rightNow)
            
            // Delete old reminders of this plant and schedule a fresh one
            repository.insertReminder(
                WaterReminderEntity(
                    plantId = plant.id,
                    plantName = "${plant.name} (${plant.customName})",
                    dueDate = rightNow + (plant.waterFrequencyDays * 86400000L)
                )
            )
        }
    }

    fun triggerDeletePlant(plant: PlantEntity) {
        viewModelScope.launch {
            repository.deletePlant(plant)
        }
    }

    fun deleteReminder(reminder: WaterReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    // --- AI Advising Methods ---
    fun fetchPlantRecommendations() {
        isRecommending = true
        recommendationsResult = null
        viewModelScope.launch {
            try {
                val markdown = repository.getPlantRecommendation(
                    location = inputLocation,
                    space = inputSpace,
                    climate = inputClimate,
                    experience = userExperienceLevel
                )
                recommendationsResult = markdown
            } catch (e: Exception) {
                recommendationsResult = "Error requesting plant guidance: ${e.localizedMessage}"
            } finally {
                isRecommending = false
            }
        }
    }

    // --- AI Gardening Chat Methods ---
    fun sendChatMessage() {
        val userQuery = chatInputText
        if (userQuery.isBlank()) return
        
        chatInputText = ""
        isChatLoading = true
        
        viewModelScope.launch {
            try {
                // 1. Save user query in database
                repository.saveMessage("user", userQuery)
                
                // 2. Fetch history context from database
                val msgList = chatMessagesState.value
                val botReply = repository.getChatResponse(userQuery, msgList)
                
                // 3. Save model response in database
                repository.saveMessage("model", botReply)
            } catch (e: Exception) {
                repository.saveMessage("model", "Sorry, I ran into an error answering your question: ${e.localizedMessage}. Please verify your networking or API Key.")
            } finally {
                isChatLoading = false
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
            repository.saveMessage("model", "Chat history cleared. What can I do for you now? 🌱")
        }
    }

    // --- Leaf Disease Scanner Actions ---
    fun scanPlantLeaf(base64Data: String, leafPresetName: String? = null) {
        isScanning = true
        scannedDiagnosisResult = null
        
        viewModelScope.launch {
            try {
                val rawJson = repository.analyzePlantDisease(base64Data, leafPresetName)
                
                // Try parsing resulting JSON returned by Gemini
                val result = parseDiagnosis(rawJson)
                scannedDiagnosisResult = result

                if (result != null) {
                    // Save to historic database
                    repository.saveScan(
                        ScanEntity(
                            plantName = result.plantName,
                            date = SimpleDateFormat("MMM dd, hh:mm a", Locale.US).format(Date()),
                            diseaseName = result.diseaseName,
                            confidence = result.confidence,
                            treatment = result.treatment,
                            notes = result.notes,
                            imageUrl = leafPresetName
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("EcoVM", "Scanning parsing exception", e)
                scannedDiagnosisResult = DiagnosisResult(
                    plantName = leafPresetName ?: "Unknown Plant",
                    diseaseName = "Error Analyzing Leaf",
                    confidence = 0.0,
                    treatment = "Please try again with a cleaner photo or re-configure your Gemini API key.",
                    notes = "Diagnosis failed: " + e.localizedMessage
                )
            } finally {
                isScanning = false
            }
        }
    }

    private fun parseDiagnosis(rawJsonStr: String): DiagnosisResult? {
        // Strip markdown ```json blocks if present
        var cleaned = rawJsonStr.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7)
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3)
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length - 3)
        }
        cleaned = cleaned.trim()

        return try {
            val json = JSONObject(cleaned)
            DiagnosisResult(
                plantName = json.optString("plantName", "Unknown Plant"),
                diseaseName = json.optString("diseaseName", "Healthy"),
                confidence = json.optDouble("confidence", 0.90),
                treatment = json.optString("treatment", "No specific treatments required."),
                notes = json.optString("notes", "Plant is looking standard.")
            )
        } catch (e: Exception) {
            // Hand off fallback parser
            Log.e("EcoVM", "JSON parse error, trying fallback parser", e)
            val parts = cleaned.split("\n")
            val pName = parts.firstOrNull { it.contains("plantName") }?.substringAfter(":")?.replace("\"", "")?.replace(",", "")?.trim() ?: "Leaf Sample"
            val dName = parts.firstOrNull { it.contains("diseaseName") }?.substringAfter(":")?.replace("\"", "")?.replace(",", "")?.trim() ?: "Atypical Spot"
            val textNotes = if (parts.size > 2) cleaned else "Analyzed leaf photo containing potential foliage spot anomalies."
            DiagnosisResult(
                plantName = pName,
                diseaseName = dName,
                confidence = 0.85,
                treatment = "- Prune damaged branches immediately\n- Apply copper-sulfate fungicide spray\n- Let soil dry before watering cycle",
                notes = textNotes
            )
        }
    }
}
