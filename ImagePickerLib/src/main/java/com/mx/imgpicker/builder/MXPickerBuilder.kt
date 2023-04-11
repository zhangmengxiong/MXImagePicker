package com.mx.imgpicker.builder

import android.content.Context
import android.content.Intent
import com.mx.imgpicker.app.picker.MXImgPickerActivity
import com.mx.imgpicker.models.MXCompressType
import com.mx.imgpicker.models.MXConfig
import com.mx.imgpicker.models.MXPickerType
import java.io.Serializable

class MXPickerBuilder : Serializable {
    private var pickerType: MXPickerType = MXPickerType.Image
    private var maxSize: Int = 1 //选择最大数量
    private var enableCamera: Boolean = true // 是否可以拍摄
    private var compressType: MXCompressType = MXCompressType.SELECT_BY_USER // 选中图片是否需要压缩至合适大小后返回
    private var targetFileSize = 200 // 图片压缩阈值，低于这个大小的图片不会被压缩 单位：KB   默认200KB以下不被压缩
    private var videoMaxLength: Int = -1 // 视频最大长度，单位：秒
    private var maxListSize: Int = 1000 // 列表最多显示条数，防止OOM

    /**
     * 设置最大选择数量
     */
    fun setMaxSize(size: Int): MXPickerBuilder {
        if (size <= 0) {
            throw IllegalArgumentException("size must > 0")
        }
        maxSize = size
        return this
    }

    /**
     * 图片是否需要压缩至合适大小后返回
     * @param type
     *      ON = 需要压缩，页面不显示“原图”按钮
     *      OFF = 不需要压缩，页面不显示“原图”按钮
     *      SELECT_BY_USER = 页面显示“原图”按钮，由用户控制是否压缩
     */
    fun setCompressType(type: MXCompressType): MXPickerBuilder {
        compressType = type
        return this
    }

    /**
     * 图片压缩阈值，低于这个大小的图片不会被压缩
     * @param size 单位：Kb  默认 = 200KB
     */
    fun setTargetFileSize(size: Int): MXPickerBuilder {
        targetFileSize = size
        return this
    }

    /**
     * 选择类型，默认=Image
     */
    fun setType(type: MXPickerType): MXPickerBuilder {
        pickerType = type
        return this
    }

    /**
     * 是否可以拍摄
     */
    fun setCameraEnable(enable: Boolean): MXPickerBuilder {
        enableCamera = enable
        return this
    }

    /**
     * 视频最长时长,单位：秒
     */
    fun setMaxVideoLength(length: Int): MXPickerBuilder {
        videoMaxLength = length
        return this
    }

    /**
     * 最长列表加载长度
     *  - -1=不限制
     *  - 默认限制长度=1000条
     */
    fun setMaxListSize(size: Int): MXPickerBuilder {
        if (size < 100) {
            throw IllegalArgumentException("size must > 100")
        }
        maxListSize = size
        return this
    }

    fun createIntent(context: Context): Intent {
        val compressType = if (pickerType == MXPickerType.Video) {
            MXCompressType.OFF
        } else compressType

        val intent = Intent()
        intent.putExtra(
            KEY_INTENT_BUILDER,
            MXConfig(
                pickerType,
                maxSize,
                enableCamera,
                compressType,
                targetFileSize,
                videoMaxLength,
                maxListSize
            )
        )
        intent.setClass(context, MXImgPickerActivity::class.java)
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