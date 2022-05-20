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
import com.mx.imgpicker.models.MXItem
import com.mx.imgpicker.models.MXPickerType
import java.io.File

internal object MXVideoSource : IMXSource {
    const val MIME_TYPE = "video/*"
    private val SOURCE_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    override fun scan(
        context: Context,
        page: Int,
        pageSize: Int,
        minTime: Long?,
        maxTime: Long?
    ): List<MXItem> {
        //扫描图片
        val resolver = context.contentResolver ?: return emptyList()
        val columns = arrayListOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_MODIFIED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            columns.add(MediaStore.Video.Media.RELATIVE_PATH)
        }

        var where = MediaStore.Video.Media.SIZE + " > ? "
        val whereArgs = arrayListOf("0")
        if (maxTime != null && minTime != null) {
            where += " and (" + MediaStore.Video.Media.DATE_MODIFIED + "<? or " + MediaStore.Video.Media.DATE_MODIFIED + ">?)"
            whereArgs.add(minTime.toString())
            whereArgs.add(maxTime.toString())
        } else if (minTime != null) {
            where += " and " + MediaStore.Video.Media.DATE_MODIFIED + "<?"
            whereArgs.add(minTime.toString())
        } else if (maxTime != null) {
            where += " and " + MediaStore.Video.Media.DATE_MODIFIED + ">?"
            whereArgs.add(maxTime.toString())
        }

        val images = ArrayList<MXItem>()
        var mCursor: Cursor? = null
        try {
            mCursor = MXContentProvide.createCursor(
                resolver, SOURCE_URI, columns.toTypedArray(),
                where, whereArgs.toTypedArray(),
                MediaStore.Video.Media.DATE_MODIFIED,
                false, pageSize, page * pageSize
            )

            //读取扫描到的图片
            if (mCursor != null && mCursor.moveToFirst()) {
                var count = 0
                do {
                    val item = cursorToImageItem(resolver, mCursor)
                    if (item != null) {
                        images.add(item)
                        count++
                    }
                } while (mCursor.moveToNext() && count < pageSize)
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

    private fun cursorToImageItem(contentResolver: ContentResolver, mCursor: Cursor): MXItem? {
        try { // 获取视频的路径
            val id = mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
            val modify =
                mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED))
            val uri = ContentUris.withAppendedId(SOURCE_URI, id)
            val path = getFilePath(uri, mCursor)

            val duration = mCursor.getInt(
                mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            )

            //获取图片时间
//            val time = File(path).lastModified() / 1000 // 单位：秒
            if (path.endsWith("downloading")) return null
            if (contentResolver.openFileDescriptor(uri, "r") != null) {
                return MXItem(path, modify, MXPickerType.Video, duration / 1000)
            }
        } catch (e: Exception) {
        }
        return null
    }

    private fun getFilePath(uri: Uri, mCursor: Cursor): String {
        var path = uri.path
        if (path != null && File(path).exists() && File(path).canRead()) return path

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            path = mCursor.getString(
                mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
            )
        }
        if (path != null && File(path).exists() && File(path).canRead()) return path

        path = mCursor.getString(
            mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
        )
        return path
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