package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Generates text response using Gemini 3.5 Flash API
     */
    suspend fun generateText(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: API Key is not set. Please set GEMINI_API_KEY in the AI Studio Secrets panel."
        }

        try {
            // Build request JSON
            val requestJson = JSONObject()
            
            // Contents list
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            contentObj.put("role", "user")
            
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // System Instruction
            if (!systemInstruction.isNullOrBlank()) {
                val sysInstObj = JSONObject()
                val sysPartsArray = JSONArray()
                sysPartsArray.put(JSONObject().put("text", systemInstruction))
                sysInstObj.put("parts", sysPartsArray)
                requestJson.put("systemInstruction", sysInstObj)
            }

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            requestJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (response.isSuccessful && !bodyStr.isNullOrEmpty()) {
                    val responseJson = JSONObject(bodyStr)
                    val textResult = parseGeminiResponseText(responseJson)
                    return@withContext textResult ?: "No response content received."
                } else {
                    Log.e(TAG, "Request failed: code=${response.code} error=$bodyStr")
                    return@withContext "Error details: HTTP ${response.code} - ${bodyStr ?: "Empty Response"}"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in generateText", e)
            return@withContext "Error calling AI Assistant: ${e.message}"
        }
    }

    /**
     * Generates multimodal response based on base64 image and text
     */
    suspend fun analyzeImageAndText(
        prompt: String,
        base64Image: String,
        mimeType: String = "image/jpeg"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: API Key is not set. Please set GEMINI_API_KEY in the AI Studio Secrets panel."
        }

        try {
            val requestJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // Page 1: Text Prompt
            val textPart = JSONObject()
            textPart.put("text", prompt)
            partsArray.put(textPart)

            // Page 2: Inline Image Data
            val imagePart = JSONObject()
            val inlineDataObj = JSONObject()
            inlineDataObj.put("mimeType", mimeType)
            inlineDataObj.put("data", base64Image)
            imagePart.put("inlineData", inlineDataObj)
            partsArray.put(imagePart)

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // System instructions
            val sysInstObj = JSONObject()
            val sysPartsArray = JSONArray()
            sysPartsArray.put(JSONObject().put("text", "You are an expert plant pathologist and botanist. Provide detailed plant diagnostics, health score, confidence, and highly practical care recommendations based on the plant leaf photo."))
            sysInstObj.put("parts", sysPartsArray)
            requestJson.put("systemInstruction", sysInstObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (response.isSuccessful && !bodyStr.isNullOrEmpty()) {
                    val responseJson = JSONObject(bodyStr)
                    return@withContext parseGeminiResponseText(responseJson) ?: "Analysis completed, but no text output was received."
                } else {
                    Log.e(TAG, "Image scan failed: code=${response.code} error=$bodyStr")
                    return@withContext "Scan failed: HTTP ${response.code} - ${bodyStr ?: "Empty Response"}"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in analyzeImageAndText", e)
            return@withContext "Scan diagnostic error: ${e.message}"
        }
    }

    private fun parseGeminiResponseText(responseJson: JSONObject): String? {
        try {
            val candidates = responseJson.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            return parts.getJSONObject(0).optString("text")
        } catch (e: Exception) {
            Log.e(TAG, "Parsing exception", e)
            return null
        }
    }
}
