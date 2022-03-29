package com.mx.imgpicker.models

import android.net.Uri
import com.mx.imgpicker.observer.MXBaseObservable
import java.io.File
import java.io.Serializable

/**
 * 类型
 */
enum class MXPickerType : Serializable {
    Image, Video, ImageAndVideo;

    companion object {
        fun from(name: String): MXPickerType {
            return when (name) {
                Image.name -> Image
                Video.name -> Video
                else -> ImageAndVideo
            }
        }
    }
}

/**
 * 类型对象
 */
data class Item(
    val path: String,
    val uri: Uri,
    val mimeType: String,
    val time: Long,
    val name: String?,
    val type: MXPickerType,
    val duration: Int = 0,
    val fromSystemUri: Boolean = true
) : Serializable {
    fun getFolderName(): String {
        val paths = path.split(File.separator)
        if (paths.size >= 2) {
            return paths[paths.size - 2]
        }
        return ""
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Item

        if (path != other.path) return false

        return true
    }
}

internal data class DbSourceItem(
    val path: String, // 路径
    val type: MXPickerType, //类型
    val mimeType: String, // mime类型
    val time: Long, // 创建时间
    val videoLength: Int // 视频时长,单位：秒
)

/**
 * 分组对象
 */
internal data class FolderItem(val name: String, val images: ArrayList<Item> = ArrayList()) :
    Serializable

internal class SourceGroup : MXBaseObservable() {
    var folderList: ArrayList<FolderItem>? = null
    var selectFolder: FolderItem? = null
    val selectList = ArrayList<Item>()

    fun getItemSize() = selectFolder?.images?.size ?: 0
    fun getItem(index: Int) = selectFolder?.images?.getOrNull(index)
    fun itemIndexOf(item: Item?): Int {
        if (item == null) return -1
        return selectFolder?.images?.indexOf(item) ?: -1
    }
}

/**
 * 图片选择回调
 */
internal interface ItemSelectCall {
    fun select(item: Item)
}
