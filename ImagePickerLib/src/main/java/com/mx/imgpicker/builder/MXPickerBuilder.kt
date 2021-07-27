package com.mx.imgpicker.builder

import android.content.Context
import android.content.Intent
import com.mx.imgpicker.app.ImgPickerActivity
import com.mx.imgpicker.models.MXPickerType
import java.io.Serializable
import java.lang.Exception

class MXPickerBuilder : Serializable {
    private var _pickerType: MXPickerType = MXPickerType.Image
    private var _maxSize: Int = 1 //选择最大数量
    private var _enableCamera: Boolean = true // 是否可以拍摄
    private var _videoMaxLength: Int = -1 // 视频最大长度，单位：秒

    fun getPickerType() = _pickerType
    fun getMaxSize() = _maxSize
    fun isEnableCamera() = _enableCamera
    fun getVideoMaxLength() = _videoMaxLength

    /**
     * 设置最大选择数量
     */
    fun setMaxSize(size: Int): MXPickerBuilder {
        if (size <= 0) {
            throw IllegalArgumentException("size must > 0")
        }
        _maxSize = size
        return this
    }


    /**
     * 选择类型，默认=Image
     */
    fun setType(type: MXPickerType): MXPickerBuilder {
        _pickerType = type
        return this
    }

    /**
     * 是否可以拍摄
     */
    fun setCameraEnable(enable: Boolean): MXPickerBuilder {
        _enableCamera = enable
        return this
    }

    /**
     * 视频最长时长,单位：秒
     */
    fun setMaxVideoLength(length: Int): MXPickerBuilder {
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