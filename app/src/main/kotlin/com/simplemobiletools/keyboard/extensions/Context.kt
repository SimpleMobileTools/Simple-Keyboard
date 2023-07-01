package com.simplemobiletools.keyboard.extensions

import android.app.KeyguardManager
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.IBinder
import android.os.UserManager
import android.util.TypedValue
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.isNougatPlus
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.views.MyTextView
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.databases.ClipsDatabase
import com.simplemobiletools.keyboard.helpers.*
import com.simplemobiletools.keyboard.interfaces.ClipsDao

val Context.config: Config get() = Config.newInstance(applicationContext.safeStorageContext)

val Context.safeStorageContext: Context
    get() = if (isNougatPlus() && isDeviceInDirectBootMode) {
        createDeviceProtectedStorageContext()
    } else {
        this
    }

val Context.isDeviceInDirectBootMode: Boolean
    get() {
        val userManager = getSystemService(Context.USER_SERVICE) as UserManager
        return isNougatPlus() && !userManager.isUserUnlocked
    }

val Context.isDeviceLocked: Boolean
    get() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isDeviceLocked || keyguardManager.isKeyguardLocked || isDeviceInDirectBootMode
    }

val Context.clipsDB: ClipsDao
    get() = ClipsDatabase.getInstance(applicationContext.safeStorageContext).ClipsDao()

fun Context.toPixel(dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
        resources.displayMetrics
    ).toInt()
}

