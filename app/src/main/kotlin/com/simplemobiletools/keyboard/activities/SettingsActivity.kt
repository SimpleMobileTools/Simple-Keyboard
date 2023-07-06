package com.simplemobiletools.keyboard.activities

import android.content.Intent
import android.os.Bundle
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.NavigationIcon
import com.simplemobiletools.commons.helpers.isTiramisuPlus
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.extensions.config
import com.simplemobiletools.keyboard.extensions.getKeyboardLanguageText
import com.simplemobiletools.keyboard.extensions.getKeyboardLanguages
import com.simplemobiletools.keyboard.helpers.*
import kotlinx.android.synthetic.main.activity_settings.*
import java.util.*
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        updateMaterialActivityViews(settings_coordinator, settings_holder, useTransparentNavigation = false, useTopSearchMenu = false)
        setupMaterialScrollListener(settings_nested_scrollview, settings_toolbar)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(settings_toolbar, NavigationIcon.Arrow)

        setupPurchaseThankYou()
        setupCustomizeColors()
        setupUseEnglish()
        setupLanguage()
        setupManageClipboardItems()
        setupVibrateOnKeypress()
        setupShowPopupOnKeypress()
        setupShowKeyBorders()
        setupKeyboardLanguage()
        setupKeyboardHeightMultiplier()
        setupShowClipboardContent()
        setupSentencesCapitalization()
        setupShowNumbersRow()

        updateTextColors(settings_nested_scrollview)

        arrayOf(settings_color_customization_section_label, settings_general_settings_label).forEach {
            it.setTextColor(getProperPrimaryColor())
        }
    }

    private fun setupPurchaseThankYou() {
        settings_purchase_thank_you_holder.beGoneIf(isOrWasThankYouInstalled())
        settings_purchase_thank_you_holder.setOnClickListener {
            launchPurchaseThankYouIntent()
        }
    }

    private fun setupCustomizeColors() {
        settings_color_customization_label.text = getCustomizeColorsString()
        settings_color_customization_holder.setOnClickListener {
            handleCustomizeColorsClick()
        }
    }

    private fun setupUseEnglish() {
        settings_use_english_holder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        settings_use_english.isChecked = config.useEnglish
        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            exitProcess(0)
        }
    }

    private fun setupLanguage() {
        settings_language.text = Locale.getDefault().displayLanguage
        settings_language_holder.beVisibleIf(isTiramisuPlus())
        settings_language_holder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun setupManageClipboardItems() {
        settings_manage_clipboard_items_holder.setOnClickListener {
            Intent(this, ManageClipboardItemsActivity::class.java).apply {
                startActivity(this)
            }
        }
    }

    private fun setupVibrateOnKeypress() {
        settings_vibrate_on_keypress.isChecked = config.vibrateOnKeypress
        settings_vibrate_on_keypress_holder.setOnClickListener {
            settings_vibrate_on_keypress.toggle()
            config.vibrateOnKeypress = settings_vibrate_on_keypress.isChecked
        }
    }

    private fun setupShowPopupOnKeypress() {
        settings_show_popup_on_keypress.isChecked = config.showPopupOnKeypress
        settings_show_popup_on_keypress_holder.setOnClickListener {
            settings_show_popup_on_keypress.toggle()
            config.showPopupOnKeypress = settings_show_popup_on_keypress.isChecked
        }
    }

    private fun setupShowKeyBorders() {
        settings_show_key_borders.isChecked = config.showKeyBorders
        settings_show_key_borders_holder.setOnClickListener {
            settings_show_key_borders.toggle()
            config.showKeyBorders = settings_show_key_borders.isChecked
        }
    }

    private fun setupKeyboardLanguage() {
        settings_keyboard_language.text = getKeyboardLanguageText(config.keyboardLanguage)
        settings_keyboard_language_holder.setOnClickListener {
            val items = getKeyboardLanguages()
            RadioGroupDialog(this@SettingsActivity, items, config.keyboardLanguage) {
                config.keyboardLanguage = it as Int
                settings_keyboard_language.text = getKeyboardLanguageText(config.keyboardLanguage)
            }
        }
    }

    private fun setupKeyboardHeightMultiplier() {
        settings_keyboard_height_multiplier.text = getKeyboardHeightPercentageText(config.keyboardHeightPercentage)
        settings_keyboard_height_multiplier_holder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(KEYBOARD_HEIGHT_50_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_50_PERCENT)),
                RadioItem(KEYBOARD_HEIGHT_60_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_60_PERCENT)),
                RadioItem(KEYBOARD_HEIGHT_75_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_75_PERCENT)),
                RadioItem(KEYBOARD_HEIGHT_90_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_90_PERCENT)),
                RadioItem(KEYBOARD_HEIGHT_100_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_100_PERCENT)),
                RadioItem(KEYBOARD_HEIGHT_125_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_125_PERCENT)),
                RadioItem(KEYBOARD_HEIGHT_150_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_150_PERCENT)),
                RadioItem(KEYBOARD_HEIGHT_175_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_175_PERCENT)),
                RadioItem(KEYBOARD_HEIGHT_200_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_200_PERCENT)),
                RadioItem(KEYBOARD_HEIGHT_250_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_250_PERCENT)),
                RadioItem(KEYBOARD_HEIGHT_300_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_300_PERCENT))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.keyboardHeightPercentage) {
                config.keyboardHeightPercentage = it as Int
                settings_keyboard_height_multiplier.text = getKeyboardHeightPercentageText(config.keyboardHeightPercentage)
            }
        }
    }

    private fun getKeyboardHeightPercentageText(keyboardHeightPercentage: Int): String = "$keyboardHeightPercentage%"

    private fun setupShowClipboardContent() {
        settings_show_clipboard_content.isChecked = config.showClipboardContent
        settings_show_clipboard_content_holder.setOnClickListener {
            settings_show_clipboard_content.toggle()
            config.showClipboardContent = settings_show_clipboard_content.isChecked
        }
    }

    private fun setupSentencesCapitalization() {
        settings_start_sentences_capitalized.isChecked = config.enableSentencesCapitalization
        settings_start_sentences_capitalized_holder.setOnClickListener {
            settings_start_sentences_capitalized.toggle()
            config.enableSentencesCapitalization = settings_start_sentences_capitalized.isChecked
        }
    }

    private fun setupShowNumbersRow() {
        settings_show_numbers_row.isChecked = config.showNumbersRow
        settings_show_numbers_row_holder.setOnClickListener {
            settings_show_numbers_row.toggle()
            config.showNumbersRow = settings_show_numbers_row.isChecked
        }
    }
}
