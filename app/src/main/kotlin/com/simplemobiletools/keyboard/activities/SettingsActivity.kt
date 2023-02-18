package com.simplemobiletools.keyboard.activities

import android.content.Intent
import android.os.Bundle
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.NavigationIcon
import com.simplemobiletools.commons.helpers.isTiramisuPlus
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.databinding.ActivitySettingsBinding
import com.simplemobiletools.keyboard.extensions.config
import com.simplemobiletools.keyboard.extensions.getKeyboardLanguageText
import com.simplemobiletools.keyboard.extensions.getKeyboardLanguages
import com.simplemobiletools.keyboard.helpers.KEYBOARD_HEIGHT_MULTIPLIER_LARGE
import com.simplemobiletools.keyboard.helpers.KEYBOARD_HEIGHT_MULTIPLIER_MEDIUM
import com.simplemobiletools.keyboard.helpers.KEYBOARD_HEIGHT_MULTIPLIER_SMALL
import java.util.*
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateMaterialActivityViews(binding.settingsCoordinator, binding.settingsHolder, useTransparentNavigation = false, useTopSearchMenu = false)
        setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsToolbar)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.settingsToolbar, NavigationIcon.Arrow)

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

        updateTextColors(binding.settingsNestedScrollview)

        arrayOf(binding.settingsColorCustomizationLabel, binding.settingsColorCustomizationLabel).forEach {
            it.setTextColor(getProperPrimaryColor())
        }
    }

    private fun setupPurchaseThankYou() {
        binding.settingsPurchaseThankYouHolder.apply {
            beGoneIf(isOrWasThankYouInstalled())
            setOnClickListener {
                launchPurchaseThankYouIntent()
            }
        }
    }

    private fun setupCustomizeColors() {
        binding.settingsColorCustomizationLabel.apply {
            text = getCustomizeColorsString()
            setOnClickListener {
                handleCustomizeColorsClick()
            }
        }
    }

    private fun setupUseEnglish() {
        binding.settingsUseEnglish.apply {
            binding.settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
            isChecked = config.useEnglish
            binding.settingsUseEnglishHolder.setOnClickListener {
                toggle()
                config.useEnglish = isChecked
                exitProcess(0)
            }
        }
    }

    private fun setupLanguage() {
        binding.settingsLanguage.text = Locale.getDefault().displayLanguage
        binding.settingsLanguageHolder.apply {
            beVisibleIf(isTiramisuPlus())
            setOnClickListener {
                launchChangeAppLanguageIntent()
            }
        }
    }

    private fun setupManageClipboardItems() {
        binding.settingsManageClipboardItemsHolder.setOnClickListener {
            Intent(this, ManageClipboardItemsActivity::class.java).apply {
                startActivity(this)
            }
        }
    }

    private fun setupVibrateOnKeypress() {
        binding.settingsVibrateOnKeypress.apply {
            isChecked = config.vibrateOnKeypress
            binding.settingsVibrateOnKeypressHolder.setOnClickListener {
                toggle()
                config.vibrateOnKeypress = isChecked
            }
        }
    }

    private fun setupShowPopupOnKeypress() {
        binding.settingsShowPopupOnKeypress.apply {
            isChecked = config.showPopupOnKeypress
            binding.settingsShowPopupOnKeypressHolder.setOnClickListener {
                toggle()
                config.showPopupOnKeypress = isChecked
            }
        }
    }

    private fun setupShowKeyBorders() {
        binding.settingsShowKeyBorders.apply {
            isChecked = config.showKeyBorders
            binding.settingsShowKeyBordersHolder.setOnClickListener {
                toggle()
                config.showKeyBorders = isChecked
            }
        }
    }

    private fun setupKeyboardLanguage() {
        binding.settingsKeyboardLanguage.apply {
            text = getKeyboardLanguageText(config.keyboardLanguage)
            binding.settingsKeyboardLanguageHolder.setOnClickListener {
                val items = getKeyboardLanguages()
                RadioGroupDialog(this@SettingsActivity, items, config.keyboardLanguage) {
                    config.keyboardLanguage = it as Int
                    text = getKeyboardLanguageText(config.keyboardLanguage)
                }
            }
        }
    }

    private fun setupKeyboardHeightMultiplier() {
        binding.settingsKeyboardHeightMultiplier.apply {
            text = getKeyboardHeightMultiplierText(config.keyboardHeightMultiplier)
            binding.settingsKeyboardHeightMultiplierHolder.setOnClickListener {
                val items = arrayListOf(
                    RadioItem(KEYBOARD_HEIGHT_MULTIPLIER_SMALL, getKeyboardHeightMultiplierText(KEYBOARD_HEIGHT_MULTIPLIER_SMALL)),
                    RadioItem(KEYBOARD_HEIGHT_MULTIPLIER_MEDIUM, getKeyboardHeightMultiplierText(KEYBOARD_HEIGHT_MULTIPLIER_MEDIUM)),
                    RadioItem(KEYBOARD_HEIGHT_MULTIPLIER_LARGE, getKeyboardHeightMultiplierText(KEYBOARD_HEIGHT_MULTIPLIER_LARGE)),
                )

                RadioGroupDialog(this@SettingsActivity, items, config.keyboardHeightMultiplier) {
                    config.keyboardHeightMultiplier = it as Int
                    text = getKeyboardHeightMultiplierText(config.keyboardHeightMultiplier)
                }
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
        binding.settingsShowClipboardContent.apply {
            isChecked = config.showClipboardContent
            binding.settingsShowKeyBordersHolder.setOnClickListener {
                toggle()
                config.showClipboardContent = isChecked
            }
        }
    }
}
