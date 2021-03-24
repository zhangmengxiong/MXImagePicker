package com.mx.imgpicker.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mx.imgpicker.ImagePickerService
import com.mx.imgpicker.R
import com.mx.imgpicker.adapts.FolderAdapt
import com.mx.imgpicker.adapts.ImgAdapt
import com.mx.imgpicker.builder.PickerBuilder
import com.mx.imgpicker.models.FolderItem
import com.mx.imgpicker.observer.ImageChangeObserver
import com.mx.imgpicker.observer.VideoChangeObserver
import com.mx.imgpicker.utils.ImagePathBiz
import com.mx.imgpicker.utils.ImagePickerProvider
import java.io.File


class ImgPickerActivity : AppCompatActivity() {
    private val pickerVM by lazy { ImgPickerVM(this) }
    private val adapt = ImgAdapt()
    private val folderAdapt = FolderAdapt()
    private var cacheFile: File? = null

    var returnBtn: ImageView? = null
    var selectBtn: TextView? = null
    var folderNameTxv: TextView? = null
    var recycleView: RecyclerView? = null
    var folderRecycleView: RecyclerView? = null
    var folderMoreLay: View? = null
    var folderMoreImg: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_img_picker)
        fullScreen(this)
        initIntent()
    }

    private fun fullScreen(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //5.x开始需要把颜色设置透明，否则导航栏会呈现系统默认的浅灰色
                val window = activity.window
                val decorView = window.decorView
                //两个 flag 要结合使用，表示让应用的主体内容占用系统状态栏的空间
                val option = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
                decorView.systemUiVisibility = option
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = Color.TRANSPARENT

                //导航栏颜色也可以正常设置
                window.navigationBarColor = resources.getColor(R.color.picker_color_background)
            } else {
                val window = activity.window
                val attributes = window.attributes
                val flagTranslucentStatus = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                val flagTranslucentNavigation =
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                attributes.flags = attributes.flags or flagTranslucentStatus
                //                attributes.flags |= flagTranslucentNavigation;
                window.attributes = attributes
            }
        }
    }

    private fun initIntent() {
        val builder =
            intent.getSerializableExtra(PickerBuilder.KEY_INTENT_BUILDER) as PickerBuilder?
        if (builder == null) {
            finish()
            return
        }

        pickerVM.type = builder.pickerType
        adapt.maxSelectSize = builder.maxPickerSize
        initView()
    }

    private fun initView() {
        returnBtn = findViewById(R.id.returnBtn)
        recycleView = findViewById(R.id.recycleView)
        folderRecycleView = findViewById(R.id.folderRecycleView)
        folderMoreLay = findViewById(R.id.folderMoreLay)
        folderMoreImg = findViewById(R.id.folderMoreImg)
        folderNameTxv = findViewById(R.id.folderNameTxv)
        selectBtn = findViewById(R.id.selectBtn)

        returnBtn?.setOnClickListener { onBackPressed() }
        recycleView?.let {
            it.setHasFixedSize(true)
            it.layoutManager = GridLayoutManager(this, 4)
            it.adapter = adapt
        }

        folderRecycleView?.let {
            it.setHasFixedSize(true)
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = folderAdapt
        }
//        findViewById<View>(R.id.folderMoreLay)?.background?.alpha = (255 * 0.5).toInt()
        folderMoreLay?.setOnClickListener {
            if (folderRecycleView?.isShown == true) {
                showFolderList(false)
            } else {
                if (folderAdapt.list.size <= 1) return@setOnClickListener
                showFolderList(true)
            }
        }
        adapt.onTakePictureClick = {
            val file = ImagePathBiz.createImageFile(this)
            val uri = ImagePickerProvider.createUri(this, file)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            startActivityForResult(intent, REQUEST_TAKE_IMG)
            cacheFile = file
        }
        adapt.onItemClick = { item, list ->
            ImagePathBiz.openImage(this, item)
        }

        selectBtn?.setOnClickListener {
            val paths = adapt.selectList.map { it.path }
            setResult(
                RESULT_OK,
                Intent().putExtra(PickerBuilder.KEY_INTENT_RESULT, ArrayList(paths))
            )
            finish()
        }
        adapt.onSelectChange = { list ->
            if (list.isEmpty()) {
                selectBtn?.visibility = View.GONE
                selectBtn?.text = "选择"
            } else {
                selectBtn?.visibility = View.VISIBLE
                selectBtn?.text = "选择(${list.size}/${adapt.maxSelectSize})"
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            false,
            imageChangeObserver
        )
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            false,
            videoChangeObserver
        )
        pickerVM.scanResult = { list ->
            if (!isFinishing && !isDestroyed) {
                folderAdapt.list.clear()
                folderAdapt.list.addAll(list)
                showFolder(folderAdapt.list.firstOrNull())
                folderAdapt.notifyDataSetChanged()
            }
        }
        folderAdapt.onItemClick = { item ->
            showFolder(item)
            folderAdapt.notifyDataSetChanged()
            showFolderList(false)

        }
        pickerVM.startScan()
    }

    private fun showFolderList(show: Boolean) {
        if (show) {
            folderMoreImg?.rotation = 180f
            folderRecycleView?.visibility = View.VISIBLE
        } else {
            folderMoreImg?.rotation = 0f
            folderRecycleView?.visibility = View.GONE
        }
    }

    private val imageChangeObserver = ImageChangeObserver {
        pickerVM.startScan()
    }
    private val videoChangeObserver = VideoChangeObserver {
        pickerVM.startScan()
    }

    private fun showFolder(folder: FolderItem?) {
        folderNameTxv?.text = folder?.name ?: "全部照片"
        adapt.list.clear()
        if (folder != null) {
            adapt.list.addAll(folder.images)
        }
        folderAdapt.selectItem = folder
        adapt.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TAKE_IMG && cacheFile?.exists() == true) {
            val file = cacheFile ?: return
            if (file.exists()) {
                ImagePathBiz.saveToGallery(this, file)
                file.delete()
                cacheFile = null
                pickerVM.startScan()
            }
        }
    }

    override fun onBackPressed() {
        if (folderRecycleView?.isShown == true) {
            showFolderList(false)
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        pickerVM.destroy()
        contentResolver.unregisterContentObserver(imageChangeObserver)
        contentResolver.unregisterContentObserver(videoChangeObserver)
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_TAKE_IMG = 0x12
    }
}