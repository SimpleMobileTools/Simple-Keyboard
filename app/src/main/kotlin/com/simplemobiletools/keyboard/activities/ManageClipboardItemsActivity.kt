package com.simplemobiletools.keyboard.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.underlineText
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.adapters.ClipsActivityAdapter
import com.simplemobiletools.keyboard.dialogs.AddOrEditClipDialog
import com.simplemobiletools.keyboard.extensions.clipsDB
import com.simplemobiletools.keyboard.models.Clip
import kotlinx.android.synthetic.main.activity_manage_clipboard_items.*

class ManageClipboardItemsActivity : SimpleActivity(), RefreshRecyclerViewListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_clipboard_items)
        updateTextColors(clipboard_items_wrapper)
        updateClips()

        clipboard_items_placeholder.text = "${getText(R.string.manage_clipboard_empty)}\n\n${getText(R.string.manage_clips)}"
        clipboard_items_placeholder_2.apply {
            underlineText()
            setTextColor(getAdjustedPrimaryColor())
            setOnClickListener {
                addOrEditClip()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_manage_clipboard_items, menu)
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_clipboard_item -> addOrEditClip()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun updateClips() {
        ensureBackgroundThread {
            val clips = clipsDB.getClips().toMutableList() as ArrayList<Clip>
            runOnUiThread {
                ClipsActivityAdapter(this, clips, clipboard_items_list, this) {
                    addOrEditClip(it as Clip)
                }.apply {
                    clipboard_items_list.adapter = this
                }

                clipboard_items_list.beVisibleIf(clips.isNotEmpty())
                clipboard_items_placeholder.beVisibleIf(clips.isEmpty())
                clipboard_items_placeholder_2.beVisibleIf(clips.isEmpty())
            }
        }
    }

    private fun addOrEditClip(clip: Clip? = null) {
        AddOrEditClipDialog(this, clip) {
            updateClips()
        }
    }

    override fun refreshItems() {
        updateClips()
    }
}
