package com.simplemobiletools.keyboard.adapters

import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.interfaces.ItemMoveCallback
import com.simplemobiletools.commons.interfaces.ItemTouchHelperContract
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.interfaces.StartReorderDragListener
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.dialogs.AddOrEditClipDialog
import com.simplemobiletools.keyboard.extensions.clipsDB
import com.simplemobiletools.keyboard.helpers.ClipsHelper
import com.simplemobiletools.keyboard.models.Clip
import kotlinx.android.synthetic.main.item_clip_in_activity.view.*
import java.util.*

class ClipsActivityAdapter(
    activity: BaseSimpleActivity, var items: ArrayList<Clip>, recyclerView: MyRecyclerView, val listener: RefreshRecyclerViewListener, itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick), ItemTouchHelperContract {

    private var touchHelper: ItemTouchHelper? = null
    private var startReorderDragListener: StartReorderDragListener
    private var wasClipMoved = false

    init {
        setupDragListener(true)

        touchHelper = ItemTouchHelper(ItemMoveCallback(this))
        touchHelper!!.attachToRecyclerView(recyclerView)

        startReorderDragListener = object : StartReorderDragListener {
            override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
                touchHelper?.startDrag(viewHolder)
            }
        }
    }

    override fun getActionMenuId() = R.menu.cab_clips

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_edit).isVisible = isOneItemSelected()
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_edit -> editClip()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = items.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = items.getOrNull(position)?.id?.toInt()

    override fun getItemKeyPosition(key: Int) = items.indexOfFirst { it.id?.toInt() == key }

    override fun onActionModeCreated() {
        notifyDataSetChanged()
    }

    override fun onActionModeDestroyed() {
        if (wasClipMoved) {
            ensureBackgroundThread {
                activity.clipsDB.deleteAll()
                items.forEach { clip ->
                    clip.id = null
                    clip.id = ClipsHelper(activity).insertClip(clip)
                }

                activity.runOnUiThread {
                    notifyDataSetChanged()
                }
            }
        } else {
            notifyDataSetChanged()
        }

        wasClipMoved = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.item_clip_in_activity, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bindView(item, true, true) { itemView, layoutPosition ->
            setupView(itemView, item, holder)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = items.size

    private fun editClip() {
        val selectedClip = getSelectedItems().firstOrNull() ?: return
        AddOrEditClipDialog(activity, selectedClip) {
            listener.refreshItems()
            finishActMode()
        }
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(activity, "", R.string.proceed_with_deletion, R.string.yes, R.string.cancel) {
            deleteSelection()
        }
    }

    private fun deleteSelection() {
        val deleteClips = ArrayList<Clip>(selectedKeys.size)
        val positions = getSelectedItemPositions()

        getSelectedItems().forEach {
            deleteClips.add(it)
        }

        items.removeAll(deleteClips)
        removeSelectedItems(positions)

        ensureBackgroundThread {
            deleteClips.forEach { clip ->
                activity.clipsDB.delete(clip.id!!.toLong())
            }

            if (items.isEmpty()) {
                listener.refreshItems()
            }
        }
    }

    private fun getSelectedItems() = items.filter { selectedKeys.contains(it.id!!.toInt()) } as ArrayList<Clip>

    private fun setupView(view: View, clip: Clip, holder: ViewHolder) {
        if (clip.id == null) {
            return
        }

        val isSelected = selectedKeys.contains(clip.id!!.toInt())
        view.apply {
            clip_value.text = clip.value
            clip_value.setTextColor(textColor)
            clip_drag_handle.applyColorFilter(textColor)

            clip_drag_handle.beVisibleIf(selectedKeys.isNotEmpty())
            clip_holder.isSelected = isSelected
            clip_drag_handle.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    startReorderDragListener.requestDrag(holder)
                }
                false
            }
        }
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        wasClipMoved = true
    }

    override fun onRowSelected(myViewHolder: ViewHolder?) {}

    override fun onRowClear(myViewHolder: ViewHolder?) {}
}
