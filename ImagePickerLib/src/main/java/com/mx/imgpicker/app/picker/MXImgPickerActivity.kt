package com.mx.imgpicker.app.picker

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import com.mx.imgpicker.utils.MXScanBiz
import com.mx.imgpicker.utils.MXUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MXImgPickerActivity : AppCompatActivity() {
    private val vm by lazy {
        ViewModelProvider(this).get(MXPickerVM::class.java)
    }

    private var currentFragment: Fragment? = null
    private val pickerFragment by lazy {
        createFragment(MXPickerFragment::class.java)
    }
    private val pickerFullScreenFragment by lazy {
        createFragment(MXFullScreenFragment::class.java)
    }

    private fun <T : Any> createFragment(clazz: Class<T>): T {
        try {
            val simpleName = clazz.name
            val cache = (supportFragmentManager.findFragmentByTag(simpleName) as T?)
            if (cache != null) {
                return cache
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return clazz.newInstance()
    }

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
        lifecycleScope.launch {
            vm.reloadMediaList()
        }

        MXUtils.log("启动")
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (!MXUtils.hasPermission(this, permissions)) {
            Toast.makeText(
                this,
                getString(R.string.mx_picker_string_need_permission_storage),
                Toast.LENGTH_SHORT
            ).show()
            permissionResult.launch(permissions)
        } else {
            initView()
        }
    }

    private val permissionResult = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        if (MXUtils.hasPermission(this, map.keys.toTypedArray())) {
            initView()
        } else {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        MXScanBiz.scanRecent(this, lifecycleScope)
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
        vm.selectDirLive.observe(this) {
            lifecycleScope.launch { vm.reloadMediaList() }
        }
        MXScanBiz.setOnUpdateListener {
            lifecycleScope.launch { vm.reloadMediaList() }
        }
        MXScanBiz.scanAll(this, lifecycleScope)
    }

    fun showLargeView(show: Boolean, target: MXItem? = null) {
        if (show) {
            pickerFullScreenFragment.setItemList(vm.mediaList)
            pickerFullScreenFragment.setTargetItem(target)
            gotoFragment(pickerFullScreenFragment)
        } else {
            gotoFragment(pickerFragment)
        }
    }

    fun showLargeSelectView() {
        val list = vm.selectMediaList
        if (list.isEmpty()) return

        pickerFullScreenFragment.setItemList(list)
        pickerFullScreenFragment.setTargetItem(list.firstOrNull())
        gotoFragment(pickerFullScreenFragment)
    }

    private val imageChangeObserver = MXSysImageObserver {
        if (isDestroyed) return@MXSysImageObserver
        if (vm.pickerType in arrayOf(MXPickerType.Image, MXPickerType.ImageAndVideo)) {
            MXScanBiz.scanRecent(this, lifecycleScope)
        }
    }
    private val videoChangeObserver = MXSysVideoObserver {
        if (isDestroyed) return@MXSysVideoObserver
        if (vm.pickerType in arrayOf(MXPickerType.Video, MXPickerType.ImageAndVideo)) {
            MXScanBiz.scanRecent(this, lifecycleScope)
        }
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
            transaction.add(R.id.rootLay, cFragment, cFragment::class.java.name)
        } else {
            transaction.show(cFragment)
        }
        transaction.commitAllowingStateLoss()
        currentFragment = cFragment
    }

    override fun onDestroy() {
        try {
            contentResolver.unregisterContentObserver(imageChangeObserver)
        } catch (_: Exception) {
        }
        try {
            contentResolver.unregisterContentObserver(videoChangeObserver)
        } catch (_: Exception) {
        }
        MXScanBiz.setOnUpdateListener(null)
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
        val isSelect = (vm.selectMediaList.contains(item))
        if (isSelect) {
            vm.selectMediaList.remove(item)
        } else {
            if (vm.selectMediaList.size >= vm.maxSize) {
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
            vm.selectMediaList.add(item)
        }
        vm.selectMediaListLive.postValue(Any())
    }

    fun onSelectFinish() {
        val paths = vm.selectMediaList.toList()
        if (paths.isEmpty()) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        lifecycleScope.launch {
            val needCompress = when (vm.compressType) {
                MXCompressType.ON -> true
                MXCompressType.SELECT_BY_USER -> vm.needCompress.value ?: false
                else -> false
            }
            val compressPath = if (needCompress) {
                withContext(Dispatchers.IO) {
                    val scale = MXImageCompress.from(this@MXImgPickerActivity)
                        .setIgnoreFileSize(vm.compressIgnoreSizeKb)
                    paths.map { item ->
                        if (item.type == MXPickerType.Image) {
                            scale.compress(item.path).absolutePath
                        } else item.path
                    }
                }
            } else {
                paths.map { it.path }
            }
            setResult(
                RESULT_OK,
                Intent().putExtra(MXPickerBuilder.KEY_INTENT_RESULT, ArrayList(compressPath))
            )
            finish()
        }
    }
}