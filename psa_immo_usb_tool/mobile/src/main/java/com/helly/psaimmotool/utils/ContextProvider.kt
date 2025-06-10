package com.helly.psaimmotool.utils

import android.content.Context

object ContextProvider {
    lateinit var appContext: Context
    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
