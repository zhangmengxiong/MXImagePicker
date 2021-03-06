package com.mx.imagepicker_sample

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.gyf.immersionbar.ImmersionBar
import com.mx.imgpicker.MXImagePicker
import com.mx.imgpicker.app.MXImgShowActivity
import com.mx.imgpicker.builder.MXCaptureBuilder
import com.mx.imgpicker.builder.MXPickerBuilder
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.compress.MXImageCompress
import com.mx.starter.MXStarter
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MXImagePicker.setDebug(true)
        MXImagePicker.registerImageLoader { item, imageView ->
            if (File(item.path).exists()) {
                Glide.with(imageView).load(File(item.path))
                    .placeholder(R.drawable.mx_icon_picker_image_place_holder).into(imageView)
            } else if (item.path.startsWith("http")) {
                Glide.with(imageView).load(item.path)
                    .placeholder(R.drawable.mx_icon_picker_image_place_holder).into(imageView)
            } else {
                Glide.with(imageView).load(Uri.parse(item.path))
                    .placeholder(R.drawable.mx_icon_picker_image_place_holder).into(imageView)
            }
        }

        MXImagePicker.registerActivityCallback { activity ->
            ImmersionBar.with(activity)
                .autoDarkModeEnable(true)
                .statusBarColorInt(activity.resources.getColor(R.color.mx_picker_color_background))
                .fitsSystemWindows(true)
                .navigationBarColor(R.color.mx_picker_color_background)
                .init()
        }
        findViewById<View>(R.id.fixPickerBtn).setOnClickListener {
            MXStarter.start(
                this,
                MXPickerBuilder().setType(MXPickerType.ImageAndVideo).setMaxSize(9)
                    .setCameraEnable(true).createIntent(this)
            ) { resultCode, data ->
                val list = MXPickerBuilder.getPickerResult(data)
                MXImgShowActivity.open(this, list)
            }
        }
        findViewById<View>(R.id.imageBtn).setOnClickListener {
            MXStarter.start(
                this,
                MXPickerBuilder().setMaxSize(9).setCameraEnable(true).createIntent(this)
            ) { resultCode, data ->
                val list = MXPickerBuilder.getPickerResult(data)
                MXImgShowActivity.open(this, list)
            }
        }
        findViewById<View>(R.id.imageScaleBtn).setOnClickListener {
            MXStarter.start(
                this,
                MXPickerBuilder().setMaxSize(1).setCameraEnable(true).createIntent(this)
            ) { resultCode, data ->
                val path = MXPickerBuilder.getPickerResult(data).firstOrNull() ?: return@start
                val scaleImg = MXImageCompress.from(this)
                    .setCacheDir(applicationContext.cacheDir) // ????????????
                    .setSupportAlpha(true) // ??????????????????(???.png?????????) ??????=???.jpg?????????
                    .setIgnoreFileSize(50) // ???????????????????????????????????????????????????
                    .compress(path).absolutePath
                MXImgShowActivity.open(this, listOf(path, scaleImg))
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
                MXImgShowActivity.open(this, list)
            }
        }
        findViewById<View>(R.id.imageCapBtn).setOnClickListener {
            PermissionUtil.requestPermission(
                this, arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) { agree ->
                if (!agree) return@requestPermission
                val builder = MXCaptureBuilder().setType(MXPickerType.Image)
                MXStarter.start(this, builder.createIntent(this)) { resultCode, data ->
                    if (!builder.getCaptureFile().exists()) return@start
                    MXImgShowActivity.open(this, listOf(builder.getCaptureFile().absolutePath))
                }
            }
        }
        findViewById<View>(R.id.videoCapBtn).setOnClickListener {
            PermissionUtil.requestPermission(
                this, arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) { agree ->
                if (!agree) return@requestPermission
                val builder = MXCaptureBuilder().setType(MXPickerType.Video).setMaxVideoLength(10)
                MXStarter.start(this, builder.createIntent(this)) { resultCode, data ->
                    if (!builder.getCaptureFile().exists()) return@start
                    MXImgShowActivity.open(this, listOf(builder.getCaptureFile().absolutePath))
                }
            }
        }
        findViewById<View>(R.id.showImgsBtn).setOnClickListener {
            MXImgShowActivity.open(
                this, arrayListOf(
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/?????????.jpg",
                    "http://8.136.101.204/v/???????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/???????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/???????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/???????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/???????????????.jpg",
                    "http://8.136.101.204/v/???????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg",
                    "http://8.136.101.204/v/????????????.jpg"
                ), "????????????"
            )
        }
    }
}