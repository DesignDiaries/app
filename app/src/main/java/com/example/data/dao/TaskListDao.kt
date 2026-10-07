package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TaskListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {

    @Query("SELECT * FROM task_lists ORDER BY isDefault DESC, name ASC")
    fun getAllLists(): Flow<List<TaskListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: TaskListEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultLists(lists: List<TaskListEntity>)

    @Delete
    suspend fun deleteList(list: TaskListEntity)

    @Query("SELECT COUNT(*) FROM task_lists")
    suspend fun getListCount(): Int
}
