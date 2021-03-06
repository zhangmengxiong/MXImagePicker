package com.mx.imgpicker.app.picker.fragment

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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

internal class MXPickerFragment : Fragment() {
    private val vm by lazy { ViewModelProvider(requireActivity()).get(MXPickerVM::class.java) }
    private val imgAdapt by lazy { ImgGridAdapt(vm) }
    private val folderAdapt by lazy { FolderAdapt(vm) }
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
                val folderList = vm.folderList.value ?: emptyList()
                if (folderList.size <= 1) return@setOnClickListener
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
            val permissions = arrayOf(Manifest.permission.CAMERA)
            if (!MXUtils.hasPermission(requireContext(), permissions)) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.mx_picker_string_need_permission_storage_camera),
                    Toast.LENGTH_SHORT
                ).show()
                MXUtils.requestPermission(
                    requireActivity(), permissions,
                    MXUtils.REQUEST_CODE_CAMERA
                )
            } else {
                val takeCall = { type: MXPickerType ->
                    val captureBuilder = MXCaptureBuilder()
                        .setType(type)
                        .setMaxVideoLength(vm.videoMaxLength)
                    val intent = captureBuilder.createIntent(requireContext())
                    val file = captureBuilder.getCaptureFile()
                    vm.addPrivateSource(file, type)
                    startActivityForResult(intent, 0x12)
                    MXUtils.log("PATH = ${file.absolutePath}")
                }

                if (vm.pickerType == MXPickerType.ImageAndVideo) {
                    AlertDialog.Builder(requireContext()).apply {
                        setItems(
                            arrayOf(
                                getString(R.string.mx_picker_string_take_pic),
                                getString(R.string.mx_picker_string_take_video)
                            )
                        ) { dialog, index ->
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
        vm.selectList.observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) {
                selectBtn?.visibility = View.GONE
                selectBtn?.text = getString(R.string.mx_picker_string_select)
                previewBtn?.alpha = 0.5f
                previewBtn?.text = getString(R.string.mx_picker_string_preview)
            } else {
                selectBtn?.visibility = View.VISIBLE
                selectBtn?.text =
                    "${getString(R.string.mx_picker_string_select)}(${vm.getSelectListSize()}/${vm.maxSize})"

                previewBtn?.alpha = 1f
                previewBtn?.text =
                    "${getString(R.string.mx_picker_string_preview)}(${vm.getSelectListSize()})"
            }
            val oldIdx = selectItemIndex?.toList() ?: emptyList()
            val newIdx = list.map { vm.itemIndexOf(it) }
            val notifyIndex = (oldIdx + newIdx).distinct()
            selectItemIndex = newIdx.toTypedArray()
            notifyIndex.forEach { index ->
                imgAdapt.notifyItemChanged(if (vm.enableCamera) index + 1 else index)
            }
        }
        vm.selectFolder.observe(viewLifecycleOwner) { folder ->
            folderNameTxv?.text = folder?.name
            emptyTxv?.visibility = if (folder?.items?.isEmpty() == true) View.VISIBLE else View.GONE
            folderAdapt.notifyDataSetChanged()
            imgAdapt.notifyDataSetChanged()
        }
        folderAdapt.onItemClick = { folder ->
            vm.selectFolder.postValue(folder)
            showFolderList(false)
        }
        vm.folderList.observe(viewLifecycleOwner) {
            folderAdapt.notifyDataSetChanged()
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