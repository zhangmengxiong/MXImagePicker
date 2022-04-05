package com.mx.imgpicker.models

import com.mx.imgpicker.observer.MXValueObservable
import java.io.File
import java.io.Serializable

/**
 * 类型
 */
enum class MXPickerType(val value: String) : Serializable {
    Image("Image"), Video("Video"), ImageAndVideo("ImageAndVideo");

    companion object {
        fun from(value: String): MXPickerType {
            return when (value) {
                Image.value -> Image
                Video.value -> Video
                else -> ImageAndVideo
            }
        }
    }
}

/**
 * 压缩类型枚举
 */
enum class MXCompressType : Serializable {
    ON, // 强制关闭
    OFF, // 强制开启
    SELECT_BY_USER // 由用户选择
}

/**
 * 配置对象
 */
internal data class MXConfig(
    val pickerType: MXPickerType = MXPickerType.Image, // 类型
    val maxSize: Int = 1, // 选取最大数量
    val enableCamera: Boolean = true, // 是否可拍摄
    val compressType: MXCompressType = MXCompressType.SELECT_BY_USER, // 压缩类型
    val compressIgnoreSizeKb: Int = 200, // 图片压缩源文件阈值
    val videoMaxLength: Int = -1 // 视频最长时长
) : Serializable

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
    private var folderName: String? = null

    init {
        val paths = path.split(File.separator)
        if (paths.size >= 2) {
            folderName = paths[paths.size - 2]
        }
    }

    fun getFolderName(): String {
        return folderName ?: "Others"
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

internal class MXDataSet {
    val folderList = MXValueObservable<List<MXFolderItem>>(ArrayList()) // 文件夹列表
    val selectFolder = MXValueObservable<MXFolderItem?>(null) // 当前选择文件夹
    val selectList = MXValueObservable<List<MXItem>>(ArrayList()) // 选中的文件列表
    val needCompress = MXValueObservable(true) // 是否需要压缩

    fun getItemSize() = selectFolder.getValue()?.items?.size ?: 0
    fun getItem(index: Int) = selectFolder.getValue()?.items?.getOrNull(index)
    fun itemIndexOf(item: MXItem?): Int {
        if (item == null) return -1
        return selectFolder.getValue()?.items?.indexOf(item) ?: -1
    }

    fun release() {
        folderList.deleteObservers()
        selectFolder.deleteObservers()
        selectList.deleteObservers()
        needCompress.deleteObservers()
    }
}

/**
 * 图片选择回调
 */
internal interface ItemSelectCall {
    fun select(item: MXItem)
}
