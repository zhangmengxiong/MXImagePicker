package com.mx.imgpicker.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import com.mx.imgpicker.ImagePickerService
import com.mx.imgpicker.R
import com.mx.imgpicker.adapts.FolderAdapt
import com.mx.imgpicker.adapts.ImgGridAdapt
import com.mx.imgpicker.adapts.ImgLargeAdapt
import com.mx.imgpicker.builder.MXCaptureBuilder
import com.mx.imgpicker.builder.PickerBuilder
import com.mx.imgpicker.db.MXSourceDB
import com.mx.imgpicker.models.FolderItem
import com.mx.imgpicker.models.Item
import com.mx.imgpicker.models.ItemSelectCall
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.observer.ImageChangeObserver
import com.mx.imgpicker.observer.VideoChangeObserver
import com.mx.imgpicker.utils.*


class ImgPickerActivity : AppCompatActivity() {
    private val builder by lazy {
        (intent.getSerializableExtra(PickerBuilder.KEY_INTENT_BUILDER) as PickerBuilder?)
            ?: PickerBuilder()
    }
    private val sourceDB by lazy { MXSourceDB(this) }
    private val pickerVM by lazy { ImgPickerVM(this, builder, sourceDB) }

    private val imageList = ArrayList<Item>()
    private val selectList = ArrayList<Item>()

    private val imgAdapt by lazy { ImgGridAdapt(imageList, selectList, builder) }
    private val imgLargeAdapt by lazy { ImgLargeAdapt(imageList, selectList, builder) }
    private val folderAdapt = FolderAdapt()

    private var returnBtn: ImageView? = null
    private var selectBtn: TextView? = null
    private var folderNameTxv: TextView? = null
    private var emptyTxv: TextView? = null
    private var recycleView: RecyclerView? = null
    private var folderRecycleView: RecyclerView? = null
    private var largeImgRecycleView: RecyclerView? = null
    private var folderMoreLay: View? = null
    private var folderMoreImg: View? = null
    private var barPlaceView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_img_picker)
        supportActionBar?.hide()
        actionBar?.hide()

        MXLog.log("启动")
        if (!MXPermissionBiz.hasPermission(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        ) {
            Toast.makeText(this, "需要读取存储权限", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initView()
        initIntent()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == MXPermissionBiz.REQUEST_CODE && MXPermissionBiz.permissionResult(this)) {
            initView()
            initIntent()
        }
    }

    private fun initIntent() {
        val activityCall = builder._activityCall ?: ImagePickerService.getGlobalActivityCall()
        if (activityCall != null) {
            barPlaceView?.visibility = View.GONE
            activityCall.invoke(this)
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
        emptyTxv = findViewById(R.id.emptyTxv)
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

        folderMoreLay?.setOnClickListener {
            if (folderRecycleView?.isShown == true) {
                showFolderList(false)
            } else {
                if (folderAdapt.list.size <= 1) return@setOnClickListener
                showFolderList(true)
            }
        }
        imgAdapt.onTakePictureClick = {
            val permissions = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            if (!MXPermissionBiz.hasPermission(this, permissions)) {
                Toast.makeText(this, "需要写入存储、相机权限", Toast.LENGTH_SHORT).show()
                MXPermissionBiz.requestPermission(this, permissions)
            } else {
                val captureBuilder = MXCaptureBuilder(this)
                    .setType(builder._pickerType)
                    .setMaxVideoLength(builder._videoMaxLength)
                val intent = captureBuilder.createIntent()
                val file = captureBuilder.getCaptureFile()
                sourceDB.addSource(file, builder._pickerType)

                startActivityForResult(intent, 0x12)
                MXLog.log("PATH = ${file.absolutePath}")
            }
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

        val onSelectChange = object : ItemSelectCall {
            override fun select(item: Item) {
                if (builder._pickerType == MXPickerType.Video && builder._videoMaxLength > 0 && item.duration > builder._videoMaxLength) {
                    Toast.makeText(
                        this@ImgPickerActivity,
                        "当前视频时长超出限制，可选择 ${builder._videoMaxLength} 秒以内的视频",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val index = imageList.indexOf(item)
                val isSelect = selectList.contains(item)
                val selectIndexList = selectList.map { imageList.indexOf(it) }

                if (isSelect) {
                    selectList.remove(item)
                    selectIndexList.forEach { index ->
                        imgAdapt.notifyItemChanged(if (builder._enableCamera) index + 1 else index)
                        imgLargeAdapt.notifyItemChanged(index)
                    }
                } else {
                    if (selectList.size >= builder._maxSize) {
                        Toast.makeText(
                            this@ImgPickerActivity,
                            "您最多只能选择${builder._maxSize}张图片！",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    selectList.add(item)
                    imgAdapt.notifyItemChanged(if (builder._enableCamera) index + 1 else index)
                    imgLargeAdapt.notifyItemChanged(index)
                }

                if (selectList.isEmpty()) {
                    selectBtn?.visibility = View.GONE
                    selectBtn?.text = "选择"
                } else {
                    selectBtn?.visibility = View.VISIBLE
                    selectBtn?.text = "选择(${selectList.size}/${builder._maxSize})"
                }
            }
        }

        imgAdapt.onSelectClick = onSelectChange
        imgLargeAdapt.onSelectChange = onSelectChange

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
        if (builder._pickerType == MXPickerType.Image) {
            pickerVM.startScan()
        }
    }
    private val videoChangeObserver = VideoChangeObserver {
        if (builder._pickerType == MXPickerType.Video) {
            pickerVM.startScan()
        }
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
        emptyTxv?.visibility = if (imgAdapt.itemCount <= 0) View.VISIBLE else View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        pickerVM.startScan()
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
        try {
            contentResolver.unregisterContentObserver(imageChangeObserver)
            contentResolver.unregisterContentObserver(videoChangeObserver)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }
}