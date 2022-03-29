package com.mx.imgpicker.utils

import android.content.Context
import com.mx.imgpicker.R
import com.mx.imgpicker.db.MXSourceDB
import com.mx.imgpicker.models.FolderItem
import com.mx.imgpicker.models.Item
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.models.SourceGroup
import com.mx.imgpicker.utils.source_loader.MXImageSource
import com.mx.imgpicker.utils.source_loader.MXVideoSource
import java.io.File
import kotlin.math.abs

internal class MXSourceScanner(
    private val context: Context,
    private val sourceDB: MXSourceDB,
    private val pickerType: MXPickerType,
    private val sourceGroup: SourceGroup
) {
    companion object {
        private const val PAGE_START = 0
        private const val PAGE_SIZE = 30
    }

    private var onChange: (() -> Unit)? = null
    private var isCancel = false

    fun setScanUpdate(call: (() -> Unit)) {
        onChange = call
    }

    fun startScan() {
        isCancel = false

        val startTime = System.currentTimeMillis()
        val folderList = ArrayList<FolderItem>()
        folderList.add(FolderItem(context.resources.getString(R.string.picker_string_all)))
        if (getSaveSource(folderList) > 0) {
            onChange?.invoke()
        }
        val oldName = sourceGroup.selectFolder?.name
        sourceGroup.selectFolder = folderList.firstOrNull {
            it.name == oldName
        } ?: folderList.firstOrNull()
        sourceGroup.folderList = folderList

        try {
            var page = PAGE_START
            while (!isCancel) {
                val newSize = getItemListByPage(folderList, page)
                if (newSize > 0 && !isCancel) {
                    onChange?.invoke()
                    MXLog.log("扫描完第${page}页 --> $newSize")
                } else {
                    break
                }
                page++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            val time = abs(System.currentTimeMillis() - startTime) / 1000f
            MXLog.log("扫描目录完成，用时：$time 秒")
        }
    }

    private fun getSaveSource(folders: ArrayList<FolderItem>): Int {
        val scanList = querySavedItem()
        if (scanList.isEmpty()) return 0

        val all = folders.firstOrNull()
        for (item in scanList) {
            val folderName = item.getFolderName()
            var folder = folders.firstOrNull { it.name == folderName }

            if (folder != null) {
                folder.images.add(item)
            } else {
                folder = FolderItem(folderName)
                folder.images.add(item)
                folders.add(folder)
            }

            all?.images?.add(item)
        }

        for (folder in folders) {
            folder.images.sortByDescending { it.time }
        }
        folders.sortByDescending { it.images.size }

        return scanList.size
    }

    private fun getItemListByPage(folders: ArrayList<FolderItem>, page: Int): Int {
        val scanList = when (pickerType) {
            MXPickerType.Image -> {
                MXImageSource.scan(context, page, PAGE_SIZE)
            }
            MXPickerType.Video -> {
                MXVideoSource.scan(context, page, PAGE_SIZE)
            }
            MXPickerType.ImageAndVideo -> {
                (MXImageSource.scan(context, page, PAGE_SIZE) +
                        MXVideoSource.scan(context, page, PAGE_SIZE))
            }
        }
        val all = folders.firstOrNull()
        for (item in scanList) {
            val folderName = item.getFolderName()
            var folder = folders.firstOrNull { it.name == folderName }

            if (folder != null) {
                folder.images.add(item)
            } else {
                folder = FolderItem(folderName)
                folder.images.add(item)
                folders.add(folder)
            }

            all?.images?.add(item)
        }

        for (folder in folders) {
            folder.images.sortByDescending { it.time }
        }
        folders.sortByDescending { it.images.size }

        return scanList.size
    }

    private fun querySavedItem(): List<Item> {
        return sourceDB.getAllSource(pickerType).mapNotNull { item ->
            val file = File(item.path)
            if (!file.exists()) return@mapNotNull null

            return@mapNotNull Item(
                item.path,
                MXImagePickerProvider.createUri(context, file),
                item.mimeType,
                file.lastModified() ?: 0L,
                file.name,
                item.type,
                item.videoLength
            )
        }
    }

    fun cancel() {
        isCancel = true
        onChange = null
    }
}