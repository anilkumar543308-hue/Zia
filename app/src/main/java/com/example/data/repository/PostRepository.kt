package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.BuildConfig
import com.example.data.api.AiServices
import com.example.data.api.FacebookGraphService
import com.example.data.api.FacebookPageInfo
import com.example.data.api.FacebookPostResult
import com.example.data.local.FacebookPageDao
import com.example.data.local.FacebookPageEntity
import com.example.data.local.PostDatabase
import com.example.data.local.PostHistoryDao
import com.example.data.local.PostHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PostRepository(
    private val context: Context,
    private val pageDao: FacebookPageDao,
    private val historyDao: PostHistoryDao,
    private val aiServices: AiServices = AiServices(),
    private val facebookGraphService: FacebookGraphService = FacebookGraphService()
) {
    val allPages: Flow<List<FacebookPageEntity>> = pageDao.getAllPages()
    val allPostHistory: Flow<List<PostHistoryEntity>> = historyDao.getAllPosts()

    suspend fun initializeDefaultPagesIfNeeded() = withContext(Dispatchers.IO) {
        val defaultPage = pageDao.getDefaultPage()
        if (defaultPage == null) {
            val envToken = try { BuildConfig.FACEBOOK_ACCESS_TOKEN } catch (e: Throwable) { "" }
            val envPageId = try { BuildConfig.FACEBOOK_PAGE_ID } catch (e: Throwable) { "" }

            val initialToken = if (envToken.isNotBlank() && envToken != "MY_FACEBOOK_PAGE_ACCESS_TOKEN") envToken else ""
            val initialPageId = if (envPageId.isNotBlank() && envPageId != "MY_FACEBOOK_PAGE_ID") envPageId else "10492817263541"

            val demoPage = FacebookPageEntity(
                pageName = "My Official Facebook Page",
                pageId = initialPageId,
                pageAccessToken = initialToken,
                isDefault = true
            )
            pageDao.insertPage(demoPage)
        }
    }

    suspend fun savePage(page: FacebookPageEntity): Long = withContext(Dispatchers.IO) {
        if (page.isDefault) {
            pageDao.clearDefaultPages()
        }
        pageDao.insertPage(page)
    }

    suspend fun setDefaultPage(pageId: Long) = withContext(Dispatchers.IO) {
        pageDao.clearDefaultPages()
        pageDao.setDefaultPage(pageId)
    }

    suspend fun deletePage(page: FacebookPageEntity) = withContext(Dispatchers.IO) {
        pageDao.deletePage(page)
    }

    suspend fun verifyFacebookToken(pageId: String, accessToken: String): Result<FacebookPageInfo> {
        return facebookGraphService.verifyPageToken(pageId, accessToken)
    }

    suspend fun generateCaption(
        masterPrompt: String,
        topic: String,
        primaryLang: String = "English",
        secondaryLang: String = "Spanish"
    ): Result<String> {
        return aiServices.generateBilingualCaption(masterPrompt, topic, primaryLang, secondaryLang)
    }

    suspend fun generateImage(
        topic: String,
        visualStylePrompt: String
    ): Result<Pair<Bitmap, String>> {
        return aiServices.generateImageWithImagen3(topic, visualStylePrompt)
    }

    suspend fun publishToFacebook(
        pageId: String,
        accessToken: String,
        caption: String,
        imageBitmap: Bitmap
    ): Result<FacebookPostResult> {
        return facebookGraphService.publishPhotoToPage(pageId, accessToken, caption, imageBitmap)
    }

    suspend fun savePostHistory(
        pageId: String,
        pageName: String,
        topic: String,
        masterPrompt: String,
        imageBitmap: Bitmap?,
        caption: String,
        status: String,
        facebookPostId: String? = null,
        errorMessage: String? = null
    ): Long = withContext(Dispatchers.IO) {
        var filePath: String? = null
        if (imageBitmap != null) {
            try {
                val filename = "post_${System.currentTimeMillis()}.jpg"
                val file = File(context.filesDir, filename)
                FileOutputStream(file).use { out ->
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                filePath = file.absolutePath
            } catch (e: Exception) {
                // ignore image save error
            }
        }

        val entity = PostHistoryEntity(
            pageId = pageId,
            pageName = pageName,
            topic = topic,
            masterPrompt = masterPrompt,
            imageFilePath = filePath,
            caption = caption,
            status = status,
            facebookPostId = facebookPostId,
            errorMessage = errorMessage
        )
        historyDao.insertPost(entity)
    }

    suspend fun deleteHistory(id: Long) = withContext(Dispatchers.IO) {
        historyDao.deletePostById(id)
    }

    companion object {
        fun create(context: Context): PostRepository {
            val db = PostDatabase.getDatabase(context)
            return PostRepository(
                context = context,
                pageDao = db.facebookPageDao(),
                historyDao = db.postHistoryDao()
            )
        }
    }
}
