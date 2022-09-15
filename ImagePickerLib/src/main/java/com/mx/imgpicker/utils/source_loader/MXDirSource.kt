package com.mx.imgpicker.utils.source_loader

import android.content.Context
import com.mx.imgpicker.models.MXDirItem
import com.mx.imgpicker.models.MXItem
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.MXUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class MXDirSource(private val dirs: List<MXDirItem>) : IMXSource {
    override suspend fun scan(
        context: Context,
        pageSize: Int,
        onScanCall: (List<MXItem>) -> Boolean
    ) = withContext(Dispatchers.IO) {
        if (dirs.isEmpty()) return@withContext
        val list = ArrayList<MXItem>()
        for (dir in dirs) {
            val files = File(dir.path).listFiles()
            if (files == null || files.isEmpty()) continue
            for (file in files) {
                val ext = file.extension?.lowercase()
                if (ext in MXUtils.IMAGE_EXT) {
                    list.add(
                        MXItem(
                            file.absolutePath,
                            file.lastModified(),
                            MXPickerType.Image
                        )
                    )
                } else if (ext in MXUtils.VIDEO_EXT) {
                    list.add(
                        MXItem(
                            file.absolutePath,
                            file.lastModified(),
                            MXPickerType.Video
                        )
                    )
                }
                if (list.size >= pageSize) {
                    val continueScan = onScanCall.invoke(list.toList())
                    list.clear()
                    if (!continueScan) {
                        return@withContext
                    }
                }
            }
        }
        if (list.isNotEmpty()) {
            onScanCall.invoke(list.toList())
        }
    }

    override fun save(context: Context, file: File): Boolean {
        return false
    }
}