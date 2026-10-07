package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_items")
data class WorkspaceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val page: Int = 1, // 0 = Tasks feed, 1 = Main home, 2 = Extra/Widgets, -1 = Dock
    val itemType: String, // "APP", "TASK_WIDGET", "SMART_STRIP", "NOTES_WIDGET", "FOLDER"
    val packageName: String? = null,
    val activityName: String? = null,
    val title: String,
    val gridX: Int = 0,
    val gridY: Int = 0,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val folderId: Long? = null
)
