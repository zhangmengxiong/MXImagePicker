package com.mx.imgpicker.factory

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.mx.imgpicker.models.Item
import com.mx.imgpicker.models.PickerType
import java.io.File
import java.util.*

/**
 * 图片显示接口
 */
interface IImageLoader {
    fun displayImage(item: Item, imageView: ImageView)
}

/**
 * 提供默认Glide显示图片
 */
class GlideImageLoader : IImageLoader {
    override fun displayImage(item: Item, imageView: ImageView) {
        if (item.type == PickerType.Image) {
            Glide.with(imageView).load(item.uri).into(imageView)
        } else {
            Glide.with(imageView).load(Uri.fromFile(File(item.path))).into(imageView)
        }
    }
}

/**
 * Uri转换器 转换成绝对路径
 */
interface IUriToFile {
    fun getPathFormSystemUri(context: Context, uri: Uri): String?
}

/**
 * 默认Uri转换器
 */
class DefaultUriToFile : IUriToFile {
    override fun getPathFormSystemUri(context: Context, uri: Uri): String? {
        return if (Build.VERSION.SDK_INT >= 19) { // api >= 19
            getRealPathFromUriAboveApi19(context, uri)
        } else { // api < 19
            getRealPathFromUriBelowAPI19(context, uri)
        }
    }

    /**
     * 适配api19以下(不包括api19),根据uri获取图片的绝对路径
     *
     * @param context 上下文对象
     * @param uri     图片的Uri
     * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
     */
    private fun getRealPathFromUriBelowAPI19(context: Context, uri: Uri): String? {
        return getDataColumn(context, uri, null, null)
    }

    /**
     * 适配api19及以上,根据uri获取图片的绝对路径
     *
     * @param context 上下文对象
     * @param uri     图片的Uri
     * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
     */
    @SuppressLint("NewApi")
    private fun getRealPathFromUriAboveApi19(context: Context, uri: Uri): String? {
        var filePath: String? = null
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // 如果是document类型的 uri, 则通过document id来进行处理
            val documentId = DocumentsContract.getDocumentId(uri)
            if (isMediaDocument(uri)) { // MediaProvider
                // 使用':'分割
                val array = documentId.split(":").dropLastWhile { it.isEmpty() }
                val type = array[0].toLowerCase(Locale.ENGLISH)
                val id = array[1]
                val typeUri = when (type) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = MediaStore.MediaColumns._ID + "=?"
                val selectionArgs = arrayOf(id)
                filePath = getDataColumn(context, typeUri, selection, selectionArgs)
            } else if (isDownloadsDocument(uri)) { // DownloadsProvider
                filePath = if (documentId.startsWith("raw:")) {
                    documentId.replaceFirst("raw:", "")
                } else {
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), documentId.toLongOrNull()
                            ?: 0
                    )
                    getDataColumn(context, contentUri, null, null)
                }
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            // 如果是 content 类型的 Uri
            filePath = getDataColumn(context, uri, null, null)
        } else if ("file" == uri.scheme) {
            // 如果是 file 类型的 Uri,直接获取图片对应的路径
            filePath = uri.path
        }
        return filePath
    }

    /**
     * 获取数据库表中的 _data 列，即返回Uri对应的文件路径
     * @return
     */
    private fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var path: String? = null

        val projection = arrayOf(MediaStore.Images.Media.DATA)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(projection[0])
                path = cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                cursor?.close()
            } catch (e: java.lang.Exception) {
            }
        }

        return path
    }

    /**
     * @param uri the Uri to check
     * @return Whether the Uri authority is MediaProvider
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri the Uri to check
     * @return Whether the Uri authority is DownloadsProvider
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }
}