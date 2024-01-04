package com.mx.imgpicker.compress

import android.content.Context
import android.graphics.Bitmap
import com.mx.imgpicker.utils.MXUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MXCompressBuild internal constructor(val context: Context) {
    internal var supportAlpha: Boolean? = null
    internal var targetFileSize: Int = 0
    internal var targetPx: Int = 2400
    internal var cacheDir: File? = null

    /**
     * 支持透明通道(’.png‘格式)
     * 默认=’.jpg‘格式
     */
    fun setSupportAlpha(support: Boolean): MXCompressBuild {
        this.supportAlpha = support
        return this
    }

    /**
     * 设置文件压缩大小需要在这个值左右
     * @param size 单位：KB 默认=0 自然压缩
     */
    fun setTargetFileSize(size: Int): MXCompressBuild {
        this.targetFileSize = size
        return this
    }

    /**
     * 设置文件压缩后宽/高像素值
     * @param pixel 单位：px
     */
    fun setTargetPixel(pixel: Int): MXCompressBuild {
        this.targetPx = pixel
        return this
    }

    /**
     * 缓存目录
     * @param dir
     */
    fun setCacheDir(dir: File): MXCompressBuild {
        this.cacheDir = dir
        return this
    }

    /**
     * 压缩
     * @param file 目标文件
     * @return 返回压缩后的图片文件
     */
    suspend fun compress(file: File): File = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            MXUtils.log("缩放图片失败，源文件不存在，返回原文件:${file.absolutePath}")
            return@withContext file
        }
        return@withContext MXImageCompress(this@MXCompressBuild).compress(file)
    }

    /**
     * 压缩
     * @param path 目标文件路径
     * @return 返回压缩后的图片文件
     */
    suspend fun compress(path: String) = compress(File(path))

    suspend fun compress(bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        return@withContext MXImageCompress(this@MXCompressBuild).compress(bitmap, 0)
    }
}