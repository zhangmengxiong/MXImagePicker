package com.mx.imgpicker.models

import android.net.Uri
import java.io.File
import java.io.Serializable

/**
 * 类型
 */
enum class PickerType : Serializable {
    Image, Video
}

/**
 * 类型对象
 */
data class Item(
    val path: String,
    val uri: Uri,
    val mimeType: String,
    val time: Long,
    val name: String,
    val type: PickerType,
    val duration: Int = 0
) : Serializable {
    fun getFolderName(): String {
        val paths = path.split(File.separator)
        if (paths.size >= 2) {
            return paths[paths.size - 2]
        }
        return ""
    }
}

data class DbSourceItem(
    val path: String, // 路径
    val type: PickerType, //类型
    val mimeType: String, // mime类型
    val time: Long, // 创建时间
    val videoLength: Int // 视频时长,单位：秒
)

/**
 * 分组对象
 */
data class FolderItem(val name: String, val images: ArrayList<Item> = ArrayList()) : Serializable

/**
 * 图片选择回调
 */
interface ItemSelectCall {
    fun select(item: Item)
}
