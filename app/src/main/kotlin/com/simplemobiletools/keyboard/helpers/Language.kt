package com.simplemobiletools.keyboard.helpers

import android.content.Context
import androidx.annotation.StringRes
import com.simplemobiletools.keyboard.R

enum class Language(@StringRes private val stringRes: Int, private val layout: Int) {
    BENGALI(
        R.string.translation_bengali, R.xml.keys_letters_bengali
    ),
    BULGARIAN(
        R.string.translation_bulgarian, R.xml.keys_letters_bulgarian
    ),
    DANISH(
        R.string.translation_danish, R.xml.keys_letters_danish
    ),
    ENGLISH_DVORAK(
        R.string.translation_english, R.xml.keys_letters_english_dvorak
    ),
    ENGLISH_QWERTY(
        R.string.translation_english, R.xml.keys_letters_english_qwerty
    ),
    ENGLISH_QWERTZ(
        R.string.translation_english, R.xml.keys_letters_english_qwertz
    ),
    FRENCH(
        R.string.translation_french, R.xml.keys_letters_french
    ),
    GERMAN(
        R.string.translation_german, R.xml.keys_letters_german
    ),
    GREEK(
        R.string.translation_greek, R.xml.keys_letters_greek
    ),
    LITHUANIAN(
        R.string.translation_lithuanian, R.xml.keys_letters_lithuanian
    ),
    NORWEGIAN(
        R.string.translation_norwegian, R.xml.keys_letters_norwegian
    ),
    ROMANIAN(
        R.string.translation_romanian, R.xml.keys_letters_romanian
    ),
    RUSSIAN(
        R.string.translation_russian, R.xml.keys_letters_russian
    ),
    SLOVENIAN(
        R.string.translation_slovenian, R.xml.keys_letters_slovenian
    ),
    SPANISH(
        R.string.translation_spanish, R.string.translation_spanish
    ),
    SWEDISH(
        R.string.translation_swedish, R.xml.keys_letters_swedish
    ),
    TURKISH_Q(
        R.string.translation_turkish, R.xml.keys_letters_turkish_q
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
        fun sorted(context: Context) = Language.values().sortedBy { context.getString(it.stringRes) }

        fun getKeyboardLayout(keyboardLanguage: Int): Int {
            return Language.values().getOrElse(keyboardLanguage) { ENGLISH_QWERTY }.layout
        }

        fun getKeyboardName(context: Context, language: Int): String {
            return Language.values().getOrElse(language) { ENGLISH_QWERTY }.getName(context)
        }
    }

}
