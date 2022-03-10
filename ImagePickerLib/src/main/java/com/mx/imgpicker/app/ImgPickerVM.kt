package com.mx.imgpicker.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.mx.imgpicker.R
import com.mx.imgpicker.builder.MXPickerBuilder
import com.mx.imgpicker.db.MXSourceDB
import com.mx.imgpicker.models.FolderItem
import com.mx.imgpicker.models.Item
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.models.SourceGroup
import com.mx.imgpicker.observer.MXBaseObservable
import com.mx.imgpicker.utils.MXImagePickerProvider
import com.mx.imgpicker.utils.MXLog
import com.mx.imgpicker.utils.source_loader.MXImageSource
import com.mx.imgpicker.utils.source_loader.MXVideoSource
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.abs

class ImgPickerVM(
    val context: Context,
    private val builder: MXPickerBuilder,
    private val sourceDB: MXSourceDB,
    private val sourceGroup: SourceGroup
) : MXBaseObservable() {
    companion object {
        private const val PAGE_START = 0
        private const val PAGE_SIZE = 30
    }

    private val isInScan = AtomicBoolean(false)

    fun startScan() {
        if (isInScan.get()) return
        thread {
            synchronized(this) {
                isInScan.set(true)
                MXLog.log("扫描目录")
                val startTime = System.currentTimeMillis()
                var page = PAGE_START
                var newSize: Int = 0
                val folderList = ArrayList<FolderItem>()
                folderList.add(FolderItem(context.resources.getString(R.string.picker_string_all)))
                try {
                    do {
                        newSize = getItemListByPage(folderList, page)
                        if (newSize > 0) {
                            if (page == PAGE_START) {
                                val oldName = sourceGroup.selectFolder?.name
                                sourceGroup.selectFolder =
                                    folderList.firstOrNull {
                                        it.name == oldName
                                    } ?: folderList.firstOrNull()
                            }

                            notifyChanged()
                        }
                        MXLog.log("扫描完第${page}页 --> $newSize")
                        page++
                    } while (newSize > 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    val time = abs(System.currentTimeMillis() - startTime) / 1000f
                    MXLog.log("扫描目录完成，用时：$time 秒")
                    isInScan.set(false)
                }
            }
        }
    }

    private fun getItemListByPage(folders: ArrayList<FolderItem>, page: Int): Int {
        var scanList = when (builder.getPickerType()) {
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
        if (page == PAGE_START) {
            scanList = scanList + querySavedItem()
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
        return sourceDB.getAllSource(builder.getPickerType()).mapNotNull { item ->
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

    fun destroy() {
        deleteObservers()
    }
}