package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WorkspaceItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    @Query("SELECT * FROM workspace_items WHERE page = :page ORDER BY gridY ASC, gridX ASC")
    fun getItemsByPage(page: Int): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_items ORDER BY page ASC, gridY ASC, gridX ASC")
    fun getAllItems(): Flow<List<WorkspaceItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WorkspaceItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WorkspaceItemEntity>)

    @Delete
    suspend fun deleteItem(item: WorkspaceItemEntity)

    @Query("DELETE FROM workspace_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM workspace_items WHERE page = :page")
    suspend fun clearPage(page: Int)

    @Query("SELECT COUNT(*) FROM workspace_items")
    suspend fun getItemCount(): Int
}
