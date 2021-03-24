package com.mx.imgpicker.app

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import com.mx.imgpicker.R
import com.mx.imgpicker.adapts.FolderAdapt
import com.mx.imgpicker.adapts.ImgGridAdapt
import com.mx.imgpicker.adapts.ImgLargeAdapt
import com.mx.imgpicker.builder.PickerBuilder
import com.mx.imgpicker.models.FolderItem
import com.mx.imgpicker.models.Item
import com.mx.imgpicker.observer.ImageChangeObserver
import com.mx.imgpicker.observer.VideoChangeObserver
import com.mx.imgpicker.utils.BarColorChangeBiz
import com.mx.imgpicker.utils.ImagePathBiz
import com.mx.imgpicker.utils.ImagePickerProvider
import java.io.File


class ImgPickerActivity : AppCompatActivity() {
    private val pickerVM by lazy { ImgPickerVM(this) }
    private var maxSelectSize = 9
    private val imageList = ArrayList<Item>()
    private val selectList = ArrayList<Item>()

    private val imgAdapt = ImgGridAdapt(imageList, selectList)
    private val imgLargeAdapt = ImgLargeAdapt(imageList, selectList)
    private val folderAdapt = FolderAdapt()
    private var cacheFile: File? = null

    private var returnBtn: ImageView? = null
    private var selectBtn: TextView? = null
    private var folderNameTxv: TextView? = null
    private var recycleView: RecyclerView? = null
    private var folderRecycleView: RecyclerView? = null
    private var largeImgRecycleView: RecyclerView? = null
    private var folderMoreLay: View? = null
    private var folderMoreImg: View? = null
    private var barPlaceView: View? = null

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

        pickerVM.type = builder.pickerType
        maxSelectSize = builder.maxPickerSize

        initView()

        if (builder.activityCallback != null) {
            barPlaceView?.visibility = View.GONE
            builder.activityCallback?.invoke(this)
        } else {
            // 默认设置
            if (BarColorChangeBiz.setFullScreen(
                    this,
                    resources.getColor(R.color.picker_color_background)
                )
            ) {
                barPlaceView?.visibility = View.VISIBLE
            } else {
                barPlaceView?.visibility = View.GONE
            }
        }
    }

    private fun initView() {
        returnBtn = findViewById(R.id.returnBtn)
        recycleView = findViewById(R.id.recycleView)
        folderRecycleView = findViewById(R.id.folderRecycleView)
        largeImgRecycleView = findViewById(R.id.largeImgRecycleView)
        folderMoreLay = findViewById(R.id.folderMoreLay)
        folderMoreImg = findViewById(R.id.folderMoreImg)
        folderNameTxv = findViewById(R.id.folderNameTxv)
        selectBtn = findViewById(R.id.selectBtn)
        barPlaceView = findViewById(R.id.barPlaceView)

        returnBtn?.setOnClickListener { onBackPressed() }
        recycleView?.let {
            it.itemAnimator = null
            it.setHasFixedSize(true)
            it.layoutManager = GridLayoutManager(this, 4)
            it.adapter = imgAdapt
        }

        folderRecycleView?.let {
            it.itemAnimator = null
            it.setHasFixedSize(true)
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = folderAdapt
        }
        largeImgRecycleView?.let {
            it.itemAnimator = null
            PagerSnapHelper().attachToRecyclerView(it)
            it.setHasFixedSize(true)
            it.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            it.adapter = imgLargeAdapt
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
        imgAdapt.onTakePictureClick = {
            val file = ImagePathBiz.createImageFile(this)
            val uri = ImagePickerProvider.createUri(this, file)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            startActivityForResult(intent, REQUEST_TAKE_IMG)
            cacheFile = file
        }
        imgAdapt.onItemClick = { item, list ->
            showLargeView(true, item)
//            ImagePathBiz.openImage(this, item)
        }

        selectBtn?.setOnClickListener {
            val paths = selectList.map { it.path }
            setResult(
                RESULT_OK,
                Intent().putExtra(PickerBuilder.KEY_INTENT_RESULT, ArrayList(paths))
            )
            finish()
        }

        val onSelectChange = { item: Item ->
            val index = imageList.indexOf(item)
            val isSelect = selectList.contains(item)
            if (isSelect) {
                selectList.remove(item)
                imgAdapt.notifyDataSetChanged()
                imgLargeAdapt.notifyDataSetChanged()
            } else {
                if (selectList.size >= maxSelectSize) {
                    Toast.makeText(this, "您最多只能选择${maxSelectSize}张图片！", Toast.LENGTH_SHORT).show()
                } else {
                    selectList.add(item)
                    imgAdapt.notifyItemChanged(index + 1)
                    imgLargeAdapt.notifyItemChanged(index)
                }
            }

            if (selectList.isEmpty()) {
                selectBtn?.visibility = View.GONE
                selectBtn?.text = "选择"
            } else {
                selectBtn?.visibility = View.VISIBLE
                selectBtn?.text = "选择(${selectList.size}/${maxSelectSize})"
            }
        }

        imgAdapt.onSelectClick = onSelectChange
        imgLargeAdapt.onSelectChange = onSelectChange

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
            showLargeView(false)
        } else {
            folderMoreImg?.rotation = 0f
            folderRecycleView?.visibility = View.GONE
        }
    }

    private fun showLargeView(show: Boolean, target: Item? = null) {
        if (show) {
            showFolderList(false)
            val index = imageList.indexOf(target)
            largeImgRecycleView?.visibility = View.VISIBLE
            largeImgRecycleView?.scrollToPosition(index)
        } else {
            largeImgRecycleView?.visibility = View.GONE
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
        imageList.clear()
        if (folder != null) {
            imageList.addAll(folder.images)
        }
        folderAdapt.selectItem = folder
        imgAdapt.notifyDataSetChanged()
        imgLargeAdapt.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TAKE_IMG && cacheFile?.exists() == true) {
            val file = cacheFile ?: return
            if (file.exists()) {
                if (ImagePathBiz.saveToGallery(this, file)) {
                    file.delete()
                    cacheFile = null
                } else {

                }
                pickerVM.startScan()
            }
        }
    }

    override fun onBackPressed() {
        if (folderRecycleView?.isShown == true) {
            showFolderList(false)
            return
        }
        if (largeImgRecycleView?.isShown == true) {
            showLargeView(false)
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