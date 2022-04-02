package com.simplemobiletools.keyboard.extensions

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.extensions.isUsingSystemDarkTheme
import com.simplemobiletools.commons.extensions.lightenColor
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.databases.ClipsDatabase
import com.simplemobiletools.keyboard.helpers.Config
import com.simplemobiletools.keyboard.interfaces.ClipsDao

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.clipsDB: ClipsDao get() = ClipsDatabase.getInstance(applicationContext).ClipsDao()

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
        val lighterColor = getProperBackgroundColor().lightenColor()
        if (lighterColor == Color.WHITE || lighterColor == Color.BLACK) {
            resources.getColor(R.color.divider_grey, theme)
        } else {
            lighterColor
        }
    }
}
