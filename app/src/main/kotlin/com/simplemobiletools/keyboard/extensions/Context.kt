package com.simplemobiletools.keyboard.extensions

import android.content.Context
import com.simplemobiletools.keyboard.helpers.Config

val Context.config: Config get() = Config.newInstance(applicationContext)
