package com.mx.imgpicker.app

import android.content.Context
import com.mx.imgpicker.MXImagePicker
import com.mx.imgpicker.R
import com.mx.imgpicker.db.MXDBSource
import com.mx.imgpicker.models.MXDataSet
import com.mx.imgpicker.models.MXFolderItem
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.MXUtils
import com.mx.imgpicker.utils.source_loader.MXImageSource
import com.mx.imgpicker.utils.source_loader.MXVideoSource
import java.io.File
import kotlin.concurrent.thread

internal class MXSource(
    private val context: Context,
    private val data: MXDataSet,
    private val type: MXPickerType
) {
    companion object {
        private const val PAGE_START = 0
        private const val PAGE_SIZE = 30
    }

    private val sourceDB by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { MXDBSource(context) }
    private var isRelease = false
    fun startScan() {
        val context = MXImagePicker.getContext()
        thread {
            data.folderList.notifyChanged(getFolderGroup())
            when (type) {
                MXPickerType.Video -> {
                    startScanVideo(context)
                    startScanImage(context)
                }
                else -> {
                    startScanImage(context)
                    startScanVideo(context)
                }
            }
            data.folderList.notifyChanged(getFolderGroup())
        }
    }

    private fun startScanImage(context: Context) {
        var page = PAGE_START
        while (!isRelease) {
            val timePair = sourceDB.getLimitTime(MXPickerType.Image)
            val list = MXImageSource.scan(
                context, page, PAGE_SIZE,
                timePair?.first, timePair?.second
            )
            if (list.isEmpty()) break
            sourceDB.addSysSource(list)
            if (page == PAGE_START || page % 4 == 0) {
                data.folderList.notifyChanged(getFolderGroup())
            }
            MXUtils.log("扫描完第${page}页 --> ${list.size}")
            page++
        }
    }

    private fun startScanVideo(context: Context) {
        var page = PAGE_START
        while (!isRelease) {
            val timePair = sourceDB.getLimitTime(MXPickerType.Video)
            val list = MXVideoSource.scan(
                context, page, PAGE_SIZE,
                timePair?.first, timePair?.second
            )
            if (list.isEmpty()) break
            sourceDB.addSysSource(list)
            if (page == PAGE_START || page % 3 == 0) {
                data.folderList.notifyChanged(getFolderGroup())
            }
            MXUtils.log("扫描完第${page}页 --> ${list.size}")
            page++
        }
    }

    fun addPrivateSource(file: File, type: MXPickerType) {
        sourceDB.addPrivateSource(file, type)
    }

    private fun getFolderGroup(): ArrayList<MXFolderItem> {
        val list = sourceDB.getAllSource(type)
        val group = list.groupBy { it.getFolderName() }.map {
            MXFolderItem(it.key, it.value)
        }.toMutableList()
        group.add(0, MXFolderItem(context.resources.getString(R.string.mx_picker_string_all), list))
        group.sortByDescending { it.items.size }
        return ArrayList(group)
    }

    fun release() {
        isRelease = true
    }
}