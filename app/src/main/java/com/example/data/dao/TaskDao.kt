package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.TaskEntity
import com.example.data.model.TaskWithSubtasks
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority DESC, dueDate ASC, id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Transaction
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority DESC, dueDate ASC, id DESC")
    fun getTasksWithSubtasks(): Flow<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskWithSubtasksById(id: Long): Flow<TaskWithSubtasks?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDate IS NOT NULL AND dueDate <= :endOfDayMillis ORDER BY priority DESC, dueDate ASC")
    fun getTasksDueBy(endOfDayMillis: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority DESC, dueDate ASC LIMIT 1")
    fun getNextPendingTask(): Flow<TaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setTaskCompletion(id: Long, isCompleted: Boolean, completedAt: Long?, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY isCompleted ASC, priority DESC")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTotalTaskCount(): Int
}
