package com.simplemobiletools.keyboard.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.gson.Gson
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.helpers.isQPlus
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.adapters.ClipsActivityAdapter
import com.simplemobiletools.keyboard.dialogs.AddOrEditClipDialog
import com.simplemobiletools.keyboard.dialogs.ExportClipsDialog
import com.simplemobiletools.keyboard.extensions.clipsDB
import com.simplemobiletools.keyboard.extensions.config
import com.simplemobiletools.keyboard.models.Clip
import kotlinx.android.synthetic.main.activity_manage_clipboard_items.*
import java.io.File
import java.io.OutputStream

class ManageClipboardItemsActivity : SimpleActivity(), RefreshRecyclerViewListener {
    private val PICK_EXPORT_CLIPS_INTENT = 21

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
            R.id.export_clips -> exportClips()
            R.id.import_clips -> importClips()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_EXPORT_CLIPS_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            exportClipsTo(outputStream)
        }
    }

    override fun refreshItems() {
        updateClips()
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

    private fun exportClips() {
        if (isQPlus()) {
            ExportClipsDialog(this, config.lastExportedClipsFolder, true) { path, filename ->
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, filename)
                    addCategory(Intent.CATEGORY_OPENABLE)
                    startActivityForResult(this, PICK_EXPORT_CLIPS_INTENT)
                }
            }
        } else {
            handlePermission(PERMISSION_WRITE_STORAGE) {
                if (it) {
                    ExportClipsDialog(this, config.lastExportedClipsFolder, false) { path, filename ->
                        val file = File(path)
                        getFileOutputStream(file.toFileDirItem(this), true) {
                            exportClipsTo(it)
                        }
                    }
                }
            }
        }
    }

    private fun exportClipsTo(outputStream: OutputStream?) {
        if (outputStream == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        ensureBackgroundThread {
            val clips = clipsDB.getClips().map { it.value }
            if (clips.isEmpty()) {
                toast(R.string.no_entries_for_exporting)
                return@ensureBackgroundThread
            }


            val json = Gson().toJson(clips)
            outputStream.bufferedWriter().use { out ->
                out.write(json)
            }

            toast(R.string.exporting_successful)
        }
    }

    private fun importClips() {}
}
