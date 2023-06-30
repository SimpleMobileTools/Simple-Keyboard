package com.simplemobiletools.keyboard.services

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Icon
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType.*
import android.text.TextUtils
import android.util.Log
import android.util.Size
import android.view.KeyEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.inputmethod.*
import android.view.inputmethod.EditorInfo.IME_ACTION_NONE
import android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION
import android.view.inputmethod.EditorInfo.IME_MASK_ACTION
import android.widget.inline.InlineContentView
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.common.ImageViewStyle
import androidx.autofill.inline.common.TextViewStyle
import androidx.autofill.inline.common.ViewStyle
import androidx.autofill.inline.v1.InlineSuggestionUi
import com.simplemobiletools.commons.extensions.getSharedPrefs
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.extensions.config
import com.simplemobiletools.keyboard.extensions.safeStorageContext
import com.simplemobiletools.keyboard.extensions.toPixel
import com.simplemobiletools.keyboard.helpers.*
import com.simplemobiletools.keyboard.interfaces.OnKeyboardActionListener
import com.simplemobiletools.keyboard.models.inline.ResponseState
import com.simplemobiletools.keyboard.models.inline.SuggestionItem
import com.simplemobiletools.keyboard.views.InlineContentClipView
import com.simplemobiletools.keyboard.views.MyKeyboardView
import kotlinx.android.synthetic.main.keyboard_view_keyboard.view.*
import java.util.Collections
import java.util.TreeMap
import java.util.concurrent.Executors

// based on https://www.androidauthority.com/lets-build-custom-keyboard-android-832362/
class SimpleKeyboardIME : InputMethodService(), OnKeyboardActionListener, SharedPreferences.OnSharedPreferenceChangeListener {
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
    private var inputTypeClass = TYPE_CLASS_TEXT
    private var inputTypeClassVariation = TYPE_CLASS_TEXT
    private var enterKeyType = IME_ACTION_NONE
    private var switchToLetters = false
    /**
     * Inline suggestion
     * */
    private val handler = Handler(Looper.getMainLooper())
    private var delayedDeletion: Runnable? = null
    private var pendingResponse: Runnable? = null
    private var responseState: ResponseState = ResponseState.RESET
    private var scrollableSuggestions: ViewGroup? = null
    private var scrollableSuggestionsClip: InlineContentClipView? = null
    private var suggestionStrip: ViewGroup? = null
    private var pinnedSuggestionsStart: ViewGroup? = null
    private var pinnedSuggestionsEnd: ViewGroup? = null
    private val TAG = "SimpleKeyboardIME"
    private val SHOWCASE_BG_FG_TRANSITION = false
    private val SHOWCASE_UP_DOWN_TRANSITION = false
    private val MOVE_SUGGESTIONS_TO_BG_TIMEOUT: Long = 5000
    private val MOVE_SUGGESTIONS_TO_FG_TIMEOUT: Long = 15000
    private val MOVE_SUGGESTIONS_UP_TIMEOUT: Long = 5000
    private val MOVE_SUGGESTIONS_DOWN_TIMEOUT: Long = 10000

