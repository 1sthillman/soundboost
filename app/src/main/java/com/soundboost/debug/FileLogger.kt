package com.soundboost.debug

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * File Logger for debugging without ADB
 * Logs are saved to: /Android/data/com.soundboost/files/logs/
 */
object FileLogger {
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    fun init(context: Context) {
        try {
            val logsDir = File(context.getExternalFilesDir(null), "logs")
            if (!logsDir.exists()) {
                logsDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            logFile = File(logsDir, "assistant_log_$timestamp.txt")
            
            log("FileLogger", "📝 Log file created: ${logFile?.absolutePath}")
            log("FileLogger", "=" .repeat(60))
        } catch (e: Exception) {
            android.util.Log.e("FileLogger", "Failed to create log file", e)
        }
    }
    
    fun log(tag: String, message: String) {
        try {
            val timestamp = dateFormat.format(Date())
            val logMessage = "$timestamp [$tag] $message\n"
            
            // Also log to Logcat
            android.util.Log.d(tag, message)
            
            // Write to file
            logFile?.appendText(logMessage)
        } catch (e: Exception) {
            android.util.Log.e("FileLogger", "Failed to write log", e)
        }
    }
    
    fun getLogPath(): String? = logFile?.absolutePath
    
    fun getLatestLogContent(): String {
        return try {
            logFile?.readText() ?: "No log file"
        } catch (e: Exception) {
            "Error reading log: ${e.message}"
        }
    }
}
