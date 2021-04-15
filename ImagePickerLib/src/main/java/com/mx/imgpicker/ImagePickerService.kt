package com.mx.imgpicker

import androidx.appcompat.app.AppCompatActivity
import com.mx.imgpicker.factory.DefaultUriToFile
import com.mx.imgpicker.factory.GlideImageLoader
import com.mx.imgpicker.factory.IMXImageLoader
import com.mx.imgpicker.factory.IUriToFile

object ImagePickerService {
    private var _imageLoader: IMXImageLoader? = null // 图片加载器
    private var _iUriToFile: IUriToFile? = null // Uri解析器
    private var _activityCall: ((AppCompatActivity) -> Unit)? = null // Activity在创建时会回调这个，可以设置样式等

    fun getImageLoader(): IMXImageLoader {
        var loader = _imageLoader
        if (loader == null) {
            loader = GlideImageLoader()
            _imageLoader = loader
        }
        return loader
    }

    /**
     * 注册图片显示加载器，默认使用Glide
     */
    fun registerImageLoader(iImageLoader: IMXImageLoader) {
        this._imageLoader = iImageLoader
    }

    /**
     * 注册文件选择器解析
     */
    fun registerUriToFile(iUriToFile: IUriToFile) {
        this._iUriToFile = iUriToFile
    }

    fun registerActivityCallback(call: ((activity: AppCompatActivity) -> Unit)) {
        _activityCall = call
    }

    fun getGlobalActivityCall() = _activityCall

    fun getUriToFile(): IUriToFile {
        var loader = _iUriToFile
        if (loader == null) {
            loader = DefaultUriToFile()
            _iUriToFile = loader
        }
        return loader
    }
}


