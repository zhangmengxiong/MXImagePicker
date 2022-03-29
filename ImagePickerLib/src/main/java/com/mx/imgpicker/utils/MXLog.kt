package com.mx.imgpicker.utils

import android.util.Log
import com.mx.imgpicker.BuildConfig

internal object MXLog {
    fun log(any: Any) {
        if (BuildConfig.DEBUG) {
            Log.v(MXLog::class.java.simpleName, any.toString())
        }
    }
}