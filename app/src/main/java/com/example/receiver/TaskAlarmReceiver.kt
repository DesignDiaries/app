package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.TaskLaunchDatabase
import com.example.service.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "task_reminders_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val notificationId = (taskId and 0x7FFFFFFFL).toInt()

        when (intent.action) {
            AlarmScheduler.ACTION_COMPLETE_TASK -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = TaskLaunchDatabase.getInstance(context)
                        db.taskDao().setTaskCompletion(taskId, true, System.currentTimeMillis())
                    } catch (_: Throwable) {
                    } finally {
                        try {
                            pendingResult.finish()
                        } catch (_: Throwable) {}
                    }
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(notificationId)
            }

            AlarmScheduler.ACTION_SNOOZE_TASK -> {
                val snoozeMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, 10)
                val newTriggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = TaskLaunchDatabase.getInstance(context)
                        val task = db.taskDao().getTaskById(taskId)
                        if (task != null) {
                            val updated = task.copy(reminderTime = newTriggerTime)
                            db.taskDao().updateTask(updated)
                            AlarmScheduler.scheduleReminder(context, updated)
                        }
                    } catch (_: Throwable) {
                    } finally {
                        try {
                            pendingResult.finish()
                        } catch (_: Throwable) {}
                    }
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(notificationId)
            }

            AlarmScheduler.ACTION_TASK_REMINDER -> {
                val title = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_TITLE) ?: "Task Reminder"
                val notes = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_NOTES) ?: ""
                try {
                    showNotification(context, taskId, title, notes)
                } catch (_: Throwable) {}
            }
        }
    }

    private fun showNotification(context: Context, taskId: Long, title: String, notes: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notificationId = (taskId and 0x7FFFFFFFL).toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled tasks"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val contentRequestCode = ((taskId * 13 + 3) and 0x7FFFFFFFL).toInt()
        val doneRequestCode = ((taskId * 13 + 1) and 0x7FFFFFFFL).toInt()
        val snoozeRequestCode = ((taskId * 13 + 2) and 0x7FFFFFFFL).toInt()

        // Tap to open app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            contentRequestCode,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Complete action
        val doneIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_COMPLETE_TASK
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            doneRequestCode,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 10m action
        val snooze10Intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_SNOOZE_TASK
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, 10)
        }
        val snooze10PendingIntent = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode,
            snooze10Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(if (notes.isNotBlank()) notes else "Due now")
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.checkbox_on_background, "Done", donePendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "+10 Min", snooze10PendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
