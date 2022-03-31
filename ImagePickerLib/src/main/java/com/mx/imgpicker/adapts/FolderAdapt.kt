package com.mx.imgpicker.adapts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.mx.imgpicker.MXImagePicker
import com.mx.imgpicker.R
import com.mx.imgpicker.models.MXFolderItem
import com.mx.imgpicker.models.SourceGroup

internal class FolderAdapt(
    private val activity: AppCompatActivity,
    private val sourceGroup: SourceGroup
) : RecyclerView.Adapter<FolderAdapt.FolderVH>() {
    var onItemClick: ((item: MXFolderItem) -> Unit)? = null

    class FolderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.img)
        val folderNameTxv: TextView = itemView.findViewById(R.id.folderNameTxv)
        val imgSizeTxv: TextView = itemView.findViewById(R.id.imgSizeTxv)
        val selectTag: ImageView = itemView.findViewById(R.id.selectTag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderVH {
        return FolderVH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.mx_picker_adapt_folder_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: FolderVH, position: Int) {
        val item = sourceGroup.folderList?.getOrNull(position) ?: return
        val isSelect = (item.name == sourceGroup.selectFolder?.name)
        item.items.firstOrNull()?.let { imgItem ->
            holder.img.setImageResource(R.drawable.mx_icon_picker_image_place_holder)
            MXImagePicker.getImageLoader()?.invoke(activity, imgItem, holder.img)
        }
        holder.folderNameTxv.text = item.name
        holder.imgSizeTxv.text = "(${item.items.size})"
        holder.selectTag.visibility = if (isSelect) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }
    }

    override fun getItemCount(): Int {
        return sourceGroup.folderList?.size ?: 0
    }
}