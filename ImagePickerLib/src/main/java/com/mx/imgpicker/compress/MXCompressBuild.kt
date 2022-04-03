package com.mx.imgpicker.compress

import android.content.Context
import java.io.File

class MXCompressBuild(val context: Context) {
    internal var supportAlpha: Boolean? = null
    internal var ignoreSize: Int = 100
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
     * 设置文件低于这个大小时，不进行压缩
     * @param size 单位：KB  默认=100KB
     */
    fun setIgnoreFileSize(size: Int): MXCompressBuild {
        this.ignoreSize = size
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

    fun compress(file: File): File {
        return MXImageCompress(this).compress(file)
    }

    fun compress(path: String): File {
        return MXImageCompress(this).compress(File(path))
    }
}