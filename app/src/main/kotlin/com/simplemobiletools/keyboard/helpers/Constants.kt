package com.simplemobiletools.keyboard.helpers

import android.content.Context
import androidx.annotation.StringRes
import com.simplemobiletools.keyboard.R

const val SHIFT_OFF = 0
const val SHIFT_ON_ONE_CHAR = 1
const val SHIFT_ON_PERMANENT = 2

// limit the count of alternative characters that show up at long pressing a key
const val MAX_KEYS_PER_MINI_ROW = 9

// shared prefs
const val VIBRATE_ON_KEYPRESS = "vibrate_on_keypress"
const val SHOW_POPUP_ON_KEYPRESS = "show_popup_on_keypress"
const val SHOW_KEY_BORDERS = "show_key_borders"
const val LAST_EXPORTED_CLIPS_FOLDER = "last_exported_clips_folder"
const val KEYBOARD_LANGUAGE = "keyboard_language"
const val HEIGHT_MULTIPLIER = "height_multiplier"
const val SHOW_CLIPBOARD_CONTENT = "show_clipboard_content"
const val SHOW_NUMBERS_ROW = "show_numbers_row"

// differentiate current and pinned clips at the keyboards' Clipboard section
const val ITEM_SECTION_LABEL = 0
const val ITEM_CLIP = 1

enum class Language(@StringRes private val stringRes: Int, private val layout: Int) {
    ENGLISH_QWERTY(
        R.string.translation_english, R.xml.keys_letters_english_qwerty
    ),
    RUSSIAN(
        R.string.translation_russian, R.xml.keys_letters_russian
    ),
    FRENCH(
        R.string.translation_french, R.xml.keys_letters_french
    ),
    ENGLISH_QWERTZ(
        R.string.translation_english, R.xml.keys_letters_english_qwertz
    ),
    SPANISH(
        R.string.translation_spanish, R.string.translation_spanish
    ),
    GERMAN(
        R.string.translation_german, R.xml.keys_letters_german
    ),
    ENGLISH_DVORAK(
        R.string.translation_english, R.xml.keys_letters_english_dvorak
    ),
    ROMANIAN(
        R.string.translation_romanian, R.xml.keys_letters_romanian
    ),
    SLOVENIAN(
        R.string.translation_slovenian, R.xml.keys_letters_slovenian
    ),
    BULGARIAN(
        R.string.translation_bulgarian, R.xml.keys_letters_bulgarian
    ),
    TURKISH_Q(
        R.string.translation_turkish, R.xml.keys_letters_turkish_q
    ),
    LITHUANIAN(
        R.string.translation_lithuanian, R.xml.keys_letters_lithuanian
    ),
    BENGALI(
        R.string.translation_bengali, R.xml.keys_letters_bengali
    ),
    GREEK(
        R.string.translation_greek, R.xml.keys_letters_greek
    );

    fun getName(context: Context): String {
        with(context) {
            return when (val language = this@Language) {
                ENGLISH_DVORAK -> "${getString(language.stringRes)} (DVORAK)"
                ENGLISH_QWERTY -> "${getString(language.stringRes)} (QWERTY)"
                ENGLISH_QWERTZ -> "${getString(language.stringRes)} (QWERTZ)"
                else -> getString(language.stringRes)
            }
        }
    }

    companion object {
        fun getKeyboardLayout(keyboardLanguage: Int): Int {
            return Language.values().getOrElse(keyboardLanguage) { ENGLISH_QWERTY }.layout
        }

        fun getKeyboardName(context: Context, language: Int): String {
            return Language.values().getOrElse(language) { ENGLISH_QWERTY }.getName(context)
        }
    }

}

// keyboard height multiplier options
const val KEYBOARD_HEIGHT_MULTIPLIER_SMALL = 1
const val KEYBOARD_HEIGHT_MULTIPLIER_MEDIUM = 2
const val KEYBOARD_HEIGHT_MULTIPLIER_LARGE = 3

const val EMOJI_SPEC_FILE_PATH = "media/emoji_spec.txt"
