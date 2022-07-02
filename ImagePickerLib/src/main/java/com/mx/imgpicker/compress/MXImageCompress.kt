package com.mx.imgpicker.compress

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.mx.imgpicker.utils.MXFileBiz
import com.mx.imgpicker.utils.MXUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil

class MXImageCompress internal constructor(val build: MXCompressBuild) {
    companion object {
        fun from(context: Context): MXCompressBuild {
            return MXCompressBuild(context)
        }
    }

    internal fun compress(source: File): File {
        if (!source.exists()) {
            MXUtils.log("缩放图片失败，源文件不存在，返回原文件:${source.absolutePath}")
            return source
        }
        val fileSize = source.length() / 1024
        if (build.ignoreSize > 0 && fileSize <= build.ignoreSize) {
            MXUtils.log("缩放图片触发阈值限制: ${build.ignoreSize}Kb , 源文件大小：${fileSize}Kb")
            return source
        }
        val (width, height) = readImageSize(source)
        if (width <= 0 || height <= 0) {
            MXUtils.log("缩放图片失败:读取源文件错误(Size)，返回原文件:${source.absolutePath}")
            return source
        }

        val tagBitmap = decodeBitmapFromFile(source, width, height)
        if (tagBitmap == null) {
            MXUtils.log("缩放图片失败:读取源文件错误(File->Bitmap)，返回原文件:${source.absolutePath}")
            return source
        }

        // 旋转图片
        val rotationBitmap = rotationIfNeed(source, tagBitmap) ?: tagBitmap

        val cacheImg = try {
            saveToCacheFile(rotationBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            MXUtils.log("缩放图片失败:目标文件写入失败，返回原文件:${source.absolutePath}")
            return source
        }

        val (new_width, new_height) = readImageSize(source)
        MXUtils.log("缩放图片：($width,$height,${fileSize}Kb) -> ($new_width,$new_height,${cacheImg.length() / 1024}Kb)")
        return cacheImg
    }

    /**
     * 读取文件大小
     */
    private fun readImageSize(file: File): Pair<Int, Int> {
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            options.inSampleSize = 1

            BitmapFactory.decodeStream(file.inputStream(), null, options)
            return Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(0, 0)
    }

    /**
     * 文件->Bitmap，这里要设置采样率防止OOM
     */
    private fun decodeBitmapFromFile(target: File, width: Int, height: Int): Bitmap? {
        try {
            val options = BitmapFactory.Options()
            options.inSampleSize = computeSampleSize(width, height)

            return BitmapFactory.decodeStream(target.inputStream(), null, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 保存文件
     */
    private fun saveToCacheFile(bitmap: Bitmap): File {
        val stream = ByteArrayOutputStream()
        val format = if (build.supportAlpha == true) {
            Bitmap.CompressFormat.PNG
        } else Bitmap.CompressFormat.JPEG
        val extension = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
        bitmap.compress(format, 70, stream)
        bitmap.recycle()
        val cacheImg = MXFileBiz.createCacheImageFile(build.context, build.cacheDir, extension)
        val fos = FileOutputStream(cacheImg)
        fos.write(stream.toByteArray())
        fos.flush()
        fos.close()
        stream.close()
        return cacheImg
    }

    /**
     * 获取图片读取采样率
     * 采样率影响生成bitmap的文件大小
     * 生成大小等于源文件大小的 1/(sampleSize * sampleSize)
     */
    private fun computeSampleSize(width: Int, height: Int): Int {
        val srcWidth = if (width % 2 == 1) width + 1 else width
        val srcHeight = if (height % 2 == 1) height + 1 else height

        val longSide = srcWidth.coerceAtLeast(srcHeight)
        val shortSide = srcWidth.coerceAtMost(srcHeight)

        val scale = shortSide.toFloat() / longSide
        return if (scale <= 1 && scale > 0.5625) {
            if (longSide < 1664) {
                1
            } else if (longSide < 4990) {
                2
            } else if (longSide in 4991..10239) {
                4
            } else {
                if (longSide / 1280 == 0) 1 else longSide / 1280
            }
        } else if (scale <= 0.5625 && scale > 0.5) {
            if (longSide / 1280 == 0) 1 else longSide / 1280
        } else {
            ceil(longSide / (1280.0 / scale)).toInt()
        }
    }

    private fun rotationIfNeed(file: File, tagBitmap: Bitmap): Bitmap? {
        try {
            val orientation = ExifInterface(file.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val degree = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            if (degree != 0) {
                MXUtils.log("缩放图片： 图片角度 = $degree")
                val matrix = Matrix()
                matrix.postRotate(degree.toFloat())
                val rotationBitmap = Bitmap.createBitmap(
                    tagBitmap, 0, 0,
                    tagBitmap.width, tagBitmap.height, matrix,
                    true
                )
                tagBitmap.recycle()
                return rotationBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            MXUtils.log("旋转错误：${e.message}")
        }
        return null
    }
}