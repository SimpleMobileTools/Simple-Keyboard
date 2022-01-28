package com.simplemobiletools.keyboard.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.extensions.removeUnderlines
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.extensions.config
import com.simplemobiletools.keyboard.models.Clip
import com.simplemobiletools.keyboard.models.ListItem
import kotlinx.android.synthetic.main.item_clip_on_keyboard.view.*
import java.util.*

class ClipsKeyboardAdapter(val context: Context, var clips: ArrayList<ListItem>, val itemClick: (clip: Clip) -> Unit) :
    RecyclerView.Adapter<ClipsKeyboardAdapter.ViewHolderr>() {

    private val layoutInflater = LayoutInflater.from(context)
    private val baseConfig = context.config
    private var textColor = baseConfig.textColor

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderr {
        val view = layoutInflater.inflate(R.layout.item_clip_on_keyboard, parent, false)
        return ViewHolderr(view)
    }

    override fun onBindViewHolder(holder: ViewHolderr, position: Int) {
        val item = clips[position]
        holder.bindView(item) { itemView ->
            when (item) {
                is Clip -> setupClip(itemView, item)
            }
        }
    }

    override fun getItemCount() = clips.size

    private fun setupClip(view: View, clip: Clip) {
        view.clip_value.apply {
            text = clip.value
            removeUnderlines()
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
