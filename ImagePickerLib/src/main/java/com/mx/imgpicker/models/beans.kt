package com.mx.imgpicker.models

import android.net.Uri
import java.io.File
import java.io.Serializable

/**
 * 类型
 */
enum class PickerType {
    Image, Video
}

/**
 * 类型对象
 */
data class ImageItem(val path: String, val uri: Uri, val mimeType: String, val time: Long, val name: String, val type: PickerType) : Serializable {
    fun getFolderName(): String {
        val paths = path.split(File.separator)
        if (paths.size >= 2) {
            return paths[paths.size - 2]
        }
        return ""
    }
}

/**
 * 分组对象
 */
data class FolderItem(val name: String, val images: ArrayList<ImageItem> = ArrayList()) : Serializable