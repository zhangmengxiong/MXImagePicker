package com.mx.imgpicker.utils.source_loader

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.mx.imgpicker.models.Item
import com.mx.imgpicker.models.MXPickerType
import java.io.File

internal object MXVideoSource : IMXSource {
    const val MIME_TYPE = "video/*"
    private val SOURCE_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    override fun scan(context: Context, page: Int, pageSize: Int): List<Item> {
        //扫描图片
        val resolver = context.contentResolver ?: return emptyList()
        val columns = arrayListOf(
            MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED, MediaStore.Video.Media._ID,
            MediaStore.Video.Media.MIME_TYPE, MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            columns.add(MediaStore.Video.Media.RELATIVE_PATH)
        }

        val mCursor = resolver.query(
            SOURCE_URI, columns.toTypedArray(),
            MediaStore.Video.Media.SIZE + " > 0 ",
            null,
            MediaStore.Video.Media.DATE_ADDED + " DESC LIMIT $pageSize OFFSET ${page * pageSize} "
        )
        val images = ArrayList<Item>()

        //读取扫描到的图片
        if (mCursor != null) {
            while (mCursor.moveToNext()) {
                val item = cursorToImageItem(resolver, mCursor)
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
        try { // 获取视频的路径
            val id = mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
            val uri = ContentUris.withAppendedId(SOURCE_URI, id)
            var path = uri.path

            if (path != null && !File(path).exists()) path = null
            if (path == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                path = mCursor.getString(
                    mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
                )
            }

            if (path != null && !File(path).exists()) path = null
            if (path == null) {
                path = mCursor.getString(
                    mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                )
            }
            if (path == null) return null

            val duration =
                mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                    .toIntOrNull() ?: 0
            //获取图片名称
            val name =
                mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
            //获取图片时间
            var time =
                mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED))
            if (time.toString().length < 13) {
                time *= 1000
            }
            //获取图片类型
            val mimeType =
                mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE))

            if (path.endsWith("downloading")) return null
            if (File(path).exists() || contentResolver.openFileDescriptor(uri, "r") != null) {
                return Item(path, uri, mimeType, time, name, MXPickerType.Video, duration / 1000)
            }
        } catch (e: Exception) {
        }
        return null
    }

    override fun save(context: Context, file: File): Boolean {
        try {
            val contentValues = ContentValues()
            contentValues.put(MediaStore.Video.Media.TITLE, file.name)
            contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            contentValues.put(MediaStore.Video.Media.MIME_TYPE, MIME_TYPE)
            contentValues.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            contentValues.put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis())
            contentValues.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis())
            contentValues.put(MediaStore.Video.Media.DURATION, getVideoLength(file))
            contentValues.put(MediaStore.Video.Media.DATA, file.absolutePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, file.absolutePath)
            }
            contentValues.put(MediaStore.Video.Media.SIZE, file.length())
            context.contentResolver.insert(SOURCE_URI, contentValues)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 获取视屏长度，返回毫秒
     */
    fun getVideoLength(file: File): Long {
        val retriever = MediaMetadataRetriever()
        var length = 0L
        try {
            retriever.setDataSource(file.absolutePath)
            length = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L)
        } catch (e: java.lang.Exception) {
        } finally {
            kotlin.runCatching { retriever.release() }
        }
        return length
    }
}