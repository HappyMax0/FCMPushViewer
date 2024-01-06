package com.happymax.fcmpushviewer

import android.R
import android.app.Application
import android.os.Build
import android.view.View
import com.google.android.material.color.DynamicColors


class FCMViewerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}