    @RequiresApi(Build.VERSION_CODES.R)
    private val mMoveScrollableSuggestionsToBg = Runnable {
        scrollableSuggestionsClip?.setZOrderedOnTop(false)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private val mMoveScrollableSuggestionsToFg = Runnable {
        scrollableSuggestionsClip?.setZOrderedOnTop(true)
    }

    private val mMoveScrollableSuggestionsUp = Runnable {
        suggestionStrip!!.animate().translationY(-50f).setDuration(500).start()
    }

    private val mMoveScrollableSuggestionsDown = Runnable {
        suggestionStrip!!.animate().translationY(0f).setDuration(500).start()
    }

    override fun onInitializeInterface() {
        super.onInitializeInterface()
        safeStorageContext.getSharedPrefs().registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateInputView(): View {
        val keyboardHolder = layoutInflater.inflate(R.layout.keyboard_view_keyboard, null)
        keyboardView = keyboardHolder.keyboard_view as MyKeyboardView
        keyboardView!!.setKeyboardHolder(keyboardHolder.keyboard_holder)
        keyboardView!!.setKeyboard(keyboard!!)
        keyboardView!!.setEditorInfo(currentInputEditorInfo)
        keyboardView!!.mOnKeyboardActionListener = this
        // Inline Suggestion Views
        // Inline Suggestion Views
        suggestionStrip = keyboardHolder.findViewById(R.id.suggestion_strip)
        pinnedSuggestionsStart = keyboardHolder.findViewById(R.id.pinned_suggestions_start)
        pinnedSuggestionsEnd = keyboardHolder.findViewById(R.id.pinned_suggestions_end)
        scrollableSuggestionsClip = keyboardHolder.findViewById(R.id.scrollable_suggestions_clip)
        scrollableSuggestions = keyboardHolder.findViewById(R.id.scrollable_suggestions)
        // return input keyboard view
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
        inputTypeClassVariation = attribute.inputType and TYPE_MASK_VARIATION
        enterKeyType = attribute.imeOptions and (IME_MASK_ACTION or IME_FLAG_NO_ENTER_ACTION)
        keyboard = createNewKeyboard()
        keyboardView?.setKeyboard(keyboard!!)
        keyboardView?.setEditorInfo(attribute)
        updateShiftKeyState()
    }

    private fun updateShiftKeyState() {
        if (keyboard?.mShiftState == ShiftState.ON_PERMANENT) {
            return
        }

        val editorInfo = currentInputEditorInfo
        if (config.enableSentencesCapitalization && editorInfo != null && editorInfo.inputType != TYPE_NULL) {
            if (currentInputConnection.getCursorCapsMode(editorInfo.inputType) != 0) {
                keyboard?.setShifted(ShiftState.ON_ONE_CHAR)
                keyboardView?.invalidateAllKeys()
                return
            }
        }

        keyboard?.setShifted(ShiftState.OFF)
        keyboardView?.invalidateAllKeys()
    }

    override fun onKey(code: Int) {
        val inputConnection = currentInputConnection
        if (keyboard == null || inputConnection == null) {
            return
        }

        if (code != MyKeyboard.KEYCODE_SHIFT) {
            lastShiftPressTS = 0
        }

        when (code) {
            MyKeyboard.KEYCODE_DELETE -> {
                val selectedText = inputConnection.getSelectedText(0)
                if (TextUtils.isEmpty(selectedText)) {
                    inputConnection.deleteSurroundingText(1, 0)
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
                val originalText = inputConnection.getExtractedText(ExtractedTextRequest(), 0)?.text

                if (Character.isLetter(codeChar) && keyboard!!.mShiftState > ShiftState.OFF) {
                    codeChar = Character.toUpperCase(codeChar)
                }

                // If the keyboard is set to symbols and the user presses space, we usually should switch back to the letters keyboard.
                // However, avoid doing that in cases when the EditText for example requires numbers as the input.
                // We can detect that by the text not changing on pressing Space.
                if (keyboardMode != KEYBOARD_LETTERS && inputTypeClass == TYPE_CLASS_TEXT && code == MyKeyboard.KEYCODE_SPACE) {
                    inputConnection.commitText(codeChar.toString(), 1)
                    val newText = inputConnection.getExtractedText(ExtractedTextRequest(), 0)?.text
                    if (originalText != newText) {
                        switchToLetters = true
                    }
                } else {
                    inputConnection.commitText(codeChar.toString(), 1)
                    if (originalText == null) {
                        updateShiftKeyState()
                    }
                }
            }
        }
    }

    override fun onActionUp() {
        if (switchToLetters) {
            // TODO: Change keyboardMode to enum class
            keyboardMode = KEYBOARD_LETTERS

            keyboard = MyKeyboard(this, getKeyboardLayoutXML(), enterKeyType)

            val editorInfo = currentInputEditorInfo
            if (editorInfo != null && editorInfo.inputType != TYPE_NULL && keyboard?.mShiftState != ShiftState.ON_PERMANENT) {
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
            context = this,
            xmlLayoutResId = keyboardXml,
            enterKeyType = enterKeyType,
        )
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (newSelStart == newSelEnd) {
            keyboardView?.closeClipboardManager()
        }
        updateShiftKeyState()
    }

    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo)
        updateShiftKeyState()
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
            LANGUAGE_DANISH -> R.xml.keys_letters_danish
            LANGUAGE_ENGLISH_DVORAK -> R.xml.keys_letters_english_dvorak
            LANGUAGE_ENGLISH_QWERTZ -> R.xml.keys_letters_english_qwertz
            LANGUAGE_FRENCH_AZERTY -> R.xml.keys_letters_french_azerty
            LANGUAGE_FRENCH_BEPO -> R.xml.keys_letters_french_bepo
            LANGUAGE_GERMAN -> R.xml.keys_letters_german
            LANGUAGE_GREEK -> R.xml.keys_letters_greek
            LANGUAGE_LITHUANIAN -> R.xml.keys_letters_lithuanian
            LANGUAGE_NORWEGIAN -> R.xml.keys_letters_norwegian
            LANGUAGE_ROMANIAN -> R.xml.keys_letters_romanian
            LANGUAGE_RUSSIAN -> R.xml.keys_letters_russian
            LANGUAGE_SLOVENIAN -> R.xml.keys_letters_slovenian
            LANGUAGE_SWEDISH -> R.xml.keys_letters_swedish
            LANGUAGE_SPANISH -> R.xml.keys_letters_spanish_qwerty
            LANGUAGE_TURKISH_Q -> R.xml.keys_letters_turkish_q
            else -> R.xml.keys_letters_english_qwerty
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        keyboardView?.setupKeyboard()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest {
        val stylesBuilder = UiVersions.newStylesBuilder()
        @SuppressLint("RestrictedApi")
        val style = InlineSuggestionUi.newStyleBuilder()
            .setSingleIconChipStyle(
                ViewStyle.Builder()
                    .setBackground(
                        Icon.createWithResource(this, R.drawable.clipboard_background)
                    )
                    .setPadding(0, 0, 0, 0)
                    .build()
            )
            .setChipStyle(
                ViewStyle.Builder()
                    .setBackground(
                        Icon.createWithResource(this, R.drawable.clipboard_background)
                    )
                    .setPadding(toPixel(5 + 8), 0, toPixel(5 + 8), 0)
                    .build()
            )
            .setStartIconStyle(ImageViewStyle.Builder().setLayoutMargin(0, 0, 0, 0).build())
            .setTitleStyle(
                TextViewStyle.Builder()
                    .setLayoutMargin(toPixel(4), 0, toPixel(4), 0)
                    .setTextColor(Color.parseColor("#ffffff"))
                    .setTextSize(16f)
                    .build()
            )
            .setSubtitleStyle(
                TextViewStyle.Builder()
                    .setLayoutMargin(0, 0, toPixel(4), 0)
                    .setTextColor(Color.parseColor("#ffffff")) // 60% opacity
                    .setTextSize(14f)
                    .build()
            )
            .setEndIconStyle(ImageViewStyle.Builder().setLayoutMargin(0, 0, 0, 0).build())
            .build()
        stylesBuilder.addStyle(style)
        val stylesBundle = stylesBuilder.build()

        val presentationSpecs = ArrayList<InlinePresentationSpec>()
        presentationSpecs.add(
            InlinePresentationSpec.Builder(
                Size(100, getHeight()),
                Size(1000, getHeight())
            ).setStyle(stylesBundle).build()
        )
        presentationSpecs.add(
            InlinePresentationSpec.Builder(
                Size(100, getHeight()),
                Size(1000, getHeight())
            ).setStyle(stylesBundle).build()
        )

        return InlineSuggestionsRequest.Builder(presentationSpecs)
            .setMaxSuggestionCount(InlineSuggestionsRequest.SUGGESTION_COUNT_UNLIMITED)
            .build()
    }

    private fun getHeight(): Int {
        return resources.getDimensionPixelSize(R.dimen.toolbar_height)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        cancelDelayedDeletion()
        postPendingResponse(response)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun postPendingResponse(response: InlineSuggestionsResponse) {
        cancelPendingResponse()
        val inlineSuggestions = response.inlineSuggestions
        responseState = ResponseState.RECEIVE_RESPONSE
        pendingResponse = Runnable {
            pendingResponse = null
            if (responseState == ResponseState.START_INPUT && inlineSuggestions.isEmpty()) {
                scheduleDelayedDeletion()
            } else {
                inflateThenShowSuggestions(inlineSuggestions)
            }
            responseState = ResponseState.RESET
        }
        handler.post(pendingResponse!!)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun scheduleDelayedDeletion() {
        if (keyboardView != null && delayedDeletion == null) {
            delayedDeletion = Runnable {
                delayedDeletion = null
                clearInlineSuggestionStrip()
            }
            handler.postDelayed(delayedDeletion!!, 200)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun clearInlineSuggestionStrip() {
        if (keyboardView != null) {
            updateInlineSuggestionStrip(emptyList())
        }
    }

    /**
    * cancel
    * */
    private fun cancelDelayedDeletion() {
        if (delayedDeletion != null) {
            handler.removeCallbacks(delayedDeletion!!)
            delayedDeletion = null
        }
    }

    private fun cancelPendingResponse() {
        if (pendingResponse != null) {
            handler.removeCallbacks(pendingResponse!!)
            pendingResponse = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun updateInlineSuggestionStrip(suggestionItems: List<SuggestionItem>) {
        pinnedSuggestionsStart!!.removeAllViews()
        scrollableSuggestions!!.removeAllViews()
        pinnedSuggestionsEnd!!.removeAllViews()
        if (suggestionItems.isEmpty()) {
            return
        }
        scrollableSuggestionsClip!!.setBackgroundColor(
            getColor(android.R.color.transparent)
        )
        suggestionStrip!!.visibility = View.VISIBLE
        for (suggestionItem in suggestionItems) {
            val suggestionView: InlineContentView = suggestionItem.view
            if (suggestionItem.isPinned) {
                if (pinnedSuggestionsStart!!.childCount <= 0) {
                    pinnedSuggestionsStart!!.addView(suggestionView)
                } else {
                    pinnedSuggestionsEnd!!.addView(suggestionView)
                }
            } else {
                scrollableSuggestions!!.addView(suggestionView)
            }
        }
        if (SHOWCASE_BG_FG_TRANSITION) {
            rescheduleShowcaseBgFgTransitions()
        }
        if (SHOWCASE_UP_DOWN_TRANSITION) {
            rescheduleShowcaseUpDownTransitions()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun rescheduleShowcaseBgFgTransitions() {
        val handler: Handler? = keyboardView?.handler
        handler?.removeCallbacks(mMoveScrollableSuggestionsToBg)
        handler?.postDelayed(mMoveScrollableSuggestionsToBg, MOVE_SUGGESTIONS_TO_BG_TIMEOUT)
        handler?.removeCallbacks(mMoveScrollableSuggestionsToFg)
        handler?.postDelayed(mMoveScrollableSuggestionsToFg, MOVE_SUGGESTIONS_TO_FG_TIMEOUT)
    }

    private fun rescheduleShowcaseUpDownTransitions() {
        val handler: Handler? = keyboardView?.handler
        handler?.removeCallbacks(mMoveScrollableSuggestionsUp)
        handler?.postDelayed(mMoveScrollableSuggestionsUp, MOVE_SUGGESTIONS_UP_TIMEOUT)
        handler?.removeCallbacks(mMoveScrollableSuggestionsDown)
        handler?.postDelayed(mMoveScrollableSuggestionsDown, MOVE_SUGGESTIONS_DOWN_TIMEOUT)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun inflateThenShowSuggestions(inlineSuggestions: List<InlineSuggestion>) {
        val totalSuggestionsCount = inlineSuggestions.size
        if (inlineSuggestions.isEmpty()) {
            mainExecutor.execute { updateInlineSuggestionStrip(emptyList()) }
            return
        }
        val suggestionMap = Collections.synchronizedMap(TreeMap<Int, SuggestionItem>())
        val executor = Executors.newSingleThreadExecutor()
        for (i in 0 until totalSuggestionsCount) {
            val inlineSuggestion = inlineSuggestions[i]
            val size = Size(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            inlineSuggestion.inflate(this, size, executor) { suggestionView: InlineContentView? ->
                if (suggestionView != null) {
                    suggestionMap[i] = SuggestionItem(
                        suggestionView,  inlineSuggestion.info.isPinned
                    )
                } else {
                    suggestionMap[i] = null
                }

                // Update the UI once the last inflation completed
                if (suggestionMap.size >= totalSuggestionsCount) {
                    val suggestionItems = java.util.ArrayList(
                        suggestionMap.values
                    )
                    mainExecutor.execute { updateInlineSuggestionStrip(suggestionItems) }
                }
            }
        }
    }
}
