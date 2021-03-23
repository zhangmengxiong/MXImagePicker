package com.mx.imgpicker.utils

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.mx.imgpicker.models.ImageItem
import com.mx.imgpicker.models.PickerType
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
        return File(context.cacheDir, imageFileName)
    }

    /**
     * 将图片保存到手机相册
     */
    fun saveToGallery(context: Context, imgFile: File) {
        val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
        val title = imgFile.name
        MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, title, "")
    }

    fun openImage(context: Context, item: ImageItem) {
        when (item.type) {
            PickerType.Image -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(item.uri, item.mimeType)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            PickerType.Video -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(Uri.fromFile(File(item.path)), item.mimeType)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}