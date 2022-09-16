package com.mx.imgpicker.adapts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.mx.imgpicker.MXImagePicker
import com.mx.imgpicker.R
import com.mx.imgpicker.app.picker.MXPickerVM
import com.mx.imgpicker.models.MXDirItem
import kotlinx.coroutines.launch

internal class FolderAdapt(
    private val vm: MXPickerVM,
    private val lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.Adapter<FolderAdapt.FolderVH>() {
    var onItemClick: ((item: MXDirItem) -> Unit)? = null

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
        val item = vm.dirList.getOrNull(position) ?: return
        val isSelect = (item.path == vm.selectDirLive.value?.path)

        if (item.lastItem == null) {
            lifecycleScope.launch {
                item.lastItem = vm.sourceDB.queryLastItem(item.path, vm.pickerType)
                this@FolderAdapt.notifyItemChanged(position)
            }
        }

        holder.img.setImageResource(R.drawable.mx_icon_picker_image_place_holder)
        item.lastItem?.let { imgItem ->
            MXImagePicker.getImageLoader()?.invoke(imgItem, holder.img)
        }
        holder.folderNameTxv.text = item.name
        holder.imgSizeTxv.text = "(${item.childSize})"
        holder.selectTag.visibility = if (isSelect) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }
    }

    override fun getItemCount(): Int {
        return vm.dirList.size
    }
}