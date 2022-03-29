package com.mx.imgpicker.utils.source_loader

import android.content.Context
import com.mx.imgpicker.models.Item
import java.io.File

internal interface IMXSource {
    fun scan(context: Context, page: Int, pageSize: Int): List<Item>
    fun save(context: Context, file: File): Boolean
}