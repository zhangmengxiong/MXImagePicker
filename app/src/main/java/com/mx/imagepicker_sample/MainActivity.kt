package com.mx.imagepicker_sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.mx.imgpicker.builder.PickerBuilder
import com.mx.starter.MXStarter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.startBtn).setOnClickListener {
            MXStarter.start(
                this,
                PickerBuilder().setMaxSize(3).createIntent(this)
            ) { resultCode, data ->

            }
        }
    }
}