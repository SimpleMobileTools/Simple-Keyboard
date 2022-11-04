package com.simplemobiletools.keyboard.dialogs

import android.view.View
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.extensions.config
import com.simplemobiletools.keyboard.helpers.*

class ChangeLanguagePopup(
    inputView: View,
    private val onSelect: () -> Unit,
) {
    private val context = inputView.context
    private val config = context.config

    init {
        val items = arrayListOf(
            RadioItem(LANGUAGE_BENGALI, getKeyboardLanguageText(LANGUAGE_BENGALI)),
            RadioItem(LANGUAGE_BULGARIAN, getKeyboardLanguageText(LANGUAGE_BULGARIAN)),
            RadioItem(LANGUAGE_ENGLISH_QWERTY, getKeyboardLanguageText(LANGUAGE_ENGLISH_QWERTY)),
            RadioItem(LANGUAGE_ENGLISH_QWERTZ, getKeyboardLanguageText(LANGUAGE_ENGLISH_QWERTZ)),
            RadioItem(LANGUAGE_ENGLISH_DVORAK, getKeyboardLanguageText(LANGUAGE_ENGLISH_DVORAK)),
            RadioItem(LANGUAGE_FRENCH, getKeyboardLanguageText(LANGUAGE_FRENCH)),
            RadioItem(LANGUAGE_GERMAN, getKeyboardLanguageText(LANGUAGE_GERMAN)),
            RadioItem(LANGUAGE_LITHUANIAN, getKeyboardLanguageText(LANGUAGE_LITHUANIAN)),
            RadioItem(LANGUAGE_ROMANIAN, getKeyboardLanguageText(LANGUAGE_ROMANIAN)),
            RadioItem(LANGUAGE_RUSSIAN, getKeyboardLanguageText(LANGUAGE_RUSSIAN)),
            RadioItem(LANGUAGE_SLOVENIAN, getKeyboardLanguageText(LANGUAGE_SLOVENIAN)),
            RadioItem(LANGUAGE_SPANISH, getKeyboardLanguageText(LANGUAGE_SPANISH)),
            RadioItem(LANGUAGE_TURKISH_Q, getKeyboardLanguageText(LANGUAGE_TURKISH_Q)),
        )

        KeyboardRadioGroupDialog(inputView, items, config.keyboardLanguage) {
            config.keyboardLanguage = it as Int
            onSelect.invoke()
        }
    }


    private fun getKeyboardLanguageText(language: Int): String {
        return when (language) {
            LANGUAGE_BENGALI -> context.getString(R.string.translation_bengali)
            LANGUAGE_BULGARIAN -> context.getString(R.string.translation_bulgarian)
            LANGUAGE_ENGLISH_DVORAK -> "${context.getString(R.string.translation_english)} (DVORAK)"
            LANGUAGE_ENGLISH_QWERTZ -> "${context.getString(R.string.translation_english)} (QWERTZ)"
            LANGUAGE_FRENCH -> context.getString(R.string.translation_french)
            LANGUAGE_GERMAN -> context.getString(R.string.translation_german)
            LANGUAGE_LITHUANIAN -> context.getString(R.string.translation_lithuanian)
            LANGUAGE_ROMANIAN -> context.getString(R.string.translation_romanian)
            LANGUAGE_RUSSIAN -> context.getString(R.string.translation_russian)
            LANGUAGE_SLOVENIAN -> context.getString(R.string.translation_slovenian)
            LANGUAGE_SPANISH -> context.getString(R.string.translation_spanish)
            LANGUAGE_TURKISH_Q -> "${context.getString(R.string.translation_turkish)} (Q)"
            else -> "${context.getString(R.string.translation_english)} (QWERTY)"
        }
    }
}
