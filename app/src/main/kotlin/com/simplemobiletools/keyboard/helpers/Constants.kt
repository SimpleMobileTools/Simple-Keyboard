package com.simplemobiletools.keyboard.helpers

import android.content.Context
import android.text.InputType
import com.simplemobiletools.keyboard.extensions.config
import com.simplemobiletools.keyboard.helpers.MyKeyboard.Companion.KEYCODE_SPACE

enum class ShiftState {
    OFF,
    ON_ONE_CHAR,
    ON_PERMANENT;

    companion object {
        private val MIN_TEXT_LENGTH = 2
        private val endOfSentenceChars: List<Char> = listOf('.', '?', '!')

        fun getDefaultShiftState(context: Context, inputTypeClassVariation: Int): ShiftState {
            if (isInputTypePassword(inputTypeClassVariation)) {
                return OFF
            }
            return when (context.config.enableSentencesCapitalization) {
                true -> ON_ONE_CHAR
                else -> OFF
            }
        }

        fun getShiftStateForText(context: Context, inputTypeClassVariation: Int, text: String?): ShiftState {
            if (isInputTypePassword(inputTypeClassVariation)) {
                return OFF
            }
            return when {
                shouldCapitalize(context, text) -> {
                    ON_ONE_CHAR
                }
                else -> {
                    OFF
                }
            }
        }

        fun shouldCapitalize(context: Context, text: String?): Boolean {
            //To capitalize first letter in textField
            if (text.isNullOrEmpty()) {
                return true
            }

            if (!context.config.enableSentencesCapitalization) {
                return false
            }

            val twoLastSymbols = text.takeLast(2)

            if (twoLastSymbols.length < MIN_TEXT_LENGTH) {
                return false
            }

            return endOfSentenceChars.contains(twoLastSymbols.first()) && twoLastSymbols.last().code == KEYCODE_SPACE
        }

        fun isInputTypePassword(inputTypeVariation: Int): Boolean {
            return inputTypeVariation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || inputTypeVariation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || inputTypeVariation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                || inputTypeVariation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
    }
}

// limit the count of alternative characters that show up at long pressing a key
const val MAX_KEYS_PER_MINI_ROW = 9

// shared prefs
const val VIBRATE_ON_KEYPRESS = "vibrate_on_keypress"
const val SHOW_POPUP_ON_KEYPRESS = "show_popup_on_keypress"
const val SHOW_KEY_BORDERS = "show_key_borders"
const val SENTENCES_CAPITALIZATION = "sentences_capitalization"
const val LAST_EXPORTED_CLIPS_FOLDER = "last_exported_clips_folder"
const val KEYBOARD_LANGUAGE = "keyboard_language"
const val HEIGHT_MULTIPLIER = "height_multiplier"
const val SHOW_CLIPBOARD_CONTENT = "show_clipboard_content"
const val SHOW_NUMBERS_ROW = "show_numbers_row"

// differentiate current and pinned clips at the keyboards' Clipboard section
const val ITEM_SECTION_LABEL = 0
const val ITEM_CLIP = 1

const val LANGUAGE_ENGLISH_QWERTY = 0
const val LANGUAGE_RUSSIAN = 1
const val LANGUAGE_FRENCH = 2
const val LANGUAGE_ENGLISH_QWERTZ = 3
const val LANGUAGE_SPANISH = 4
const val LANGUAGE_GERMAN = 5
const val LANGUAGE_ENGLISH_DVORAK = 6
const val LANGUAGE_ROMANIAN = 7
const val LANGUAGE_SLOVENIAN = 8
const val LANGUAGE_BULGARIAN = 9
const val LANGUAGE_TURKISH_Q = 10
const val LANGUAGE_LITHUANIAN = 11
const val LANGUAGE_BENGALI = 12
const val LANGUAGE_GREEK = 13

// keyboard height multiplier options
const val KEYBOARD_HEIGHT_MULTIPLIER_SMALL = 1
const val KEYBOARD_HEIGHT_MULTIPLIER_MEDIUM = 2
const val KEYBOARD_HEIGHT_MULTIPLIER_LARGE = 3

const val EMOJI_SPEC_FILE_PATH = "media/emoji_spec.txt"
