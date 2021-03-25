package com.mx.imgpicker.adapts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import com.mx.imgpicker.ImagePickerService
import com.mx.imgpicker.R
import com.mx.imgpicker.builder.PickerBuilder
import com.mx.imgpicker.models.Item
import com.mx.imgpicker.models.PickerSelectCall
import com.mx.imgpicker.models.PickerType
import com.mx.imgpicker.utils.ImagePathBiz
import com.mx.imgpicker.utils.PickerFormatBiz
import com.mx.imgpicker.views.PickerTextView

class ImgLargeAdapt(
    private val list: ArrayList<Item>,
    private val selectList: ArrayList<Item>,
    builder: PickerBuilder
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var onSelectChange: PickerSelectCall? = null

    class ImgScanVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView: PhotoView = itemView.findViewById(R.id.photoView)
        val indexTxv: PickerTextView = itemView.findViewById(R.id.indexTxv)
        val indexLay: RelativeLayout = itemView.findViewById(R.id.indexLay)
    }

    class ImgScanVideoVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.img)
        val indexTxv: PickerTextView = itemView.findViewById(R.id.indexTxv)
        val indexLay: RelativeLayout = itemView.findViewById(R.id.indexLay)
        val playBtn: ImageView = itemView.findViewById(R.id.playBtn)
        val videoLengthTxv: TextView = itemView.findViewById(R.id.videoLengthTxv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                ImgScanVideoVH(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.adapt_img_scan_video_item, parent, false)
                )
            }
            else -> {
                ImgScanVH(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.adapt_img_scan_item, parent, false)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list.getOrNull(position) ?: return
        if (holder is ImgScanVH) {
            ImagePickerService.getImageLoader().displayImage(item, holder.photoView)
            val isSelect = selectList.contains(item)
            val index = selectList.indexOf(item)
            holder.indexTxv.isChecked = isSelect

            holder.indexLay.setOnClickListener {
                onSelectChange?.invoke(item)
            }
            if (isSelect) {
                holder.indexTxv.text = (index + 1).toString()
            } else {
                holder.indexTxv.text = ""
            }
        } else if (holder is ImgScanVideoVH) {
            ImagePickerService.getImageLoader().displayImage(item, holder.img)
            val isSelect = selectList.contains(item)
            val index = selectList.indexOf(item)
            holder.indexTxv.isChecked = isSelect
            holder.videoLengthTxv.text =
                if (item.duration > 0) PickerFormatBiz.timeToString(item.duration) else ""

            holder.indexLay.setOnClickListener {
                onSelectChange?.invoke(item)
            }
            holder.playBtn.setOnClickListener {
                ImagePathBiz.openItem(it.context, item)
            }
            if (isSelect) {
                holder.indexTxv.text = (index + 1).toString()
            } else {
                holder.indexTxv.text = ""
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemViewType(position: Int): Int {
        val item = list.getOrNull(position)
        return if (item?.type == PickerType.Video) 0 else 1
    }
}