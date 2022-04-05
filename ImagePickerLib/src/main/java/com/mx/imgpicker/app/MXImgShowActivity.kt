package com.mx.imgpicker.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.mx.imgpicker.MXImagePicker
import com.mx.imgpicker.R
import com.mx.imgpicker.adapts.ImgShowAdapt
import com.mx.imgpicker.models.MXItem
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.MXUtils

class MXImgShowActivity : AppCompatActivity() {
    companion object {
        private const val EXTRAS_LIST = "EXTRAS_LIST"
        private const val EXTRAS_TITLE = "EXTRAS_TITLE"
        fun open(context: Context, list: List<String>, title: String? = null) {
            if (list.isEmpty()) return
            val list = list.map { MXItem(it, 0L, MXPickerType.Image) }
            context.startActivity(
                Intent(context, MXImgShowActivity::class.java)
                    .putExtra(EXTRAS_LIST, ArrayList(list))
                    .putExtra(EXTRAS_TITLE, title)
            )
        }
    }


    private val returnBtn by lazy { findViewById<View>(R.id.returnBtn) }
    private val recycleView by lazy { findViewById<RecyclerView>(R.id.recycleView) }
    private val titleTxv by lazy { findViewById<TextView>(R.id.titleTxv) }
    private val indexTxv by lazy { findViewById<TextView>(R.id.indexTxv) }

    private val imgList = ArrayList<MXItem>()
    private val adapt by lazy { ImgShowAdapt(imgList) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mx_picker_activity_img_show)
        MXImagePicker.init(application)

        initView()
        initIntent()
    }

    private fun initView() {
        returnBtn.setOnClickListener { onBackPressed() }
        titleTxv.text = intent.getStringExtra(EXTRAS_TITLE) ?: getString(R.string.mx_picker_string_show_list)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recycleView.layoutManager = layoutManager
        PagerSnapHelper().attachToRecyclerView(recycleView)
        recycleView.adapter = adapt
        recycleView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val index = layoutManager.findFirstVisibleItemPosition()
                    if (index >= 0) {
                        indexTxv.text = "${index + 1} / ${imgList.size}"
                    }
                }
            }
        })
    }

    private fun initIntent() {
        try {
            val list = intent.getSerializableExtra(EXTRAS_LIST) as ArrayList<MXItem>
            imgList.addAll(list)
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
            return
        }
        indexTxv.text = "1 / ${imgList.size}"
        MXUtils.log("显示图片：${imgList.joinToString(",") { it.path }}")
        adapt.notifyDataSetChanged()
    }
}