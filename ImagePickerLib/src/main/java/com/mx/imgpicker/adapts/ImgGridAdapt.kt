package com.mx.imgpicker.adapts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mx.imgpicker.R
import com.mx.imgpicker.app.picker.MXPickerVM
import com.mx.imgpicker.models.MXItem
import com.mx.imgpicker.views.MXAdaptItemView

internal class ImgGridAdapt(
    private val vm: MXPickerVM,
    val imgList: ArrayList<MXItem> = ArrayList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var onSelectClick: ((item: MXItem, isSelect: Boolean) -> Unit)? = null
    var onItemClick: ((item: MXItem, list: List<MXItem>) -> Unit)? = null
    var onTakePictureClick: (() -> Unit)? = null

    class CameraVH(itemView: View) : RecyclerView.ViewHolder(itemView)
    class ImgVH(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) CameraVH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.mx_picker_adapt_img_camera, parent, false)
        ) else ImgVH(
            MXAdaptItemView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
            }
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CameraVH) {
            holder.itemView.setOnClickListener { onTakePictureClick?.invoke() }
        } else if (holder is ImgVH) {
            val position = if (vm.enableCamera) (position - 1) else position
            val item = imgList.getOrNull(position) ?: return
            val selectIndex = vm.selectMediaList.indexOf(item)
            (holder.itemView as MXAdaptItemView).setData(item, selectIndex, onSelectClick)

            holder.itemView.setOnClickListener {
                onItemClick?.invoke(item, vm.selectMediaList)
            }
        }
    }

    override fun getItemCount(): Int {
        val size = imgList.size
        return if (vm.enableCamera) size + 1 else size
    }

    override fun getItemViewType(position: Int): Int {
        return if (vm.enableCamera && position == 0) 0 else 1
    }
}