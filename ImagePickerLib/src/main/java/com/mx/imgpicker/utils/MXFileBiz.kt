package com.mx.imgpicker.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.mx.imgpicker.R
import com.mx.imgpicker.models.MXItem
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.source_loader.MXImageSource
import com.mx.imgpicker.utils.source_loader.MXVideoSource
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicLong


internal object MXFileBiz {
    const val PREFIX_IMAGE = "MX_IMG"
    const val PREFIX_VIDEO = "MX_VIDEO"
    private val fileIndex = AtomicLong(1000)

    /**
     * 生成缓存图片路径
     */
    fun createImageFile(context: Context): File {
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "${PREFIX_IMAGE}_${time}_${fileIndex.incrementAndGet()}.jpg"
        return File(getExistExtDir(context), imageFileName)
    }

    /**
     * 生成缓存视频路径
     */
    fun createVideoFile(context: Context): File {
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "${PREFIX_VIDEO}_${time}_${fileIndex.incrementAndGet()}.mp4"
        return File(getExistExtDir(context), imageFileName)
    }

    fun getExistExtDir(context: Context): File {
        val file = context.externalCacheDir
        if (file != null && file.exists() && file.canWrite()) { //判断文件目录是否存在
            return file
        }

        return context.cacheDir
    }

    /**
     * 打开视频或者图片
     */
    fun openItem(context: Context, item: MXItem) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val mimeType = when (item.type) {
                MXPickerType.Video -> MXVideoSource.MIME_TYPE
                else -> MXImageSource.MIME_TYPE
            }
            intent.setDataAndType(Uri.parse(item.path), mimeType)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                context.getString(R.string.mx_picker_string_open_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}