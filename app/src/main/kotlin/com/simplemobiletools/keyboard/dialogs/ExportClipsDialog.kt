package com.simplemobiletools.keyboard.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.databinding.DialogExportClipsBinding
import com.simplemobiletools.keyboard.extensions.config

class ExportClipsDialog(
    val activity: BaseSimpleActivity, path: String, val hidePath: Boolean, callback: (path: String, filename: String) -> Unit
) {
    init {
        var folder = if (path.isNotEmpty() && activity.getDoesFilePathExist(path)) {
            path
        } else {
            activity.internalStoragePath
        }

        val binding = DialogExportClipsBinding.inflate(activity.layoutInflater)
        val view = activity.layoutInflater.inflate(R.layout.dialog_export_clips, null).apply {
            binding.exportClipsFilename.setText("${activity.getString(R.string.app_launcher_name)}_${activity.getCurrentFormattedDateTime()}")

            if (hidePath) {
                binding.exportClipsPathLabel.beGone()
                binding.exportClipsPath.beGone()
            } else {
                binding.exportClipsPath.text = activity.humanizePath(folder)
                binding.exportClipsPath.setOnClickListener {
                    FilePickerDialog(activity, folder, false, showFAB = true) {
                        binding.exportClipsPath.text = activity.humanizePath(it)
                        folder = it
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.export_clipboard_items) { alertDialog ->
                    alertDialog.showKeyboard(binding.exportClipsFilename)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = binding.exportClipsFilename.value
                        if (filename.isEmpty()) {
                            activity.toast(R.string.filename_cannot_be_empty)
                            return@setOnClickListener
                        }

                        val newPath = "${folder.trimEnd('/')}/$filename"
                        if (!newPath.getFilenameFromPath().isAValidFilename()) {
                            activity.toast(R.string.filename_invalid_characters)
                            return@setOnClickListener
                        }

                        activity.config.lastExportedClipsFolder = folder
                        if (!hidePath && activity.getDoesFilePathExist(newPath)) {
                            val title = String.format(activity.getString(R.string.file_already_exists_overwrite), newPath.getFilenameFromPath())
                            ConfirmationDialog(activity, title) {
                                callback(newPath, filename)
                                alertDialog.dismiss()
                            }
                        } else {
                            callback(newPath, filename)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }
}
