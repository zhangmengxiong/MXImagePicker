package com.mx.imgpicker.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.mx.imgpicker.builder.PickerBuilder
import com.mx.imgpicker.db.MXSourceDB
import com.mx.imgpicker.models.FolderItem
import com.mx.imgpicker.models.Item
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.MXImagePickerProvider
import com.mx.imgpicker.utils.MXLog
import com.mx.imgpicker.utils.source_loader.MXImageSource
import com.mx.imgpicker.utils.source_loader.MXVideoSource
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class ImgPickerVM(
    val context: Context,
    private val builder: PickerBuilder,
    private val sourceDB: MXSourceDB
) {
    companion object {
        private val imageList = ArrayList<Item>()
    }

    private val isInScan = AtomicBoolean(false)
    private val mHandler = Handler(Looper.getMainLooper())
    var scanResult: ((List<FolderItem>) -> Unit)? = null

    fun startScan() {
        if (isInScan.get()) return
        thread {
            synchronized(this) {
                isInScan.set(true)
                MXLog.log("扫描目录")

                val savedSource = sourceDB.getAllSource(builder._pickerType).mapNotNull { item ->
                    val file = File(item.path)
                    if (!file.exists()) return@mapNotNull null

                    return@mapNotNull Item(
                        item.path,
                        MXImagePickerProvider.createUri(context, file),
                        item.mimeType,
                        file.lastModified(),
                        file.name,
                        builder._pickerType,
                        item.videoLength
                    )
                }

                val images = if (builder._pickerType == MXPickerType.Image) {
                    (MXImageSource.scan(context) + savedSource).sortedByDescending { it.time }
                } else {
                    MXVideoSource.scan(context) + savedSource.sortedByDescending { it.time }
                }

                val hasChange = (!images.toTypedArray().contentEquals(imageList.toTypedArray()))
                if (hasChange) {
                    imageList.clear()
                    imageList.addAll(images)
                    val allItems = imageList.sortedByDescending { it.time }
                    val folderList = (arrayOf(
                        FolderItem(
                            "全部",
                            ArrayList(allItems)
                        )
                    ) + allItems.groupBy { it.getFolderName() }.map { entry ->
                        FolderItem(entry.key, ArrayList(entry.value))
                    }).toList()
                    mHandler.post {
                        scanResult?.invoke(folderList)
                    }
                }
                isInScan.set(false)
            }
        }
    }

    fun destroy() {
        scanResult = null
        imageList.clear()
    }
}