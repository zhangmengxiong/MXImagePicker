package com.mx.imgpicker.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.mx.imgpicker.MXImagePicker
import com.mx.imgpicker.R
import com.mx.imgpicker.app.fragment.MXFullScreenFragment
import com.mx.imgpicker.app.fragment.MXPickerFragment
import com.mx.imgpicker.builder.MXPickerBuilder
import com.mx.imgpicker.compress.MXImageCompress
import com.mx.imgpicker.models.*
import com.mx.imgpicker.observer.MXSysImageObserver
import com.mx.imgpicker.observer.MXSysVideoObserver
import com.mx.imgpicker.utils.MXUtils
import kotlin.concurrent.thread


class MXImgPickerActivity : AppCompatActivity() {
    private val config by lazy {
        (intent.getSerializableExtra(
            MXPickerBuilder.KEY_INTENT_BUILDER
        ) as? MXConfig) ?: MXConfig()
    }

    private val data = MXDataSet()
    private val source by lazy { MXSource(this, data, config.pickerType) }

    private var currentFragment: Fragment? = null
    private val pickerFragment by lazy { MXPickerFragment(data, source, config) }
    private val pickerFullScreenFragment by lazy { MXFullScreenFragment(data, source, config) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mx_picker_activity_img_picker)
        MXImagePicker.init(application)
        supportActionBar?.hide()
        actionBar?.hide()

        MXUtils.log("启动")
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!MXUtils.hasPermission(this, permissions)) {
            Toast.makeText(
                this,
                getString(R.string.mx_picker_string_need_permission_storage),
                Toast.LENGTH_SHORT
            ).show()
            MXUtils.requestPermission(this, permissions, MXUtils.REQUEST_CODE_READ)
            return
        } else {
            initView()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MXUtils.REQUEST_CODE_READ) {
            if (MXUtils.hasPermission(this, permissions)) {
                initView()
            } else {
                finish()
            }
        }
    }

    private fun initView() {
        gotoFragment(pickerFragment)

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            imageChangeObserver
        )
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            videoChangeObserver
        )
        data.folderList.addObserver { list ->
            val selectName = data.selectFolder.getValue()?.name
            val selectFolder = list.firstOrNull {
                it.name == selectName
            } ?: list.firstOrNull()
            data.selectFolder.notifyChanged(selectFolder)
            MXUtils.log("数据刷新：${data.getItemSize()}")
        }

        source.startScan()
    }

    fun showLargeView(show: Boolean, target: MXItem? = null) {
        if (show) {
            pickerFullScreenFragment.setItemList(data.selectFolder.getValue()?.items)
            pickerFullScreenFragment.setTargetItem(target)
            gotoFragment(pickerFullScreenFragment)
        } else {
            gotoFragment(pickerFragment)
        }
    }

    fun showLargeSelectView() {
        val list = data.selectList.getValue()
        if (list.isEmpty()) return

        pickerFullScreenFragment.setItemList(data.selectList.getValue())
        pickerFullScreenFragment.setTargetItem(list.firstOrNull())
        gotoFragment(pickerFullScreenFragment)
    }

    private val imageChangeObserver = MXSysImageObserver {
        if (config.pickerType in arrayOf(MXPickerType.Image, MXPickerType.ImageAndVideo)) {
            source.startScan()
        }
    }
    private val videoChangeObserver = MXSysVideoObserver {
        if (config.pickerType in arrayOf(MXPickerType.Video, MXPickerType.ImageAndVideo)) {
            source.startScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        source.startScan()
    }

    override fun onBackPressed() {
        if (currentFragment == pickerFullScreenFragment) {
            gotoFragment(pickerFragment)
            return
        }
        if (pickerFragment.isFolderListShow()) {
            pickerFragment.dismissFolder()
            return
        }
        super.onBackPressed()
    }

    private fun gotoFragment(cFragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.animator.mx_fragment_in, R.animator.mx_fragment_out)
        if (currentFragment === cFragment) {
            return
        }
        currentFragment?.let { transaction.hide(it) }
        if (!cFragment.isAdded) {
            transaction.add(R.id.rootLay, cFragment, cFragment.javaClass.simpleName)
        } else {
            transaction.show(cFragment)
        }
        transaction.commitAllowingStateLoss()
        currentFragment = cFragment
    }

    override fun onDestroy() {
        source.release()
        data.release()
        try {
            contentResolver.unregisterContentObserver(imageChangeObserver)
            contentResolver.unregisterContentObserver(videoChangeObserver)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }

    fun onSelectChange(item: MXItem) {
        if (config.pickerType == MXPickerType.Video && config.videoMaxLength > 0 && item.duration > config.videoMaxLength) {
            val format = getString(R.string.mx_picker_string_video_limit_length_tip)
            Toast.makeText(
                this@MXImgPickerActivity,
                String.format(format, config.videoMaxLength),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val isSelect = (data.selectList.getValue().contains(item))
        val list = ArrayList(data.selectList.getValue())
        if (isSelect) {
            list.remove(item)
        } else {
            if (data.selectList.getValue().size >= config.maxSize) {
                val format = if (config.pickerType == MXPickerType.Video) {
                    getString(R.string.mx_picker_string_video_limit_tip)
                } else {
                    getString(R.string.mx_picker_string_image_limit_tip)
                }
                Toast.makeText(
                    this@MXImgPickerActivity,
                    String.format(format, config.maxSize),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            list.add(item)
        }
        data.selectList.notifyChanged(list)
    }

    fun onSelectFinish() {
        val setResult = { list: List<String> ->
            setResult(
                RESULT_OK, Intent().putExtra(
                    MXPickerBuilder.KEY_INTENT_RESULT,
                    ArrayList(list)
                )
            )
            finish()
        }
        val needCompress = when (config.compressType) {
            MXCompressType.ON -> true
            MXCompressType.SELECT_BY_USER -> data.needCompress.getValue()
            else -> false
        }

        val paths = data.selectList.getValue()
        if (!needCompress) {
            setResult.invoke(paths.map { it.path })
        } else {
            val scale = MXImageCompress.from(this).setIgnoreFileSize(config.compressIgnoreSizeKb)
            thread {
                val compressPath = paths.map { item ->
                    if (item.type == MXPickerType.Image) {
                        scale.compress(item.path).absolutePath
                    } else item.path
                }
                runOnUiThread {
                    setResult.invoke(compressPath)
                }
            }
        }
    }
}