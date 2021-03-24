package com.mx.imgpicker.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import com.mx.imgpicker.models.Item
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


object ImagePathBiz {
    /**
     * 生成缓存图片路径
     */
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = String.format("IMG_%s.jpg", timeStamp)
        return File(getExistExtDir(context), imageFileName)
    }

    /**
     * 生成缓存视频路径
     */
    fun createVideoFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = String.format("VIDEO_%s.mp4", timeStamp)
        return File(getExistExtDir(context), imageFileName)
    }

    private fun getExistExtDir(context: Context): File {
        if (Environment.isExternalStorageEmulated()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val file = context.externalMediaDirs.firstOrNull { it.exists() }
                if (file != null) {
                    return file
                }
            }
            return Environment.getExternalStorageDirectory()
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