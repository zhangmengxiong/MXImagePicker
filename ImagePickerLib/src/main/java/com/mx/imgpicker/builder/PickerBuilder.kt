package com.mx.imgpicker.builder

import android.content.Context
import android.content.Intent
import com.mx.imgpicker.app.ImgPickerActivity
import com.mx.imgpicker.models.PickerType
import java.io.Serializable

class PickerBuilder : Serializable {
    var pickerType: PickerType = PickerType.Image
    var maxPickerSize: Int = 1


    /**
     * 设置最大选择数量
     */
    fun setMaxSize(size: Int): PickerBuilder {
        maxPickerSize = size
        return this
    }

    /**
     * 选择类型，默认=Image
     */
    fun setType(type: PickerType): PickerBuilder {
        pickerType = type
        return this
    }


    fun createIntent(context: Context): Intent {
        val intent = Intent()
        intent.putExtra(KEY_INTENT_BUILDER, this)
        intent.setClass(context, ImgPickerActivity::class.java)
        return intent
    }

    companion object {
        const val KEY_INTENT_BUILDER = "PickerBuilder"
        const val KEY_INTENT_RESULT = "PickerResult"

        fun getPickerResult(intent: Intent?): List<String> {
            val list = intent?.getSerializableExtra(KEY_INTENT_RESULT) as ArrayList<String>?
            return list ?: emptyList()
        }
    }
}