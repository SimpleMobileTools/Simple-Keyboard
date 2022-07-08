package com.simplemobiletools.keyboard.activities

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.adapters.ClipsActivityAdapter
import com.simplemobiletools.keyboard.dialogs.AddOrEditClipDialog
import com.simplemobiletools.keyboard.dialogs.ExportClipsDialog
import com.simplemobiletools.keyboard.extensions.clipsDB
import com.simplemobiletools.keyboard.extensions.config
import com.simplemobiletools.keyboard.helpers.ClipsHelper
import com.simplemobiletools.keyboard.models.Clip
import kotlinx.android.synthetic.main.activity_manage_clipboard_items.*
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class ManageClipboardItemsActivity : SimpleActivity(), RefreshRecyclerViewListener {
    private val PICK_EXPORT_CLIPS_INTENT = 21
    private val PICK_IMPORT_CLIPS_SOURCE_INTENT = 22

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_clipboard_items)
        setupOptionsMenu()
        updateTextColors(clipboard_items_wrapper)
        updateClips()

        clipboard_items_placeholder.text = "${getText(R.string.manage_clipboard_empty)}\n\n${getText(R.string.manage_clips)}"
        clipboard_items_placeholder_2.apply {
            underlineText()
            setTextColor(getProperPrimaryColor())
            setOnClickListener {
                addOrEditClip()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(clipboard_toolbar, NavigationIcon.Arrow)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_EXPORT_CLIPS_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            exportClipsTo(outputStream)
        } else if (requestCode == PICK_IMPORT_CLIPS_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val inputStream = contentResolver.openInputStream(resultData.data!!)
            parseFile(inputStream)
        }
    }


    private fun setupOptionsMenu() {
        clipboard_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_clipboard_item -> {
                    addOrEditClip()
                    true
                }
                R.id.export_clips -> {
                    exportClips()
                    true
                }
                R.id.import_clips -> {
                    importClips()
                    true
                }
                else -> false
            }
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

                    try {
                        startActivityForResult(this, PICK_EXPORT_CLIPS_INTENT)
                    } catch (e: ActivityNotFoundException) {
                        toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                    } catch (e: Exception) {
                        showErrorToast(e)
                    }
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

    private fun importClips() {
        if (isQPlus()) {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"

                try {
                    startActivityForResult(this, PICK_IMPORT_CLIPS_SOURCE_INTENT)
                } catch (e: ActivityNotFoundException) {
                    toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        } else {
            handlePermission(PERMISSION_READ_STORAGE) {
                if (it) {
                    FilePickerDialog(this) {
                        ensureBackgroundThread {
                            parseFile(File(it).inputStream())
                        }
                    }
                }
            }
        }
    }

    private fun parseFile(inputStream: InputStream?) {
        if (inputStream == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        var clipsImported = 0
        ensureBackgroundThread {
            try {
                val token = object : TypeToken<List<String>>() {}.type
                val clipValues = Gson().fromJson<ArrayList<String>>(inputStream.bufferedReader(), token) ?: ArrayList()
                clipValues.forEach { value ->
                    val clip = Clip(null, value)
                    if (ClipsHelper(this).insertClip(clip) > 0) {
                        clipsImported++
                    }
                }

                runOnUiThread {
                    val msg = if (clipsImported > 0) R.string.importing_successful else R.string.no_new_entries_for_importing
                    toast(msg)
                    updateClips()
                }
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }
}
