package com.mx.imgpicker.utils

import android.content.Context
import com.mx.imgpicker.db.MXDBSource
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.source_loader.MXDirSource
import com.mx.imgpicker.utils.source_loader.MXImageSource
import com.mx.imgpicker.utils.source_loader.MXVideoSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

object MXScanBiz {
    private val lock = Object()
    private const val PAGE_SIZE = 20
    private const val SCAN_SIZE = PAGE_SIZE * 2

    private var hasDeleteAllFirstTime = false
    private var hasScanAllImage = false
    private var hasScanAllVideo = false
    private var hasScanAllDirs = false
    private var listener: (() -> Unit)? = null
    internal suspend fun preScan(context: Context) = withContext(Dispatchers.Main) {
        if (!hasDeleteAllFirstTime) {
            MXUtils.log("MXScanBiz -- deleteAll")
            MXDBSource.instance.deleteAll()
            hasDeleteAllFirstTime = true
        }
        scanAll(context)
    }

    internal suspend fun scanAll(context: Context) = withContext(Dispatchers.Main) {
        MXUtils.log("MXScanBiz -- scanAll 开始扫描")
        if (!hasScanAllImage) {
            val size = scanImage(context, Int.MAX_VALUE)
            if (size > 0) {
                hasScanAllImage = true
            }
        }
        if (!hasScanAllVideo) {
            val size = scanVideo(context, Int.MAX_VALUE)
            if (size > 0) {
                hasScanAllVideo = true
            }
        }
        if (!hasScanAllDirs) {
            val size = scanDirs(context, Int.MAX_VALUE)
            if (size > 0) {
                hasScanAllDirs = true
            }
        }

        MXUtils.log("MXScanBiz -- scanAll 结束扫描")
        listener?.invoke()
    }

    fun scanRecent(context: Context, scope: CoroutineScope) {
        MXUtils.log("MXScanBiz -- scanRecent")
        val job = scope.launch {
            MXUtils.log("MXScanBiz -- 开始扫描")
            scanImage(context, SCAN_SIZE)
            scanVideo(context, SCAN_SIZE)
        }
        job.invokeOnCompletion {
            MXUtils.log("MXScanBiz -- 结束扫描")
            listener?.invoke()
        }
    }

    internal fun setOnUpdateListener(call: (() -> Unit)?) {
        listener = call
    }

    private suspend fun scanImage(context: Context, scanSize: Int) = withContext(Dispatchers.IO) {
        var sumSize = 0
        var currentScanSize = 0
        var currentScanPage = 0
        val scanPageSize = min(PAGE_SIZE, scanSize)
        do {
            val list = synchronized(lock) {
                MXImageSource.scan(context, scanPageSize, sumSize)
            } ?: break
            currentScanSize = list.size
            sumSize += currentScanSize
            MXDBSource.instance.addSysSource(list)

            currentScanPage++
            if (currentScanPage < 3 || currentScanPage % 10 == 0) {
                listener?.invoke()
            }
        } while (currentScanSize > 0 && sumSize < scanSize)

        MXUtils.log("MXScanBiz -- scanImage 结束扫描 -->${sumSize}")
        return@withContext sumSize
    }

    private suspend fun scanVideo(context: Context, scanSize: Int) = withContext(Dispatchers.IO) {
        var sumSize = 0
        var currentScanSize = 0
        var currentScanPage = 0
        val scanPageSize = min(PAGE_SIZE, scanSize)
        do {
            val list = synchronized(lock) {
                MXVideoSource.scan(context, scanPageSize, sumSize)
            } ?: break
            currentScanSize = list.size
            sumSize += currentScanSize
            MXDBSource.instance.addSysSource(list)

            currentScanPage++
            if (currentScanPage < 3 || currentScanPage % 10 == 0) {
                listener?.invoke()
            }
        } while (currentScanSize > 0 && sumSize < scanSize)
        MXUtils.log("MXScanBiz -- scanVideo 结束扫描 -->${sumSize}")
        return@withContext sumSize
    }

    private suspend fun scanDirs(context: Context, scanSize: Int) = withContext(Dispatchers.IO) {
        val dirs = MXDBSource.instance.getAllDirList(MXPickerType.ImageAndVideo)
        var sumSize = 0
        var currentScanSize = 0
        var currentScanPage = 0
        val scanPageSize = min(PAGE_SIZE, scanSize)
        do {
            val list = synchronized(lock) {
                MXDirSource(dirs).scan(context, scanPageSize, sumSize)
            } ?: break
            currentScanSize = list.size
            sumSize += currentScanSize
            MXDBSource.instance.addSysSource(list)

            currentScanPage++
            if (currentScanPage < 3 || currentScanPage % 10 == 0) {
                listener?.invoke()
            }
        } while (currentScanSize > 0 && sumSize < scanSize)
        MXUtils.log("MXScanBiz -- scanDirs 结束扫描 -->${sumSize}")
        return@withContext sumSize
    }
}