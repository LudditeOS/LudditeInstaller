package com.example.ludditeinstaller

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class LogDisplay(private val context: Context, private val logTextView: TextView) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    init {
        logTextView.setTextIsSelectable(true)
    }

    fun log(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] $tag: $message\n"

        mainHandler.post {
            logTextView.append(logMessage)
            // Auto-scroll to bottom
            val scrollAmount = logTextView.layout?.getLineTop(logTextView.lineCount) ?: 0
            logTextView.scrollTo(0, scrollAmount)
        }

        // Also log to system log
        Log.d(tag, message)
    }

    fun clear() {
        mainHandler.post {
            logTextView.text = ""
        }
    }
}