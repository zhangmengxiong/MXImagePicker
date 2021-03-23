package com.mx.imgpicker.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.mx.imgpicker.models.ImageItem
import com.mx.imgpicker.models.PickerType
import java.io.File

/**
 * 图片、视频扫描工具类
 */
object ImgSourceBiz {
    /**
     * 加载手机所有的图片
     */
    fun loadImageFromPhone(context: Context): List<ImageItem> {
        //扫描图片
        val mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val mContentResolver = context.contentResolver ?: return emptyList()

        val mCursor = mContentResolver.query(mImageUri, arrayOf(
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE),
                MediaStore.MediaColumns.SIZE + ">0",
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC")
        val images = ArrayList<ImageItem>()

        //读取扫描到的图片
        if (mCursor != null) {
            while (mCursor.moveToNext()) {
                val item = cursorToImageItem(mContentResolver, mCursor, PickerType.Image)
                if (item != null) {
                    images.add(item)
                }
            }
            mCursor.close()
        }
        return images
    }

    private fun cursorToImageItem(contentResolver: ContentResolver, mCursor: Cursor, type: PickerType): ImageItem? {
        try { // 获取图片的路径
            val id = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Images.Media._ID))
            val path = mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DATA))
            //获取图片名称
            val name = mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME))
            //获取图片时间
            var time = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED))
            if (time.toString().length < 13) {
                time *= 1000
            }
            //获取图片类型
            val mimeType = mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE))

            if (path.endsWith("downloading")) return null
            //获取图片uri
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendPath(id.toString()).build()

            if (File(path).exists() || contentResolver.openFileDescriptor(uri, "r") != null) {
                return ImageItem(path, uri, mimeType, time, name, type)
            }
        } catch (e: java.lang.Exception) {
        }
        return null
    }

    fun loadVideoFromPhone(context: Context): List<ImageItem> {
        //扫描图片
        val mImageUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val mContentResolver = context.contentResolver ?: return emptyList()

        val mCursor = mContentResolver.query(mImageUri, arrayOf(
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE),
                MediaStore.MediaColumns.SIZE + ">0",
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC")
        val images = ArrayList<ImageItem>()

        //读取扫描到的图片
        if (mCursor != null) {
            while (mCursor.moveToNext()) {
                val item = cursorToImageItem(mContentResolver, mCursor, PickerType.Video)
                if (item != null) {
                    images.add(item)
                }
            }
            mCursor.close()
        }
        return images
    }
}