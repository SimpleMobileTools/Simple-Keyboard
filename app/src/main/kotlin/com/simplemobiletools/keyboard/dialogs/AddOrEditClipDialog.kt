package com.simplemobiletools.keyboard.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.helpers.ClipsHelper
import com.simplemobiletools.keyboard.models.Clip
import kotlinx.android.synthetic.main.dialog_add_or_edit_clip.view.*

class AddOrEditClipDialog(val activity: BaseSimpleActivity, val originalClip: Clip?, val callback: () -> Unit) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_add_or_edit_clip, null).apply {
            if (originalClip != null) {
                add_clip_value.setText(originalClip.value)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    alertDialog.showKeyboard(view.add_clip_value)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val clipValue = view.add_clip_value.value
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
