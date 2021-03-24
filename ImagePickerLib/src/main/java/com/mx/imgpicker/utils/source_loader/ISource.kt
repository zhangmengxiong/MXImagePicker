package com.mx.imgpicker.utils.source_loader

import android.content.Context
import com.mx.imgpicker.models.Item
import java.io.File

interface ISource {
    fun scan(context: Context): List<Item>
    fun save(context: Context, file: File): Boolean
}