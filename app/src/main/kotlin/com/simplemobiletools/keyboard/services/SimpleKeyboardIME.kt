package com.simplemobiletools.keyboard.services

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.EditorInfo.IME_ACTION_NONE
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
    private val KEYBOARD_LETTERS = 0
    private val KEYBOARD_SYMBOLS = 1
    private val KEYBOARD_SYMBOLS_SHIFT = 2

    private var keyboard: MyKeyboard? = null
    private var keyboardView: MyKeyboardView? = null
    private var lastShiftPressTS = 0L
    private var keyboardMode = KEYBOARD_LETTERS
    private var inputType = InputType.TYPE_CLASS_TEXT
    private var enterKeyType = IME_ACTION_NONE

    override fun onInitializeInterface() {
        super.onInitializeInterface()
        keyboard = MyKeyboard(this, R.xml.keys_letters, enterKeyType)
    }

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view_keyboard, null) as MyKeyboardView
        keyboardView!!.setKeyboard(keyboard!!)
        keyboardView!!.onKeyboardActionListener = this
        return keyboardView!!
    }

    override fun onPress(primaryCode: Int) {
        keyboardView?.performHapticFeedback()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        inputType = attribute!!.inputType and InputType.TYPE_MASK_CLASS
        enterKeyType = attribute.imeOptions and (EditorInfo.IME_MASK_ACTION or EditorInfo.IME_FLAG_NO_ENTER_ACTION)

        val shiftMode = when (currentInputConnection.getCursorCapsMode(attribute.inputType)) {
            InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS -> SHIFT_ON_PERMANENT
            InputType.TYPE_TEXT_FLAG_CAP_WORDS, InputType.TYPE_TEXT_FLAG_CAP_SENTENCES -> SHIFT_ON_ONE_CHAR
            else -> SHIFT_OFF
        }

        val keyboardXml = when (inputType) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_DATETIME, InputType.TYPE_CLASS_PHONE -> {
                keyboardMode = KEYBOARD_SYMBOLS
                R.xml.keys_symbols
            }
            else -> {
                keyboardMode = KEYBOARD_LETTERS
                R.xml.keys_letters
            }
        }

        keyboard = MyKeyboard(this, keyboardXml, enterKeyType)
        keyboard!!.setShifted(shiftMode)
        keyboardView?.setKeyboard(keyboard!!)
    }

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
                if (keyboardMode == KEYBOARD_LETTERS) {
                    when {
                        keyboard!!.shiftState == SHIFT_ON_PERMANENT -> keyboard!!.shiftState = SHIFT_OFF
                        System.currentTimeMillis() - lastShiftPressTS < SHIFT_PERM_TOGGLE_SPEED -> keyboard!!.shiftState = SHIFT_ON_PERMANENT
                        keyboard!!.shiftState == SHIFT_ON_ONE_CHAR -> keyboard!!.shiftState = SHIFT_OFF
                        keyboard!!.shiftState == SHIFT_OFF -> keyboard!!.shiftState = SHIFT_ON_ONE_CHAR
                    }

                    lastShiftPressTS = System.currentTimeMillis()
                } else {
                    val keyboardXml = if (keyboardMode == KEYBOARD_SYMBOLS) {
                        keyboardMode = KEYBOARD_SYMBOLS_SHIFT
                        R.xml.keys_symbols_shift
                    } else {
                        keyboardMode = KEYBOARD_SYMBOLS
                        R.xml.keys_symbols
                    }
                    keyboard = MyKeyboard(this, keyboardXml, enterKeyType)
                    keyboardView!!.setKeyboard(keyboard!!)
                }
                keyboardView!!.invalidateAllKeys()
            }
            MyKeyboard.KEYCODE_ENTER -> {
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            MyKeyboard.KEYCODE_MODE_CHANGE -> {
                val keyboardXml = if (keyboardMode == KEYBOARD_LETTERS) {
                    keyboardMode = KEYBOARD_SYMBOLS
                    R.xml.keys_symbols
                } else {
                    keyboardMode = KEYBOARD_LETTERS
                    R.xml.keys_letters
                }
                keyboard = MyKeyboard(this, keyboardXml, enterKeyType)
                keyboardView!!.setKeyboard(keyboard!!)
            }
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

    override fun onRelease(primaryCode: Int) {}
}
