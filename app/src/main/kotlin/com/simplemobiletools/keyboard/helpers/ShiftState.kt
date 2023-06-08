package com.simplemobiletools.keyboard.helpers

import android.content.Context
import android.text.InputType
import com.simplemobiletools.keyboard.extensions.config

enum class ShiftState {
    OFF,
    ON_ONE_CHAR,
    ON_PERMANENT;

    companion object {
        private val endOfSentenceChars: List<Char> = listOf('.', '?', '!')
        private const val MIN_TEXT_LENGTH = 2

        /**
         * Input Type exceptions for capitalizing.
         */
        private val inputTypeExceptions = listOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_NUMBER_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI
        )

        fun getDefaultShiftState(context: Context, inputTypeClassVariation: Int): ShiftState {
            if (isInputTypeAllowedCapitalizing(inputTypeClassVariation)) {
                return OFF
            }

            return if (context.config.enableSentencesCapitalization) {
                ON_ONE_CHAR
            } else {
                OFF
            }
        }

        fun getShiftStateForText(context: Context, inputTypeClassVariation: Int, text: String?): ShiftState {
            if (isInputTypeAllowedCapitalizing(inputTypeClassVariation)) {
                return OFF
            }

            return if (shouldCapitalize(context, text)) {
                ON_ONE_CHAR
            } else {
                OFF
            }
        }

        /**
         * The function is checking whether there is a need in capitalizing based on the given text
         * @param context Used for checking current sentences capitalization setting
         * @param text Last text from the input
         */
        fun shouldCapitalize(context: Context, text: String?): Boolean {
            // check whether it is the first letter in textField
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

            return endOfSentenceChars.contains(twoLastSymbols.first()) && twoLastSymbols.last().code == MyKeyboard.KEYCODE_SPACE
        }

        fun isInputTypeAllowedCapitalizing(inputTypeVariation: Int): Boolean {
            return inputTypeVariation in inputTypeExceptions
        }
    }
}
