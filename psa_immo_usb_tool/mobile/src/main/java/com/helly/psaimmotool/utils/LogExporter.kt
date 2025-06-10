package com.helly.psaimmotool.utils

import android.content.Context
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogExporter {
    fun exportLogs(context: Context, content: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportDir = File(context.getExternalFilesDir(null), "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, "logs_$timestamp.txt")
            FileOutputStream(file).use {
                it.write(content.toByteArray())
            }

            Toast.makeText(context, "Logs exportés : ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur export logs : ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
