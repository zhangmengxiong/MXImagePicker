package com.mx.imgpicker.app.picker.fragment

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.mx.imgpicker.R
import com.mx.imgpicker.adapts.FolderAdapt
import com.mx.imgpicker.adapts.ImgGridAdapt
import com.mx.imgpicker.app.picker.MXImgPickerActivity
import com.mx.imgpicker.app.picker.MXPickerVM
import com.mx.imgpicker.builder.MXCaptureBuilder
import com.mx.imgpicker.models.MXCompressType
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.MXUtils
import kotlinx.coroutines.launch
import java.io.File

internal class MXPickerFragment : Fragment() {
    private val vm by lazy { ViewModelProvider(requireActivity()).get(MXPickerVM::class.java) }
    private val imgAdapt by lazy { ImgGridAdapt(vm) }
    private val folderAdapt by lazy { FolderAdapt(vm, lifecycleScope) }
    private val permissions = arrayOf(Manifest.permission.CAMERA)
    private var selectItemIndex: Array<Int>? = null

    private var returnBtn: ImageView? = null
    private var selectBtn: TextView? = null
    private var previewBtn: TextView? = null
    private var folderNameTxv: TextView? = null
    private var emptyTxv: TextView? = null
    private var recycleView: RecyclerView? = null
    private var folderRecycleView: RecyclerView? = null
    private var folderMoreLay: View? = null
    private var folderMoreImg: View? = null
    private var bottomLay: View? = null
    private var willResizeLay: View? = null
    private var willResizeImg: ImageView? = null

    private var targetFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.mx_picker_fragment_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycleView = view.findViewById(R.id.recycleView)

        returnBtn = view.findViewById(R.id.returnBtn)
        emptyTxv = view.findViewById(R.id.emptyTxv)
        folderRecycleView = view.findViewById(R.id.folderRecycleView)
        folderMoreLay = view.findViewById(R.id.folderMoreLay)
        folderMoreImg = view.findViewById(R.id.folderMoreImg)
        folderNameTxv = view.findViewById(R.id.folderNameTxv)
        selectBtn = view.findViewById(R.id.selectBtn)
        previewBtn = view.findViewById(R.id.previewBtn)
        bottomLay = view.findViewById(R.id.bottomLay)
        willResizeLay = view.findViewById(R.id.willResizeLay)
        willResizeImg = view.findViewById(R.id.willResizeImg)

        bottomLay?.setOnClickListener { }
        returnBtn?.setOnClickListener { requireActivity().onBackPressed() }