fun Context.getCurrentClip(): String? {
    val clipboardManager = (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
    return clipboardManager.primaryClip?.getItemAt(0)?.text?.trim()?.toString()
}

fun Context.getStrokeColor(): Int {
    return if (config.isUsingSystemTheme) {
        if (isUsingSystemDarkTheme()) {
            resources.getColor(R.color.md_grey_800, theme)
        } else {
            resources.getColor(R.color.md_grey_400, theme)
        }
    } else {
        val lighterColor = safeStorageContext.getProperBackgroundColor().lightenColor()
        if (lighterColor == Color.WHITE || lighterColor == Color.BLACK) {
            resources.getColor(R.color.divider_grey, theme)
        } else {
            lighterColor
        }
    }
}

fun Context.getKeyboardDialogBuilder() = if (safeStorageContext.baseConfig.isUsingSystemTheme) {
    MaterialAlertDialogBuilder(this, R.style.MyKeyboard_Alert)
} else {
    AlertDialog.Builder(this, R.style.MyKeyboard_Alert)
}

fun Context.setupKeyboardDialogStuff(
    windowToken: IBinder,
    view: View,
    dialog: AlertDialog.Builder,
    titleId: Int = 0,
    titleText: String = "",
    cancelOnTouchOutside: Boolean = true,
    callback: ((alertDialog: AlertDialog) -> Unit)? = null
) {
    val textColor = getProperTextColor()
    val backgroundColor = getProperBackgroundColor()
    val primaryColor = getProperPrimaryColor()
    if (view is ViewGroup) {
        updateTextColors(view)
    } else if (view is MyTextView) {
        view.setColors(textColor, primaryColor, backgroundColor)
    }

    if (dialog is MaterialAlertDialogBuilder) {
        dialog.create().apply {
            if (titleId != 0) {
                setTitle(titleId)
            } else if (titleText.isNotEmpty()) {
                setTitle(titleText)
            }

            val lp = window?.attributes
            lp?.token = windowToken
            lp?.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
            window?.attributes = lp
            window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

            setView(view)
            setCancelable(cancelOnTouchOutside)
            show()

            val bgDrawable = when {
                isBlackAndWhiteTheme() -> ResourcesCompat.getDrawable(resources, R.drawable.black_dialog_background, theme)
                baseConfig.isUsingSystemTheme -> ResourcesCompat.getDrawable(resources, R.drawable.dialog_you_background, theme)
                else -> resources.getColoredDrawableWithColor(R.drawable.dialog_bg, baseConfig.backgroundColor)
            }

            window?.setBackgroundDrawable(bgDrawable)
            callback?.invoke(this)
        }
    } else {
        var title: TextView? = null
        if (titleId != 0 || titleText.isNotEmpty()) {
            title = LayoutInflater.from(this).inflate(R.layout.dialog_title, null) as TextView
            title.apply {
                if (titleText.isNotEmpty()) {
                    text = titleText
                } else {
                    setText(titleId)
                }
                setTextColor(textColor)
            }
        }

        // if we use the same primary and background color, use the text color for dialog confirmation buttons
        val dialogButtonColor = if (primaryColor == baseConfig.backgroundColor) {
            textColor
        } else {
            primaryColor
        }

        dialog.create().apply {
            setView(view)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCustomTitle(title)

            val lp = window?.attributes
            lp?.token = windowToken
            lp?.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
            window?.attributes = lp
            window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

            setCanceledOnTouchOutside(cancelOnTouchOutside)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(dialogButtonColor)
            getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(dialogButtonColor)
            getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(dialogButtonColor)

            val bgDrawable = when {
                isBlackAndWhiteTheme() -> ResourcesCompat.getDrawable(resources, R.drawable.black_dialog_background, theme)
                baseConfig.isUsingSystemTheme -> ResourcesCompat.getDrawable(resources, R.drawable.dialog_you_background, theme)
                else -> resources.getColoredDrawableWithColor(R.drawable.dialog_bg, baseConfig.backgroundColor)
            }

            window?.setBackgroundDrawable(bgDrawable)
            callback?.invoke(this)
        }
    }
}

fun Context.getKeyboardLanguages(): ArrayList<RadioItem> {
    return arrayListOf(
        RadioItem(LANGUAGE_BENGALI, getKeyboardLanguageText(LANGUAGE_BENGALI)),
        RadioItem(LANGUAGE_BULGARIAN, getKeyboardLanguageText(LANGUAGE_BULGARIAN)),
        RadioItem(LANGUAGE_DANISH, getKeyboardLanguageText(LANGUAGE_DANISH)),
        RadioItem(LANGUAGE_ENGLISH_QWERTY, getKeyboardLanguageText(LANGUAGE_ENGLISH_QWERTY)),
        RadioItem(LANGUAGE_ENGLISH_QWERTZ, getKeyboardLanguageText(LANGUAGE_ENGLISH_QWERTZ)),
        RadioItem(LANGUAGE_ENGLISH_DVORAK, getKeyboardLanguageText(LANGUAGE_ENGLISH_DVORAK)),
        RadioItem(LANGUAGE_FRENCH_AZERTY, getKeyboardLanguageText(LANGUAGE_FRENCH_AZERTY)),
        RadioItem(LANGUAGE_FRENCH_BEPO, getKeyboardLanguageText(LANGUAGE_FRENCH_BEPO)),
        RadioItem(LANGUAGE_GERMAN, getKeyboardLanguageText(LANGUAGE_GERMAN)),
        RadioItem(LANGUAGE_GREEK, getKeyboardLanguageText(LANGUAGE_GREEK)),
        RadioItem(LANGUAGE_LITHUANIAN, getKeyboardLanguageText(LANGUAGE_LITHUANIAN)),
        RadioItem(LANGUAGE_NORWEGIAN, getKeyboardLanguageText(LANGUAGE_NORWEGIAN)),
        RadioItem(LANGUAGE_ROMANIAN, getKeyboardLanguageText(LANGUAGE_ROMANIAN)),
        RadioItem(LANGUAGE_RUSSIAN, getKeyboardLanguageText(LANGUAGE_RUSSIAN)),
        RadioItem(LANGUAGE_SLOVENIAN, getKeyboardLanguageText(LANGUAGE_SLOVENIAN)),
        RadioItem(LANGUAGE_SPANISH, getKeyboardLanguageText(LANGUAGE_SPANISH)),
        RadioItem(LANGUAGE_SWEDISH, getKeyboardLanguageText(LANGUAGE_SWEDISH)),
        RadioItem(LANGUAGE_TURKISH_Q, getKeyboardLanguageText(LANGUAGE_TURKISH_Q)),
    )
}

fun Context.getKeyboardLanguageText(language: Int): String {
    return when (language) {
        LANGUAGE_BENGALI -> getString(R.string.translation_bengali)
        LANGUAGE_BULGARIAN -> getString(R.string.translation_bulgarian)
        LANGUAGE_DANISH -> getString(R.string.translation_danish)
        LANGUAGE_ENGLISH_DVORAK -> "${getString(R.string.translation_english)} (DVORAK)"
        LANGUAGE_ENGLISH_QWERTZ -> "${getString(R.string.translation_english)} (QWERTZ)"
        LANGUAGE_FRENCH_AZERTY -> "${getString(R.string.translation_french)} (AZERTY)"
        LANGUAGE_FRENCH_BEPO -> "${getString(R.string.translation_french)} (BEPO)"
        LANGUAGE_GERMAN -> getString(R.string.translation_german)
        LANGUAGE_GREEK -> getString(R.string.translation_greek)
        LANGUAGE_LITHUANIAN -> getString(R.string.translation_lithuanian)
        LANGUAGE_NORWEGIAN -> getString(R.string.translation_norwegian)
        LANGUAGE_ROMANIAN -> getString(R.string.translation_romanian)
        LANGUAGE_RUSSIAN -> getString(R.string.translation_russian)
        LANGUAGE_SLOVENIAN -> getString(R.string.translation_slovenian)
        LANGUAGE_SPANISH -> getString(R.string.translation_spanish)
        LANGUAGE_SWEDISH -> getString(R.string.translation_swedish)
        LANGUAGE_TURKISH_Q -> "${getString(R.string.translation_turkish)} (Q)"
        else -> "${getString(R.string.translation_english)} (QWERTY)"
    }
}


