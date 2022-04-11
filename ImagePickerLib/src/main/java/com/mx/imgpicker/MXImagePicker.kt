package com.mx.imgpicker

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.mx.imgpicker.app.MXImgPickerActivity
import com.mx.imgpicker.app.MXImgShowActivity
import com.mx.imgpicker.models.MXItem
import com.mx.imgpicker.utils.MXUtils
import java.util.concurrent.atomic.AtomicBoolean

object MXImagePicker {
    private val hasInit = AtomicBoolean(false)
    private var application: Application? = null
    internal fun init(application: Application) {
        if (hasInit.get()) return
        application.registerActivityLifecycleCallbacks(activityLifecycleCall)
        hasInit.set(true)
        this.application = application
    }

    internal fun getContext() = application!!

    private var _imageLoader: ((item: MXItem, imageView: ImageView) -> Unit)? =
        null // 图片加载器
    private var _activityCall: ((AppCompatActivity) -> Unit)? = null // Activity在创建时会回调这个，可以设置样式等

    /**
     * 注册图片显示加载器，默认使用Glide
     */
    fun registerImageLoader(iImageLoader: ((item: MXItem, imageView: ImageView) -> Unit)) {
        this._imageLoader = iImageLoader
    }

    internal fun getImageLoader() = _imageLoader

    fun setDebug(debug: Boolean) {
        MXUtils.setDebug(debug)
    }

    /**
     * 注册全局页面启动回调
     */
    fun registerActivityCallback(call: ((activity: AppCompatActivity) -> Unit)) {
        _activityCall = call
    }

    private val activityLifecycleCall = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        }

        override fun onActivityStarted(p0: Activity) {
            if (p0 is MXImgPickerActivity) {
                _activityCall?.invoke(p0)
            } else if (p0 is MXImgShowActivity) {
                _activityCall?.invoke(p0)
            }
        }

        override fun onActivityResumed(p0: Activity) {
        }

        override fun onActivityPaused(p0: Activity) {
        }

        override fun onActivityStopped(p0: Activity) {
        }

        override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        }

        override fun onActivityDestroyed(p0: Activity) {
        }

    }
}


