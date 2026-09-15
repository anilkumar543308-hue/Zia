package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "facebook_pages")
data class FacebookPageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pageName: String,
    val pageId: String,
    val pageAccessToken: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "post_history")
data class PostHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pageId: String,
    val pageName: String,
    val topic: String,
    val masterPrompt: String,
    val imageFilePath: String?,
    val caption: String,
    val status: String, // DRAFT, POSTED, FAILED
    val facebookPostId: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
