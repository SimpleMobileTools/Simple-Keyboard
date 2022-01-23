package com.simplemobiletools.keyboard.helpers

import android.content.Context
import com.simplemobiletools.commons.helpers.BaseConfig

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var showClipboard: Boolean
        get() = prefs.getBoolean(SHOW_CLIPBOARD, true)
        set(showClipboard) = prefs.edit().putBoolean(SHOW_CLIPBOARD, showClipboard).apply()
}
