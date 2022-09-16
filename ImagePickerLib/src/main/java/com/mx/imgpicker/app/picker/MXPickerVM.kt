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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

internal class MXPickerVM : ViewModel() {
    companion object {
        private const val PAGE_SIZE = 40
    }

    private val allResStr by lazy { MXImagePicker.getContext().resources.getString(R.string.mx_picker_string_all) }
    private val allDir by lazy { MXDirItem(allResStr, "", 0) }
    val sourceDB by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MXDBSource(MXImagePicker.getContext())
    }

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
    var maxListSize: Int = -1 // 最长列表加载长度
        private set

    fun setConfig(config: MXConfig) {
        this.pickerType = config.pickerType
        this.maxSize = config.maxSize
        this.enableCamera = config.enableCamera
        this.compressType = config.compressType
        this.compressIgnoreSizeKb = config.compressIgnoreSizeKb
        this.videoMaxLength = config.videoMaxLength
        this.maxListSize = config.maxListSize
    }

    val dirListLive = MutableLiveData(Any()) // 文件夹列表
    private var _dirList: List<MXDirItem>? = null
    val dirList: List<MXDirItem>
        get() = _dirList ?: emptyList()

    val selectDirLive = MutableLiveData(allDir) // 当前选择文件夹
    val mediaListLive = MutableLiveData(Any()) // 文件列表
    private var _mediaList: List<MXItem>? = null
    val mediaList: List<MXItem>
        get() = _mediaList ?: emptyList()

    val selectMediaListLive = MutableLiveData(Any()) // 选中的文件列表
    val selectMediaList = ArrayList<MXItem>()

    val needCompress = MutableLiveData(true) // 是否需要压缩
    val fullScreenSelectIndex = MutableLiveData(0) // 是否需要压缩

    private val scanLock = AtomicBoolean(false)
    private var lastReloadTime = 0L
    fun startScan() {
        viewModelScope.launch {
            if (scanLock.get()) return@launch
            scanLock.set(true)
            MXUtils.log("开始扫描--> <--")
            val context = MXImagePicker.getContext()
            val scanResult: ((List<MXItem>) -> Boolean) = { list ->
                viewModelScope.launch {
                    if (list.isEmpty()) return@launch
                    val hasSave = withContext(Dispatchers.IO) {
                        mediaList.containsAll(list)
                    }
                    if (hasSave) return@launch
                    sourceDB.addSysSource(list)
                    if (abs(System.currentTimeMillis() - lastReloadTime) > 3000) {
                        reloadMediaList()
                    }
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
            scanLock.set(false)
        }
    }

    fun addPrivateSource(file: File, type: MXPickerType) {
        viewModelScope.launch { sourceDB.addPrivateSource(file, type) }
    }

    suspend fun reloadMediaList() = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val selectDir = selectDirLive.value ?: allDir
        val mediaList = sourceDB.getAllSource(pickerType, selectDir.path, maxListSize)
        if (!MXUtils.compareList(_mediaList, mediaList)) {
            MXUtils.log("刷新->图片列表 ${_mediaList?.size}->${mediaList.size}")
            _mediaList = mediaList
            mediaListLive.postValue(Any())
        }

        val dirs = sourceDB.getAllDirList(pickerType)
        allDir.childSize = dirs.sumOf { it.childSize }
        val allDirs = listOf(allDir) + dirs

        if (!MXUtils.compareList(_dirList, allDirs)) {
            MXUtils.log("刷新->目录列表:${_dirList?.size}->${allDirs.size}")
            _dirList = allDirs
            dirListLive.postValue(Any())
        }
        MXUtils.log("刷新->加载时长：${(System.currentTimeMillis() - start) / 1000f} 秒")

        if (mediaList.isNotEmpty()) {
            lastReloadTime = System.currentTimeMillis()
        }
    }
}