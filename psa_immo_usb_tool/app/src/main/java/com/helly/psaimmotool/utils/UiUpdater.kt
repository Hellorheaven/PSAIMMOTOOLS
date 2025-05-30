package com.helly.psaimmotool.utils

import android.widget.TextView
import com.helly.psaimmotool.R

object UiUpdater {
    var outputText: TextView? = null
    var statusText: TextView? = null

    fun appendLog(message: String) {
        outputText?.post {
            outputText?.append("$message\n")
        }
    }

    fun setConnectedStatus(message: String, source: String) {
        statusText?.post {
            statusText?.text = message
        }
        appendLog(message)
    }

    fun setDisconnectedStatus(disconnectMessage: String) {
        statusText?.post {
            statusText?.text = disconnectMessage
        }
        appendLog(disconnectMessage)
    }
}
