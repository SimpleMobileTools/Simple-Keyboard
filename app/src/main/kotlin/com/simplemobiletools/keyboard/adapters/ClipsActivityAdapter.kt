package com.simplemobiletools.keyboard.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.interfaces.ItemMoveCallback
import com.simplemobiletools.commons.interfaces.ItemTouchHelperContract
import com.simplemobiletools.commons.interfaces.StartReorderDragListener
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.commons.views.bottomactionmenu.BottomActionMenuView
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.models.Clip
import kotlinx.android.synthetic.main.item_clip_in_activity.view.*
import java.util.*

class ClipsActivityAdapter(
    activity: BaseSimpleActivity, var items: ArrayList<Clip>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick), ItemTouchHelperContract {

    private var touchHelper: ItemTouchHelper? = null
    private var startReorderDragListener: StartReorderDragListener

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

    override fun onBottomActionMenuCreated(view: BottomActionMenuView) {}

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_delete -> deleteSelection()
        }
    }

    override fun getSelectableItemCount() = items.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = items.getOrNull(position)?.id?.toInt()

    override fun getItemKeyPosition(key: Int) = items.indexOfFirst { it.id == key.toLong() }

    override fun onActionModeDestroyed() {
        notifyDataSetChanged()
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

    private fun deleteSelection() {}

    private fun getItemWithKey(key: Int): Clip? = items.firstOrNull { it.id == key.toLong() }

    private fun getSelectedItems() = items.filter { selectedKeys.contains(it.id!!.toInt()) } as ArrayList<Clip>

    private fun setupView(view: View, clip: Clip, holder: ViewHolder) {
        val isSelected = selectedKeys.contains(clip.id!!.toInt())
        view.apply {
            clip_value.text = clip.value
            clip_value.setTextColor(textColor)

            clip_holder.isSelected = isSelected
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
    }

    override fun onRowSelected(myViewHolder: ViewHolder?) {}

    override fun onRowClear(myViewHolder: ViewHolder?) {}
}
