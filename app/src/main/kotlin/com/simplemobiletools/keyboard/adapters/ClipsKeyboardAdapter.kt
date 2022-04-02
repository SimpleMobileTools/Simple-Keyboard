package com.simplemobiletools.keyboard.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.extensions.config
import com.simplemobiletools.keyboard.extensions.getCurrentClip
import com.simplemobiletools.keyboard.extensions.getStrokeColor
import com.simplemobiletools.keyboard.helpers.ClipsHelper
import com.simplemobiletools.keyboard.helpers.ITEM_CLIP
import com.simplemobiletools.keyboard.helpers.ITEM_SECTION_LABEL
import com.simplemobiletools.keyboard.interfaces.RefreshClipsListener
import com.simplemobiletools.keyboard.models.Clip
import com.simplemobiletools.keyboard.models.ClipsSectionLabel
import com.simplemobiletools.keyboard.models.ListItem
import kotlinx.android.synthetic.main.item_clip_on_keyboard.view.*
import kotlinx.android.synthetic.main.item_section_label.view.*

class ClipsKeyboardAdapter(
    val context: Context, var items: ArrayList<ListItem>, val refreshClipsListener: RefreshClipsListener,
    val itemClick: (clip: Clip) -> Unit
) : RecyclerView.Adapter<ClipsKeyboardAdapter.ViewHolder>() {

    private val layoutInflater = LayoutInflater.from(context)
    private var textColor = context.getProperTextColor()
    private var backgroundColor = context.getProperBackgroundColor()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = when (viewType) {
            ITEM_SECTION_LABEL -> R.layout.item_section_label
            else -> R.layout.item_clip_on_keyboard
        }

        val view = layoutInflater.inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
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
        view.apply {
            val rippleBg = clip_holder.background as RippleDrawable
            val layerDrawable = rippleBg.findDrawableByLayerId(R.id.clipboard_background_holder) as LayerDrawable
            layerDrawable.findDrawableByLayerId(R.id.clipboard_background_stroke).applyColorFilter(context.getStrokeColor())
            layerDrawable.findDrawableByLayerId(R.id.clipboard_background_shape).applyColorFilter(backgroundColor)

            clip_value.apply {
                text = clip.value
                removeUnderlines()
                setTextColor(textColor)
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupSection(view: View, sectionLabel: ClipsSectionLabel) {
        view.apply {
            clips_section_label.apply {
                text = sectionLabel.value
                setTextColor(textColor)
            }

            clips_section_icon.apply {
                applyColorFilter(textColor)

                if (sectionLabel.isCurrent) {
                    setOnLongClickListener { context.toast(R.string.pin_text); true; }
                    setImageDrawable(resources.getDrawable(R.drawable.ic_pin))
                    setOnClickListener {
                        ensureBackgroundThread {
                            val currentClip = context.getCurrentClip() ?: return@ensureBackgroundThread
                            val clip = Clip(null, currentClip)
                            ClipsHelper(context).insertClip(clip)
                            refreshClipsListener.refreshClips()
                            context.toast(R.string.text_pinned)
                            if (context.config.vibrateOnKeypress) {
                                performHapticFeedback()
                            }
                        }
                    }
                } else {
                    setImageDrawable(resources.getDrawable(R.drawable.ic_pin_filled))
                    background = null   // avoid doing any animations on clicking clipboard_manager_holder
                }
            }
        }
    }

    open inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
