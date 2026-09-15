package com.example.data.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
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

data class GenerationResult(
    val caption: String,
    val imageBitmap: Bitmap?,
    val imageBase64: String?,
    val error: String? = null
)

class AiServices(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Generates a bilingual caption with trending hashtags using Gemini 3.5 Flash.
     */
    suspend fun generateBilingualCaption(
        masterPrompt: String,
        topic: String,
        primaryLang: String = "English",
        secondaryLang: String = "Spanish"
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please set your GEMINI_API_KEY in the Secrets panel.")
            )
        }

        val promptText = buildString {
            appendLine("You are an expert social media manager crafting viral, highly engaging Facebook Page content.")
            appendLine("Master Guidelines/Brand Identity:")
            appendLine(masterPrompt.ifBlank { "Professional, authentic, value-packed, and engaging brand voice." })
            appendLine()
            appendLine("Today's Topic: $topic")
            appendLine()
            appendLine("Requirements:")
            appendLine("1. Craft a high-impact Facebook post in TWO languages: primary ($primaryLang) and secondary ($secondaryLang).")
            appendLine("2. Include an attention-grabbing hook/headline.")
            appendLine("3. Provide concise, compelling body copy in $primaryLang, followed by the exact equivalent in $secondaryLang separated by a clean aesthetic divider line (e.g. ───────────────).")
            appendLine("4. Include a clear call-to-action (e.g., share your thoughts, drop a reaction).")
            appendLine("5. Include 5-8 relevant, trending hashtags (mix of topic-specific and high-volume tags).")
            appendLine("6. Do NOT include markdown code blocks or metadata notes; output the ready-to-publish Facebook caption directly.")
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", promptText) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.75)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody)
                return@withContext Result.failure(Exception("Gemini API error (${response.code}): $errorMsg"))
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (text.isNullOrBlank()) {
                Result.failure(Exception("No caption text returned by Gemini API."))
            } else {
                Result.success(text.trim())
            }
        } catch (e: Exception) {
            Log.e("AiServices", "Caption generation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Generates an image using Imagen 3 API (model: imagen-3.0-generate-002).
     * Falls back to Gemini 2.5 Flash Image if predict endpoint is unavailable.
     */
    suspend fun generateImageWithImagen3(
        topic: String,
        visualStylePrompt: String
    ): Result<Pair<Bitmap, String>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please set your GEMINI_API_KEY in the Secrets panel.")
            )
        }

        val enrichedPrompt = buildString {
            append("A high-impact, professional, photorealistic social media visual about: $topic. ")
            if (visualStylePrompt.isNotBlank()) {
                append("Visual guidelines: $visualStylePrompt. ")
            }
            append("Ultra high resolution, 8k, cinematic lighting, aesthetically pleasing composition suitable for Facebook feed.")
        }

        // 1. Try Imagen 3 predict endpoint
        val imagenResult = callImagen3Predict(apiKey, enrichedPrompt)
        if (imagenResult.isSuccess) {
            return@withContext imagenResult
        }

        Log.w("AiServices", "Imagen 3 predict call failed: ${imagenResult.exceptionOrNull()?.message}. Attempting gemini-2.5-flash-image fallback...")

        // 2. Fallback to Gemini 2.5 Flash Image (supports generateContent with IMAGE modality)
        callGeminiFlashImage(apiKey, enrichedPrompt)
    }

    private fun callImagen3Predict(apiKey: String, prompt: String): Result<Pair<Bitmap, String>> {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/imagen-3.0-generate-002:predict?key=$apiKey"
            val requestJson = JSONObject().apply {
                val instances = JSONArray().apply {
                    put(JSONObject().apply { put("prompt", prompt) })
                }
                put("instances", instances)
                put("parameters", JSONObject().apply {
                    put("sampleCount", 1)
                    put("aspectRatio", "1:1")
                    put("outputOptions", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody)
                return Result.failure(Exception("Imagen 3 error (${response.code}): $errorMsg"))
            }

            val json = JSONObject(responseBody)
            val predictions = json.optJSONArray("predictions")
            val firstPrediction = predictions?.optJSONObject(0)
            val base64 = firstPrediction?.optString("bytesBase64Encoded")

            if (base64.isNullOrBlank()) {
                return Result.failure(Exception("Imagen 3 response did not contain image data."))
            }

            val imageBytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: return Result.failure(Exception("Failed to decode image bitmap from Imagen 3."))

            return Result.success(Pair(bitmap, base64))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun callGeminiFlashImage(apiKey: String, prompt: String): Result<Pair<Bitmap, String>> {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    val modalities = JSONArray().apply {
                        put("IMAGE")
                    }
                    put("responseModalities", modalities)
                    put("imageConfig", JSONObject().apply {
                        put("aspectRatio", "1:1")
                        put("imageSize", "1K")
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody)
                return Result.failure(Exception("Image generation error (${response.code}): $errorMsg"))
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i)
                    val inlineData = part?.optJSONObject("inlineData")
                    val base64 = inlineData?.optString("data")
                    if (!base64.isNullOrBlank()) {
                        val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        if (bitmap != null) {
                            return Result.success(Pair(bitmap, base64))
                        }
                    }
                }
            }

            return Result.failure(Exception("No image bytes returned in fallback model response."))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun parseErrorMessage(jsonString: String): String {
        return try {
            val json = JSONObject(jsonString)
            val error = json.optJSONObject("error")
            error?.optString("message") ?: jsonString.take(200)
        } catch (e: Exception) {
            jsonString.take(200)
        }
    }
}
