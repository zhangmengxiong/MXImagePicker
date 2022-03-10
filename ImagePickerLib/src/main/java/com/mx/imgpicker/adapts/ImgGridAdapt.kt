package com.mx.imgpicker.adapts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.mx.imgpicker.ImagePickerService
import com.mx.imgpicker.R
import com.mx.imgpicker.builder.MXPickerBuilder
import com.mx.imgpicker.models.Item
import com.mx.imgpicker.models.ItemSelectCall
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.models.SourceGroup
import com.mx.imgpicker.views.MXPickerTextView

class ImgGridAdapt(
    private val sourceGroup: SourceGroup,
    private val builder: MXPickerBuilder
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var onSelectClick: ItemSelectCall? = null
    var onItemClick: ((item: Item, list: ArrayList<Item>) -> Unit)? = null
    var onTakePictureClick: (() -> Unit)? = null

    class ImgVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.img)
        val selectBG: ImageView = itemView.findViewById(R.id.selectBG)
        val videoTag: ImageView = itemView.findViewById(R.id.videoTag)
        val indexTxv: MXPickerTextView = itemView.findViewById(R.id.indexTxv)
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
            val position = if (builder.isEnableCamera()) position - 1 else position
            val item = sourceGroup.getItem(position) ?: return
            ImagePickerService.getImageLoader().displayImage(item, holder.img)
            val isSelect = sourceGroup.selectList.contains(item)
            val index = sourceGroup.selectList.indexOf(item)
            holder.indexTxv.isChecked = isSelect

            if (item.type == MXPickerType.Video) {
                holder.videoTag.visibility = View.VISIBLE
            } else {
                holder.videoTag.visibility = View.GONE
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
            holder.itemView.setOnClickListener {
                onItemClick?.invoke(
                    item,
                    ArrayList(sourceGroup.selectList)
                )
            }
        }
    }

    override fun getItemCount(): Int {
        val size = sourceGroup.getItemSize()
        return if (builder.isEnableCamera()) size + 1 else size
    }

    override fun getItemViewType(position: Int): Int {
        return if (builder.isEnableCamera() && position == 0) 0 else 1
    }
}