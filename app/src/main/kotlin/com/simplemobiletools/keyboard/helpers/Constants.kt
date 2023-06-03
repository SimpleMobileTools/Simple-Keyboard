package com.simplemobiletools.keyboard.helpers

import android.content.Context
import com.simplemobiletools.keyboard.extensions.config
import com.simplemobiletools.keyboard.helpers.MyKeyboard.Companion.KEYCODE_SPACE

enum class ShiftState {
    OFF,
    ON_ONE_CHAR,
    ON_PERMANENT;

    companion object {
        private val endOfSentenceChars: List<Char> = listOf('.', '?', '!')

        fun getDefaultShiftState(context: Context): ShiftState {
            return when (context.config.enableSentencesCapitalization) {
                true -> ON_ONE_CHAR
                else -> OFF
            }
        }

        fun getShiftStateForText(context: Context, newText: String?): ShiftState {
            if (!context.config.enableSentencesCapitalization) {
                return OFF
            }

            val twoLastSymbols = newText?.takeLast(2)
            return when {
                shouldCapitalizeSentence(previousChar = twoLastSymbols?.getOrNull(0), currentChar = twoLastSymbols?.getOrNull(1)) -> {
                    ON_ONE_CHAR
                }
                else -> {
                    OFF
                }
            }
        }

        fun getCapitalizationOnDelete(context: Context, text: CharSequence?): ShiftState {
            if (!context.config.enableSentencesCapitalization) {
                return OFF
            }

            return if (text.isNullOrEmpty() || shouldCapitalizeSentence(currentChar = text.last(), previousChar = text.getOrNull(text.lastIndex - 1))) {
                ON_ONE_CHAR
            } else {
                OFF
            }
        }

        private fun shouldCapitalizeSentence(previousChar: Char?, currentChar: Char?): Boolean {
            if (previousChar == null || currentChar == null) {
                return false
            }

            return currentChar.code == KEYCODE_SPACE && endOfSentenceChars.contains(previousChar)
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
const val LANGUAGE_FRENCH_AZERTY = 2
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
const val LANGUAGE_NORWEGIAN = 14
const val LANGUAGE_SWEDISH = 15
const val LANGUAGE_DANISH = 16

// keyboard height multiplier options
const val KEYBOARD_HEIGHT_MULTIPLIER_SMALL = 1
const val KEYBOARD_HEIGHT_MULTIPLIER_MEDIUM = 2
const val KEYBOARD_HEIGHT_MULTIPLIER_LARGE = 3

const val EMOJI_SPEC_FILE_PATH = "media/emoji_spec.txt"
