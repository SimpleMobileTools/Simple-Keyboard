package com.simplemobiletools.keyboard.activities

import android.os.Bundle
import android.view.Menu
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.extensions.config
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

        setupPurchaseThankYou()
        setupCustomizeColors()
        setupUseEnglish()
        setupClipboard()

        updateTextColors(settings_scrollview)

        arrayOf(settings_color_customization_label, settings_general_settings_label).forEach {
            it.setTextColor(getAdjustedPrimaryColor())
        }

        arrayOf(settings_color_customization_holder, settings_general_settings_holder).forEach {
            it.background.applyColorFilter(baseConfig.backgroundColor.getContrastColor())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun setupPurchaseThankYou() {
        settings_purchase_thank_you_holder.beGoneIf(isOrWasThankYouInstalled())

        // make sure the corners at ripple fit the stroke rounded corners
        if (settings_purchase_thank_you_holder.isGone()) {
            settings_use_english_holder.background = resources.getDrawable(R.drawable.ripple_top_corners, theme)
        }

        settings_purchase_thank_you_holder.setOnClickListener {
            handleCustomizeColorsClick()
        }
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_label.text = getCustomizeColorsString()
        settings_customize_colors_holder.setOnClickListener {
            handleCustomizeColorsClick()
        }
    }

    private fun setupUseEnglish() {
        settings_use_english_holder.beVisibleIf(config.wasUseEnglishToggled || Locale.getDefault().language != "en")
        settings_use_english.isChecked = config.useEnglish

        if (settings_use_english_holder.isGone() && settings_purchase_thank_you_holder.isGone()) {
            settings_show_clipboard_holder.background = resources.getDrawable(R.drawable.ripple_all_corners, theme)
        }

        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            exitProcess(0)
        }
    }

    private fun setupClipboard() {
        settings_show_clipboard.isChecked = config.showClipboard
        settings_show_clipboard_holder.setOnClickListener {
            settings_show_clipboard.toggle()
            config.showClipboard = settings_show_clipboard.isChecked
        }
    }
}
