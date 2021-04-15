package com.mx.imgpicker.builder

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.MXFileBiz
import com.mx.imgpicker.utils.MXImagePickerProvider
import java.io.File
import java.io.Serializable

/**
 * 拍摄Intent生成器，自动绑定Provider
 */
class MXCaptureBuilder(val context: Context) : Serializable {
    private var _pickerType: MXPickerType = MXPickerType.Image
    private var _videoMaxLength: Int = -1 // 视频最大长度，单位：秒
    private var _dstFile: File = MXFileBiz.createImageFile(context)

    /**
     * 选择类型，默认=Image
     */
    fun setType(type: MXPickerType): MXCaptureBuilder {
        _pickerType = type
        _dstFile = if (type == MXPickerType.Image) {
            MXFileBiz.createImageFile(context)
        } else {
            MXFileBiz.createVideoFile(context)
        }
        return this
    }

    /**
     * 设置拍摄文件位置，不建议这么设置，默认位置在cache目录下
     */
    fun setCaptureFile(file: File) {
        _dstFile = file
    }

    /**
     * 视频最长时长,单位：秒
     */
    fun setMaxVideoLength(length: Int): MXCaptureBuilder {
        _videoMaxLength = length
        return this
    }

    fun createIntent(): Intent {
        val file = _dstFile
        val uri = MXImagePickerProvider.createUri(context, file)
        val intent = if (_pickerType == MXPickerType.Image) {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        } else {
            Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        }
        if (_pickerType == MXPickerType.Video && _videoMaxLength > 0) {
            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, _videoMaxLength)
        }

        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        return intent
    }

    fun getCaptureFile(): File {
        return _dstFile
    }
}