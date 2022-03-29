package com.mx.imgpicker.adapts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import com.mx.imgpicker.ImagePickerService
import com.mx.imgpicker.R
import com.mx.imgpicker.models.Item

internal class ImgShowAdapt(private val list: ArrayList<Item>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ImgShowVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView: PhotoView = itemView as PhotoView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ImgShowVH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.adapt_img_show_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list.getOrNull(position) ?: return
        if (holder is ImgShowVH) {
            ImagePickerService.getImageLoader().displayImage(item, holder.photoView)

        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}