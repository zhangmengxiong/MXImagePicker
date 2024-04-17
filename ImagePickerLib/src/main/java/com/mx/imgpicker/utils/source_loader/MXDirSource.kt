package com.mx.imgpicker.utils.source_loader

import android.content.Context
import com.mx.imgpicker.models.MXDirItem
import com.mx.imgpicker.models.MXItem
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.MXUtils
import java.io.File

internal class MXDirSource(private val dirs: List<MXDirItem>) : IMXSource {
    override fun scan(context: Context, size: Int, offset: Int): List<MXItem>? {
        if (dirs.isEmpty()) return null
        val list = ArrayList<MXItem>()
        var index = 0
        for (dir in dirs) {
            val files = File(dir.path).listFiles()?.sortedBy { it.name }
            if (files == null || files.isEmpty()) continue
            for (file in files) {
                if (!file.exists() || file.length() <= 0 || !file.canRead()) {
                    continue
                }
                val item = when (file.extension.lowercase()) {
                    in MXUtils.IMAGE_EXT -> {
                        MXItem(file.absolutePath, file.lastModified(), MXPickerType.Image)
                    }

                    in MXUtils.VIDEO_EXT -> {
                        MXItem(file.absolutePath, file.lastModified(), MXPickerType.Video)
                    }

                    else -> null
                }
                if (item != null) {
                    index++
                    if (index > offset) {
                        list.add(item)
                    }
                    if (list.size >= size) {
                        return list
                    }
                }
            }
        }
        return list
    }

    override fun save(context: Context, file: File): Boolean {
        return false
    }
}