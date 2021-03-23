package com.mx.imgpicker.app

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_img_picker)
        initIntent()
    }

    private fun initIntent() {
        val builder =
            intent.getSerializableExtra(PickerBuilder.KEY_INTENT_BUILDER) as PickerBuilder?
        if (builder == null) {
            finish()
            return
        }
        ImagePickerService.getActivityCall()?.invoke(this)

        pickerVM.type = builder.pickerType
        adapt.maxSelectSize = builder.maxPickerSize
        initView()
    }

    private fun initView() {
        findViewById<ImageView>(R.id.returnBtn)?.setOnClickListener { onBackPressed() }

        findViewById<RecyclerView>(R.id.recycleView)?.let {
            it.setHasFixedSize(true)
            it.layoutManager = GridLayoutManager(this, 4)
            it.adapter = adapt
        }
        val folderRecycleView = findViewById<RecyclerView>(R.id.folderRecycleView)
        folderRecycleView?.let {
            it.setHasFixedSize(true)
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = folderAdapt
        }
//        findViewById<View>(R.id.folderMoreLay)?.background?.alpha = (255 * 0.5).toInt()
        findViewById<View>(R.id.folderMoreLay)?.setOnClickListener {
            if (folderRecycleView.isShown) {
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

        val selectBtn = findViewById<TextView>(R.id.selectBtn)
        selectBtn.setOnClickListener {
            val paths = adapt.selectList.map { it.path }
            setResult(
                RESULT_OK,
                Intent().putExtra(PickerBuilder.KEY_INTENT_RESULT, ArrayList(paths))
            )
            finish()
        }
        adapt.onSelectChange = { list ->
            if (list.isEmpty()) {
                selectBtn.visibility = View.GONE
                selectBtn.text = "选择"
            } else {
                selectBtn.visibility = View.VISIBLE
                selectBtn.text = "选择(${list.size}/${adapt.maxSelectSize})"
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
            findViewById<View>(R.id.folderMoreImg)?.rotation = 180f
            findViewById<View>(R.id.folderRecycleView)?.visibility = View.VISIBLE
        } else {
            findViewById<View>(R.id.folderMoreImg)?.rotation = 0f
            findViewById<View>(R.id.folderRecycleView)?.visibility = View.GONE
        }
    }

    private val imageChangeObserver = ImageChangeObserver {
        pickerVM.startScan()
    }
    private val videoChangeObserver = VideoChangeObserver {
        pickerVM.startScan()
    }

    private fun showFolder(folder: FolderItem?) {
        findViewById<TextView>(R.id.folderNameTxv)?.text = folder?.name ?: "全部照片"
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
        if (findViewById<View>(R.id.folderRecycleView)?.isShown == true) {
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