package com.mx.imgpicker.adapts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import com.mx.imgpicker.ImagePickerService
import com.mx.imgpicker.R
import com.mx.imgpicker.models.ImageItem
import com.mx.imgpicker.views.PickerTextView

class ImgLargeAdapt(
    private val list: ArrayList<ImageItem>,
    private val selectList: ArrayList<ImageItem>
) : RecyclerView.Adapter<ImgLargeAdapt.ImgScanVH>() {
    var maxSelectSize = 9
    var onSelectChange: ((item: ImageItem) -> Unit)? = null

    class ImgScanVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView: PhotoView = itemView.findViewById(R.id.photoView)
        val indexTxv: PickerTextView = itemView.findViewById(R.id.indexTxv)
        val indexLay: RelativeLayout = itemView.findViewById(R.id.indexLay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImgScanVH {
        return ImgScanVH(
            LayoutInflater.from(parent.context).inflate(R.layout.adapt_img_scan_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ImgScanVH, position: Int) {
        val item = list.getOrNull(position) ?: return
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
    }

    override fun getItemCount(): Int {
        return list.size
    }
}