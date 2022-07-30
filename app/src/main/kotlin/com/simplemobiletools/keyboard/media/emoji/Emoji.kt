package com.simplemobiletools.keyboard.media.emoji


data class Emoji(val value: String, val name: String, val keywords: List<String>) {
    override fun toString(): String {
        return "Emoji { value=$value, name=$name, keywords=$keywords }"
    }
}
