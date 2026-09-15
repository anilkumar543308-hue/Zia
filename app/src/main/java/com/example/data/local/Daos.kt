package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FacebookPageDao {
    @Query("SELECT * FROM facebook_pages ORDER BY isDefault DESC, createdAt DESC")
    fun getAllPages(): Flow<List<FacebookPageEntity>>

    @Query("SELECT * FROM facebook_pages WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultPage(): FacebookPageEntity?

    @Query("SELECT * FROM facebook_pages WHERE id = :id LIMIT 1")
    suspend fun getPageById(id: Long): FacebookPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: FacebookPageEntity): Long

    @Update
    suspend fun updatePage(page: FacebookPageEntity)

    @Delete
    suspend fun deletePage(page: FacebookPageEntity)

    @Query("UPDATE facebook_pages SET isDefault = 0")
    suspend fun clearDefaultPages()

    @Query("UPDATE facebook_pages SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultPage(id: Long)
}

@Dao
interface PostHistoryDao {
    @Query("SELECT * FROM post_history ORDER BY createdAt DESC")
    fun getAllPosts(): Flow<List<PostHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostHistoryEntity): Long

    @Update
    suspend fun updatePost(post: PostHistoryEntity)

    @Delete
    suspend fun deletePost(post: PostHistoryEntity)

    @Query("DELETE FROM post_history WHERE id = :id")
    suspend fun deletePostById(id: Long)
}
