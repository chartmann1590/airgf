package com.airgf.app.data.feedback

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun collectDiagnostics(): String {
        val appInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = appInfo.versionName ?: "unknown"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            appInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            appInfo.versionCode.toString()
        }

        val deviceBrand = Build.BRAND
        val deviceModel = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        val androidVersion = Build.VERSION.RELEASE
        val apiLevel = Build.VERSION.SDK_INT.toString()
        val locale = Locale.getDefault().toString()
        val timeZone = TimeZone.getDefault().id

        val storageFree = formatBytes(getStorageFree())
        val storageTotal = formatBytes(getStorageTotal())
        val memoryFree = formatBytes(getMemoryFree())
        val memoryTotal = formatBytes(getMemoryTotal())

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date())

        return buildString {
            appendLine("## Diagnostics")
            appendLine()
            appendLine("- App: AirGF")
            appendLine("- Package: ${context.packageName}")
            appendLine("- Version: $versionName ($versionCode)")
            appendLine("- Device: $deviceBrand $deviceModel")
            appendLine("- Manufacturer: $manufacturer")
            appendLine("- Android: $androidVersion / API $apiLevel")
            appendLine("- Locale: $locale")
            appendLine("- Time Zone: $timeZone")
            appendLine("- Storage Free/Total: $storageFree / $storageTotal")
            appendLine("- Memory Free/Total: $memoryFree / $memoryTotal")
            appendLine("- Timestamp: $timestamp")
        }
    }

    private fun getStorageFree(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            0L
        }
    }

    private fun getStorageTotal(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.blockCountLong * stat.blockSizeLong
        } catch (_: Exception) {
            0L
        }
    }

    private fun getMemoryFree(): Long {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            memInfo.availMem
        } catch (_: Exception) {
            0L
        }
    }

    private fun getMemoryTotal(): Long {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            memInfo.totalMem
        } catch (_: Exception) {
            0L
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            .coerceAtMost(units.lastIndex)
        return String.format(
            Locale.US,
            "%.1f %s",
            bytes / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }
}