        folderRecycleView?.let {
            val animator = it.itemAnimator
            if (animator is SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
            it.itemAnimator = null
            it.setHasFixedSize(true)
            it.layoutManager = LinearLayoutManager(requireContext())
            it.adapter = folderAdapt
        }
        folderMoreLay?.setOnClickListener {
            if (folderRecycleView?.isShown == true) {
                showFolderList(false)
            } else {
                if (vm.dirList.size <= 1) return@setOnClickListener
                showFolderList(true)
            }
        }

        selectBtn?.setOnClickListener {
            (requireActivity() as MXImgPickerActivity).onSelectFinish()
        }
        previewBtn?.setOnClickListener {
            (requireActivity() as MXImgPickerActivity).showLargeSelectView()
        }

        recycleView?.let {
            val animator = it.itemAnimator
            if (animator is SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
            it.itemAnimator = null
            it.setHasFixedSize(true)
            it.layoutManager = GridLayoutManager(requireContext(), 4)
            it.adapter = imgAdapt
        }

        imgAdapt.onTakePictureClick = {
            if (!MXUtils.hasPermission(requireContext(), permissions)) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.mx_picker_string_need_permission_storage_camera),
                    Toast.LENGTH_SHORT
                ).show()
                permissionResult.launch(permissions)
            } else {
                val takeCall = { type: MXPickerType ->
                    val captureBuilder = MXCaptureBuilder()
                        .setType(type)
                        .setMaxVideoLength(vm.videoMaxLength)
                    val intent = captureBuilder.createIntent(requireContext())
                    val file = captureBuilder.getCaptureFile()
                    vm.addPrivateSource(file, type)
                    targetFile = file
                    MXUtils.log("PATH = ${file.absolutePath}")
                    captureResult.launch(intent)
                }

                if (vm.pickerType == MXPickerType.ImageAndVideo) {
                    AlertDialog.Builder(requireContext()).apply {
                        setItems(
                            arrayOf(
                                getString(R.string.mx_picker_string_take_pic),
                                getString(R.string.mx_picker_string_take_video)
                            )
                        ) { _, index ->
                            val type = if (index == 0) MXPickerType.Image else MXPickerType.Video
                            takeCall.invoke(type)
                        }
                    }.create().show()
                } else {
                    takeCall.invoke(vm.pickerType)
                }
            }
        }
        imgAdapt.onItemClick = { item, list ->
            (requireActivity() as? MXImgPickerActivity)?.showLargeView(true, item)
        }
        imgAdapt.onSelectClick = { item, _ ->
            (requireActivity() as? MXImgPickerActivity)?.onSelectChange(item)
        }
        willResizeLay?.setOnClickListener {
            vm.needCompress.postValue(!(vm.needCompress.value ?: true))
        }
        if (vm.compressType == MXCompressType.SELECT_BY_USER) {
            willResizeLay?.visibility = View.VISIBLE
        } else {
            willResizeLay?.visibility = View.GONE
        }

        vm.needCompress.observe(viewLifecycleOwner) { compress ->
            if (!compress) {
                willResizeImg?.setImageResource(R.drawable.mx_picker_radio_select)
                willResizeImg?.setColorFilter(resources.getColor(R.color.mx_picker_color_select))
            } else {
                willResizeImg?.setImageResource(R.drawable.mx_picker_radio_unselect)
                willResizeImg?.setColorFilter(resources.getColor(R.color.mx_picker_color_important))
            }
        }
        vm.selectMediaListLive.observe(viewLifecycleOwner) {
            updateSelectStatus()
            val list = vm.selectMediaList
            val oldIdx = selectItemIndex?.toList() ?: emptyList()
            val newIdx = list.map { vm.mediaList.indexOf(it) }
            val notifyIndex = (oldIdx + newIdx).distinct()
            selectItemIndex = newIdx.toTypedArray()
            notifyIndex.forEach { index ->
                imgAdapt.notifyItemChanged(if (vm.enableCamera) index + 1 else index)
            }
        }
        vm.selectDirLive.observe(viewLifecycleOwner) { dir ->
            folderNameTxv?.text = dir?.name
            folderAdapt.notifyDataSetChanged()
        }
        vm.mediaListLive.observe(viewLifecycleOwner) {
            val list = vm.mediaList
            emptyTxv?.visibility = if (vm.mediaList.isEmpty()) View.VISIBLE else View.GONE
            imgAdapt.imgList.clear()
            imgAdapt.imgList.addAll(list)
            imgAdapt.notifyDataSetChanged()
            MXUtils.log("刷新列表：${imgAdapt.imgList.size}")
        }
        folderAdapt.onItemClick = { folder ->
            vm.selectDirLive.postValue(folder)
            showFolderList(false)
        }
        vm.dirListLive.observe(viewLifecycleOwner) {
            folderAdapt.notifyDataSetChanged()
        }
    }

    private val captureResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val file = targetFile ?: return@registerForActivityResult
        if (!file.exists()) return@registerForActivityResult
        lifecycleScope.launch {
            vm.onMediaInsert(file)
        }
        targetFile = null
    }

    private val permissionResult = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (MXUtils.hasPermission(requireContext(), permissions)) {
            imgAdapt.onTakePictureClick?.invoke()
        }
    }

    private fun updateSelectStatus() {
        val selectSize = vm.selectMediaList.size
        if (selectSize <= 0) {
            selectBtn?.visibility = View.GONE
            selectBtn?.text = getString(R.string.mx_picker_string_select)
            previewBtn?.alpha = 0.5f
            previewBtn?.text = getString(R.string.mx_picker_string_preview)
        } else {
            selectBtn?.visibility = View.VISIBLE
            selectBtn?.text =
                "${getString(R.string.mx_picker_string_select)}(${selectSize}/${vm.maxSize})"
            previewBtn?.alpha = 1f
            previewBtn?.text =
                "${getString(R.string.mx_picker_string_preview)}(${selectSize})"
        }
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

    fun isFolderListShow(): Boolean {
        return folderRecycleView?.visibility == View.VISIBLE
    }

    fun dismissFolder() {
        showFolderList(false)
    }
}