package com.simplemobiletools.keyboard.helpers

import android.content.Context
import org.json.JSONObject
import java.io.InputStream

private var cachedEmojiData: MutableList<String>? = null
val cachedVNTelexData: HashMap<String, String> = HashMap()

/**
 * Reads the emoji list at the given [path] and returns an parsed [MutableList]. If the
 * given file path does not exist, an empty [MutableList] is returned.
 *
 * @param context The initiating view's context.
 * @param path The path to the asset file.
 */
fun parseRawEmojiSpecsFile(context: Context, path: String): MutableList<String> {
    if (cachedEmojiData != null) {
        return cachedEmojiData!!
    }

    val emojis = mutableListOf<String>()
    var emojiEditorList: MutableList<String>? = null

    fun commitEmojiEditorList() {
        emojiEditorList?.let {
            // add only the base emoji for now, ignore the variations
            emojis.add(it.first())
        }
        emojiEditorList = null
    }

    context.assets.open(path).bufferedReader().useLines { lines ->
        for (line in lines) {
            if (line.startsWith("#")) {
                // Comment line
            } else if (line.startsWith("[")) {
                commitEmojiEditorList()
            } else if (line.trim().isEmpty()) {
                // Empty line
                continue
            } else {
                if (!line.startsWith("\t")) {
                    commitEmojiEditorList()
                }

                // Assume it is a data line
                val data = line.split(";")
                if (data.size == 3) {
                    val emoji = data[0].trim()
                    if (emojiEditorList != null) {
                        emojiEditorList!!.add(emoji)
                    } else {
                        emojiEditorList = mutableListOf(emoji)
                    }
                }
            }
        }
        commitEmojiEditorList()
    }

    cachedEmojiData = emojis
    return emojis
}


fun parseRawJsonSpecsFile(context: Context, path: String): HashMap<String, String> {
    if (cachedVNTelexData.isNotEmpty()) {
        return cachedVNTelexData
    }

    try {
        val inputStream: InputStream = context.assets.open(path)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonData = JSONObject(jsonString)
        val rulesObj = jsonData.getJSONObject("rules")
        val ruleKeys = rulesObj.keys()
        while (ruleKeys.hasNext()) {
            val key = ruleKeys.next()
            val value = rulesObj.getString(key)
            cachedVNTelexData[key] = value
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
        return HashMap()
    }
    return cachedVNTelexData
}
