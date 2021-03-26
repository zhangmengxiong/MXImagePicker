package com.mx.imgpicker.adapts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.mx.imgpicker.ImagePickerService
import com.mx.imgpicker.R
import com.mx.imgpicker.builder.PickerBuilder
import com.mx.imgpicker.models.Item
import com.mx.imgpicker.models.ItemSelectCall
import com.mx.imgpicker.models.PickerType
import com.mx.imgpicker.views.PickerTextView

class ImgGridAdapt(
    private val list: ArrayList<Item>,
    private val selectList: ArrayList<Item>,
    private val builder: PickerBuilder
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var onSelectClick: ItemSelectCall? = null
    var onItemClick: ((item: Item, list: ArrayList<Item>) -> Unit)? = null
    var onTakePictureClick: (() -> Unit)? = null

    class ImgVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.img)
        val selectBG: ImageView = itemView.findViewById(R.id.selectBG)
        val videoTag: ImageView = itemView.findViewById(R.id.videoTag)
        val indexTxv: PickerTextView = itemView.findViewById(R.id.indexTxv)
        val indexLay: RelativeLayout = itemView.findViewById(R.id.indexLay)
    }

    class CameraVH(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) CameraVH(
            LayoutInflater.from(parent.context).inflate(R.layout.adapt_img_camera, parent, false)
        ) else ImgVH(
            LayoutInflater.from(parent.context).inflate(R.layout.adapt_img_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CameraVH) {
            holder.itemView.setOnClickListener { onTakePictureClick?.invoke() }
        } else if (holder is ImgVH) {
            holder.indexTxv.visibility = View.VISIBLE
            holder.selectBG.visibility = View.VISIBLE
            val position = if (builder._enableCamera) position - 1 else position
            val item = list.getOrNull(position) ?: return
            ImagePickerService.getImageLoader().displayImage(item, holder.img)
            val isSelect = selectList.contains(item)
            val index = selectList.indexOf(item)
            holder.indexTxv.isChecked = isSelect

            if (item.type == PickerType.Image) {
                holder.videoTag.visibility = View.GONE
            } else {
                holder.videoTag.visibility = View.VISIBLE
            }
            holder.indexLay.setOnClickListener {
                onSelectClick?.select(item)
            }
            if (isSelect) {
                holder.selectBG.alpha = 1f
                holder.indexTxv.text = (index + 1).toString()
            } else {
                holder.selectBG.alpha = 0.2f
                holder.indexTxv.text = ""
            }
            holder.itemView.setOnClickListener { onItemClick?.invoke(item, ArrayList(selectList)) }
        }
    }

    override fun getItemCount(): Int {
        return if (builder._enableCamera) list.size + 1 else list.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (builder._enableCamera && position == 0) 0 else 1
    }
}