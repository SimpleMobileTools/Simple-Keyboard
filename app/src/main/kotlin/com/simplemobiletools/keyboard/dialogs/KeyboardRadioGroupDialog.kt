package com.simplemobiletools.keyboard.dialogs

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.extensions.onGlobalLayout
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.extensions.getKeyboardDialogBuilder
import com.simplemobiletools.keyboard.extensions.setupKeyboardDialogStuff

class KeyboardRadioGroupDialog(
    private val inputView: View,
    private val items: ArrayList<RadioItem>,
    private val checkedItemId: Int = -1,
    private val titleId: Int = 0,
    showOKButton: Boolean = false,
    private val cancelCallback: (() -> Unit)? = null,
    private val callback: (newValue: Any) -> Unit
) {
    private val context = ContextThemeWrapper(inputView.context, R.style.MyKeyboard_Alert)
    private var dialog: AlertDialog? = null
    private var wasInit = false
    private var selectedItemId = -1
    private val layoutInflater = LayoutInflater.from(context)

    init {
        val view = layoutInflater.inflate(R.layout.dialog_radio_group, null)
        val radioGroup = view.findViewById<RadioGroup>(R.id.dialog_radio_group).apply {
            for (i in 0 until items.size) {
                val radioButton = (layoutInflater.inflate(R.layout.radio_button, null) as RadioButton).apply {
                    text = items[i].title
                    isChecked = items[i].id == checkedItemId
                    id = i
                    setOnClickListener { itemSelected(i) }
                }

                if (items[i].id == checkedItemId) {
                    selectedItemId = i
                }

                addView(radioButton, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }

        val builder = context.getKeyboardDialogBuilder()
            .setOnCancelListener { cancelCallback?.invoke() }

        if (selectedItemId != -1 && showOKButton) {
            builder.setPositiveButton(R.string.ok) { _, _ -> itemSelected(selectedItemId) }
        }

        builder.apply {
            context.setupKeyboardDialogStuff(inputView.windowToken, view, this, titleId) { alertDialog ->
                dialog = alertDialog
            }
        }

        if (selectedItemId != -1) {
            view.findViewById<ScrollView>(R.id.dialog_radio_holder).apply {
                onGlobalLayout {
                    scrollY = radioGroup.findViewById<View>(selectedItemId).bottom - height
                }
            }
        }

        wasInit = true
    }

    private fun itemSelected(checkedId: Int) {
        if (wasInit) {
            callback(items[checkedId].value)
            dialog?.dismiss()
        }
    }
}
