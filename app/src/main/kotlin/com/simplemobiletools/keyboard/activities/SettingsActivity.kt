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
import com.simplemobiletools.keyboard.helpers.KEYBOARD_HEIGHT_MULTIPLIER_LARGE
import com.simplemobiletools.keyboard.helpers.KEYBOARD_HEIGHT_MULTIPLIER_MEDIUM
import com.simplemobiletools.keyboard.helpers.KEYBOARD_HEIGHT_MULTIPLIER_SMALL
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
        settings_keyboard_height_multiplier.text = getKeyboardHeightMultiplierText(config.keyboardHeightMultiplier)
        settings_keyboard_height_multiplier_holder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(KEYBOARD_HEIGHT_MULTIPLIER_SMALL, getKeyboardHeightMultiplierText(KEYBOARD_HEIGHT_MULTIPLIER_SMALL)),
                RadioItem(KEYBOARD_HEIGHT_MULTIPLIER_MEDIUM, getKeyboardHeightMultiplierText(KEYBOARD_HEIGHT_MULTIPLIER_MEDIUM)),
                RadioItem(KEYBOARD_HEIGHT_MULTIPLIER_LARGE, getKeyboardHeightMultiplierText(KEYBOARD_HEIGHT_MULTIPLIER_LARGE)),
            )

            RadioGroupDialog(this@SettingsActivity, items, config.keyboardHeightMultiplier) {
                config.keyboardHeightMultiplier = it as Int
                settings_keyboard_height_multiplier.text = getKeyboardHeightMultiplierText(config.keyboardHeightMultiplier)
            }
        }
    }

    private fun getKeyboardHeightMultiplierText(multiplier: Int): String {
        return when (multiplier) {
            KEYBOARD_HEIGHT_MULTIPLIER_SMALL -> getString(R.string.small)
            KEYBOARD_HEIGHT_MULTIPLIER_MEDIUM -> getString(R.string.medium)
            KEYBOARD_HEIGHT_MULTIPLIER_LARGE -> getString(R.string.large)
            else -> getString(R.string.small)
        }
    }

    private fun setupShowClipboardContent() {
        settings_show_clipboard_content.isChecked = config.showClipboardContent
        settings_show_clipboard_content_holder.setOnClickListener {
            settings_show_clipboard_content.toggle()
            config.showClipboardContent = settings_show_clipboard_content.isChecked
        }
    }

    private fun setupSentencesCapitalization() {
        settings_enable_sentences_capitalization_row.isChecked = config.enableSentencesCapitalization
        settings_enable_capitalization_row_holder.setOnClickListener {
            settings_enable_sentences_capitalization_row.toggle()
            config.enableSentencesCapitalization = settings_show_numbers_row.isChecked
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
