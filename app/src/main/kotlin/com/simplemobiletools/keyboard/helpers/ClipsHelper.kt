package com.simplemobiletools.keyboard.helpers

import android.content.Context
import com.simplemobiletools.keyboard.extensions.clipsDB
import com.simplemobiletools.keyboard.models.Clip

class ClipsHelper(val context: Context) {

    // make sure clips have unique values
    fun insertClip(clip: Clip): Long {
        clip.value = clip.value.trim()
        return if (context.clipsDB.getClipWithValue(clip.value) == null) {
            context.clipsDB.insertOrUpdate(clip)
        } else {
            -1
        }
    }
}
