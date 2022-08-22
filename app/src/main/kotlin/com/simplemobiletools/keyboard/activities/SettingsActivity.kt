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
import com.simplemobiletools.keyboard.helpers.*
import kotlinx.android.synthetic.main.activity_settings.*
import java.util.*
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
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
        setupKeyboardLanguage()
        setupKeyboardHeightMultiplier()

        updateTextColors(settings_nested_scrollview)

        arrayOf(settings_color_customization_label, settings_general_settings_label).forEach {
            it.setTextColor(getProperPrimaryColor())
        }

        arrayOf(settings_color_customization_holder, settings_general_settings_holder).forEach {
            it.background.applyColorFilter(getProperBackgroundColor().getContrastColor())
        }
    }

    private fun setupPurchaseThankYou() {
        settings_purchase_thank_you_holder.beGoneIf(isOrWasThankYouInstalled())

        // make sure the corners at ripple fit the stroke rounded corners
        if (settings_purchase_thank_you_holder.isGone()) {
            settings_use_english_holder.background = resources.getDrawable(R.drawable.ripple_top_corners, theme)
            settings_language_holder.background = resources.getDrawable(R.drawable.ripple_top_corners, theme)
        }

        settings_purchase_thank_you_holder.setOnClickListener {
            launchPurchaseThankYouIntent()
        }
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_label.text = getCustomizeColorsString()
        settings_customize_colors_holder.setOnClickListener {
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

        if (settings_use_english_holder.isGone() && settings_language_holder.isGone() && settings_purchase_thank_you_holder.isGone()) {
            settings_manage_clipboard_items_holder.background = resources.getDrawable(R.drawable.ripple_top_corners, theme)
        }

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

    private fun setupKeyboardLanguage() {
        settings_keyboard_language.text = getKeyboardLanguageText(config.keyboardLanguage)
        settings_keyboard_language_holder.setOnClickListener {
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

            RadioGroupDialog(this@SettingsActivity, items, config.keyboardLanguage) {
                config.keyboardLanguage = it as Int
                settings_keyboard_language.text = getKeyboardLanguageText(config.keyboardLanguage)
            }
        }
    }

    private fun getKeyboardLanguageText(language: Int): String {
        return when (language) {
            LANGUAGE_BENGALI -> getString(R.string.translation_bengali)
            LANGUAGE_BULGARIAN -> getString(R.string.translation_bulgarian)
            LANGUAGE_ENGLISH_DVORAK -> "${getString(R.string.translation_english)} (DVORAK)"
            LANGUAGE_ENGLISH_QWERTZ -> "${getString(R.string.translation_english)} (QWERTZ)"
            LANGUAGE_FRENCH -> getString(R.string.translation_french)
            LANGUAGE_GERMAN -> getString(R.string.translation_german)
            LANGUAGE_LITHUANIAN -> getString(R.string.translation_lithuanian)
            LANGUAGE_ROMANIAN -> getString(R.string.translation_romanian)
            LANGUAGE_RUSSIAN -> getString(R.string.translation_russian)
            LANGUAGE_SLOVENIAN -> getString(R.string.translation_slovenian)
            LANGUAGE_SPANISH -> getString(R.string.translation_spanish)
            LANGUAGE_TURKISH_Q -> "${getString(R.string.translation_turkish)} (Q)"
            else -> "${getString(R.string.translation_english)} (QWERTY)"
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
}
