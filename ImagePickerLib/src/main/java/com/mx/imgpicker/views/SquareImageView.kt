package com.mx.imgpicker.views

import android.content.Context
import android.util.AttributeSet
import kotlin.math.roundToInt

class SquareImageView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {
    private var ratio_width: Float = 1f
    private var ratio_height: Float = 1f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (ratio_width > 0f && ratio_height > 0f) {
            //获取宽度的模式和尺寸
            val widthSize = MeasureSpec.getSize(widthMeasureSpec)
            val heightSize = ((widthSize * ratio_height) / ratio_width).roundToInt()//根据宽度和比例计算高度
            val heightMeasureSpec1 = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec1)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}