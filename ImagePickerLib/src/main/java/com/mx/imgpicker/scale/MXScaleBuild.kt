package com.mx.imgpicker.scale

import android.content.Context
import android.graphics.Bitmap
import java.io.File

class MXScaleBuild(val context: Context) {
    internal var compressFormat: Bitmap.CompressFormat? = null
    internal var ignoreSize: Int = 100
    internal var cacheDir: File? = null

    /**
     * 设置图片格式
     * 仅支持JPG、PGN两种格式
     */
    fun setCompressFormat(fmt: Bitmap.CompressFormat): MXScaleBuild {
        this.compressFormat = fmt
        return this
    }

    /**
     * 设置文件低于这个大小时，不进行压缩
     * @param size 单位：KB  默认=100KB
     */
    fun setIgnoreFileSize(size: Int): MXScaleBuild {
        this.ignoreSize = size
        return this
    }

    /**
     * 缓存目录
     * @param dir
     */
    fun setCacheDir(dir: File): MXScaleBuild {
        this.cacheDir = dir
        return this
    }

    fun get(file: File): File {
        return MXImageScale(this).compress(file)
    }

    fun get(path: String): File {
        return MXImageScale(this).compress(File(path))
    }
}