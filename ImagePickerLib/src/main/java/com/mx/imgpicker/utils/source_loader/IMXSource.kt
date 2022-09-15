package com.mx.imgpicker.utils.source_loader

import android.content.Context
import com.mx.imgpicker.models.MXItem
import java.io.File

internal interface IMXSource {
    suspend fun scan(
        context: Context,
        pageSize: Int,
        onScanCall: ((List<MXItem>) -> Boolean)
    )

    fun save(context: Context, file: File): Boolean
}