package com.mx.imgpicker.utils.source_loader

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.mx.imgpicker.models.Item
import com.mx.imgpicker.models.MXPickerType
import java.io.File

internal object MXImageSource : IMXSource {
    const val MIME_TYPE = "image/*"
    private val SOURCE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    override fun scan(context: Context, page: Int, pageSize: Int): List<Item> {
        //扫描图片
        val resolver = context.contentResolver ?: return emptyList()
        val columns = arrayListOf(
            MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media._ID,
            MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.SIZE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            columns.add(MediaStore.Images.Media.RELATIVE_PATH)
        }
        val images = ArrayList<Item>()
        var mCursor: Cursor? = null
        try {
            mCursor = resolver.query(
                SOURCE_URI, columns.toTypedArray(),
                MediaStore.Images.Media.SIZE + " > 0 ",
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT $pageSize OFFSET ${page * pageSize} "
            )
            //读取扫描到的图片
            if (mCursor != null && mCursor.moveToFirst()) {
                do {
                    val item = cursorToImageItem(resolver, mCursor)
                    if (item != null) {
                        images.add(item)
                    }
                } while (mCursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                mCursor?.close()
            } catch (e: Exception) {
            }
        }
        return images
    }

    private fun cursorToImageItem(
        contentResolver: ContentResolver,
        mCursor: Cursor
    ): Item? {
        try { // 获取图片的路径
            val id = mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            val uri = ContentUris.withAppendedId(SOURCE_URI, id)
            val path = getFilePath(uri, mCursor)

            //获取图片名称
            val name =
                mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
            //获取图片时间
            var time =
                mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
            if (time.toString().length < 13) {
                time *= 1000
            }
            //获取图片类型
            val mimeType =
                mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))

            if (path.endsWith("downloading")) return null
            if (contentResolver.openFileDescriptor(uri, "r") != null) {
                return Item(path, uri, mimeType, time, name, MXPickerType.Image)
            }
        } catch (e: java.lang.Exception) {
        }
        return null
    }


    private fun getFilePath(uri: Uri, mCursor: Cursor): String {
        var path = uri.path
        if (path != null && File(path).exists()) return path

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            path = mCursor.getString(
                mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            )
        }
        if (path != null && File(path).exists()) return path

        path = mCursor.getString(
            mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        )
        return path
    }

    override fun save(context: Context, file: File): Boolean {
        try {
            val contentValues = ContentValues()
            contentValues.put(MediaStore.Images.Media.TITLE, file.name)
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE)
            contentValues.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            contentValues.put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis())
            contentValues.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
            contentValues.put(MediaStore.Images.Media.DATA, file.absolutePath)
            contentValues.put(MediaStore.Images.Media.SIZE, file.length())
            context.contentResolver.insert(SOURCE_URI, contentValues)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val title = file.name
            MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, title, "")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}