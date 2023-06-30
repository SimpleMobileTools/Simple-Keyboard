package com.simplemobiletools.keyboard.extensions

import android.content.Context
import android.util.TypedValue

fun Context.toPixel(dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
        resources.displayMetrics
    ).toInt()
}
