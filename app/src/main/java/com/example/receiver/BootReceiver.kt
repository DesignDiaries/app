package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.TaskLaunchDatabase
import com.example.service.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = TaskLaunchDatabase.getInstance(context)
                    val allTasks = db.taskDao().getAllTasks().firstOrNull() ?: emptyList()
                    val now = System.currentTimeMillis()
                    for (task in allTasks) {
                        if (!task.isCompleted && task.reminderTime != null && task.reminderTime > now) {
                            AlarmScheduler.scheduleReminder(context, task)
                        }
                    }
                } catch (_: Throwable) {
                } finally {
                    try {
                        pendingResult.finish()
                    } catch (_: Throwable) {}
                }
            }
        }
    }
}
