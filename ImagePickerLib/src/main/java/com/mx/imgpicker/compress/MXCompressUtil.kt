package com.mx.imgpicker.compress

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import com.mx.imgpicker.utils.MXUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.*

object MXCompressUtil {

    /**
     * 读取文件大小
     */
    fun readImageSize(file: File): Pair<Int, Int> {
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
    fun decodeBitmapFromFile(target: File, width: Int, height: Int): Bitmap? {
        try {
            val options = BitmapFactory.Options()
            val inSampleSize = computeSampleSize(width, height)
//            MXUtils.log("缩放图片：decodeBitmapFromFile(inSampleSize=$inSampleSize)")
            options.inSampleSize = inSampleSize
            return BitmapFactory.decodeStream(target.inputStream(), null, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 保存文件
     */
    fun saveToCacheFile(bitmap: Bitmap, format: Bitmap.CompressFormat, target: File) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(format, 70, stream)
        bitmap.recycle()
        val fos = FileOutputStream(target)
        fos.write(stream.toByteArray())
        fos.flush()
        fos.close()
        stream.close()
    }

    /**
     * 保存文件
     */
    fun saveToCacheFile(bytes: ByteArray, target: File) {
        val fos = FileOutputStream(target)
        fos.write(bytes)
        fos.flush()
        fos.close()
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

    fun getBitmapDegree(file: File): Int {
        try {
            val orientation = ExifInterface(file.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: java.lang.Exception) {
        }
        return 0
    }

    /**
     * 压缩到指定大小
     */
    fun compressByQualityForByteArray(
        bitmap: Bitmap,
        maxSize: Int,
        format: Bitmap.CompressFormat
    ): ByteArray? {
        val stream = ByteArrayOutputStream()
        val minRange = (maxSize * 0.95f).toInt()
        try {
            var qualityMax = 95
            var qualityMin = 1
            bitmap.compress(format, qualityMax, stream)
            if ((stream.size() / 1024) < maxSize) {
                return null
            }

            while (qualityMax - qualityMin > 2) {
                val quality = (qualityMax + qualityMin) / 2
                stream.reset()
                bitmap.compress(format, quality, stream)
                val size = (stream.size() / 1024)
                if (size in minRange until maxSize) {
                    break
                }
                if (size < maxSize) {
                    qualityMin = quality
                } else {
                    qualityMax = quality
                }
            }
            MXUtils.log("缩放图片:quality=${(qualityMax + qualityMin) / 2} ($qualityMax -> $qualityMin) size=${(stream.size() / 1024)} Kb / $maxSize Kb")
            return stream.toByteArray()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            stream.reset()
            stream.close()
        }
        return null
    }

}