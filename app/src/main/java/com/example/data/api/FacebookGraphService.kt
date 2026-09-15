package com.example.data.api

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class FacebookPageInfo(
    val id: String,
    val name: String,
    val pictureUrl: String? = null
)

data class FacebookPostResult(
    val success: Boolean,
    val postId: String? = null,
    val photoId: String? = null,
    val errorMessage: String? = null
)

class FacebookGraphService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    /**
     * Verifies the Facebook Page token and retrieves page details.
     */
    suspend fun verifyPageToken(
        pageId: String,
        accessToken: String
    ): Result<FacebookPageInfo> = withContext(Dispatchers.IO) {
        if (pageId.isBlank() || accessToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Page ID and Access Token must not be blank."))
        }

        try {
            val url = "https://graph.facebook.com/v21.0/$pageId?fields=id,name,picture&access_token=$accessToken"
            val request = Request.Builder().url(url).get().build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = parseGraphError(responseBody)
                return@withContext Result.failure(Exception("Facebook API error (${response.code}): $errorMsg"))
            }

            val json = JSONObject(responseBody)
            val id = json.optString("id")
            val name = json.optString("name", "Facebook Page")
            val picture = json.optJSONObject("picture")?.optJSONObject("data")?.optString("url")

            Result.success(FacebookPageInfo(id = id, name = name, pictureUrl = picture))
        } catch (e: Exception) {
            Log.e("FacebookGraphService", "Verify page token failed", e)
            Result.failure(e)
        }
    }

    /**
     * Publishes a photo with a caption directly to the specified Facebook Page feed.
     */
    suspend fun publishPhotoToPage(
        pageId: String,
        accessToken: String,
        caption: String,
        imageBitmap: Bitmap
    ): Result<FacebookPostResult> = withContext(Dispatchers.IO) {
        if (pageId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please specify a Facebook Page ID."))
        }
        if (accessToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please provide a Page Access Token with 'pages_manage_posts' permission."))
        }

        try {
            val stream = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val byteArray = stream.toByteArray()

            val imageRequestBody = byteArray.toRequestBody("image/jpeg".toMediaType())

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("access_token", accessToken)
                .addFormDataPart("caption", caption)
                .addFormDataPart("source", "photo_${System.currentTimeMillis()}.jpg", imageRequestBody)
                .build()

            val url = "https://graph.facebook.com/v21.0/$pageId/photos"
            val request = Request.Builder()
                .url(url)
                .post(multipartBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = parseGraphError(responseBody)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(responseBody)
            val photoId = json.optString("id")
            val postId = json.optString("post_id").ifEmpty {
                if (photoId.isNotEmpty()) "${pageId}_$photoId" else null
            }

            Result.success(
                FacebookPostResult(
                    success = true,
                    postId = postId,
                    photoId = photoId
                )
            )
        } catch (e: Exception) {
            Log.e("FacebookGraphService", "Publish to Facebook failed", e)
            Result.failure(e)
        }
    }

    private fun parseGraphError(jsonString: String): String {
        return try {
            val json = JSONObject(jsonString)
            val error = json.optJSONObject("error")
            if (error != null) {
                val message = error.optString("message")
                val type = error.optString("type")
                val code = error.optInt("code", -1)
                val userMsg = error.optString("error_user_msg")

                buildString {
                    if (userMsg.isNotEmpty()) append("$userMsg. ")
                    append(message)
                    if (code != -1) append(" (Error code: $code, $type)")
                }
            } else {
                jsonString.take(200)
            }
        } catch (e: Exception) {
            jsonString.take(200)
        }
    }
}
