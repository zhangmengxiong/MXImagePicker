package com.mx.imgpicker.builder

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.mx.imgpicker.app.ImgPickerActivity
import com.mx.imgpicker.models.MXPickerType
import java.io.Serializable

class PickerBuilder : Serializable {
    var _pickerType: MXPickerType = MXPickerType.Image
    var _maxSize: Int = 1 //选择最大数量
    var _enableCamera: Boolean = true // 是否可以拍摄
    var _videoMaxLength: Int = -1 // 视频最大长度，单位：秒

    /**
     * 设置最大选择数量
     */
    fun setMaxSize(size: Int): PickerBuilder {
        _maxSize = size
        return this
    }

    /**
     * 选择类型，默认=Image
     */
    fun setType(type: MXPickerType): PickerBuilder {
        _pickerType = type
        return this
    }

    /**
     * 是否可以拍摄
     */
    fun setCameraEnable(enable: Boolean): PickerBuilder {
        _enableCamera = enable
        return this
    }

    /**
     * 视频最长时长,单位：秒
     */
    fun setMaxVideoLength(length: Int): PickerBuilder {
        _videoMaxLength = length
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