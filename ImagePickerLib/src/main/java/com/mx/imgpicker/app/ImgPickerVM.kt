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
import com.mx.imgpicker.utils.MXSourceScanner
import com.mx.imgpicker.utils.source_loader.MXImageSource
import com.mx.imgpicker.utils.source_loader.MXVideoSource
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.abs

internal class ImgPickerVM(
    val context: Context,
    private val builder: MXPickerBuilder,
    private val sourceDB: MXSourceDB,
    private val sourceGroup: SourceGroup
) : MXBaseObservable() {
    var sourceScanner: MXSourceScanner? = null
    fun startScan() {
        thread {
            MXLog.log("扫描目录")
            sourceScanner?.cancel()
            val scanner = MXSourceScanner(
                context, sourceDB,
                builder.getPickerType(), sourceGroup
            )
            scanner.setScanUpdate { notifyChanged() }
            sourceScanner = scanner
            scanner.startScan()
        }
    }

    fun destroy() {
        sourceScanner?.cancel()
        sourceScanner = null
        deleteObservers()
    }
}