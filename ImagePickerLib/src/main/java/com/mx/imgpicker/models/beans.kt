package com.mx.imgpicker.models

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
    val targetFileSize: Int = 200, // 图片压缩源文件阈值
    val videoMaxLength: Int = -1, // 视频最长时长
    val maxListSize: Int = -1 // 最长列表加载长度
) : Serializable {

}

/**
 * 类型对象
 * @property path 绝对路径
 * @property timeInMs 创建时间
 * @property type 对象类型  图片/视频
 * @property duration 视频长度  单位：秒
 *
 */
data class MXItem(
    val path: String,
    val timeInMs: Long,
    val type: MXPickerType,
    val duration: Int = 0
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MXItem

        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + timeInMs.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + duration
        return result
    }

    override fun toString(): String {
        return "MXItem(path='$path', timeInMs=$timeInMs, type=$type)"
    }
}

/**
 * 分组对象
 */
internal data class MXDirItem(
    val name: String,
    val path: String,
    var childSize: Int,
    var lastItem: MXItem? = null
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MXDirItem

        if (name != other.name) return false
        if (path != other.path) return false
        if (childSize != other.childSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + childSize
        return result
    }

    override fun toString(): String {
        return "MXDirItem(name='$name', path='$path', childSize=$childSize)"
    }
}