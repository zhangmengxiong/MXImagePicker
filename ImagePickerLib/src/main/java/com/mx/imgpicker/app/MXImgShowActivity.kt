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
            context.startActivity(
                Intent(context, MXImgShowActivity::class.java)
                    .putExtra(EXTRAS_LIST, ArrayList(list))
                    .putExtra(EXTRAS_TITLE, title)
            )
        }
    }

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
        findViewById<View>(R.id.returnBtn)?.setOnClickListener { onBackPressed() }

        val recycleView = findViewById<RecyclerView>(R.id.recycleView)
        val titleTxv = findViewById<TextView>(R.id.titleTxv)
        titleTxv.text = intent.getStringExtra(EXTRAS_TITLE) ?: "图片查看"

        recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(recycleView)
        recycleView.adapter = adapt
    }

    private fun initIntent() {
        try {
            val list = intent.getSerializableExtra(EXTRAS_LIST) as ArrayList<String>
            imgList.addAll(list.map { path ->
                MXItem(path, 0L, MXPickerType.Image)
            })
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
            return
        }
        MXUtils.log("显示图片：${imgList.joinToString(",") { it.path }}")
        adapt.notifyDataSetChanged()
    }
}