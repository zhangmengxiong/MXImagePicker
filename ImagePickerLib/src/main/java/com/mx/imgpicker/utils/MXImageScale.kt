package com.mx.imgpicker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.ceil

object MXImageScale {

    /**
     * 对文件大小压缩
     * @param target 源文件
     * @param focusAlpha 是否保留透明通道
     * @param ignoreBy 压缩阈值，低于这个值直接返回源文件
     */
    @Throws(IOException::class)
    fun scaleFileSize(
        context: Context,
        target: File,
        focusAlpha: Boolean = false,
        ignoreBy: Int = 400,
        targetDir: File? = null
    ): File {
        // 阈值判断
        if (target.length() <= ignoreBy * 1024) return target

        val (width, height) = readImageSize(target)

        val options = BitmapFactory.Options()
        options.inSampleSize = computeSampleSize(width, height)

        val tagBitmap = BitmapFactory.decodeStream(
            target.inputStream(),
            null, options
        )
        if (tagBitmap == null) {
            MXLog.log("缩放图片失败，返回原文件:${target.absolutePath}")
            return target
        }
        val stream = ByteArrayOutputStream()

//        if (Checker.SINGLE.isJPG(srcImg.open())) {
//            tagBitmap = rotatingImage(tagBitmap, Checker.SINGLE.getOrientation(srcImg.open()))
//        }
        val format = if (focusAlpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val extension = if (focusAlpha) "png" else "jpeg"
        tagBitmap.compress(format, 60, stream)
        tagBitmap.recycle()
        val cacheImg = MXFileBiz.createCacheImageFile(context, targetDir, extension)
        val fos = FileOutputStream(cacheImg)
        fos.write(stream.toByteArray())
        fos.flush()
        fos.close()
        stream.close()

        val (new_width, new_height) = readImageSize(target)
        MXLog.log("缩放图片：($width,$height,${target.length() / 1024}Kb) -> ($new_width,$new_height,${cacheImg.length() / 1024}Kb)")

        return cacheImg
    }

    private fun readImageSize(file: File): Pair<Int, Int> {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        options.inSampleSize = 1

        BitmapFactory.decodeStream(file.inputStream(), null, options)
        return Pair(options.outWidth, options.outHeight)
    }

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
}