package com.mx.imgpicker.app.picker

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mx.imgpicker.MXImagePicker
import com.mx.imgpicker.R
import com.mx.imgpicker.db.MXDBSource
import com.mx.imgpicker.models.*
import com.mx.imgpicker.utils.MXUtils
import com.mx.imgpicker.utils.source_loader.MXDirSource
import com.mx.imgpicker.utils.source_loader.MXImageSource
import com.mx.imgpicker.utils.source_loader.MXVideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal class MXPickerVM : ViewModel() {
    companion object {
        private const val PAGE_SIZE = 40
    }

    private val allResStr by lazy {
        MXImagePicker.getContext().resources.getString(R.string.mx_picker_string_all)
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

    val dirListLive = MutableLiveData<List<MXDirItem>>(ArrayList()) // 文件夹列表
    val dirList: List<MXDirItem>
        get() = dirListLive.value ?: emptyList()

    val selectDirLive = MutableLiveData(MXDirItem(allResStr, "", 0)) // 当前选择文件夹
    val mediaListLive = MutableLiveData<List<MXItem>>(ArrayList()) // 文件列表
    val mediaList: List<MXItem>
        get() = mediaListLive.value ?: emptyList()

    val selectMediaListLive = MutableLiveData<List<MXItem>>(ArrayList()) // 选中的文件列表
    val selectMediaList: List<MXItem>
        get() = selectMediaListLive.value ?: emptyList()

    val needCompress = MutableLiveData(true) // 是否需要压缩
    val fullScreenSelectIndex = MutableLiveData(0) // 是否需要压缩

    fun startScan() {
        viewModelScope.launch {
            MXUtils.log("开始扫描--> <--")
            val context = MXImagePicker.getContext()
            reloadMediaList()

            val scanResult: ((List<MXItem>) -> Boolean) = { list ->
                viewModelScope.launch {
                    val hasSave = withContext(Dispatchers.IO) {
                        mediaList.containsAll(list)
                    }
                    if (hasSave) return@launch
                    MXUtils.log("扫描结果--> ${list.size}")
                    sourceDB.addSysSource(list)
                    reloadMediaList()
                }
                this.isActive
            }
            if (pickerType != MXPickerType.Video) {
                MXImageSource.scan(context, PAGE_SIZE, scanResult)
            }
            if (pickerType != MXPickerType.Image) {
                MXVideoSource.scan(context, PAGE_SIZE, scanResult)
            }

            val dirs = sourceDB.getAllDirList(pickerType)
            MXDirSource(dirs).scan(context, PAGE_SIZE, scanResult)

            reloadMediaList()
            MXUtils.log("结束扫描--> <--")
        }
    }

    fun addPrivateSource(file: File, type: MXPickerType) {
        viewModelScope.launch { sourceDB.addPrivateSource(file, type) }
    }

    suspend fun reloadMediaList() = withContext(Dispatchers.IO) {
        val dirs = sourceDB.getAllDirList(pickerType)
        val dir = selectDirLive.value
        val allDir = MXDirItem(
            allResStr,
            "",
            dirs.sumOf { it.childSize },
            sourceDB.queryLastItem("", pickerType)
        )
        val allDirs = listOf(allDir) + dirs

        val selectDir = allDirs.firstOrNull { it.path == (dir?.path ?: "") }
            ?: allDirs.first()

        if (!MXUtils.compareList(dirListLive.value, allDirs)) {
            dirListLive.postValue(allDirs)
        }
        if (dir != selectDir) {
            selectDirLive.postValue(selectDir)
        }

        val mediaList = sourceDB.getAllSource(pickerType, selectDir.path)
        if (!MXUtils.compareList(mediaListLive.value, mediaList)) {
            mediaListLive.postValue(mediaList)
        }
    }

    fun release() {
        isRelease = true
    }
}