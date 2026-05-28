package com.example.util

import android.content.Context
import java.util.LinkedList

object ErrorLogger {
    private const val MAX_LOGS = 20
    private const val PREFS_NAME = "error_logs_prefs"
    private const val KEY_LOGS = "logs_delimited"

    fun log(context: Context, message: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LOGS, "") ?: ""
        val logs = if (raw.isEmpty()) LinkedList<String>() else LinkedList(raw.split("\n"))
        
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        logs.addFirst("$timestamp: $message")
        while (logs.size > MAX_LOGS) {
            logs.removeLast()
        }
        
        prefs.edit().putString(KEY_LOGS, logs.joinToString("\n")).apply()
    }

    fun getLogs(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LOGS, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split("\n")
    }
}
