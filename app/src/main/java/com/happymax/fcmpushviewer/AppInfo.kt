package com.happymax.fcmpushviewer

import android.graphics.drawable.Drawable

data class AppInfo(val appName:String, val packageName:String, val icon:Drawable?, val systemApp:Boolean, val supportFCM:Boolean) {
}