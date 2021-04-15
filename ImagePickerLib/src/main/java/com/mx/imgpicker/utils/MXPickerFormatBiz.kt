package com.mx.imgpicker.utils

object MXPickerFormatBiz {
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
}