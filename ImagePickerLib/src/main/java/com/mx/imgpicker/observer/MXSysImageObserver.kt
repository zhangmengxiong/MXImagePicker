package com.mx.imgpicker.observer

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.mx.imgpicker.utils.MXUtils

internal class MXSysImageObserver(private val onChangeCall: (() -> Unit)? = null) :
    ContentObserver(Handler(Looper.getMainLooper())) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        if (uri?.toString()?.contains("image", ignoreCase = true) == true) {
            MXUtils.log("MXSysImageObserver onChange $selfChange $uri")
            onChangeCall?.invoke()
        }
    }
}