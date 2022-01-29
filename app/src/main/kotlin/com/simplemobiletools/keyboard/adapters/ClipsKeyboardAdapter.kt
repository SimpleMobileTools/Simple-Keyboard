package com.simplemobiletools.keyboard.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.simplemobiletools.commons.extensions.removeUnderlines
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.extensions.config
import com.simplemobiletools.keyboard.helpers.ITEM_CLIP
import com.simplemobiletools.keyboard.helpers.ITEM_SECTION_LABEL
import com.simplemobiletools.keyboard.models.Clip
import com.simplemobiletools.keyboard.models.ClipsSectionLabel
import com.simplemobiletools.keyboard.models.ListItem
import kotlinx.android.synthetic.main.item_clip_on_keyboard.view.*
import kotlinx.android.synthetic.main.item_section_label.view.*
import java.util.*

class ClipsKeyboardAdapter(val context: Context, var items: ArrayList<ListItem>, val itemClick: (clip: Clip) -> Unit) :
    RecyclerView.Adapter<ClipsKeyboardAdapter.ViewHolderr>() {

    private val layoutInflater = LayoutInflater.from(context)
    private val baseConfig = context.config
    private var textColor = baseConfig.textColor

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderr {
        val layoutId = when (viewType) {
            ITEM_SECTION_LABEL -> R.layout.item_section_label
            else -> R.layout.item_clip_on_keyboard
        }

        val view = layoutInflater.inflate(layoutId, parent, false)
        return ViewHolderr(view)
    }

    override fun onBindViewHolder(holder: ViewHolderr, position: Int) {
        val item = items[position]
        holder.bindView(item) { itemView ->
            when (item) {
                is Clip -> setupClip(itemView, item)
                is ClipsSectionLabel -> setupSection(itemView, item)
            }

            (itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams).isFullSpan = item is ClipsSectionLabel
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when {
        items[position] is ClipsSectionLabel -> ITEM_SECTION_LABEL
        else -> ITEM_CLIP
    }

    private fun setupClip(view: View, clip: Clip) {
        view.clip_value.apply {
            text = clip.value
            removeUnderlines()
            setTextColor(textColor)
        }
    }

    private fun setupSection(view: View, label: ClipsSectionLabel) {
        view.clips_section_label.apply {
            text = label.value
            setTextColor(textColor)
        }
    }

    open inner class ViewHolderr(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(any: Any, callback: (itemView: View) -> Unit): View {
            return itemView.apply {
                callback(this)

                if (any is Clip) {
                    setOnClickListener {
                        itemClick.invoke(any)
                    }
                }
            }
        }
    }
}
