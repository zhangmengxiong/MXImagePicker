package com.mx.imgpicker.app.picker.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.mx.imgpicker.R
import com.mx.imgpicker.adapts.ImgLargeAdapt
import com.mx.imgpicker.app.picker.MXImgPickerActivity
import com.mx.imgpicker.app.picker.MXPickerVM
import com.mx.imgpicker.models.MXCompressType
import com.mx.imgpicker.models.MXItem

internal class MXFullScreenFragment : Fragment() {
    private val vm by lazy { ViewModelProvider(requireActivity()).get(MXPickerVM::class.java) }
    private val imgList = ArrayList<MXItem>()
    private val imgLargeAdapt = ImgLargeAdapt(imgList)
    private var firstShowItem: MXItem? = null
    private var returnBtn: ImageView? = null
    private var titleTxv: TextView? = null
    private var selectBtn: TextView? = null
    private var recycleView: RecyclerView? = null

    private var willResizeLay: View? = null
    private var willResizeImg: ImageView? = null
    private var selectLay: View? = null
    private var selectImg: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.mx_picker_fragment_picker_full, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        returnBtn = view.findViewById(R.id.returnBtn)
        titleTxv = view.findViewById(R.id.titleTxv)
        selectBtn = view.findViewById(R.id.selectBtn)
        recycleView = view.findViewById(R.id.recycleView)
        willResizeLay = view.findViewById(R.id.willResizeLay)
        willResizeImg = view.findViewById(R.id.willResizeImg)
        selectLay = view.findViewById(R.id.selectLay)
        selectImg = view.findViewById(R.id.selectImg)

        returnBtn?.setOnClickListener {
            (requireActivity() as? MXImgPickerActivity)?.showLargeView(false)
        }
        selectBtn?.setOnClickListener {
            (requireActivity() as? MXImgPickerActivity)?.onSelectFinish()
        }
        willResizeLay?.setOnClickListener {
            vm.needCompress.postValue(!(vm.needCompress.value ?: false))
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

        recycleView?.let { recycleView ->
            val animator = recycleView.itemAnimator
            if (animator is SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
            recycleView.itemAnimator = null
            PagerSnapHelper().attachToRecyclerView(recycleView)
            recycleView.setHasFixedSize(true)
            val layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            recycleView.layoutManager = layoutManager
            recycleView.adapter = imgLargeAdapt
            recycleView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val index = layoutManager.findFirstVisibleItemPosition()
                        if (vm.fullScreenSelectIndex.value != index) {
                            vm.fullScreenSelectIndex.postValue(index)
                        }
                    }
                }
            })
        }
        selectLay?.setOnClickListener {
            val index = vm.fullScreenSelectIndex.value ?: 0
            val item = imgList.getOrNull(index) ?: return@setOnClickListener
            (requireActivity() as? MXImgPickerActivity)?.onSelectChange(item)
        }
        vm.fullScreenSelectIndex.observe(viewLifecycleOwner) { index ->
            titleTxv?.text = "${index + 1} / ${imgList.size}"
            val item = imgList.getOrNull(index) ?: return@observe
            val isSelect = (vm.selectMediaList.indexOf(item) >= 0)
            if (isSelect) {
                selectImg?.setImageResource(R.drawable.mx_picker_radio_select)
                selectImg?.setColorFilter(resources.getColor(R.color.mx_picker_color_select))
            } else {
                selectImg?.setImageResource(R.drawable.mx_picker_radio_unselect)
                selectImg?.setColorFilter(resources.getColor(R.color.mx_picker_color_important))
            }
        }

        vm.selectMediaListLive.observe(viewLifecycleOwner) {
            if (vm.selectMediaList.isEmpty()) {
                selectBtn?.visibility = View.GONE
                selectBtn?.text = getString(R.string.mx_picker_string_select)
            } else {
                selectBtn?.visibility = View.VISIBLE
                selectBtn?.text =
                    "${getString(R.string.mx_picker_string_select)}(${vm.selectMediaList.size}/${vm.maxSize})"
            }
            vm.fullScreenSelectIndex.postValue(vm.fullScreenSelectIndex.value)
        }
    }

    fun setTargetItem(target: MXItem?) {
        firstShowItem = target
        refreshViews()
    }

    fun setItemList(items: List<MXItem>?) {
        imgList.clear()
        items?.let { imgList.addAll(it) }

        if (recycleView == null) return
        imgLargeAdapt.notifyDataSetChanged()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            refreshViews()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshViews()
    }

    private fun refreshViews() {
        if (recycleView == null) return
        val item = firstShowItem ?: return
        val index = imgList.indexOf(item)
        if (index < 0) {
            recycleView?.scrollToPosition(0)
            vm.fullScreenSelectIndex.postValue(0)
        } else {
            recycleView?.scrollToPosition(index)
            vm.fullScreenSelectIndex.postValue(index)
        }
        firstShowItem = null
    }
}