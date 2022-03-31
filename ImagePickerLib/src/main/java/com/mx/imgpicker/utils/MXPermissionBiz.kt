package com.mx.imgpicker.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

internal object MXPermissionBiz {
    const val REQUEST_CODE_READ = 0x21
    const val REQUEST_CODE_CAMERA = 0x22

    fun requestPermission(activity: Activity, array: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, array, requestCode)
    }

    fun hasPermission(context: Context, array: Array<out String>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        val list = ArrayList<String>()
        array.forEach {
            if (ContextCompat.checkSelfPermission(
                    context, it
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                list.add(it)
            }
        }
        return list.isEmpty()
    }
}