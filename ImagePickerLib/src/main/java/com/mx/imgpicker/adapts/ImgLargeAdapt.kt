package com.mx.imgpicker.adapts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import com.mx.imgpicker.MXImagePicker
import com.mx.imgpicker.R
import com.mx.imgpicker.models.MXDataSet
import com.mx.imgpicker.models.MXItem
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.MXFileBiz
import com.mx.imgpicker.utils.MXUtils

internal class ImgLargeAdapt(private val list: List<MXItem>, private val data: MXDataSet) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class ImgScanVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView: PhotoView = itemView.findViewById(R.id.photoView)
    }

    class ImgScanVideoVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.img)
        val playBtn: ImageView = itemView.findViewById(R.id.playBtn)
        val videoLengthTxv: TextView = itemView.findViewById(R.id.videoLengthTxv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                ImgScanVideoVH(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.mx_picker_adapt_img_scan_video_item, parent, false)
                )
            }
            else -> {
                ImgScanVH(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.mx_picker_adapt_img_scan_item, parent, false)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list.getOrNull(position) ?: return
        if (holder is ImgScanVH) {
            holder.photoView.setImageResource(R.drawable.mx_icon_picker_image_place_holder)
            MXImagePicker.getImageLoader()?.invoke(item, holder.photoView)
        } else if (holder is ImgScanVideoVH) {
            holder.img.setImageResource(R.drawable.mx_icon_picker_image_place_holder)
            MXImagePicker.getImageLoader()?.invoke(item, holder.img)
            holder.videoLengthTxv.text =
                if (item.duration > 0) MXUtils.timeToString(item.duration) else ""

            holder.playBtn.setOnClickListener {
                MXFileBiz.openItem(it.context, item)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemViewType(position: Int): Int {
        val item = list.getOrNull(position)
        return if (item?.type == MXPickerType.Video) 0 else 1
    }
}