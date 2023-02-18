package com.simplemobiletools.keyboard.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.databinding.DialogAddOrEditClipBinding
import com.simplemobiletools.keyboard.helpers.ClipsHelper
import com.simplemobiletools.keyboard.models.Clip

class AddOrEditClipDialog(val activity: BaseSimpleActivity, val originalClip: Clip?, val callback: () -> Unit) {
    init {
        val binding = DialogAddOrEditClipBinding.inflate(activity.layoutInflater)
        if (originalClip != null) {
            binding.addClipValue.setText(originalClip.value)
        }
        /*
        val view = activity.layoutInflater.inflate(R.layout.dialog_add_or_edit_clip, null).apply {
            if (originalClip != null) {
                add_clip_value.setText(originalClip.value)
            }
        }
        */

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->  // TODO check correct view is passed
                    alertDialog.showKeyboard(binding.addClipValue)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val clipValue = binding.addClipValue.value
                        if (clipValue.isEmpty()) {
                            activity.toast(R.string.value_cannot_be_empty)
                            return@setOnClickListener
                        }

                        val clip = Clip(null, clipValue)
                        if (originalClip != null) {
                            clip.id = originalClip.id
                        }

                        ensureBackgroundThread {
                            ClipsHelper(activity).insertClip(clip)
                            activity.runOnUiThread {
                                callback()
                                alertDialog.dismiss()
                            }
                        }
                    }
                }
            }
    }
}
