package com.mx.imgpicker.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import com.mx.imgpicker.models.Item
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicLong


object ImagePathBiz {
    private val fileIndex = AtomicLong(1000)

    /**
     * 生成缓存图片路径
     */
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName =
            String.format("IMG_%s_${fileIndex.incrementAndGet()}.jpg", timeStamp)
        return File(getExistExtDir(context), imageFileName)
    }

    /**
     * 生成缓存视频路径
     */
    fun createVideoFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = String.format("VIDEO_%s_${fileIndex.incrementAndGet()}.mp4", timeStamp)
        return File(getExistExtDir(context), imageFileName)
    }

    private fun getExistExtDir(context: Context): File {
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) { //判断外部存储是否可用
            val file = context.getExternalFilesDir("IMG")
            file?.mkdirs()
            if (file?.exists() == true) {
                return file
            }
        }
        val file = File(context.filesDir, "IMG")
        file.mkdirs()
        if (file.exists()) { //判断文件目录是否存在
            return file
        }
        return context.cacheDir
    }

    /**
     * 打开视频或者图片
     */
    fun openItem(context: Context, item: Item) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(item.uri, item.mimeType)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "打开失败！", Toast.LENGTH_SHORT).show()
        }
    }
}