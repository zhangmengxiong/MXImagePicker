package com.mx.imgpicker.adapts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import com.mx.imgpicker.ImagePickerService
import com.mx.imgpicker.R
import com.mx.imgpicker.models.ImageItem

class ImgLargeAdapt(
    private val list: ArrayList<ImageItem>,
    private val selectList: ArrayList<ImageItem>
) : RecyclerView.Adapter<ImgLargeAdapt.ImgScanVH>() {
    var maxSelectSize = 9

    class ImgScanVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView: PhotoView = itemView.findViewById(R.id.photoView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImgScanVH {
        return ImgScanVH(
            LayoutInflater.from(parent.context).inflate(R.layout.adapt_img_scan_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ImgScanVH, position: Int) {
        val item = list.getOrNull(position) ?: return
        holder.photoView.setScale(1f, false)
        ImagePickerService.getImageLoader().displayImage(item, holder.photoView)
    }

    override fun getItemCount(): Int {
        return list.size
    }
}