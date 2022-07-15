package com.mx.imgpicker.app.picker

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mx.imgpicker.MXImagePicker
import com.mx.imgpicker.R
import com.mx.imgpicker.db.MXDBSource
import com.mx.imgpicker.models.*
import com.mx.imgpicker.utils.MXUtils
import com.mx.imgpicker.utils.source_loader.MXImageSource
import com.mx.imgpicker.utils.source_loader.MXVideoSource
import java.io.File
import kotlin.concurrent.thread

internal class MXPickerVM : ViewModel() {
    companion object {
        private const val PAGE_START = 0
        private const val PAGE_SIZE = 30
    }

    private val sourceDB by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MXDBSource(MXImagePicker.getContext())
    }
    private var isRelease = false

    var pickerType: MXPickerType = MXPickerType.Image  // 类型
        private set
    var maxSize: Int = 1  // 选取最大数量
        private set
    var enableCamera: Boolean = true  // 是否可拍摄
        private set
    var compressType: MXCompressType = MXCompressType.SELECT_BY_USER  // 压缩类型
        private set
    var compressIgnoreSizeKb: Int = 200  // 图片压缩源文件阈值
        private set
    var videoMaxLength: Int = -1 // 视频最长时长
        private set

    fun setConfig(config: MXConfig) {
        this.pickerType = config.pickerType
        this.maxSize = config.maxSize
        this.enableCamera = config.enableCamera
        this.compressType = config.compressType
        this.compressIgnoreSizeKb = config.compressIgnoreSizeKb
        this.videoMaxLength = config.videoMaxLength
    }

    val folderList = MutableLiveData<List<MXFolderItem>>(ArrayList()) // 文件夹列表
    val selectFolder = MutableLiveData<MXFolderItem?>(null) // 当前选择文件夹
    val selectList = MutableLiveData<List<MXItem>>(ArrayList()) // 选中的文件列表
    val needCompress = MutableLiveData(true) // 是否需要压缩
    val fullScreenSelectIndex = MutableLiveData(0) // 是否需要压缩

    fun getItemSize() = selectFolder.value?.items?.size ?: 0
    fun getItem(index: Int) = selectFolder.value?.items?.getOrNull(index)
    fun itemIndexOf(item: MXItem?): Int {
        if (item == null) return -1
        return selectFolder.value?.items?.indexOf(item) ?: -1
    }

    fun getSelectList() = selectList.value ?: emptyList()
    fun getSelectListSize() = selectList.value?.size ?: 0
    fun getSelectIndexOf(item: MXItem?): Int {
        if (item == null) return -1
        return selectList.value?.indexOf(item) ?: -1
    }

    fun startScan() {
        val context = MXImagePicker.getContext()
        thread {
            folderList.postValue(getFolderGroup())
            when (pickerType) {
                MXPickerType.Video -> {
                    startScanVideo(context)
                    startScanImage(context)
                }
                else -> {
                    startScanImage(context)
                    startScanVideo(context)
                }
            }
            startScanAllDirs()
            folderList.postValue(getFolderGroup())
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
                folderList.postValue(getFolderGroup())
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
                folderList.postValue(getFolderGroup())
            }
            MXUtils.log("扫描完第${page}页 --> ${list.size}")
            page++
        }
    }

    /**
     * 搜索文件夹
     */
    private fun startScanAllDirs() {
        val dirs = sourceDB.getAllDirs()
        if (dirs.isEmpty()) return
        for (dir in dirs) {
            val list = dir.listFiles()
            if (list == null || list.isEmpty()) continue
            val mediaList = ArrayList<MXItem>()
            for (file in list) {
                val ext = file.extension?.lowercase()
                if (ext in MXUtils.IMAGE_EXT) {
                    mediaList.add(
                        MXItem(
                            file.absolutePath,
                            file.lastModified(),
                            MXPickerType.Image
                        )
                    )
                } else if (ext in MXUtils.VIDEO_EXT) {
                    mediaList.add(
                        MXItem(
                            file.absolutePath,
                            file.lastModified(),
                            MXPickerType.Video
                        )
                    )
                }
            }
            MXUtils.log("扫描目录 --> ${dir.absolutePath}  ${mediaList.size}")
            if (mediaList.isNotEmpty()) {
                sourceDB.addSysSource(mediaList)
            }
        }
    }

    fun addPrivateSource(file: File, type: MXPickerType) {
        sourceDB.addPrivateSource(file, type)
    }

    private fun getFolderGroup(): ArrayList<MXFolderItem> {
        val list = sourceDB.getAllSource(pickerType)
        val group = list.groupBy { it.getFolderName() }.map {
            MXFolderItem(it.key, it.value)
        }.toMutableList()
        group.add(
            0,
            MXFolderItem(
                MXImagePicker.getContext().resources.getString(R.string.mx_picker_string_all),
                list
            )
        )
        group.sortByDescending { it.items.size }
        return ArrayList(group)
    }

    fun release() {
        isRelease = true
    }
}