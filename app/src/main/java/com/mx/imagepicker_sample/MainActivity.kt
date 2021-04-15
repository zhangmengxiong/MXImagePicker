package com.mx.imagepicker_sample

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gyf.immersionbar.ImmersionBar
import com.mx.imgpicker.builder.PickerBuilder
import com.mx.imgpicker.models.MXPickerType
import com.mx.starter.MXStarter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.imageBtn).setOnClickListener {
            PermissionUtil.requestPermission(
                this, arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) { success ->
                if (success) {
                    MXStarter.start(
                        this,
                        PickerBuilder().setMaxSize(9).setCameraEnable(true)
                            .setActivityCallback { activity ->
                                ImmersionBar.with(activity)
                                    .autoDarkModeEnable(true)
                                    .statusBarColorInt(activity.resources.getColor(R.color.picker_color_background))
                                    .fitsSystemWindows(true)
                                    .navigationBarColor(R.color.picker_color_background)
                                    .init()
                            }.createIntent(this)
                    ) { resultCode, data ->
                        val list = PickerBuilder.getPickerResult(data) ?: return@start
                        Toast.makeText(this, list.joinToString(","), Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
        findViewById<View>(R.id.videoBtn).setOnClickListener {
            PermissionUtil.requestPermission(
                this, arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) { success ->
                if (success) {
                    MXStarter.start(
                        this,
                        PickerBuilder().setMaxSize(3).setMaxVideoLength(15)
                            .setActivityCallback { activity ->
                                ImmersionBar.with(activity)
                                    .autoDarkModeEnable(true)
                                    .statusBarColorInt(activity.resources.getColor(R.color.picker_color_background))
                                    .fitsSystemWindows(true)
                                    .navigationBarColor(R.color.picker_color_background)
                                    .init()
                            }.setType(MXPickerType.Video).createIntent(this)
                    ) { resultCode, data ->
                        val list = PickerBuilder.getPickerResult(data) ?: return@start
                        Toast.makeText(this, list.joinToString(","), Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }
}