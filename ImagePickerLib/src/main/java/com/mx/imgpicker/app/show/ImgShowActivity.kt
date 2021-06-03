package com.mx.imgpicker.app.show

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.mx.imgpicker.ImagePickerService
import com.mx.imgpicker.R
import com.mx.imgpicker.adapts.ImgShowAdapt
import com.mx.imgpicker.models.Item
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.source_loader.MXImageSource

class ImgShowActivity : AppCompatActivity() {
    companion object {
        private const val EXTRAS_LIST = "EXTRAS_LIST"
        private const val EXTRAS_TITLE = "EXTRAS_TITLE"
        fun open(context: Context, list: List<String>, title: String? = null) {
            context.startActivity(
                Intent(context, ImgShowActivity::class.java)
                    .putExtra(EXTRAS_LIST, ArrayList(list))
                    .putExtra(EXTRAS_TITLE, title)
            )
        }
    }

    private val imgList = ArrayList<Item>()
    private val adapt = ImgShowAdapt(imgList)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_img_show)
        try {
            val list = intent.getSerializableExtra(EXTRAS_LIST) as ArrayList<String>
            imgList.addAll(list.map {
                Item(it, Uri.parse(it), MXImageSource.MIME_TYPE, 0L, "", MXPickerType.Image)
            })
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
            return
        }
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
        adapt.notifyDataSetChanged()
    }

    private fun initIntent() {
        ImagePickerService.getGlobalActivityCall()?.invoke(this)
    }
}