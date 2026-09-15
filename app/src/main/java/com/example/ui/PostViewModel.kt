package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FacebookPageEntity
import com.example.data.local.PostHistoryEntity
import com.example.data.repository.PostRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiNotification(
    val message: String,
    val isError: Boolean = false
)

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository.create(application)

    val pages: StateFlow<List<FacebookPageEntity>> = repository.allPages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<PostHistoryEntity>> = repository.allPostHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Inputs
    val defaultMasterPrompt = "Brand Style: Energetic, authentic, modern tone. Emphasize actionable value, positive mindset, and community engagement. Use vivid imagery concepts."
    private val _masterPrompt = MutableStateFlow(defaultMasterPrompt)
    val masterPrompt: StateFlow<String> = _masterPrompt.asStateFlow()

    private val _todayTopic = MutableStateFlow("")
    val todayTopic: StateFlow<String> = _todayTopic.asStateFlow()

    private val _selectedPage = MutableStateFlow<FacebookPageEntity?>(null)
    val selectedPage: StateFlow<FacebookPageEntity?> = _selectedPage.asStateFlow()

    private val _primaryLang = MutableStateFlow("English")
    val primaryLang: StateFlow<String> = _primaryLang.asStateFlow()

    private val _secondaryLang = MutableStateFlow("Spanish")
    val secondaryLang: StateFlow<String> = _secondaryLang.asStateFlow()

    // Generated post state
    private val _generatedCaption = MutableStateFlow("")
    val generatedCaption: StateFlow<String> = _generatedCaption.asStateFlow()

    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    // Status & Loading
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationStage = MutableStateFlow("")
    val generationStage: StateFlow<String> = _generationStage.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _postedId = MutableStateFlow<String?>(null)
    val postedId: StateFlow<String?> = _postedId.asStateFlow()

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    // UI Dialogs
    private val _showPageDialog = MutableStateFlow(false)
    val showPageDialog: StateFlow<Boolean> = _showPageDialog.asStateFlow()

    private val _showHistoryDialog = MutableStateFlow(false)
    val showHistoryDialog: StateFlow<Boolean> = _showHistoryDialog.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultPagesIfNeeded()
        }

        // Auto-select first/default page when pages list is updated
        viewModelScope.launch {
            pages.collect { list ->
                if (_selectedPage.value == null && list.isNotEmpty()) {
                    _selectedPage.value = list.firstOrNull { it.isDefault } ?: list.first()
                } else if (_selectedPage.value != null && list.isNotEmpty()) {
                    // Update reference if matched
                    val match = list.firstOrNull { it.id == _selectedPage.value?.id }
                    if (match != null) _selectedPage.value = match
                }
            }
        }
    }

    fun updateMasterPrompt(prompt: String) {
        _masterPrompt.value = prompt
    }

    fun updateTodayTopic(topic: String) {
        _todayTopic.value = topic
    }

    fun updateSelectedPage(page: FacebookPageEntity) {
        _selectedPage.value = page
    }

    fun updateLanguages(primary: String, secondary: String) {
        _primaryLang.value = primary
        _secondaryLang.value = secondary
    }

    fun updateGeneratedCaption(caption: String) {
        _generatedCaption.value = caption
    }

    fun setShowPageDialog(show: Boolean) {
        _showPageDialog.value = show
    }

    fun setShowHistoryDialog(show: Boolean) {
        _showHistoryDialog.value = show
    }

    fun clearNotification() {
        _notification.value = null
    }

    fun applyTopicPreset(topic: String) {
        _todayTopic.value = topic
    }

    fun applyPromptPreset(prompt: String) {
        _masterPrompt.value = prompt
    }

    fun generatePost() {
        val topic = _todayTopic.value.trim()
        if (topic.isBlank()) {
            _notification.value = UiNotification("Please enter today's topic first.", isError = true)
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _postedId.value = null
            _generationStage.value = "Starting AI engines..."

            try {
                // Launch caption & image generation
                _generationStage.value = "Imagen 3: Rendering visual..."
                val imageDeferred = async {
                    repository.generateImage(
                        topic = topic,
                        visualStylePrompt = _masterPrompt.value
                    )
                }

                _generationStage.value = "Gemini: Composing bilingual caption & hashtags..."
                val captionDeferred = async {
                    repository.generateCaption(
                        masterPrompt = _masterPrompt.value,
                        topic = topic,
                        primaryLang = _primaryLang.value,
                        secondaryLang = _secondaryLang.value
                    )
                }

                val captionResult = captionDeferred.await()
                val imageResult = imageDeferred.await()

                if (captionResult.isSuccess) {
                    _generatedCaption.value = captionResult.getOrThrow()
                } else {
                    _notification.value = UiNotification(
                        captionResult.exceptionOrNull()?.message ?: "Caption generation error",
                        isError = true
                    )
                }

                if (imageResult.isSuccess) {
                    val (bitmap, _) = imageResult.getOrThrow()
                    _generatedBitmap.value = bitmap
                } else {
                    _notification.value = UiNotification(
                        imageResult.exceptionOrNull()?.message ?: "Image generation error",
                        isError = true
                    )
                }

                if (captionResult.isSuccess && imageResult.isSuccess) {
                    _notification.value = UiNotification("Content generated successfully! Preview ready.")
                }
            } catch (e: Exception) {
                _notification.value = UiNotification("Generation failed: ${e.message}", isError = true)
            } finally {
                _isGenerating.value = false
                _generationStage.value = ""
            }
        }
    }

    fun postToFacebook() {
        val page = _selectedPage.value
        if (page == null) {
            _notification.value = UiNotification("Please select or configure a Facebook Page first.", isError = true)
            return
        }
        if (page.pageAccessToken.isBlank()) {
            _notification.value = UiNotification("The selected page does not have a valid Page Access Token.", isError = true)
            return
        }
        val caption = _generatedCaption.value
        val bitmap = _generatedBitmap.value
        if (bitmap == null) {
            _notification.value = UiNotification("Please generate an image first.", isError = true)
            return
        }

        viewModelScope.launch {
            _isPosting.value = true
            _notification.value = UiNotification("Publishing to Facebook Page '${page.pageName}'...")

            val result = repository.publishToFacebook(
                pageId = page.pageId,
                accessToken = page.pageAccessToken,
                caption = caption,
                imageBitmap = bitmap
            )

            if (result.isSuccess) {
                val postRes = result.getOrThrow()
                _postedId.value = postRes.postId ?: postRes.photoId
                _notification.value = UiNotification("Posted successfully to Facebook! Post ID: ${_postedId.value}")

                // Save to Room history
                repository.savePostHistory(
                    pageId = page.pageId,
                    pageName = page.pageName,
                    topic = _todayTopic.value,
                    masterPrompt = _masterPrompt.value,
                    imageBitmap = bitmap,
                    caption = caption,
                    status = "POSTED",
                    facebookPostId = _postedId.value
                )
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unknown Facebook Graph API error"
                _notification.value = UiNotification("Facebook publish error: $err", isError = true)

                repository.savePostHistory(
                    pageId = page.pageId,
                    pageName = page.pageName,
                    topic = _todayTopic.value,
                    masterPrompt = _masterPrompt.value,
                    imageBitmap = bitmap,
                    caption = caption,
                    status = "FAILED",
                    errorMessage = err
                )
            }
            _isPosting.value = false
        }
    }

    fun addNewPage(name: String, pageId: String, token: String, isDefault: Boolean) {
        viewModelScope.launch {
            val entity = FacebookPageEntity(
                pageName = name.ifBlank { "Page $pageId" },
                pageId = pageId.trim(),
                pageAccessToken = token.trim(),
                isDefault = isDefault
            )
            val newId = repository.savePage(entity)
            if (isDefault) {
                repository.setDefaultPage(newId)
            }
            _selectedPage.value = entity.copy(id = newId)
            _notification.value = UiNotification("Page '$name' saved successfully.")
        }
    }

    fun deletePage(page: FacebookPageEntity) {
        viewModelScope.launch {
            repository.deletePage(page)
            if (_selectedPage.value?.id == page.id) {
                _selectedPage.value = pages.value.firstOrNull { it.id != page.id }
            }
            _notification.value = UiNotification("Page removed.")
        }
    }

    fun verifyPageToken(pageId: String, token: String, onResult: (String, Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.verifyFacebookToken(pageId, token)
            if (res.isSuccess) {
                val info = res.getOrThrow()
                onResult("Verified: ${info.name} (ID: ${info.id})", true)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Verification failed"
                onResult("Error: $err", false)
            }
        }
    }

    fun loadFromHistory(historyItem: PostHistoryEntity) {
        _todayTopic.value = historyItem.topic
        _masterPrompt.value = historyItem.masterPrompt
        _generatedCaption.value = historyItem.caption
        if (historyItem.imageFilePath != null) {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeFile(historyItem.imageFilePath)
                if (bitmap != null) {
                    _generatedBitmap.value = bitmap
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        _postedId.value = historyItem.facebookPostId
        _showHistoryDialog.value = false
        _notification.value = UiNotification("Loaded draft from history.")
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }
}
