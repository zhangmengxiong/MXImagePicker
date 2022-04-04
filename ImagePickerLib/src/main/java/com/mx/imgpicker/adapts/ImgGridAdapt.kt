package com.mx.imgpicker.adapts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mx.imgpicker.MXImagePicker
import com.mx.imgpicker.R
import com.mx.imgpicker.models.MXConfig
import com.mx.imgpicker.models.MXDataSet
import com.mx.imgpicker.models.MXItem
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.MXUtils
import com.mx.imgpicker.views.MXPickerTextView

internal class ImgGridAdapt(
    private val data: MXDataSet,
    private val config: MXConfig
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var onSelectClick: ((item: MXItem, isSelect: Boolean) -> Unit)? = null
    var onItemClick: ((item: MXItem, list: List<MXItem>) -> Unit)? = null
    var onTakePictureClick: (() -> Unit)? = null

    class ImgVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.img)
        val selectBG: ImageView = itemView.findViewById(R.id.selectBG)
        val videoTag: View = itemView.findViewById(R.id.videoTag)
        val videoLengthTxv: TextView = itemView.findViewById(R.id.videoLengthTxv)
        val indexTxv: MXPickerTextView = itemView.findViewById(R.id.indexTxv)
        val indexLay: RelativeLayout = itemView.findViewById(R.id.indexLay)
    }

    class CameraVH(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) CameraVH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.mx_picker_adapt_img_camera, parent, false)
        ) else ImgVH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.mx_picker_adapt_img_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CameraVH) {
            holder.itemView.setOnClickListener { onTakePictureClick?.invoke() }
        } else if (holder is ImgVH) {
            holder.indexTxv.visibility = View.VISIBLE
            holder.selectBG.visibility = View.VISIBLE
            val position = if (config.enableCamera) (position - 1) else position
            val item = data.getItem(position) ?: return
            holder.img.setImageResource(R.drawable.mx_icon_picker_image_place_holder)
            MXImagePicker.getImageLoader()?.invoke(item, holder.img)
            val index = data.selectList.getValue().indexOf(item)
            val isSelect = index >= 0
            holder.indexTxv.isChecked = isSelect

            if (item.type == MXPickerType.Video) {
                holder.videoTag.visibility = View.VISIBLE
                holder.videoLengthTxv.text = MXUtils.timeToString(item.duration)
            } else {
                holder.videoTag.visibility = View.GONE
            }
            holder.indexLay.setOnClickListener {
                onSelectClick?.invoke(item, isSelect)
            }
            if (isSelect) {
                holder.selectBG.alpha = 1f
                holder.indexTxv.text = (index + 1).toString()
            } else {
                holder.selectBG.alpha = 0.2f
                holder.indexTxv.text = ""
            }
            holder.itemView.setOnClickListener {
                onItemClick?.invoke(item, data.selectList.getValue())
            }
        }
    }

    override fun getItemCount(): Int {
        val size = data.getItemSize()
        return if (config.enableCamera) size + 1 else size
    }

    override fun getItemViewType(position: Int): Int {
        return if (config.enableCamera && position == 0) 0 else 1
    }
}