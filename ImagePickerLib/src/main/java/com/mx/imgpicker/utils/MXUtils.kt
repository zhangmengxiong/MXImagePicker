package com.mx.imgpicker.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

internal object MXUtils {
    val IMAGE_EXT = arrayOf(
        "svg", "png", "webp", "jpg", "jpeg", "gif", "bmp", "heic", "heif"
    )
    val VIDEO_EXT = arrayOf(
        "mp4", "3gp", "mov", "m4a", "ts", "webm", "mkv"
    )

    private var isDebug = false
    fun setDebug(debug: Boolean) {
        isDebug = debug
    }

    fun log(any: Any) {
        if (isDebug) {
            Log.v(MXUtils::class.java.simpleName, any.toString())
        }
    }

    fun compareList(list1: List<*>?, list2: List<*>?): Boolean {
        if (list1 == null || list2 == null || list1.size != list2.size) return false
        list1.forEachIndexed { index, oldItem ->
            if (list2.getOrNull(index)?.equals(oldItem) != true) return false
        }
        return true
    }

    fun timeToString(second: Int): String {
        val hour = second / (60 * 60)
        val minute = (second / 60) % 60
        val second = (second % 60) % 60

        val hourStr = toTimeString(hour)
        val minuteStr = toTimeString(minute)
        val secondStr = toTimeString(second)
        return when {
            hour > 0 -> {
                "$hourStr:$minuteStr:$secondStr"
            }
            else -> {
                "$minuteStr:$secondStr"
            }
        }
    }

    private fun toTimeString(time: Int): String {
        return if (time < 10) "0$time" else time.toString()
    }

    const val REQUEST_CODE_READ = 0x21
    const val REQUEST_CODE_CAMERA = 0x22

    /**
     * 权限申请
     */
    fun requestPermission(activity: Activity, array: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, array, requestCode)
    }

    /**
     * 权限判断
     */
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