package com.mx.imgpicker.app.picker

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mx.imgpicker.MXImagePicker
import com.mx.imgpicker.R
import com.mx.imgpicker.db.MXDBSource
import com.mx.imgpicker.models.*
import com.mx.imgpicker.utils.MXUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal class MXPickerVM : ViewModel() {
    private val allResStr by lazy { MXImagePicker.getContext().resources.getString(R.string.mx_picker_string_all) }
    private val allDir by lazy { MXDirItem(allResStr, "", 0) }

    var pickerType: MXPickerType = MXPickerType.Image  // 类型
        private set
    var maxSize: Int = 1  // 选取最大数量
        private set
    var enableCamera: Boolean = true  // 是否可拍摄
        private set
    var compressType: MXCompressType = MXCompressType.SELECT_BY_USER  // 压缩类型
        private set
    var targetFileSize: Int = 200  // 图片压缩源文件阈值
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
        this.targetFileSize = config.targetFileSize
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

    fun addPrivateSource(file: File, type: MXPickerType) {
        viewModelScope.launch { MXDBSource.instance.addPrivateSource(file, type) }
    }

    suspend fun reloadMediaList() = withContext(Dispatchers.IO) {
        // val start = System.currentTimeMillis()
        val selectDir = selectDirLive.value ?: allDir
        val mediaList = MXDBSource.instance.getAllSource(pickerType, selectDir.path, maxListSize)
        if (!MXUtils.compareList(_mediaList, mediaList)) {
            MXUtils.log("刷新->图片列表 ${_mediaList?.size}->${mediaList.size}")
            _mediaList = mediaList
            mediaListLive.postValue(Any())
        }

        val dirs = MXDBSource.instance.getAllDirList(pickerType)
        allDir.childSize = dirs.sumOf { it.childSize }
        val allDirs = listOf(allDir) + dirs

        if (!MXUtils.compareList(_dirList, allDirs)) {
            MXUtils.log("刷新->目录列表:${_dirList?.size}->${allDirs.size}")
            _dirList = allDirs
            dirListLive.postValue(Any())
        }
        // MXUtils.log("刷新->加载时长：${(System.currentTimeMillis() - start) / 1000f} 秒")
    }

    suspend fun onMediaInsert(file: File) = withContext(Dispatchers.IO) {
        val ext = file.extension?.lowercase()
        val type = if (ext in MXUtils.IMAGE_EXT) MXPickerType.Image else MXPickerType.Video
        val item = MXItem(file.absolutePath, file.lastModified(), type)
        _mediaList = ArrayList(_mediaList ?: emptyList()).apply {
            if (!this.contains(item)) add(0, item)
        }
        mediaListLive.postValue(Any())
    }
}