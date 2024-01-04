package com.mx.imgpicker.compress

import android.content.Context
import android.graphics.Bitmap
import com.mx.imgpicker.utils.MXFileBiz
import com.mx.imgpicker.utils.MXUtils
import java.io.File

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
        MXUtils.log("缩放图片：目标像素值=$targetPx Px  /  targetFileSize=$targetFileSize Kb  /  保存文件后缀=$extension")
        val degree = MXCompressUtil.getBitmapDegree(source)
        val (width, height) = MXCompressUtil.readImageSize(source) ?: return source

        // 第一步：从源文件读取
        val bitmap = MXCompressUtil.decodeBitmapFromFile(source, width, height) ?: return source
        val cacheFile = compress(bitmap, degree)
        bitmap.recycle()

        if (cacheFile.exists()) {
            val (new_width, new_height) = MXCompressUtil.readImageSize(cacheFile) ?: Pair(0, 0)
            MXUtils.log("缩放图片：($width,$height,${source.length() / 1024}Kb) -> ($new_width,$new_height,${cacheFile.length() / 1024}Kb)")
            return cacheFile
        }
        return source
    }

    internal fun compress(bitmap: Bitmap, degree: Int): File {
        val cacheFile = MXFileBiz.createCacheImageFile(build.context, build.cacheDir, extension)
        val targetBitmap = MXCompressUtil.bitmapMatrix(bitmap, degree, targetPx)
        if (targetFileSize > 0) {
            val bytes = MXCompressUtil.compressByQualityForByteArray(
                targetBitmap, targetFileSize, format
            )
            if (bytes != null) {
                MXCompressUtil.saveToCacheFile(bytes, cacheFile)
                return cacheFile
            }
        }
        MXCompressUtil.saveToCacheFile(targetBitmap, format, cacheFile)
        return cacheFile
    }
}