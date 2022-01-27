package com.simplemobiletools.keyboard.extensions

import android.content.Context
import com.simplemobiletools.keyboard.databases.ClipsDatabase
import com.simplemobiletools.keyboard.helpers.Config
import com.simplemobiletools.keyboard.interfaces.ClipsDao

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.clipsDB: ClipsDao get() = ClipsDatabase.getInstance(applicationContext).ClipsDao()
