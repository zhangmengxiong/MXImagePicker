package com.mx.imgpicker.adapts

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

class ImgGridAdapt(
    private val list: ArrayList<ImageItem>,
    private val selectList: ArrayList<ImageItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var maxSelectSize = 9
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
                        Toast.makeText(
                            it.context,
                            "您最多只能选择${maxSelectSize}张图片！",
                            Toast.LENGTH_SHORT
                        ).show()
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

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) 0 else 1
    }
}