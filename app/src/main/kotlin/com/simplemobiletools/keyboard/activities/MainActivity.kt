package com.simplemobiletools.keyboard.activities

import android.content.Intent
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import com.simplemobiletools.commons.dialogs.ConfirmationAdvancedDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.LICENSE_GSON
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.keyboard.BuildConfig
import com.simplemobiletools.keyboard.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched(BuildConfig.APPLICATION_ID)
        change_keyboard_holder.setOnClickListener {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isKeyboardEnabled()) {
            ConfirmationAdvancedDialog(this, messageId = R.string.redirection_note, positive = R.string.ok, negative = 0) { success ->
                if (success) {
                    Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(this)
                    }
                } else {
                    finish()
                }
            }
        }

        updateTextColors(main_holder)
        updateChangeKeyboardColor()
        main_holder.setBackgroundColor(getProperBackgroundColor())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses = LICENSE_GSON

        val faqItems = arrayListOf(
            FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons),
            FAQItem(R.string.faq_6_title_commons, R.string.faq_6_text_commons),
        )

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
    }

    private fun updateChangeKeyboardColor() {
        val applyBackground = resources.getDrawable(R.drawable.button_background_rounded, theme) as RippleDrawable
        (applyBackground as LayerDrawable).findDrawableByLayerId(R.id.button_background_holder).applyColorFilter(getProperPrimaryColor())
        change_keyboard.background = applyBackground
        change_keyboard.setTextColor(getProperPrimaryColor().getContrastColor())
    }

    private fun isKeyboardEnabled(): Boolean {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledKeyboards = inputMethodManager.enabledInputMethodList
        return enabledKeyboards.any {
            it.settingsActivity == SettingsActivity::class.java.canonicalName
        }
    }
}
