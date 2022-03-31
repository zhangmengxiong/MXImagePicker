package com.mx.imgpicker.adapts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import com.mx.imgpicker.MXImagePicker
import com.mx.imgpicker.R
import com.mx.imgpicker.models.ItemSelectCall
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.models.SourceGroup
import com.mx.imgpicker.utils.MXFileBiz
import com.mx.imgpicker.utils.MXPickerFormatBiz
import com.mx.imgpicker.views.MXPickerTextView

internal class ImgLargeAdapt(
    private val activity: AppCompatActivity,
    private val sourceGroup: SourceGroup
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var onSelectChange: ItemSelectCall? = null

    class ImgScanVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView: PhotoView = itemView.findViewById(R.id.photoView)
        val indexTxv: MXPickerTextView = itemView.findViewById(R.id.indexTxv)
        val indexLay: RelativeLayout = itemView.findViewById(R.id.indexLay)
    }

    class ImgScanVideoVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.img)
        val indexTxv: MXPickerTextView = itemView.findViewById(R.id.indexTxv)
        val indexLay: RelativeLayout = itemView.findViewById(R.id.indexLay)
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
        val item = sourceGroup.getItem(position) ?: return
        if (holder is ImgScanVH) {
            holder.photoView.setImageResource(R.drawable.mx_icon_picker_image_place_holder)
            MXImagePicker.getImageLoader()?.invoke(activity, item, holder.photoView)
            val isSelect = sourceGroup.selectList.contains(item)
            val index = sourceGroup.selectList.indexOf(item)
            holder.indexTxv.isChecked = isSelect

            holder.indexLay.setOnClickListener {
                onSelectChange?.select(item)
            }
            if (isSelect) {
                holder.indexTxv.text = (index + 1).toString()
            } else {
                holder.indexTxv.text = ""
            }
        } else if (holder is ImgScanVideoVH) {
            holder.img.setImageResource(R.drawable.mx_icon_picker_image_place_holder)
            MXImagePicker.getImageLoader()?.invoke(activity, item, holder.img)
            val isSelect = sourceGroup.selectList.contains(item)
            val index = sourceGroup.selectList.indexOf(item)
            holder.indexTxv.isChecked = isSelect
            holder.videoLengthTxv.text =
                if (item.duration > 0) MXPickerFormatBiz.timeToString(item.duration) else ""

            holder.indexLay.setOnClickListener {
                onSelectChange?.select(item)
            }
            holder.playBtn.setOnClickListener {
                MXFileBiz.openItem(it.context, item)
            }
            if (isSelect) {
                holder.indexTxv.text = (index + 1).toString()
            } else {
                holder.indexTxv.text = ""
            }
        }
    }

    override fun getItemCount(): Int {
        return sourceGroup.getItemSize()
    }

    override fun getItemViewType(position: Int): Int {
        val item = sourceGroup.getItem(position)
        return if (item?.type == MXPickerType.Video) 0 else 1
    }
}