package com.mx.imgpicker.compress

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.mx.imgpicker.utils.MXFileBiz
import com.mx.imgpicker.utils.MXUtils
import java.io.File
import kotlin.math.min

class MXImageCompress internal constructor(private val build: MXCompressBuild) {
    companion object {
        fun from(context: Context): MXCompressBuild {
            return MXCompressBuild(context)
        }
    }

    private val format = if (build.supportAlpha == true) {
        Bitmap.CompressFormat.PNG
    } else Bitmap.CompressFormat.JPEG
    private val extension = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
    private val targetPx = build.targetPx
    private val targetFileSize = build.targetFileSize

    internal fun compress(source: File): File {
        MXUtils.log("缩放图片：目标像素值=$targetPx Px  /  targetFileSize=$targetFileSize Kb  /  保存文件后缀=$extension ")
        val degree = MXCompressUtil.getBitmapDegree(source)
        val cacheFile = MXFileBiz.createCacheImageFile(build.context, build.cacheDir, extension)
        if (!source.exists()) {
            MXUtils.log("缩放图片失败，源文件不存在，返回原文件:${source.absolutePath}")
            return source
        }
        val (width, height) = MXCompressUtil.readImageSize(source)
        if (width <= 0 || height <= 0) {
            MXUtils.log("缩放图片失败:读取源文件错误 -> readImageSize")
            return source
        }
        // 第一步：从源文件读取
        var targetBitmap = readBitmapFromFile(source, width, height) ?: return source
        // 第二步：缩放旋转
        targetBitmap = bitmapMatrix(targetBitmap, degree)

        // 第三步：从文件大小缩放
        if (targetFileSize > 0) {
            val bytes = MXCompressUtil.compressByQualityForByteArray(
                targetBitmap, targetFileSize, format
            )
            if (bytes != null) {
                MXCompressUtil.saveToCacheFile(bytes, cacheFile)
            } else {
                MXCompressUtil.saveToCacheFile(targetBitmap, format, cacheFile)
            }
        } else {
            MXCompressUtil.saveToCacheFile(targetBitmap, format, cacheFile)
        }
        targetBitmap.recycle()

        if (cacheFile.exists()) {
            val (new_width, new_height) = MXCompressUtil.readImageSize(cacheFile)
            MXUtils.log("缩放图片：($width,$height,${source.length() / 1024}Kb) -> ($new_width,$new_height,${cacheFile.length() / 1024}Kb)")
            return cacheFile
        }
        return source
    }

    /**
     * 从源文件读取，防止OOM
     */
    private fun readBitmapFromFile(source: File, width: Int, height: Int): Bitmap? {
        val bitmap = MXCompressUtil.decodeBitmapFromFile(
            source, width, height
        )
        if (bitmap == null) {
            MXUtils.log("缩放图片失败:读取源文件错误 -> decodeBitmapFromFile")
            return null
        }
        MXUtils.log("STEP 1 读取源文件 ($width x $height) -> (${bitmap.width} x ${bitmap.height})")
        return bitmap
    }

    /**
     * 缩放到指定大小
     */
    private fun bitmapMatrix(bitmap: Bitmap, degree: Int): Bitmap {
        val oldWidth = bitmap.width
        val oldHeight = bitmap.height
        val scale = targetPx.toFloat() / min(oldWidth, oldHeight).toFloat() // 缩放倍数
        if ((scale < 0 || scale >= 1f) && degree == 0) { // 需要放大且不旋转时，跳过！
            MXUtils.log("STEP 2 缩放图片旋转:跳过")
            return bitmap
        }
        val matrix = Matrix()
        if (degree != 0) {
            matrix.postRotate(degree.toFloat())
        }
        if (scale < 1f && scale > 0f) {
            matrix.postScale(scale, scale)
        }
        val matrixBitmap = Bitmap.createBitmap(bitmap, 0, 0, oldWidth, oldHeight, matrix, true)
        if (matrixBitmap != null) {
            MXUtils.log("STEP 2 缩放图片旋转:宽高缩放(${oldWidth}x$oldHeight) -> ${matrixBitmap.width}x${matrixBitmap.height} 旋转角度：$degree")
        }
        bitmap.recycle()
        return matrixBitmap
    }
}