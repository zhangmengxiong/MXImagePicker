package com.mx.imgpicker.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import com.mx.imgpicker.ImagePickerService
import com.mx.imgpicker.R
import com.mx.imgpicker.adapts.FolderAdapt
import com.mx.imgpicker.adapts.ImgGridAdapt
import com.mx.imgpicker.adapts.ImgLargeAdapt
import com.mx.imgpicker.builder.MXCaptureBuilder
import com.mx.imgpicker.builder.MXPickerBuilder
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
        (intent.getSerializableExtra(MXPickerBuilder.KEY_INTENT_BUILDER) as MXPickerBuilder?)
            ?: MXPickerBuilder()
    }
    private val sourceDB by lazy { MXSourceDB(this) }
    private val pickerVM by lazy { ImgPickerVM(this, builder, sourceDB) }

    private val imageList = ArrayList<Item>()
    private val selectList = ArrayList<Item>()

    private val imgAdapt by lazy { ImgGridAdapt(imageList, selectList, builder) }
    private val imgLargeAdapt by lazy { ImgLargeAdapt(imageList, selectList) }
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
            Toast.makeText(
                this,
                getString(R.string.picker_string_need_permission_storage),
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        initView()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == MXPermissionBiz.REQUEST_CODE && MXPermissionBiz.permissionResult(this)) {
            initView()
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
        ImagePickerService.getGlobalActivityCall()?.invoke(this)

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
                Toast.makeText(
                    this,
                    getString(R.string.picker_string_need_permission_storage_camera),
                    Toast.LENGTH_SHORT
                ).show()
                MXPermissionBiz.requestPermission(this, permissions)
            } else {
                val takeCall = { type: MXPickerType ->
                    val captureBuilder = MXCaptureBuilder()
                        .setType(type)
                        .setMaxVideoLength(builder.getVideoMaxLength())
                    val intent = captureBuilder.createIntent(this)
                    val file = captureBuilder.getCaptureFile()
                    sourceDB.addSource(file, type)

                    startActivityForResult(intent, 0x12)
                    MXLog.log("PATH = ${file.absolutePath}")
                }

                if (builder.getPickerType() == MXPickerType.ImageAndVideo) {
                    AlertDialog.Builder(this).apply {
                        setItems(arrayOf("拍摄图片", "拍摄视频")) { dialog, index ->
                            val type = if (index == 0) MXPickerType.Image else MXPickerType.Video
                            takeCall.invoke(type)
                        }
                    }.create().show()
                } else {
                    takeCall.invoke(builder.getPickerType())
                }
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
                Intent().putExtra(MXPickerBuilder.KEY_INTENT_RESULT, ArrayList(paths))
            )
            finish()
        }

        val onSelectChange = object : ItemSelectCall {
            override fun select(item: Item) {
                if (builder.getPickerType() == MXPickerType.Video && builder.getVideoMaxLength() > 0 && item.duration > builder.getVideoMaxLength()) {
                    val format = getString(R.string.picker_string_video_limit_length_tip)
                    Toast.makeText(
                        this@ImgPickerActivity,
                        String.format(format, builder.getVideoMaxLength()),
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
                        imgAdapt.notifyItemChanged(if (builder.isEnableCamera()) index + 1 else index)
                        imgLargeAdapt.notifyItemChanged(index)
                    }
                } else {
                    if (selectList.size >= builder.getMaxSize()) {
                        val format = if (builder.getPickerType() == MXPickerType.Video) {
                            getString(R.string.picker_string_video_limit_tip)
                        } else {
                            getString(R.string.picker_string_image_limit_tip)
                        }
                        Toast.makeText(
                            this@ImgPickerActivity,
                            String.format(format, builder.getMaxSize()),
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    selectList.add(item)
                    imgAdapt.notifyItemChanged(if (builder.isEnableCamera()) index + 1 else index)
                    imgLargeAdapt.notifyItemChanged(index)
                }

                if (selectList.isEmpty()) {
                    selectBtn?.visibility = View.GONE
                    selectBtn?.text = getString(R.string.picker_string_select)
                } else {
                    selectBtn?.visibility = View.VISIBLE
                    selectBtn?.text =
                        "${getString(R.string.picker_string_select)}(${selectList.size}/${builder.getMaxSize()})"
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
        if (builder.getPickerType() == MXPickerType.Image) {
            pickerVM.startScan()
        }
    }
    private val videoChangeObserver = VideoChangeObserver {
        if (builder.getPickerType() == MXPickerType.Video) {
            pickerVM.startScan()
        }
    }

    private fun showFolder(folder: FolderItem?) {
        folderNameTxv?.text = folder?.name ?: getString(R.string.picker_string_all)
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