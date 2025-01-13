package com.happymax.fcmpushviewer

import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable

data class AppInfo(val appName:String, val packageName:String, val icon:Drawable?, val systemApp:Boolean):
    Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()?: "",
        parcel.readString() ?: "",
        null,
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(appName)
        parcel.writeString(packageName)

        parcel.writeByte(if (systemApp) 1 else 0)

    }
    override fun describeContents(): Int { return 0 }
    companion object CREATOR : Parcelable.Creator<AppInfo> {
        override fun createFromParcel(parcel: Parcel): AppInfo { return AppInfo(parcel) }
        override fun newArray(size: Int): Array<AppInfo?> { return arrayOfNulls(size) }
    }
}