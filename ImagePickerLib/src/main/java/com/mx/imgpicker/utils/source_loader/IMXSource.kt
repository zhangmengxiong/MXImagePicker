package com.mx.imgpicker.utils.source_loader

import android.content.Context
import com.mx.imgpicker.models.MXItem
import java.io.File

internal interface IMXSource {
    fun scan(
        context: Context,
        page: Int,
        pageSize: Int,
        minTime: Long?,
        maxTime: Long?
    ): List<MXItem>

    fun save(context: Context, file: File): Boolean
}