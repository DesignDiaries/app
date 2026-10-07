package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.SubtaskDao
import com.example.data.dao.TaskDao
import com.example.data.dao.TaskListDao
import com.example.data.dao.WorkspaceDao
import com.example.data.model.Priority
import com.example.data.model.Recurrence
import com.example.data.model.SubtaskEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskListEntity
import com.example.data.model.WorkspaceItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        TaskListEntity::class,
        WorkspaceItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskLaunchDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun taskListDao(): TaskListDao
    abstract fun workspaceDao(): WorkspaceDao

    companion object {
        @Volatile
        private var INSTANCE: TaskLaunchDatabase? = null

        fun getInstance(context: Context): TaskLaunchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskLaunchDatabase::class.java,
                    "tasklaunch.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedIfEmpty(database: TaskLaunchDatabase) {
            try {
                val listDao = database.taskListDao()
                val taskDao = database.taskDao()
                val subtaskDao = database.subtaskDao()
                val workspaceDao = database.workspaceDao()

                if (taskDao.getTotalTaskCount() > 0) return

                val inboxListId = listDao.insertList(
                    TaskListEntity(name = "Inbox", colorHex = "#6366F1", iconName = "Inbox", isDefault = true)
                )
            val workListId = listDao.insertList(
                TaskListEntity(name = "Work", colorHex = "#3B82F6", iconName = "Work", isDefault = false)
            )
            val personalListId = listDao.insertList(
                TaskListEntity(name = "Personal", colorHex = "#10B981", iconName = "Home", isDefault = false)
            )
            val groceriesListId = listDao.insertList(
                TaskListEntity(name = "Shopping", colorHex = "#F59E0B", iconName = "Shopping", isDefault = false)
            )

            // Calculate today midnight
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val todayMidnight = cal.timeInMillis

            // Sample tasks
            val task1Id = taskDao.insertTask(
                TaskEntity(
                    title = "Welcome to TaskLaunch! Tap the checkmark to complete",
                    notes = "Your home screen is now centered around what you need to achieve today.",
                    priority = Priority.URGENT,
                    dueDate = todayMidnight,
                    dueTimeMinutes = 9 * 60, // 9:00 AM
                    listId = inboxListId,
                    tags = "onboarding",
                    recurrence = Recurrence.NONE
                )
            )

            subtaskDao.insertAll(
                listOf(
                    SubtaskEntity(taskId = task1Id, title = "Swipe left to view full Task Feed", isCompleted = false, sortOrder = 0),
                    SubtaskEntity(taskId = task1Id, title = "Swipe up for App Drawer", isCompleted = false, sortOrder = 1),
                    SubtaskEntity(taskId = task1Id, title = "Try typing 'Call Alex tomorrow 5pm !high #work'", isCompleted = false, sortOrder = 2)
                )
            )

            taskDao.insertTask(
                TaskEntity(
                    title = "Review product roadmap & sprint goals",
                    notes = "Prepare notes for team sync",
                    priority = Priority.HIGH,
                    dueDate = todayMidnight,
                    dueTimeMinutes = 14 * 60, // 2:00 PM
                    listId = workListId,
                    tags = "work,planning",
                    recurrence = Recurrence.NONE
                )
            )

            taskDao.insertTask(
                TaskEntity(
                    title = "Pick up groceries & almond milk",
                    notes = "Bananas, oat bread, coffee beans",
                    priority = Priority.MEDIUM,
                    dueDate = todayMidnight,
                    dueTimeMinutes = 18 * 60, // 6:00 PM
                    listId = groceriesListId,
                    tags = "shopping",
                    recurrence = Recurrence.NONE
                )
            )

            // Seed initial workspace dock & items
            workspaceDao.insertAll(
                listOf(
                    WorkspaceItemEntity(page = 1, itemType = "TASK_WIDGET", title = "Today's Agenda", gridX = 0, gridY = 0, spanX = 4, spanY = 2),
                    WorkspaceItemEntity(page = 1, itemType = "SMART_STRIP", title = "Next Up", gridX = 0, gridY = 2, spanX = 4, spanY = 1),
                    WorkspaceItemEntity(page = 2, itemType = "NOTES_WIDGET", title = "Scratchpad", gridX = 0, gridY = 0, spanX = 4, spanY = 2)
                )
            )
        } catch (_: Throwable) {}
    }
}
}
