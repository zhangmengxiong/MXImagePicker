package com.mx.imgpicker.builder

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.mx.imgpicker.app.ImgPickerActivity
import com.mx.imgpicker.models.PickerType
import java.io.Serializable

class PickerBuilder : Serializable {
    var pickerType: PickerType = PickerType.Image
    var maxPickerSize: Int = 1 //选择最大数量
    var activityCallback: ((AppCompatActivity) -> Unit)? = null // Activity在创建时会回调这个，可以设置样式等
    var enableCamera: Boolean = true // 是否可以拍摄
    var videoMaxLength: Int = -1 // 视频最大长度，单位：秒

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

    /**
     * 是否可以拍摄
     */
    fun setCameraEnable(enable: Boolean): PickerBuilder {
        enableCamera = enable
        return this
    }

    /**
     * 视频最长时长,单位：秒
     */
    fun setMaxVideoLength(length: Int): PickerBuilder {
        videoMaxLength = length
        return this
    }

    /**
     * Activity创建后回调，可以设置整体样式
     * 一般搭配ImmersionBar设置导航栏和状态栏的颜色等
     * 一次选择只会调用一次回调！
     */
    fun setActivityCallback(call: ((activity: AppCompatActivity) -> Unit)): PickerBuilder {
        activityCallback = call
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