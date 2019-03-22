package com.jetbridge.testapp.yevhen

import android.databinding.DataBindingUtil
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jetbridge.testapp.yevhen.databinding.ItemBubbleOptionBinding

class BubbleAdapter<T : Any>(private val data: List<T>,
                             private val selectionEnabled: Boolean = true,
                             private val multiSelectionEnabled: Boolean = true,
                             private val darkText: Boolean = false)
    : RecyclerView.Adapter<BubbleAdapter.ViewHolder>() {

    private val selectedIndices = mutableSetOf<Int>()

    override fun onCreateViewHolder(parentView: ViewGroup, viewType: Int): BubbleAdapter.ViewHolder {
        return ViewHolder(DataBindingUtil.inflate(
            LayoutInflater.from(parentView.context),
            R.layout.item_bubble_option, parentView, false))
    }

    override fun getItemId(position: Int) = selectedIndices.hashCode().toLong()

    override fun getItemCount() = data.size

    override fun onBindViewHolder(viewHolder: BubbleAdapter.ViewHolder, index: Int) {
        // setup click listener
        if (selectionEnabled)
            viewHolder.binding.cntBubble.setOnClickListener {
                if (index in selectedIndices) {
                    selectedIndices.remove(index)
                } else {
                    if (!multiSelectionEnabled) {
                        selectedIndices.clear()
                    }
                    selectedIndices.add(index)
                }
                notifyItemChanged(index)
            }
        // set right (end) margin for the right-most item
        (viewHolder.binding.cntBubble.layoutParams as RecyclerView.LayoutParams).marginEnd =
            if (index == data.size - 1) viewHolder.binding.cntBubble.context.resources
                .getDimensionPixelOffset(R.dimen.margin_mid) else 0
        // choose color for bubble backround
        val bubbleBackgroundColorId =
            if (darkText) R.drawable.bg_selected_bubble_light
            else R.drawable.bg_selected_bubble_dark
        // set backround for bubble item
        viewHolder.binding.cntBubble.setBackgroundResource(
            if (index in selectedIndices || !selectionEnabled) bubbleBackgroundColorId
            else android.R.color.transparent)
        val dataItem = data[index]
        // display bubble item text
        viewHolder.binding.tvBubbleText.text =
            if (dataItem is ReadableEntity) dataItem.readableText
            else dataItem.toString()
        // set dark/light color depending on the option
        viewHolder.binding.tvBubbleText.setTextColor(ResourcesCompat
            .getColor(viewHolder.itemView.context.resources,
                if (darkText) R.color.profile_grey_dark else R.color.white, null))
    }

    fun getSelectedData(): List<T> = selectedIndices.map { data[it] }

    class ViewHolder(val binding: ItemBubbleOptionBinding)
        : RecyclerView.ViewHolder(binding.root)
}

interface ReadableEntity {
    val readableText: String get() = throw NotImplementedError()
}
