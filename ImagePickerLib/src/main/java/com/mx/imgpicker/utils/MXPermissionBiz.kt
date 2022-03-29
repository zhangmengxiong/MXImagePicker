package com.mx.imgpicker.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

internal object MXPermissionBiz {
    const val REQUEST_CODE = 0x21
    private var permissions: Array<String>? = null
    fun requestPermission(activity: Activity, array: Array<String>) {
        ActivityCompat.requestPermissions(activity, array, REQUEST_CODE)
    }

    fun permissionResult(activity: Activity): Boolean {
        val permissions = permissions ?: return false
        return hasPermission(activity, permissions)
    }

    fun hasPermission(context: Context, array: Array<String>): Boolean {
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