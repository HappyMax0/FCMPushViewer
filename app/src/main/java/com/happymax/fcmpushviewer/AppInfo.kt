package com.happymax.fcmpushviewer

import android.R.drawable
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable

data class AppInfo(val appName:String, val packageName:String, val icon:Bitmap?, val systemApp:Boolean, val supportFCM: Boolean)
