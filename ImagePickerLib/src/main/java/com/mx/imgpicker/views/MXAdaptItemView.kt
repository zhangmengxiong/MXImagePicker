package com.mx.imgpicker.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.mx.imgpicker.MXImagePicker
import com.mx.imgpicker.R
import com.mx.imgpicker.models.MXItem
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.MXUtils
import com.mx.imgpicker.views.MXPickerTextView

class MXAdaptItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {
    init {
        View.inflate(context, R.layout.mx_picker_adapt_img_item, this)
    }

    private val img: ImageView by lazy { findViewById(R.id.img) }
    private val selectBG: ImageView by lazy { findViewById(R.id.selectBG) }
    private val videoTag: View by lazy { findViewById(R.id.videoTag) }
    private val videoLengthTxv: TextView by lazy { findViewById(R.id.videoLengthTxv) }
    private val indexTxv: MXPickerTextView by lazy { findViewById(R.id.indexTxv) }
    private val indexLay: RelativeLayout by lazy { findViewById(R.id.indexLay) }

    private var showItem: MXItem? = null
    private var selectIndex: Int? = null

    fun setData(item: MXItem, selectIndex: Int, onSelectClick: ((MXItem, Boolean) -> Unit)?) {
        indexTxv.visibility = View.VISIBLE
        selectBG.visibility = View.VISIBLE

        img.setImageResource(R.drawable.mx_icon_picker_image_place_holder)
        MXImagePicker.getImageLoader()?.invoke(item, img)

        val isSelect = selectIndex >= 0
        indexTxv.isChecked = isSelect

        if (item.type == MXPickerType.Video) {
            videoTag.visibility = View.VISIBLE
            videoLengthTxv.text = MXUtils.timeToString(item.duration)
        } else {
            videoTag.visibility = View.GONE
        }
        indexLay.setOnClickListener {
            onSelectClick?.invoke(item, isSelect)
        }
        if (isSelect) {
            selectBG.alpha = 1f
            indexTxv.text = (selectIndex + 1).toString()
        } else {
            selectBG.alpha = 0.2f
            indexTxv.text = ""
        }
        this.showItem = item
        this.selectIndex = selectIndex
    }
}