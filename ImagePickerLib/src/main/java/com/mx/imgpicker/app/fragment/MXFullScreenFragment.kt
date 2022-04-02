package com.mx.imgpicker.app.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.mx.imgpicker.R
import com.mx.imgpicker.adapts.ImgLargeAdapt
import com.mx.imgpicker.app.MXImgPickerActivity
import com.mx.imgpicker.app.MXSource
import com.mx.imgpicker.builder.MXPickerBuilder
import com.mx.imgpicker.models.MXDataSet
import com.mx.imgpicker.models.MXItem

internal class MXFullScreenFragment(
    private val data: MXDataSet,
    private val source: MXSource,
    private val builder: MXPickerBuilder
) : Fragment() {
    private var firstShowItem: MXItem? = null
    private val imgLargeAdapt by lazy { ImgLargeAdapt(data) }
    private var returnBtn: ImageView? = null
    private var titleTxv: TextView? = null
    private var selectBtn: TextView? = null
    private var recycleView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.mx_fragment_picker_full, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        returnBtn = view.findViewById(R.id.returnBtn)
        titleTxv = view.findViewById(R.id.titleTxv)
        selectBtn = view.findViewById(R.id.selectBtn)
        recycleView = view.findViewById(R.id.recycleView)

        returnBtn?.setOnClickListener {
            (requireActivity() as? MXImgPickerActivity)?.showLargeView(false)
        }
        selectBtn?.setOnClickListener {
            val paths = data.selectList.getValue().map { it.path }
            requireActivity().setResult(
                AppCompatActivity.RESULT_OK,
                Intent().putExtra(MXPickerBuilder.KEY_INTENT_RESULT, ArrayList(paths))
            )
            requireActivity().finish()
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
                        val index = layoutManager.findFirstCompletelyVisibleItemPosition()
                        titleTxv?.text = "${index + 1} / ${data.getItemSize()}"
                    }
                }
            })
        }

        imgLargeAdapt.onSelectClick = { item, _ ->
            (requireActivity() as? MXImgPickerActivity)?.onSelectChange(item)
        }
        titleTxv?.text = "1 / ${data.getItemSize()}"

        data.selectFolder.addObserver {
            imgLargeAdapt.notifyDataSetChanged()
        }
        data.selectList.addObserver { list ->
            imgLargeAdapt.notifyDataSetChanged()
            if (list.isEmpty()) {
                selectBtn?.visibility = View.GONE
                selectBtn?.text = getString(R.string.mx_picker_string_select)
            } else {
                selectBtn?.visibility = View.VISIBLE
                selectBtn?.text =
                    "${getString(R.string.mx_picker_string_select)}(${data.selectList.getValue().size}/${builder.getMaxSize()})"
            }
        }
        scrollToTarget()
    }

    private fun scrollToTarget() {
        if (recycleView == null) return
        val item = firstShowItem ?: return
        val index = data.itemIndexOf(item)
        recycleView?.scrollToPosition(index)
        firstShowItem = null
        titleTxv?.text = "${index + 1} / ${data.getItemSize()}"
    }

    fun setTargetItem(target: MXItem?) {
        firstShowItem = target
        scrollToTarget()
    }
}