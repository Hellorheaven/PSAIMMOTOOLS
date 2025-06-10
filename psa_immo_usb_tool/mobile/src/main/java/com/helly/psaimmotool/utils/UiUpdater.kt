package com.helly.psaimmotool.utils

import android.widget.TextView

object UiUpdater {
    var outputText: TextView? = null
    var statusText: TextView? = null

    fun appendLog(message: String) {
        outputText?.post {
            outputText?.append("$message\n")
        }
    }

    fun setConnectedStatus(message: String, source: String) {
        val status = "$source: $message"
        statusText?.post {
            statusText?.text = status
        }
        appendLog(status)
    }

    fun setDisconnectedStatus(disconnectMessage: String) {
        statusText?.post {
            statusText?.text = disconnectMessage
        }
        appendLog(disconnectMessage)
    }
}
