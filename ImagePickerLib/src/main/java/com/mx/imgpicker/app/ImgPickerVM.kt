package com.mx.imgpicker.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.mx.imgpicker.models.FolderItem
import com.mx.imgpicker.models.ImageItem
import com.mx.imgpicker.models.PickerType
import com.mx.imgpicker.utils.ImgSourceBiz
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class ImgPickerVM(val context: Context) {
    companion object {
        private val imageList = ArrayList<ImageItem>()
    }

    private val isInScan = AtomicBoolean(false)
    private val mHandler = Handler(Looper.getMainLooper())
    var type: PickerType = PickerType.Image
    var scanResult: ((List<FolderItem>) -> Unit)? = null

    fun startScan() {
        if (isInScan.get()) return
        thread {
            synchronized(this) {
                isInScan.set(true)

                val images = if (type == PickerType.Image) {
                    ImgSourceBiz.loadImageFromPhone(context).sortedByDescending { it.time }
                } else {
                    ImgSourceBiz.loadVideoFromPhone(context).sortedByDescending { it.time }
                }

                val hasChange = (!images.toTypedArray().contentEquals(imageList.toTypedArray()))
                if (hasChange) {
                    imageList.clear()
                    imageList.addAll(images)
                    val allItems = imageList.sortedByDescending { it.time }
                    val folderList = (arrayOf(FolderItem("全部", ArrayList(allItems))) + allItems.groupBy { it.getFolderName() }.map { entry ->
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