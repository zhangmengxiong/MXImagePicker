package com.mx.imagepicker_sample

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.tbruyelle.rxpermissions3.RxPermissions

object PermissionUtil {
    fun requestPermission(activity: FragmentActivity, array: Array<String>, call: ((Boolean) -> Unit)) {
        RxPermissions(activity)
                .request(*array)
                .subscribe { granted ->
                    call.invoke(granted)
                }
    }

    fun hasPermission(context: Context, array: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        val list = ArrayList<String>()
        array.forEach {
            if (ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED) {
                list.add(it)
            }
        }
        return list.isEmpty()
    }

    fun getUnPermissions(activity: Activity, array: Array<String>): Array<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return emptyArray()
        }
        val list = ArrayList<String>()
        array.forEach {
            if (ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED) {
                list.add(it)
            }
        }
        return list.toTypedArray()
    }
}