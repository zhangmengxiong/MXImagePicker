package com.mx.imagepicker_sample

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gyf.immersionbar.ImmersionBar
import com.mx.imgpicker.ImagePickerService
import com.mx.imgpicker.app.show.ImgShowActivity
import com.mx.imgpicker.builder.MXCaptureBuilder
import com.mx.imgpicker.builder.MXPickerBuilder
import com.mx.imgpicker.models.MXPickerType
import com.mx.starter.MXStarter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        PermissionUtil.requestPermission(
            this, arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) { success ->
            if (!success) finish()
        }
        ImagePickerService.registerActivityCallback { activity ->
            ImmersionBar.with(activity)
                .autoDarkModeEnable(true)
                .statusBarColorInt(activity.resources.getColor(R.color.picker_color_background))
                .fitsSystemWindows(true)
                .navigationBarColor(R.color.picker_color_background)
                .init()
        }

        findViewById<View>(R.id.imageBtn).setOnClickListener {
            MXStarter.start(
                this,
                MXPickerBuilder().setMaxSize(9).setCameraEnable(true).createIntent(this)
            ) { resultCode, data ->
                val list = MXPickerBuilder.getPickerResult(data)
                Toast.makeText(this, list.joinToString(","), Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<View>(R.id.videoBtn).setOnClickListener {
            MXStarter.start(
                this,
                MXPickerBuilder().setMaxSize(3).setMaxVideoLength(15)
                    .setType(MXPickerType.Video)
                    .createIntent(this)
            ) { resultCode, data ->
                val list = MXPickerBuilder.getPickerResult(data) ?: return@start
                Toast.makeText(this, list.joinToString(","), Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<View>(R.id.imageCapBtn).setOnClickListener {
            val builder = MXCaptureBuilder().setType(MXPickerType.Image)
            MXStarter.start(this, builder.createIntent(this)) { resultCode, data ->
                Toast.makeText(this, builder.getCaptureFile().absolutePath, Toast.LENGTH_SHORT)
                    .show()
            }
        }
        findViewById<View>(R.id.videoCapBtn).setOnClickListener {
            val builder = MXCaptureBuilder().setType(MXPickerType.Video).setMaxVideoLength(10)
            MXStarter.start(this, builder.createIntent(this)) { resultCode, data ->
                Toast.makeText(this, builder.getCaptureFile().absolutePath, Toast.LENGTH_SHORT)
                    .show()
            }
        }
        findViewById<View>(R.id.showImgsBtn).setOnClickListener {
            ImgShowActivity.open(
                this, arrayListOf(
                    "http://videos.jzvd.org/v/饺子主动.jpg",
                    "http://videos.jzvd.org/v/饺子运动.jpg",
                    "http://videos.jzvd.org/v/饺子有活.jpg",
                    "http://videos.jzvd.org/v/饺子星光.jpg",
                    "http://videos.jzvd.org/v/饺子想吹.jpg",
                    "http://videos.jzvd.org/v/饺子汪汪.jpg",
                    "http://videos.jzvd.org/v/饺子偷人.jpg",
                    "http://videos.jzvd.org/v/饺子跳.jpg",
                    "http://videos.jzvd.org/v/饺子受不了.jpg",
                    "http://videos.jzvd.org/v/饺子三位.jpg",
                    "http://videos.jzvd.org/v/饺子起飞.jpg",
                    "http://videos.jzvd.org/v/饺子你听.jpg",
                    "http://videos.jzvd.org/v/饺子可以了.jpg",
                    "http://videos.jzvd.org/v/饺子还小.jpg",
                    "http://videos.jzvd.org/v/饺子高冷.jpg",
                    "http://videos.jzvd.org/v/饺子堵住了.jpg",
                    "http://videos.jzvd.org/v/饺子都懂.jpg",
                    "http://videos.jzvd.org/v/饺子打电话.jpg",
                    "http://videos.jzvd.org/v/饺子不服.jpg",
                    "http://videos.jzvd.org/v/饺子还年轻.jpg",
                    "http://videos.jzvd.org/v/饺子好妈妈.jpg",
                    "http://videos.jzvd.org/v/饺子可以.jpg",
                    "http://videos.jzvd.org/v/饺子挺住.jpg",
                    "http://videos.jzvd.org/v/饺子想听.jpg",
                    "http://videos.jzvd.org/v/饺子真会.jpg",
                    "http://videos.jzvd.org/v/饺子真萌.jpg"
                ), "图片详情"
            )
        }
    }
}