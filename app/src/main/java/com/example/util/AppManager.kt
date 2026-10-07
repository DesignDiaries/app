package com.example.util

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val iconBitmap: ImageBitmap?,
    val category: AppCategory = AppCategory.TOOLS,
    val isSystemApp: Boolean = false
)

enum class AppCategory(val title: String) {
    ALL("All"),
    RECENT("Recent"),
    PRODUCTIVITY("Productivity"),
    WORK("Work"),
    SOCIAL("Social"),
    COMMUNICATION("Communication"),
    FINANCE("Finance"),
    READING("Reading"),
    MEDIA("Media"),
    TOOLS("Tools"),
    GAMES("Games")
}

class AppManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val iconCache = mutableMapOf<String, ImageBitmap?>()

    suspend fun loadInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        try {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    launcherIntent,
                    PackageManager.ResolveInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(launcherIntent, 0)
            }

            val apps = resolveInfos.mapNotNull { resolveInfo ->
                try {
                    val pkg = resolveInfo.activityInfo.packageName
                    val activity = resolveInfo.activityInfo.name
                    // Don't show our own launcher in the app list to avoid recursion
                    if (pkg == context.packageName) return@mapNotNull null

                    val label = resolveInfo.loadLabel(packageManager).toString()
                    val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    val iconBitmap = iconCache.getOrPut(pkg) {
                        try {
                            val drawable = resolveInfo.loadIcon(packageManager)
                            drawableToBitmap(drawable)?.asImageBitmap()
                        } catch (_: Throwable) {
                            null
                        }
                    }

                    val category = classifyApp(pkg, label, resolveInfo.activityInfo.applicationInfo)

                    InstalledApp(
                        packageName = pkg,
                        activityName = activity,
                        label = label,
                        iconBitmap = iconBitmap,
                        category = category,
                        isSystemApp = isSystem
                    )
                } catch (_: Throwable) {
                    null
                }
            }.sortedBy { it.label.lowercase() }

            apps
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun classifyApp(packageName: String, label: String, appInfo: ApplicationInfo): AppCategory {
        val lowerPkg = packageName.lowercase()
        val lowerLabel = label.lowercase()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
                ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
                ApplicationInfo.CATEGORY_IMAGE, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_AUDIO -> return AppCategory.MEDIA
            }
        }

        return when {
            lowerPkg.contains("bank") || lowerPkg.contains("wallet") || lowerPkg.contains("pay") ||
            lowerPkg.contains("finance") || lowerPkg.contains("crypto") || lowerPkg.contains("invest") ||
            lowerPkg.contains("stock") || lowerLabel.contains("bank") || lowerLabel.contains("pay") ||
            lowerLabel.contains("wallet") || lowerLabel.contains("money") || lowerLabel.contains("budget") -> AppCategory.FINANCE

            lowerPkg.contains("book") || lowerPkg.contains("read") || lowerPkg.contains("kindle") ||
            lowerPkg.contains("news") || lowerPkg.contains("medium") || lowerPkg.contains("pocket") ||
            lowerLabel.contains("book") || lowerLabel.contains("reader") || lowerLabel.contains("news") -> AppCategory.READING

            lowerPkg.contains("slack") || lowerPkg.contains("teams") || lowerPkg.contains("zoom") ||
            lowerPkg.contains("meet") || lowerPkg.contains("trello") || lowerPkg.contains("jira") ||
            lowerPkg.contains("asana") || lowerPkg.contains("notion") || lowerLabel.contains("slack") ||
            lowerLabel.contains("teams") || lowerLabel.contains("zoom") -> AppCategory.WORK

            lowerPkg.contains("mail") || lowerPkg.contains("calendar") || lowerPkg.contains("clock") ||
            lowerPkg.contains("note") || lowerPkg.contains("todo") || lowerPkg.contains("doc") ||
            lowerPkg.contains("sheet") || lowerLabel.contains("task") || lowerLabel.contains("calc") -> AppCategory.PRODUCTIVITY

            lowerPkg.contains("dialer") || lowerPkg.contains("phone") || lowerPkg.contains("message") ||
            lowerPkg.contains("sms") || lowerPkg.contains("chat") || lowerPkg.contains("contact") -> AppCategory.COMMUNICATION

            lowerPkg.contains("facebook") || lowerPkg.contains("instagram") || lowerPkg.contains("twitter") ||
            lowerPkg.contains("reddit") || lowerPkg.contains("tiktok") || lowerPkg.contains("linkedin") -> AppCategory.SOCIAL

            lowerPkg.contains("camera") || lowerPkg.contains("gallery") || lowerPkg.contains("photo") ||
            lowerPkg.contains("music") || lowerPkg.contains("video") || lowerPkg.contains("youtube") ||
            lowerPkg.contains("spotify") || lowerPkg.contains("sound") -> AppCategory.MEDIA

            lowerPkg.contains("game") || lowerPkg.contains("play.games") -> AppCategory.GAMES

            else -> AppCategory.TOOLS
        }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun isDefaultLauncher(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                        return true
                    }
                }
            }
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            val defaultPkg = resolveInfo?.activityInfo?.packageName
            defaultPkg == context.packageName
        } catch (_: Throwable) {
            false
        }
    }

    fun requestDefaultLauncherIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                }
            } catch (_: Throwable) {}
        }
        return try {
            Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (_: Throwable) {
            Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        return try {
            val size = 96
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            bitmap
        } catch (_: Throwable) {
            null
        }
    }
}
