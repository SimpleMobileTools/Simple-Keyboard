package com.simplemobiletools.keyboard.helpers

const val SHIFT_OFF = 0
const val SHIFT_ON_ONE_CHAR = 1
const val SHIFT_ON_PERMANENT = 2

// limit the count of alternative characters that show up at long pressing a key
const val MAX_KEYS_PER_MINI_ROW = 8

// shared prefs
const val VIBRATE_ON_KEYPRESS = "vibrate_on_keypress"
const val SHOW_POPUP_ON_KEYPRESS = "show_popup_on_keypress"
const val LAST_EXPORTED_CLIPS_FOLDER = "last_exported_clips_folder"

// differentiate current and pinned clips at the keyboards' Clipboard section
const val ITEM_SECTION_LABEL = 0
const val ITEM_CLIP = 1
