package com.mx.imgpicker.utils

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.widget.Toast
import com.mx.imgpicker.models.Item
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
    fun saveToGallery(context: Context, imgFile: File): Boolean {
        val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
        val title = imgFile.name
        try {
            MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, title, "")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
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