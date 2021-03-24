package com.mx.imgpicker.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

class BarPlaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    init {
        val height = getStatusBarHeight(context)
        setPadding(0, height, 0, 0)
    }

    private fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }
}