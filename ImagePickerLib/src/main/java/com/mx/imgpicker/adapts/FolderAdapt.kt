package com.mx.imgpicker.adapts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mx.imgpicker.ImagePickerService
import com.mx.imgpicker.R
import com.mx.imgpicker.models.FolderItem

class FolderAdapt(val list: ArrayList<FolderItem> = ArrayList()) : RecyclerView.Adapter<FolderAdapt.FolderVH>() {
    var selectItem: FolderItem? = null
    var onItemClick: ((item: FolderItem) -> Unit)? = null

    class FolderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.img)
        val folderNameTxv: TextView = itemView.findViewById(R.id.folderNameTxv)
        val imgSizeTxv: TextView = itemView.findViewById(R.id.imgSizeTxv)
        val selectTag: ImageView = itemView.findViewById(R.id.selectTag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderVH {
        return FolderVH(LayoutInflater.from(parent.context).inflate(R.layout.adapt_folder_item, parent, false))
    }

    override fun onBindViewHolder(holder: FolderVH, position: Int) {
        val item = list.getOrNull(position) ?: return
        val isSelect = item.name == selectItem?.name
        item.images.firstOrNull()?.let {
            ImagePickerService.getImageLoader().displayImage(it, holder.img)
        }
        holder.folderNameTxv.text = item.name
        holder.imgSizeTxv.text = "(${item.images.size})"
        holder.selectTag.visibility = if (isSelect) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}