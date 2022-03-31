package com.mx.imgpicker.models

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
 * @property path 绝对路径
 * @property time 创建时间
 * @property type 对象类型  图片/视频
 * @property duration 视频长度  单位：秒
 *
 */
data class MXItem(val path: String, val time: Long, val type: MXPickerType, val duration: Int = 0) :
    Serializable {
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

        other as MXItem

        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + duration
        return result
    }
}

/**
 * 分组对象
 */
internal data class MXFolderItem(val name: String, val items: List<MXItem> = ArrayList())

internal class SourceGroup {
    var folderList: ArrayList<MXFolderItem>? = null
    var selectFolder: MXFolderItem? = null
    val selectList = ArrayList<MXItem>()

    fun getItemSize() = selectFolder?.items?.size ?: 0
    fun getItem(index: Int) = selectFolder?.items?.getOrNull(index)
    fun itemIndexOf(item: MXItem?): Int {
        if (item == null) return -1
        return selectFolder?.items?.indexOf(item) ?: -1
    }
}

/**
 * 图片选择回调
 */
internal interface ItemSelectCall {
    fun select(item: MXItem)
}
