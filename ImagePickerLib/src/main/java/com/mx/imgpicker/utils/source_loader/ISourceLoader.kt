package com.mx.imgpicker.utils.source_loader

import com.mx.imgpicker.models.Item

interface ISourceLoader {
    fun scan(): List<Item>
}