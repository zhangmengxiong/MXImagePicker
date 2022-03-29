package com.mx.imgpicker.views

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable

internal class MXPickerTextView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr), Checkable {
    private var mChecked = false

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (mChecked) {
            mergeDrawableStates(drawableState, intArrayOf(android.R.attr.state_checked))
        }
        return drawableState
    }

    override fun isChecked(): Boolean {
        return mChecked
    }

    override fun setChecked(arg0: Boolean) {
        mChecked = arg0
        refreshDrawableState()
    }

    override fun toggle() {
        mChecked = !mChecked
        postInvalidate()
    }
}