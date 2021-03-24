package com.mx.imgpicker.utils.source_loader

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.mx.imgpicker.models.Item
import com.mx.imgpicker.models.PickerType
import java.io.File

class ImageSourceLoader(val context: Context) : ISourceLoader {
    override fun scan( ): List<Item> {
        //扫描图片
        val mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val mContentResolver = context.contentResolver ?: return emptyList()

        val mCursor = mContentResolver.query(
            mImageUri, arrayOf(
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE
            ),
            MediaStore.MediaColumns.SIZE + ">0",
            null,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )
        val images = ArrayList<Item>()

        //读取扫描到的图片
        if (mCursor != null) {
            while (mCursor.moveToNext()) {
                val item = cursorToImageItem(mContentResolver, mCursor)
                if (item != null) {
                    images.add(item)
                }
            }
            mCursor.close()
        }
        return images
    }

    private fun cursorToImageItem(
        contentResolver: ContentResolver,
        mCursor: Cursor
    ): Item? {
        try { // 获取图片的路径
            val id = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Images.Media._ID))
            val path = mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DATA))
            //获取图片名称
            val name =
                mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME))
            //获取图片时间
            var time = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED))
            if (time.toString().length < 13) {
                time *= 1000
            }
            //获取图片类型
            val mimeType =
                mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE))

            if (path.endsWith("downloading")) return null
            //获取图片uri
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                .appendPath(id.toString()).build()

            if (File(path).exists() || contentResolver.openFileDescriptor(uri, "r") != null) {
                return Item(path, uri, mimeType, time, name, PickerType.Image)
            }
        } catch (e: java.lang.Exception) {
        }
        return null
    }
}