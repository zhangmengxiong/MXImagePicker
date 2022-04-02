package com.mx.imgpicker.app.fragment

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.mx.imgpicker.R
import com.mx.imgpicker.adapts.FolderAdapt
import com.mx.imgpicker.adapts.ImgGridAdapt
import com.mx.imgpicker.app.MXImgPickerActivity
import com.mx.imgpicker.app.MXSource
import com.mx.imgpicker.builder.MXCaptureBuilder
import com.mx.imgpicker.builder.MXPickerBuilder
import com.mx.imgpicker.models.MXDataSet
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.MXUtils

internal class MXPickerFragment(
    private val data: MXDataSet,
    private val source: MXSource,
    private val builder: MXPickerBuilder
) : Fragment() {
    private val imgAdapt by lazy { ImgGridAdapt(data, builder) }
    private val folderAdapt by lazy { FolderAdapt(data) }
    private var selectItemIndex: Array<Int>? = null

    private var returnBtn: ImageView? = null
    private var selectBtn: TextView? = null
    private var folderNameTxv: TextView? = null
    private var emptyTxv: TextView? = null
    private var recycleView: RecyclerView? = null
    private var folderRecycleView: RecyclerView? = null
    private var folderMoreLay: View? = null
    private var folderMoreImg: View? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.mx_fragment_picker, container, false)
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
                val folderList = data.folderList.getValue()
                if (folderList.size <= 1) return@setOnClickListener
                showFolderList(true)
            }
        }

        selectBtn?.setOnClickListener {
            val paths = data.selectList.getValue().map { it.path }
            requireActivity().setResult(
                AppCompatActivity.RESULT_OK,
                Intent().putExtra(MXPickerBuilder.KEY_INTENT_RESULT, ArrayList(paths))
            )
            requireActivity().finish()
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
                        .setMaxVideoLength(builder.getVideoMaxLength())
                    val intent = captureBuilder.createIntent(requireContext())
                    val file = captureBuilder.getCaptureFile()
                    source.addPrivateSource(file, type)

                    startActivityForResult(intent, 0x12)
                    MXUtils.log("PATH = ${file.absolutePath}")
                }

                if (builder.getPickerType() == MXPickerType.ImageAndVideo) {
                    AlertDialog.Builder(requireContext()).apply {
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
            (requireActivity() as? MXImgPickerActivity)?.showLargeView(true, item)
        }
        imgAdapt.onSelectClick = { item, _ ->
            (requireActivity() as? MXImgPickerActivity)?.onSelectChange(item)
        }

        data.selectList.addObserver { list ->
            if (list.isEmpty()) {
                selectBtn?.visibility = View.GONE
                selectBtn?.text = getString(R.string.mx_picker_string_select)
            } else {
                selectBtn?.visibility = View.VISIBLE
                selectBtn?.text =
                    "${getString(R.string.mx_picker_string_select)}(${data.selectList.getValue().size}/${builder.getMaxSize()})"
            }
            val oldIdx = selectItemIndex?.toList() ?: emptyList()
            val newIdx = list.map { data.itemIndexOf(it) }
            val notifyIndex = (oldIdx + newIdx).distinct()
            selectItemIndex = newIdx.toTypedArray()
            notifyIndex.forEach { index ->
                imgAdapt.notifyItemChanged(if (builder.isEnableCamera()) index + 1 else index)
            }
        }
        data.selectFolder.addObserver { folder ->
            folderNameTxv?.text = folder?.name
            emptyTxv?.visibility = if (folder?.items?.isEmpty() == true) View.VISIBLE else View.GONE
            folderAdapt.notifyDataSetChanged()
            imgAdapt.notifyDataSetChanged()
        }
        folderAdapt.onItemClick = { folder ->
            data.selectFolder.notifyChanged(folder)
            showFolderList(false)
        }
        data.folderList.addObserver {
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