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
import com.mx.imgpicker.models.MXItem
import com.mx.imgpicker.models.MXPickerType
import java.io.File


internal object MXImageSource : IMXSource {
    const val MIME_TYPE = "image/*"
    private val SOURCE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    override fun scan(context: Context, size: Int, offset: Int): List<MXItem>? {
        val images = ArrayList<MXItem>()
        //扫描图片
        val resolver = context.contentResolver ?: return null
        val columns = arrayListOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_MODIFIED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            columns.add(MediaStore.Images.Media.RELATIVE_PATH)
        }

        val where = MediaStore.Images.Media.SIZE + " > ? "
        val whereArgs = arrayListOf("0")

        var mCursor: Cursor? = null
        try {
            mCursor = MXContentProvide.createCursor(
                resolver, SOURCE_URI, columns.toTypedArray(),
                where, whereArgs.toTypedArray(),
                MediaStore.Images.Media.DATE_MODIFIED,
                false
            )

            if (mCursor == null || !mCursor.moveToFirst()) {
                return null
            }
            if (offset > 0) {
                if (offset < mCursor.count) {
                    mCursor.move(offset)
                } else {
                    return null
                }
            }
            do {
                val item = cursorToImageItem(resolver, mCursor)
                if (item != null) {
                    images.add(item)
                }
                if (images.size >= size) {
                    break
                }
            } while (mCursor.moveToNext())
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                mCursor?.close()
            } catch (_: Exception) {
            }
        }
        return images
    }

    private fun cursorToImageItem(contentResolver: ContentResolver, mCursor: Cursor): MXItem? {
        try { // 获取图片的路径
            val id = mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            val modify =
                mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED))
            val uri = ContentUris.withAppendedId(SOURCE_URI, id)
            val path = getFilePath(uri, mCursor)

            //获取图片时间
//            val time = File(path).lastModified() / 1000 // 单位：秒
            if (path.endsWith("downloading")) return null
            val desc = contentResolver.openFileDescriptor(uri, "r")
            if (desc != null) {
                desc.close()
                return MXItem(path, modify * 1000, MXPickerType.Image)
            }
        } catch (_: Exception) {
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