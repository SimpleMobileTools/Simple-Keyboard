package com.simplemobiletools.keyboard.services

import android.inputmethodservice.InputMethodService
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import com.simplemobiletools.commons.extensions.performHapticFeedback
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.helpers.MyKeyboard
import com.simplemobiletools.keyboard.helpers.SHIFT_OFF
import com.simplemobiletools.keyboard.helpers.SHIFT_ON_ONE_CHAR
import com.simplemobiletools.keyboard.helpers.SHIFT_ON_PERMANENT
import com.simplemobiletools.keyboard.views.MyKeyboardView

// based on https://www.androidauthority.com/lets-build-custom-keyboard-android-832362/
class SimpleKeyboardIME : InputMethodService(), MyKeyboardView.OnKeyboardActionListener {
    private var SHIFT_PERM_TOGGLE_SPEED = 500   // how quickly do we have to doubletap shift to enable permanent caps lock

    private var keyboard: MyKeyboard? = null
    private var keyboardView: MyKeyboardView? = null
    private var lastShiftPressTS = 0L

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view_keyboard, null) as MyKeyboardView
        keyboard = MyKeyboard(this, R.xml.keys_letters)
        keyboardView!!.setKeyboard(keyboard!!)
        keyboardView!!.onKeyboardActionListener = this
        return keyboardView!!
    }

    override fun onPress(primaryCode: Int) {
        keyboardView?.performHapticFeedback()
    }

    override fun onRelease(primaryCode: Int) {}

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val inputConnection = currentInputConnection
        if (keyboard == null || inputConnection == null) {
            return
        }

        when (primaryCode) {
            MyKeyboard.KEYCODE_DELETE -> {
                if (keyboard!!.shiftState == SHIFT_ON_ONE_CHAR) {
                    keyboard!!.shiftState = SHIFT_OFF
                }

                val selectedText = inputConnection.getSelectedText(0)
                if (TextUtils.isEmpty(selectedText)) {
                    inputConnection.deleteSurroundingText(1, 0)
                } else {
                    inputConnection.commitText("", 1)
                }
                keyboardView!!.invalidateAllKeys()
            }
            MyKeyboard.KEYCODE_SHIFT -> {
                when {
                    keyboard!!.shiftState == SHIFT_ON_PERMANENT -> keyboard!!.shiftState = SHIFT_OFF
                    System.currentTimeMillis() - lastShiftPressTS < SHIFT_PERM_TOGGLE_SPEED -> keyboard!!.shiftState = SHIFT_ON_PERMANENT
                    keyboard!!.shiftState == SHIFT_ON_ONE_CHAR -> keyboard!!.shiftState = SHIFT_OFF
                    keyboard!!.shiftState == SHIFT_OFF -> keyboard!!.shiftState = SHIFT_ON_ONE_CHAR
                }

                lastShiftPressTS = System.currentTimeMillis()
                keyboardView!!.invalidateAllKeys()
            }
            MyKeyboard.KEYCODE_DONE -> inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            MyKeyboard.KEYCODE_MODE_CHANGE -> {}
            else -> {
                var code = primaryCode.toChar()
                if (Character.isLetter(code) && keyboard!!.shiftState > SHIFT_OFF) {
                    code = Character.toUpperCase(code)
                }

                inputConnection.commitText(code.toString(), 1)
                if (keyboard!!.shiftState == SHIFT_ON_ONE_CHAR) {
                    keyboard!!.shiftState = SHIFT_OFF
                    keyboardView!!.invalidateAllKeys()
                }
            }
        }
    }

    override fun onText(text: CharSequence?) {}

    override fun swipeLeft() {}

    override fun swipeRight() {}

    override fun swipeDown() {}

    override fun swipeUp() {}
}
