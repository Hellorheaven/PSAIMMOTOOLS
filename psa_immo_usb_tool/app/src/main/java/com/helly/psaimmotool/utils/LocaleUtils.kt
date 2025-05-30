package com.helly.psaimmotool.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import java.util.*

object LocaleUtils {
    fun setLocaleAndRestart(activity: Activity, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        val resources = activity.resources
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        // Redémarre l'activité pour appliquer la langue
        val refresh = Intent(activity, activity::class.java)
        refresh.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(refresh)
        activity.finish()
    }
}