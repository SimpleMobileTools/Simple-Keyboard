package com.simplemobiletools.keyboard.services

import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.text.InputType.*
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.EditorInfo.IME_ACTION_NONE
import android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION
import android.view.inputmethod.EditorInfo.IME_MASK_ACTION
import android.view.inputmethod.ExtractedTextRequest
import com.simplemobiletools.commons.extensions.getSharedPrefs
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.extensions.config
import com.simplemobiletools.keyboard.helpers.*
import com.simplemobiletools.keyboard.views.MyKeyboardView
import kotlinx.android.synthetic.main.keyboard_view_keyboard.view.keyboard_holder
import kotlinx.android.synthetic.main.keyboard_view_keyboard.view.keyboard_view

// based on https://www.androidauthority.com/lets-build-custom-keyboard-android-832362/
class SimpleKeyboardIME : InputMethodService(), MyKeyboardView.OnKeyboardActionListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private var SHIFT_PERM_TOGGLE_SPEED = 500   // how quickly do we have to doubletap shift to enable permanent caps lock
    private val KEYBOARD_LETTERS = 0
    private val KEYBOARD_SYMBOLS = 1
    private val KEYBOARD_SYMBOLS_SHIFT = 2
    private val KEYBOARD_NUMBERS = 3
    private val KEYBOARD_PHONE = 4

    private var keyboard: MyKeyboard? = null
    private var keyboardView: MyKeyboardView? = null
    private var lastShiftPressTS = 0L
    private var keyboardMode = KEYBOARD_LETTERS
    private var inputTypeClass = InputType.TYPE_CLASS_TEXT
    private var inputTypeClassVariation = InputType.TYPE_CLASS_TEXT
    private var enterKeyType = IME_ACTION_NONE
    private var switchToLetters = false

    override fun onInitializeInterface() {
        super.onInitializeInterface()
        getSharedPrefs().registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateInputView(): View {
        val keyboardHolder = layoutInflater.inflate(R.layout.keyboard_view_keyboard, null)
        keyboardView = keyboardHolder.keyboard_view as MyKeyboardView
        keyboardView!!.setKeyboard(keyboard!!)
        keyboardView!!.setKeyboardHolder(keyboardHolder.keyboard_holder)
        keyboardView!!.setEditorInfo(currentInputEditorInfo)
        keyboardView!!.mOnKeyboardActionListener = this
        return keyboardHolder!!
    }

    override fun onPress(primaryCode: Int) {
        if (primaryCode != 0) {
            keyboardView?.vibrateIfNeeded()
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        inputTypeClass = attribute!!.inputType and TYPE_MASK_CLASS
        inputTypeClassVariation = attribute!!.inputType and TYPE_MASK_VARIATION

        enterKeyType = attribute.imeOptions and (IME_MASK_ACTION or IME_FLAG_NO_ENTER_ACTION)
        keyboard = createNewKeyboard()
        keyboardView?.setKeyboard(keyboard!!)
        keyboardView?.setEditorInfo(attribute)
        updateShiftKeyState(null)
    }

    private fun updateShiftKeyState(code: Int?) {
        if (keyboardMode != KEYBOARD_LETTERS || ShiftState.isInputTypeAllowedCapitalizing(inputTypeClassVariation)) {
            return
        }

        if (code == MyKeyboard.KEYCODE_SHIFT || code == MyKeyboard.KEYCODE_DELETE) {
            return
        }

        val text = currentInputConnection.getTextBeforeCursor(2, 0) ?: return
        // capitalize first letter on startup or if text is empty
        if (code == null || text.isEmpty()) {
            keyboard!!.setShifted(ShiftState.ON_ONE_CHAR)
            keyboardView?.invalidateAllKeys()
            return
        }

        // capitalize sentences if needed
        if (config.enableSentencesCapitalization) {

            // capitalize on Enter click
            if (code == MyKeyboard.KEYCODE_ENTER) {
                keyboard!!.setShifted(ShiftState.ON_ONE_CHAR)
                keyboardView?.invalidateAllKeys()
                return
            }


            if (ShiftState.shouldCapitalize(this, text.toString())) {
                keyboard!!.setShifted(ShiftState.ON_ONE_CHAR)
                keyboardView?.invalidateAllKeys()
                return
            } else {
                // try capitalizing based on the editor info like google keep or google messenger apps
                val editorInfo = currentInputEditorInfo

                if (editorInfo != null && editorInfo.inputType != InputType.TYPE_NULL) {
                    if (currentInputConnection.getCursorCapsMode(editorInfo.inputType) != 0) {
                        keyboard?.setShifted(ShiftState.ON_ONE_CHAR)
                        keyboardView?.invalidateAllKeys()
                        return
                    }
                }
            }
        }

        // In other cases reset shift to OFF
        keyboard?.setShifted(ShiftState.OFF)
        keyboardView?.invalidateAllKeys()
    }

    override fun onKey(code: Int) {
        val inputConnection = currentInputConnection
        if (keyboard == null || inputConnection == null) {
            return
        }

//        this.keyboardView.setEditorInfo(EditorInfo)

        if (code != MyKeyboard.KEYCODE_SHIFT) {
            lastShiftPressTS = 0
        }

        when (code) {
            MyKeyboard.KEYCODE_DELETE -> {

                val selectedText = inputConnection.getSelectedText(0)
                if (TextUtils.isEmpty(selectedText)) {
                    val text = inputConnection.getTextBeforeCursor(3, 0)?.dropLast(1)

                    if (keyboard?.mShiftState != ShiftState.ON_PERMANENT) {
                        keyboard?.setShifted(ShiftState.getShiftStateForText(this, inputTypeClassVariation, text?.toString()))
                        keyboardView?.invalidateAllKeys()
                    }

                    inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                    inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                } else {


                    inputConnection.commitText("", 1)
                }
            }

            MyKeyboard.KEYCODE_SHIFT -> {
                if (keyboardMode == KEYBOARD_LETTERS) {
                    when {
                        keyboard!!.mShiftState == ShiftState.ON_PERMANENT -> keyboard!!.mShiftState = ShiftState.OFF
                        System.currentTimeMillis() - lastShiftPressTS < SHIFT_PERM_TOGGLE_SPEED -> keyboard!!.mShiftState = ShiftState.ON_PERMANENT
                        keyboard!!.mShiftState == ShiftState.ON_ONE_CHAR -> keyboard!!.mShiftState = ShiftState.OFF
                        keyboard!!.mShiftState == ShiftState.OFF -> keyboard!!.mShiftState = ShiftState.ON_ONE_CHAR
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
                val imeOptionsActionId = getImeOptionsActionId()
                if (imeOptionsActionId != IME_ACTION_NONE) {
                    inputConnection.performEditorAction(imeOptionsActionId)
                } else {
                    inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
            }

            MyKeyboard.KEYCODE_MODE_CHANGE -> {
                val keyboardXml = if (keyboardMode == KEYBOARD_LETTERS) {
                    keyboardMode = KEYBOARD_SYMBOLS
                    R.xml.keys_symbols
                } else {
                    keyboardMode = KEYBOARD_LETTERS
                    getKeyboardLayoutXML()
                }
                keyboard = MyKeyboard(this, keyboardXml, enterKeyType)
                keyboardView!!.setKeyboard(keyboard!!)
            }

            MyKeyboard.KEYCODE_EMOJI -> {
                keyboardView?.openEmojiPalette()
            }

            else -> {
                var codeChar = code.toChar()
                val originalText = inputConnection.getExtractedText(ExtractedTextRequest(), 0)?.text ?: return

                if (Character.isLetter(codeChar) && keyboard!!.mShiftState > ShiftState.OFF) {
                    codeChar = Character.toUpperCase(codeChar)
                }

                // If the keyboard is set to symbols and the user presses space, we usually should switch back to the letters keyboard.
                // However, avoid doing that in cases when the EditText for example requires numbers as the input.
                // We can detect that by the text not changing on pressing Space.
                if (keyboardMode != KEYBOARD_LETTERS && code == MyKeyboard.KEYCODE_SPACE) {
                    inputConnection.commitText(codeChar.toString(), 1)
                    val newText = inputConnection.getExtractedText(ExtractedTextRequest(), 0)?.text
                    if (originalText != newText) {
                        switchToLetters = true
                    }
                } else {
                    inputConnection.commitText(codeChar.toString(), 1)
                }
            }
        }
        if (keyboard!!.mShiftState != ShiftState.ON_PERMANENT) {
            updateShiftKeyState(code)
        }
    }

    override fun onActionUp() {
        if (switchToLetters) {
            // TODO: Change keyboardMode to enum class
            keyboardMode = KEYBOARD_LETTERS

            //Check if capitalization is needed after switching to letters layout
            val text = currentInputConnection?.getTextBeforeCursor(2, 0)
            val newShiftState = ShiftState.getShiftStateForText(this, inputTypeClassVariation, text?.toString())

            keyboard = MyKeyboard(this, getKeyboardLayoutXML(), enterKeyType, shiftState = newShiftState)

            val editorInfo = currentInputEditorInfo
            if (editorInfo != null && editorInfo.inputType != InputType.TYPE_NULL && keyboard?.mShiftState != ShiftState.ON_PERMANENT) {
                if (currentInputConnection.getCursorCapsMode(editorInfo.inputType) != 0) {
                    keyboard?.setShifted(ShiftState.ON_ONE_CHAR)
                }
            }

            keyboardView!!.setKeyboard(keyboard!!)
            switchToLetters = false
        }
    }

    override fun moveCursorLeft() {
        moveCursor(false)
    }

    override fun moveCursorRight() {
        moveCursor(true)
    }

    override fun onText(text: String) {
        currentInputConnection?.commitText(text, 0)
    }

    override fun reloadKeyboard() {
        val keyboard = createNewKeyboard()
        this.keyboard = keyboard
        keyboardView?.setKeyboard(keyboard)
    }

    private fun createNewKeyboard(): MyKeyboard {
        val keyboardXml = when (inputTypeClass) {
            TYPE_CLASS_NUMBER -> {
                keyboardMode = KEYBOARD_NUMBERS
                R.xml.keys_numbers
            }

            TYPE_CLASS_PHONE -> {
                keyboardMode = KEYBOARD_PHONE
                R.xml.keys_phone
            }

            TYPE_CLASS_DATETIME -> {
                keyboardMode = KEYBOARD_SYMBOLS
                R.xml.keys_symbols
            }

            else -> {
                keyboardMode = KEYBOARD_LETTERS
                getKeyboardLayoutXML()
            }
        }
        return MyKeyboard(
            context = this, xmlLayoutResId = keyboardXml, enterKeyType = enterKeyType, shiftState = ShiftState.getDefaultShiftState(this, inputTypeClassVariation)
        )
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (newSelStart == newSelEnd) {
            keyboardView?.closeClipboardManager()
        }
    }

    private fun moveCursor(moveRight: Boolean) {
        val extractedText = currentInputConnection?.getExtractedText(ExtractedTextRequest(), 0) ?: return
        var newCursorPosition = extractedText.selectionStart
        newCursorPosition = if (moveRight) {
            newCursorPosition + 1
        } else {
            newCursorPosition - 1
        }

        currentInputConnection?.setSelection(newCursorPosition, newCursorPosition)
    }

    private fun getImeOptionsActionId(): Int {
        return if (currentInputEditorInfo.imeOptions and IME_FLAG_NO_ENTER_ACTION != 0) {
            IME_ACTION_NONE
        } else {
            currentInputEditorInfo.imeOptions and IME_MASK_ACTION
        }
    }

    private fun getKeyboardLayoutXML(): Int {
        return when (baseContext.config.keyboardLanguage) {
            LANGUAGE_BENGALI -> R.xml.keys_letters_bengali
            LANGUAGE_BULGARIAN -> R.xml.keys_letters_bulgarian
            LANGUAGE_ENGLISH_DVORAK -> R.xml.keys_letters_english_dvorak
            LANGUAGE_ENGLISH_QWERTZ -> R.xml.keys_letters_english_qwertz
            LANGUAGE_FRENCH -> R.xml.keys_letters_french
            LANGUAGE_GERMAN -> R.xml.keys_letters_german
            LANGUAGE_GREEK -> R.xml.keys_letters_greek
            LANGUAGE_LITHUANIAN -> R.xml.keys_letters_lithuanian
            LANGUAGE_ROMANIAN -> R.xml.keys_letters_romanian
            LANGUAGE_RUSSIAN -> R.xml.keys_letters_russian
            LANGUAGE_SLOVENIAN -> R.xml.keys_letters_slovenian
            LANGUAGE_SPANISH -> R.xml.keys_letters_spanish_qwerty
            LANGUAGE_TURKISH_Q -> R.xml.keys_letters_turkish_q
            else -> R.xml.keys_letters_english_qwerty
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        keyboardView?.setupKeyboard()
    }
}
