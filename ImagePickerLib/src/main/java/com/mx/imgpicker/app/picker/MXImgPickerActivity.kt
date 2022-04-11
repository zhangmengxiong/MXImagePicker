package com.mx.imgpicker.app.picker

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mx.imgpicker.MXImagePicker
import com.mx.imgpicker.R
import com.mx.imgpicker.app.picker.fragment.MXFullScreenFragment
import com.mx.imgpicker.app.picker.fragment.MXPickerFragment
import com.mx.imgpicker.builder.MXPickerBuilder
import com.mx.imgpicker.compress.MXImageCompress
import com.mx.imgpicker.models.MXCompressType
import com.mx.imgpicker.models.MXConfig
import com.mx.imgpicker.models.MXItem
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.observer.MXSysImageObserver
import com.mx.imgpicker.observer.MXSysVideoObserver
import com.mx.imgpicker.utils.MXUtils
import kotlin.concurrent.thread


class MXImgPickerActivity : AppCompatActivity() {
    private val vm by lazy {
        ViewModelProvider(this).get(MXPickerVM::class.java)
    }

    private var currentFragment: Fragment? = null
    private val pickerFragment by lazy { MXPickerFragment() }
    private val pickerFullScreenFragment by lazy { MXFullScreenFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MXImagePicker.init(application)
        setContentView(R.layout.mx_picker_activity_img_picker)
        supportActionBar?.hide()
        actionBar?.hide()
        vm.setConfig(
            (intent.getSerializableExtra(MXPickerBuilder.KEY_INTENT_BUILDER)
                    as? MXConfig) ?: MXConfig()
        )

        MXUtils.log("启动")
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!MXUtils.hasPermission(this, permissions)) {
            Toast.makeText(
                this,
                getString(R.string.mx_picker_string_need_permission_storage),
                Toast.LENGTH_SHORT
            ).show()
            MXUtils.requestPermission(this, permissions, MXUtils.REQUEST_CODE_READ)
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
        vm.folderList.observe(this) { list ->
            val selectName = vm.selectFolder.value?.name
            val selectFolder = list.firstOrNull {
                it.name == selectName
            } ?: list.firstOrNull()
            vm.selectFolder.postValue(selectFolder)
            MXUtils.log("数据刷新：${vm.getItemSize()}")
        }

        vm.startScan()
    }

    fun showLargeView(show: Boolean, target: MXItem? = null) {
        if (show) {
            pickerFullScreenFragment.setItemList(vm.selectFolder.value?.items)
            pickerFullScreenFragment.setTargetItem(target)
            gotoFragment(pickerFullScreenFragment)
        } else {
            gotoFragment(pickerFragment)
        }
    }

    fun showLargeSelectView() {
        val list = vm.getSelectList()
        if (list.isEmpty()) return

        pickerFullScreenFragment.setItemList(vm.getSelectList())
        pickerFullScreenFragment.setTargetItem(list.firstOrNull())
        gotoFragment(pickerFullScreenFragment)
    }

    private val imageChangeObserver = MXSysImageObserver {
        if (vm.pickerType in arrayOf(MXPickerType.Image, MXPickerType.ImageAndVideo)) {
            vm.startScan()
        }
    }
    private val videoChangeObserver = MXSysVideoObserver {
        if (vm.pickerType in arrayOf(MXPickerType.Video, MXPickerType.ImageAndVideo)) {
            vm.startScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        vm.startScan()
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
        vm.release()
        try {
            contentResolver.unregisterContentObserver(imageChangeObserver)
            contentResolver.unregisterContentObserver(videoChangeObserver)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }

    fun onSelectChange(item: MXItem) {
        if (vm.pickerType == MXPickerType.Video && vm.videoMaxLength > 0 && item.duration > vm.videoMaxLength) {
            val format = getString(R.string.mx_picker_string_video_limit_length_tip)
            Toast.makeText(
                this@MXImgPickerActivity,
                String.format(format, vm.videoMaxLength),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val selectList = vm.getSelectList()
        val isSelect = (selectList.contains(item))
        val list = ArrayList(selectList)
        if (isSelect) {
            list.remove(item)
        } else {
            if (selectList.size >= vm.maxSize) {
                val format = if (vm.pickerType == MXPickerType.Video) {
                    getString(R.string.mx_picker_string_video_limit_tip)
                } else {
                    getString(R.string.mx_picker_string_image_limit_tip)
                }
                Toast.makeText(
                    this@MXImgPickerActivity,
                    String.format(format, vm.maxSize),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            list.add(item)
        }
        vm.selectList.postValue(list)
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
        val needCompress = when (vm.compressType) {
            MXCompressType.ON -> true
            MXCompressType.SELECT_BY_USER -> vm.needCompress.value ?: false
            else -> false
        }

        val paths = vm.getSelectList()
        if (!needCompress) {
            setResult.invoke(paths.map { it.path })
        } else {
            val scale = MXImageCompress.from(this).setIgnoreFileSize(vm.compressIgnoreSizeKb)
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