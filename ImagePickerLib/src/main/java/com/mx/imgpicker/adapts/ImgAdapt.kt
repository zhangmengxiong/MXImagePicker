package com.mx.imgpicker.adapts

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.mx.imgpicker.ImagePickerService
import com.mx.imgpicker.R
import com.mx.imgpicker.models.ImageItem
import com.mx.imgpicker.models.PickerType
import com.mx.imgpicker.views.PickerTextView

class ImgAdapt(val list: ArrayList<ImageItem> = ArrayList()) : RecyclerView.Adapter<ImgAdapt.ImgVH>() {
    var maxSelectSize = 9
    val selectList = ArrayList<ImageItem>()
    var onSelectChange: ((list: ArrayList<ImageItem>) -> Unit)? = null
    var onItemClick: ((item: ImageItem, list: ArrayList<ImageItem>) -> Unit)? = null
    var onTakePictureClick: (() -> Unit)? = null

    class ImgVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.img)
        val selectBG: ImageView = itemView.findViewById(R.id.selectBG)
        val videoTag: ImageView = itemView.findViewById(R.id.videoTag)
        val indexTxv: PickerTextView = itemView.findViewById(R.id.indexTxv)
        val indexLay: RelativeLayout = itemView.findViewById(R.id.indexLay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImgVH {
        return ImgVH(LayoutInflater.from(parent.context).inflate(R.layout.adapt_img_item, parent, false))
    }

    override fun onBindViewHolder(holder: ImgVH, position: Int) {
        if (position == 0) {
            holder.selectBG.visibility = View.GONE
            holder.indexTxv.visibility = View.GONE
            holder.videoTag.visibility = View.GONE
            holder.img.setImageResource(R.drawable.icon_picker_camera)
            holder.img.setColorFilter(holder.itemView.context.resources.getColor(R.color.picker_color_important))
            holder.itemView.setOnClickListener { onTakePictureClick?.invoke() }
        } else {
            holder.img.setColorFilter(Color.TRANSPARENT)
            holder.indexTxv.visibility = View.VISIBLE
            holder.selectBG.visibility = View.VISIBLE
            val item = list.getOrNull(position - 1) ?: return
            ImagePickerService.getImageLoader().displayImage(item, holder.img)
            val isSelect = selectList.contains(item)
            val index = selectList.indexOf(item)
            holder.indexTxv.isChecked = isSelect

            if (item.type == PickerType.Image) {
                holder.videoTag.visibility = View.GONE
            } else {
                holder.videoTag.visibility = View.VISIBLE
            }

            if (isSelect) {
                holder.selectBG.alpha = 1f
                holder.indexTxv.text = (index + 1).toString()
                holder.indexLay.setOnClickListener {
                    selectList.remove(item)
                    notifyDataSetChanged()
                    onSelectChange?.invoke(ArrayList(selectList))
                }
            } else {
                holder.selectBG.alpha = 0.2f
                holder.indexTxv.text = ""
                holder.indexLay.setOnClickListener {
                    if (selectList.size >= maxSelectSize) {
                        Toast.makeText(it.context, "您最多只能选择${maxSelectSize}张图片！", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    selectList.add(item)
                    notifyItemChanged(position)
                    onSelectChange?.invoke(ArrayList(selectList))
                }
            }
            holder.itemView.setOnClickListener { onItemClick?.invoke(item, ArrayList(selectList)) }
        }
    }

    override fun getItemCount(): Int {
        return list.size + 1
    }
}